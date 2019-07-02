package icblive.chatserver;
import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonSyntaxException;

import icblive.chatserver.api.APICallbackHandler;
import icblive.chatserver.api.APIManager;
import icblive.chatserver.api.MessageUtils;
import icblive.chatserver.api.SessionActionHandler;
import icblive.chatserver.api.RoomMessageQueue;
import icblive.chatserver.data.LiveCache;
import icblive.chatserver.model.DisableMsgUserData;
import icblive.chatserver.model.KickOffUserData;
import icblive.chatserver.model.RoomData;
import icblive.chatserver.model.UserData;
import icblive.chatserver.model.json.AudienceListMessage;
import icblive.chatserver.model.json.BaseResponseMessage;
import icblive.chatserver.model.json.ChatRequestMessage;
import icblive.chatserver.model.json.ChatResponseMessage;
import icblive.chatserver.model.json.DisableChatResponseMessage;
import icblive.chatserver.model.json.BaseMessage;
import icblive.chatserver.model.json.ErrorResponseMessage;
import icblive.chatserver.model.json.UserInfo;
import icblive.chatserver.utils.CBUrlParser;
import icblive.chatserver.utils.ChangbaMetrics;
import icblive.chatserver.utils.ChatCount;
import icblive.chatserver.utils.ChatHelper;
import icblive.chatserver.utils.DesHelper;
import icblive.chatserver.utils.LanguageUtils;
import icblive.chatserver.utils.LoggerUtils;
import icblive.chatserver.utils.RedisHelper;
import icblive.chatserver.utils.TokenHelper;
import io.dropwizard.metrics.Meter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
/**
 * 
 * @author yangfei
 * @date   2016/10/25
 */
public class WebSocketMessageHandler extends SimpleChannelInboundHandler<Object> {

	
	private static final String WEBSOCKET_PATH = "/websocket"; 
	
	private WebSocketServerHandshaker handshaker;
	private static Logger logger = LogManager.getLogger("default");
	
	private UserData curUser;
	
	private String clientLogInfo = "";
	
	private static Timer checkControlMicTimer = new Timer(true);
	private static Timer updateAudienceTimer = new Timer(true);
	private static Timer clearChatCountTimer = new Timer(true);
	private static AtomicInteger audiencemsgcounter = new AtomicInteger(1);
	private static Timer checkSessionSendMsgTimer = new Timer(true);
	/* ------------------------------------------------------------------------------------ */
	
	static {
		// 初始化http连接池
		APIManager.getDefaultHttpClient();
	}
	static {
		checkControlMicTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				Iterator<Entry<String, RoomData>> e = LiveCache.getAllRoomIterator();
				while (e.hasNext()) {
					Entry<String, RoomData> entry = e.next();
					RoomData roomdata = entry.getValue();
					// 清理空房间：
					if (LiveCache.validateRoom(roomdata) == 1) {
						logger.info("validateroom " + roomdata);
					}
				}
			}
		}, 1000, 5000);
	}
	static {
		updateAudienceTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				Iterator<Entry<String, RoomData>> e = LiveCache.getAllRoomIterator();
				int counter = audiencemsgcounter.incrementAndGet()%10;
				while (e.hasNext()) {
					Entry<String, RoomData> entry = e.next();
					RoomData roomdata = entry.getValue();
					
					//更新观众列表
					try{
						//每秒一次的消息发送 可以保证10s之内每个房间都被轮一次
						if (Integer.parseInt(roomdata.sessionid)%10 != counter) {
							continue;
						}
						AudienceListMessage audienceListMsg = RedisHelper.getAudienceList(roomdata.userid, roomdata.sessionid,0,10);
						logger.debug("update audience list " + roomdata.sessionid+ " num:"+audienceListMsg.audienceamount);
						logger.debug("getAudienceList "+JSON.toJSONString(audienceListMsg));

						MessageUtils.sendToRoom(roomdata, JSON.toJSONString(audienceListMsg), audienceListMsg.type);
					}catch(Exception ep) {
						logger.error("update audience list failed" + roomdata.sessionid);
						LoggerUtils.printExceprionLogger(ep);
					}
				}
			}
		}, 1000, 1000);
	}
	static {
		// 当房间的数量变多的时候 这个timer是完全可以扩展为多线程的
		// sendtoroom这个方法的核心原则是100毫秒给一个房间发送一次消息
		// 即100毫秒分配出来一个线程用于指定的房间发消息
		// 有两个地方驱动消息的发送 一个是这个timer被动触发 一个是addmessage主动触发
		checkSessionSendMsgTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				Iterator<Entry<String, RoomData>> e = LiveCache.getAllRoomIterator();
				while (e.hasNext()) {
					Entry<String, RoomData> entry = e.next();
					RoomData roomdata = entry.getValue();
					RoomMessageQueue.getRoomMessageQueue(roomdata).addMessage("", "checksendmessage");
				}
				//RoomMessageQueue.getSendMsgThreadPoolStatus();
			}
		}, 1000, 300);
	}
	static {
		clearChatCountTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				//每天执行十次，清理下无效的聊天记录
				ChatCount.clearUnusedCount();
			}
		}, 2000, 8640000);
	}
	/* ------------------------------------------------------------------------------------ */

	// 1、管道接收请求
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(this.curUser!=null){
			clientLogInfo = "addr:"+ctx.channel().remoteAddress().toString().substring(1) +" userinfo:"+this.curUser.toString();
		}else{
			clientLogInfo = "addr:"+ctx.channel().remoteAddress().toString().substring(1) ;
		}
		// 按请求协议分类处理请求
		if (msg instanceof FullHttpRequest) {
			// http请求，如spotlite后端服务
			handleHttpRequest(ctx, (FullHttpRequest) msg);
		} else if (msg instanceof WebSocketFrame) {
			// socket请求，如app和spotlite后端服务都会有
			handleWebSocketFrame(ctx, (WebSocketFrame) msg);
		}
	}
	
	// 3、1、客户端ws消息处理
	private void onMessage(ChannelHandlerContext ctx, String requestMessage) throws Exception {
		if (this.curUser == null || Strings.isNullOrEmpty(this.curUser.getUserid())) {
			logger.error( clientLogInfo+" onMessage: curuser is null when jsonMessage="+requestMessage );
			return;
		}
		BaseMessage chatMsg ;
		try{
			chatMsg = JSON.parseObject(requestMessage, BaseMessage.class);
		}catch(JSONException e){
			logger.error( clientLogInfo+" onMessage: json exception " + e);
			return;
		}
		String type = chatMsg.type;
		logger.info( clientLogInfo+" onMessage:"+requestMessage);
		//统计
		//ChangbaMetrics.incrMeter(type);
		
		switch(type){
		case "publicchat":
		case "privatechat":
			// 评论
			onChatMessage(type, requestMessage);
			break;
		case "joinsession":
			// 进入房间
			SessionActionHandler.joinSession(requestMessage, curUser);
			break;
		case "exitsession":
			SessionActionHandler.exitSession(requestMessage, curUser);
			break;
		case "getaudiencelist":
			SessionActionHandler.getAudienceList(requestMessage, curUser);
			break;
		case "share":
			SessionActionHandler.share(requestMessage, curUser);
			break;
		case "disablemsg":
			SessionActionHandler.disableMsg(requestMessage, curUser);
			break;
		case "kickoff":
			SessionActionHandler.kickOff(requestMessage, curUser);
			break;
		case "pause":
		case "resume":
			SessionActionHandler.pauseOrResume(requestMessage, curUser);
			break;
		default:
			//  其他类型的消息，转发给PHP
			//APIManager.wsmessagedispach(type, jsonMessage, curUser, new APICallbackHandler(curUser));
			break;
		}
		
	}
	 
	// 3（1）...、处理直播评论消息
	private void onChatMessage(String type, String requestMessage) {
		ChatRequestMessage msg = JSON.parseObject(requestMessage, ChatRequestMessage.class);
		long banchattime = LiveCache.disableMsgUserTimeStamp(curUser.getUserid(), curUser.ownerid);
		if (banchattime > 0) {
			DisableChatResponseMessage disablemsg = new DisableChatResponseMessage(curUser.getUserid(), banchattime);
			MessageUtils.sendToUser(curUser, JSON.toJSONString(disablemsg));
			return;
		}
		// 是否禁言
		if(ChatHelper.isUserGaged(curUser.getUserid())) {
			String errmsg = LanguageUtils.getInstance().getContents(this.curUser.lang, "ERROR_GAG");
			ErrorResponseMessage err = ErrorResponseMessage.makeErrorMessage(errmsg);
			MessageUtils.sendToUser(curUser, JSON.toJSONString(err));
			return;
		}
		if (ChatHelper.hasForbiddenWord(msg.msgbody)){
			String errmsg = LanguageUtils.getInstance().getContents(this.curUser.lang, "ERROR_DISABLE_WORDS");
			ErrorResponseMessage err = ErrorResponseMessage.makeErrorMessage(errmsg);
			MessageUtils.sendToUser(curUser, JSON.toJSONString(err));
			return;
		}
		if (msg.msgbody.length() > ChatHelper.PUBLICCHAT_CONTENT_LENGTH) {
			String errmsg = LanguageUtils.getInstance().getContents(this.curUser.lang, "ERROR_WORDS_LENGTH");
			ErrorResponseMessage err = ErrorResponseMessage.makeErrorMessage(errmsg);
			MessageUtils.sendToUser(curUser, JSON.toJSONString(err));
			return;
		}
		
		if (msg.msgbody.equals("@kickoff")) {
			
		}

		// 如果距上次用户发言不到1S,发提醒
		long lastTimeStamp = LiveCache.getLastSendMsgTimeStamp(curUser.getUserid(), curUser.ownerid);
		long currTimStamp = new Date().getTime();
		logger.debug("curr time : " + currTimStamp + " room id:" + curUser.ownerid + "user id " + curUser.getUserid()
				+ "last time:" + lastTimeStamp);
		if (lastTimeStamp > 0) {
			if (currTimStamp - lastTimeStamp <= 1000) {
				String errmsg = LanguageUtils.getInstance().getContents(this.curUser.lang, "ERROR_WORDS_FAST");
				ErrorResponseMessage err = ErrorResponseMessage.makeErrorMessage(errmsg);
				MessageUtils.sendToUser(curUser, JSON.toJSONString(err));
				return;
			}
		}

		//ChatCount.increKey(curUser.sessionid);
		if (Objects.equal("publicchat", type)) {
			ChatResponseMessage responsemsg = new ChatResponseMessage(type, curUser.getUserid(), curUser.getNickName(),curUser.userinfo.level,"",msg.msgbody);
			responsemsg.setUserMedalInfo(curUser.getUserMedalInfo());  // 加入medal信息
			RoomData room = LiveCache.getRoomBySessionid(curUser.sessionid);
			logger.debug("onChatMessage: room"+room.toString()+" msg:"+responsemsg.msgbody);
			// 发送并返回发送结果
			if (!MessageUtils.sendToRoom(room, JSON.toJSONString(responsemsg), "publicchat")) {
				// 公聊太多了 直接echo回去
				MessageUtils.sendToUser(this.curUser, JSON.toJSONString(responsemsg));
			}
			// TODO
			//这里要考虑一下公聊频率 觉得是否丢包，但是应该以房间来考虑，不应该以总频率来考虑 后续在想吧
			//ChangbaMetrics.incrMeter("publicchat_msg");
			LiveCache.addLastSendMsgTimeStamp(curUser.getUserid(), curUser.ownerid);
		}
	}


	// 2...、
	private boolean onOpen(ChannelHandlerContext ctx, FullHttpRequest req) {
		List<NameValuePair> realParams = null;
		boolean debugMode = false;
		logger.info(req.getUri());
		try {
			List<NameValuePair> params = URLEncodedUtils.parse(new URI(req.getUri()), "UTF-8");
			for (NameValuePair param : params) {
				  if (param.getName().equals("kulvdebug") && param.getValue().equals("20160809")){
					  realParams = params; //Hack mode
					  debugMode = true;
					  break;
				  }
				  if (param.getName().equals("param") ){
					  String secret = param.getValue();
					  String decodeString = DesHelper.decode("newtv.beijing!", secret);
					  logger.info(decodeString);
					  realParams = URLEncodedUtils.parse(new URI("/?"+decodeString), "UTF-8");
					  break;
				  }
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		if (realParams==null || realParams.isEmpty()){
			return false;
		}
		//UserInfo userinfo = new UserInfo();
		UserData userdata = new UserData();
		String token = "";
		String userid = "";
		for (NameValuePair param : realParams) {
			switch(param.getName()){
				case "uid":
					userid = param.getValue();
					break;
				case "token":
					token = param.getValue();
					break;
				case "owner":
					userdata.ownerid = param.getValue();
					break;
				case "version":
					userdata.version = param.getValue();
					break;
				case "session":
					userdata.sessionid = param.getValue();
					break;
				case "clienttype":
					userdata.clienttype = param.getValue();
					break;
				case "lang":
					int lang = Integer.parseInt(param.getValue());
					userdata.lang = LanguageUtils.getInstance().isSupportLangs(lang);
					break;
			}
		}
		if (Strings.isNullOrEmpty(userid) || Strings.isNullOrEmpty(userdata.ownerid) || Strings.isNullOrEmpty(userdata.sessionid) || Strings.isNullOrEmpty(userdata.clienttype)){
			logger.error("onOpen userid:"+userid+" ownerid:"+userdata.ownerid+" sessionid:"+userdata.sessionid);
			return false;
		}
		if (debugMode
				|| TokenHelper.checkToken(userid, token)
				) {
			if(LiveCache.isKickOffUser(userid, userdata.ownerid)){
				return false;
			}
			RoomData roomdata = LiveCache.getRoomBySessionid(userdata.sessionid);
			if(roomdata == null){
				roomdata = LiveCache.getRoomByOwnerid(userdata.ownerid);
			}
			if( roomdata == null || !roomdata.isLive() || !roomdata.userid.equals(userdata.ownerid) ){
				logger.error("onOpen sessionid wrong: "+userid + " ownerid:"+userdata.ownerid);
				return false;
			}
			UserInfo userinfo = RedisHelper.getUserInfo(userid);  // 获取当前用户的详情信息，昵称、性别、国家等信息
			if( userinfo == null){
				logger.error("token验证成功，获取用户信息失败 userid: "+userid);
				return false;
			}
			userdata.userinfo = userinfo;
			userdata.wshander  = this;
			userdata.ctx = ctx;
			this.curUser = userdata;
			return true;
		}
		return false;
	}

	// 2、处理http请求
	private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
		// Handle a bad request.
		if (!req.getDecoderResult().isSuccess()) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
			return;
		}
		// Allow only GET methods.
		if (req.getMethod() != GET) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
			return;
		}
		// spotlite后端请求
		if ( req.getUri().startsWith("/admin/") ) {
			adminHttpMode(ctx, req);
			return;
		}
		if (req.getUri().startsWith("/available")){
			ByteBuf buf = copiedBuffer("ok", CharsetUtil.UTF_8);;
			FullHttpResponse response = new DefaultFullHttpResponse( HTTP_1_1, HttpResponseStatus.OK,buf);
			response.headers().set(HttpHeaders.CONTENT_LENGTH, buf.readableBytes());
			sendHttpResponse(ctx, req, response );
			return;
		}
		// Handshake
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, false);
		handshaker = wsFactory.newHandshaker(req);
		if (handshaker == null) {
			WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
		} else {
			ctx.channel().closeFuture().addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					//onClose(_ctx);
					//clientCleanUp(_ctx);
				}
			});
			if (onOpen(ctx, req)) {
				handshaker.handshake(ctx.channel(), req);
			} else {
				sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
				logger.error(clientLogInfo + " OPEN: open failed, req:" + req.toString().replace("\n", " # ") );
				clientCleanUp(ctx);
			}
		}
	}
	private static String getWebSocketLocation(FullHttpRequest req) {
		return "ws://" + req.headers().get(HOST) + WEBSOCKET_PATH;
	}
	private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
		// Generate an error page if response getStatus code is not OK (200).
		if (res.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
			setContentLength(res, res.content().readableBytes());
		}

		// Send the response and close the connection if necessary.
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!isKeepAlive(req) || res.getStatus().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	// 3、处理ws请求
	private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
		
		// Check for closing frame
		if (frame instanceof CloseWebSocketFrame) {
			logger.info(ctx);
			handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			clientCleanUp(ctx);
			return;
		}
		if (frame instanceof PingWebSocketFrame) {
			//更新ping时间
           if (this.curUser!=null){
        	   		RoomData room = this.curUser.getRoom();
        	   		logger.debug("receive ping,uid: "+room.userid+" roomsession:"+room.sessionid);
        	   		if (room!=null && Objects.equal(this.curUser.getUserid(), room.userid)){
       					if(room.isPause()){
       						room.pauseOrResume("resume","1000");
       						MessageUtils.sendToRoom(room, "{'type':'resume'}","resume");
       					}
        	   			room.timestamp = new Date().getTime();
        	   			logger.debug("update ping timestamp:"+room.timestamp +" uid: "+room.userid+" roomsession:"+room.sessionid);
        	   			ctx.channel().writeAndFlush(new PongWebSocketFrame()); 
        	   		}
           }
           return;
		}
		if (frame instanceof PongWebSocketFrame) {
			// logger.info("Info client returned pong ");
		}
		if (!(frame instanceof TextWebSocketFrame)) {
			return;
		}
		// Send the uppercase string back.
		String request = ((TextWebSocketFrame) frame).text();
		try {
			onMessage(ctx, request);
		} catch (JsonSyntaxException e) {
			logger.error(clientLogInfo+" ERROR: json error.", e );
		} catch (Exception e) {
			logger.error(clientLogInfo+" ERROR: onmessage.", e );
        }
	}
	
	private void adminHttpMode(ChannelHandlerContext ctx, FullHttpRequest req) {
		//http的接口，用来获取人在哪个房间，以及所有在线的人等。
		String uri = req.getUri() ;
		ByteBuf buf = null;
		String file = CBUrlParser.UrlPage(uri);
		logger.info(clientLogInfo + "adminHttpMode uri:"+uri + " file:"+ file);
		Map<String, String> mapRequest = CBUrlParser.URLRequest(uri);
		String key = mapRequest.get("adminkey");
		if( !key.equals("MattDamon") ){
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
			return;
		}
		FullHttpResponse response = null ;
		if (file.equals( "/admin/isuserinroom")) {
			// 校验主播是否还在线
			//http://59.151.33.36:86/admin/isuserinroom?roomid=222222&userid=28946468
			String userid = mapRequest.get("userid");
			String roomid = mapRequest.get("session");
			UserData userdata = LiveCache.getUserData(userid);
			String result = "0";
			if (userdata != null && userdata.wshander != null && Objects.equal(userdata.getRoomid(), roomid)) {
				result = "1";
			}
			buf = copiedBuffer(result , CharsetUtil.UTF_8);
			logger.debug(clientLogInfo + " adminHttpMode res:" + result);
		} else if (file.equals( "/admin/getalluid")) {
			//http://59.151.33.36:86/admin/getalluidrooms
			StringBuilder result = new StringBuilder();
			Iterator<UserData> userit = LiveCache.getAllUserIterator();
			while(userit.hasNext()) {
				UserData userdata = userit.next();
				if (userdata != null && userdata.wshander != null) {
					result.append(userdata.getUserid());
					result.append("@");
					result.append(userdata.getRoomid());
					result.append("\n");
				}
			}
			buf = copiedBuffer(result.toString()  , CharsetUtil.UTF_8);
			logger.debug(clientLogInfo + " adminHttpMode "+file+" res:" + result.toString());
		} else if (file.equals( "/admin/getchatcount")) {
			StringBuilder result = new StringBuilder();
			Iterator<Entry<String, RoomData>> e = LiveCache.getAllRoomIterator();
			while(e.hasNext()) {
				String sessionid = e.next().getValue().sessionid;
				result.append(sessionid);
				result.append(" ");
				result.append(ChatCount.getAndput(sessionid,0));
				result.append("\n");
			}
			buf = copiedBuffer(result.toString()  , CharsetUtil.UTF_8);
			logger.debug(clientLogInfo + " adminHttpMode "+file+" res:" + result.toString());
		} else if (file.equals( "/admin/getsessionbyuserid")) {
			//http://59.151.33.36:86/admin/getinroomidbyuserid?userid=28946468
			String userid = mapRequest.get("userid");
			UserData userdata = LiveCache.getUserData(userid);
			String result = "0";
			if (userdata != null && userdata.wshander != null) {
				result = userdata.getRoomid();
			}
			buf = copiedBuffer(result , CharsetUtil.UTF_8);
			logger.debug(clientLogInfo + " adminHttpMode res:" + result);
		} else if (file.equals( "/admin/getuidsbysession")) {
			String roomid = mapRequest.get("session");
			RoomData roomdata = LiveCache.getRoomBySessionid(roomid);
			StringBuilder result = new StringBuilder();
			if (roomdata != null) {
				Iterator<UserData> userit = roomdata.getUserIterator();
				while(userit.hasNext()) {
					result.append(userit.next().getUserid());
					result.append("\n");
				}
			}
			buf = copiedBuffer(result, CharsetUtil.UTF_8);
		} else if (file.equals("/admin/getroomids")) {
			Iterator<Entry<String, RoomData>> e = LiveCache.getAllRoomIterator();
			StringBuilder sb = new StringBuilder();
			while(e.hasNext()) {
				sb.append(e.next().getKey());
				sb.append("\n");
			}
			buf = copiedBuffer(sb.toString(), CharsetUtil.UTF_8);
		} else if (file.equals("/admin/serverstatus")) {
			Map<String,Object> statsMap = Maps.newHashMap();
			statsMap.put("roomcnt", (int) LiveCache.getRoomCount());
			statsMap.put("clientcnt", (int) ChangbaMetrics.userCount.getCount());
			// 这里统计比较重的，还有privatechat,提升麦序，提升管理员这种类型的消息就没必要统计了
			String[] requesttype = {"joinroom", "exitroom", "finishmic","publicchat"};
			int readps = 0;
			for(String type:requesttype) {
				readps += (int)ChangbaMetrics.getMeter(type).getOneMinuteRate();
			}
			statsMap.put("writeps", (int) ChangbaMetrics.getMeter("sent_msg").getOneMinuteRate());
			statsMap.put("readps", (int) readps);
			statsMap.put("updtime", new Date().getTime()) ;
			buf = copiedBuffer(JSON.toJSONString(statsMap), CharsetUtil.UTF_8);  
		} else if (file.equals("/admin/alldiablemsginfo")) {
			Iterator<Entry<String, DisableMsgUserData>> kuinfo = LiveCache.getDisableMsgUserIterator();
			StringBuilder result = new StringBuilder();
			while (kuinfo.hasNext()) {
				Entry<String, DisableMsgUserData> e = kuinfo.next();
				result.append(e.getKey());
				result.append("\t");
				result.append(e.getValue().getTimestamp());
				result.append("\n");
			}
			buf = copiedBuffer(result, CharsetUtil.UTF_8);  
		} else if (file.equals("/admin/isdisablemsguser")) {
			String userid = mapRequest.get("userid");
			String roomid = mapRequest.get("ownerid");
			String result = "" + LiveCache.disableMsgUserTimeStamp(userid, roomid);
			buf = copiedBuffer(result, CharsetUtil.UTF_8);
		} else if (file.equals("/admin/undisablemsg")) {
			String userid = mapRequest.get("userid");
			String roomid = mapRequest.get("ownerid");
			LiveCache.removeDisableMsgUser(userid, roomid);
			buf = copiedBuffer("success", CharsetUtil.UTF_8);
		} else if (file.equals("/admin/allkickoffinfo")) {
			Iterator<Entry<String, KickOffUserData>> kuinfo = LiveCache.getKickOffUserIterator();
			StringBuilder result = new StringBuilder();
			while (kuinfo.hasNext()) {
				Entry<String, KickOffUserData> e = kuinfo.next();
				result.append(e.getKey());
				result.append("\t");
				result.append(e.getValue().getTimestamp());
				result.append("\n");
			}
			buf = copiedBuffer(result, CharsetUtil.UTF_8);  
		} else if (file.equals("/admin/iskickoffuser")) {
			String userid = mapRequest.get("userid");
			String roomid = mapRequest.get("ownerid");
			String result = "" + LiveCache.kickOffUserTimeStamp(userid, roomid);
			buf = copiedBuffer(result, CharsetUtil.UTF_8);
		} else if (file.equals("/admin/unkickoffuser")) {
			String userid = mapRequest.get("userid");
			String roomid = mapRequest.get("ownerid");
			LiveCache.removeKickOffUser(userid, roomid);
			buf = copiedBuffer("success", CharsetUtil.UTF_8);
		} else if (file.equals("/admin/setwsflowlimit")) {
			int limit = Integer.parseInt(mapRequest.get("flowlimit"));
			RoomMessageQueue.WS_FLOW_LIMIT = limit;
			buf = copiedBuffer("success", CharsetUtil.UTF_8);
		} else if (file.equals("/admin/getroomcount")) {
			String roomid = mapRequest.get("session");
			int count =  LiveCache.getRoomBySessionid(roomid).count();
			buf = copiedBuffer("" + count, CharsetUtil.UTF_8);
		} else if (file.equals( "/admin/isuserinrooms")) {
			// 批量校验主播是否还在线
			//http://59.151.33.36:86/admin/isuserinroom?roomid=222222&userid=28946468
			String[] userids = mapRequest.get("userids").split(",");
			String[] roomids = mapRequest.get("sessions").split(",");
			String result = "";
			if(userids.length == roomids.length) {
				for (int i = 0; i < userids.length; i++) {
					String userid = userids[i];
					String roomid = roomids[i];
					UserData userdata = LiveCache.getUserData(userid);
					if (userdata != null && userdata.wshander != null && Objects.equal(userdata.getRoomid(), roomid)) {
						result += "1,";
					}else{
						result += "0,";
					}
				}
				if(result.length() >= 2) {
					result = result.substring(0, result.length() - 1);
				}
			}
			buf = copiedBuffer(result, CharsetUtil.UTF_8);
			logger.debug(clientLogInfo + " adminHttpMode res:" + result);
		}
		response = new DefaultFullHttpResponse( HTTP_1_1, HttpResponseStatus.OK, buf );
		response.headers().set(HttpHeaders.CONTENT_LENGTH, buf.readableBytes());
		sendHttpResponse(ctx, req, response );
	}
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		//连接断开后 就会调用这里
		logger.info(curUser+" @ "+ctx +" disconnected by channelInactive");
		if(curUser !=null && curUser.userinfo !=null){
			SessionActionHandler.exitroom(Integer.parseInt(curUser.sessionid), curUser);
		}
	}
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(curUser+" @ "+ctx +" disconnected by exceptionCaught" + cause.getMessage());
		if(curUser !=null && curUser.userinfo !=null){
			SessionActionHandler.exitroom(Integer.parseInt(curUser.sessionid), curUser);
		}
	}
	public static void clientCleanUp(ChannelHandlerContext ctx) {
		//TODO onClose();
		ctx.channel().close();
		ctx.close();
	}
	
	
}


