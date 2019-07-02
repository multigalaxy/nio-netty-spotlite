/**
 * 
 */
package icblive.chatserver.api;

import java.util.Iterator;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import icblive.chatserver.data.LiveCache;
import icblive.chatserver.model.RoomData;
import icblive.chatserver.model.UserData;
import icblive.chatserver.model.json.ChatResponseMessage;
import icblive.chatserver.utils.AbstractClientMsg;
import icblive.chatserver.utils.ChangbaConfig;
import icblive.chatserver.utils.ChangbaMetrics;
import icblive.chatserver.utils.GzipUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.ReferenceCountUtil;

/**
 * @author jgao
 *
 */
public class MessageUtils {
	private static Logger logger = LogManager.getLogger(MessageUtils.class);
	private static final ListeningExecutorService exe = MoreExecutors.listeningDecorator(
			Executors.newFixedThreadPool(6));
	public static final int MAX_LIMIT_MSG_COUNT = ChangbaConfig.getConfig().max_msg_per_second; //最高的房间消息数
	
//	private static String filterUnsaveCharacters(String msg) {
//		return msg; // TODO
//	}
	private static String removeUnreadableChar(String str){
        if(str == null || str.equals("") ){
                return "";
        }
        StringBuilder appender = new StringBuilder();
        for (int i=0;i<str.length();i++){
                char c =  str.charAt(i);
                int ch = (int)c;
                if (ch < 0x20 && (ch != 0x9) && (ch != 0xA) && (ch != 0xD)){ 
                	continue;
                }
                appender.append(c);  
        }
        return appender.toString();
	}
	static String makeMessageString(String [] messages ){
		String[] newMsgs = new String[messages.length];
		for(int i=0; i<messages.length; i++){
			newMsgs[i] = removeUnreadableChar(messages[i]);
		}
		StringBuilder sb = new StringBuilder();
		sb.append("{\"result\":[").append(Joiner.on(",").join(newMsgs)).append( "]}");
		return sb.toString();
	}
	/* 单独使用这个函数的场景: 只用来批量发送消息的时候，复用frame */
	static void sendFrameToUser(UserData user, WebSocketFrame frame){
		if (user.ctx != null && user.ctx.channel().isActive()) {
			user.ctx.channel().writeAndFlush(frame);
			//统计
			ChangbaMetrics.incrMeter("sent_msg");
		}
	}
	public static void sendToUser(UserData user, String message) {
		sendToUser(user, new String[]{message});
	}
	public static void sendToUser(UserData user, String [] messages) {
		if (user.ctx == null || !user.ctx.channel().isActive()) {
			return;
		}
		String msg = makeMessageString(messages);
		ByteBuf tmp = Unpooled.wrappedBuffer(GzipUtils.encode(msg));
		
		if (Objects.equal("0.0.1", user.getVersion())) {
			//只给后台用户发，数量不多，所以这么写
			TextWebSocketFrame textframe = new TextWebSocketFrame(msg);
			sendFrameToUser(user,  textframe);
		}else{
			WebSocketFrame frame = new BinaryWebSocketFrame(tmp);
			sendFrameToUser(user,frame);
		}
		logger.debug(user.getUserid() + "@" + user.getRoomid() + " send message: "+ msg);
	}
	
	//异步发送消息给用户, 不是为了提高效率, 而是用异步线程,减少执行时间, 保证并发线程访问数据的安全
	public static void sendToUserAsync(final UserData user, final String [] messages) {
		exe.submit(new Runnable() {
			@Override
			public void run() {
				sendToUser(user, messages);
			}
		});
	}

	@Deprecated //请用下面的sendToRoom()
	public static void sendToRoom(RoomData room, String[] messages) {
		Iterator<UserData> it = room.getUserIterator();
		while(it.hasNext()){
			UserData user = it.next();
			sendToUser(user,  messages);
		}
	}
	//多语言版本
	public static void sendToRoom(RoomData room, AbstractClientMsg msg) {
		Iterator<UserData> it = room.getUserIterator();
		while(it.hasNext()){
			UserData user = it.next();
			sendToUser(user,  new String[]{msg.getMessage(user.lang)});
		}
	}
	public static boolean sendToRoom(RoomData room, String message, String type) {
		RoomMessageQueue rq = RoomMessageQueue.getRoomMessageQueue(room);
		boolean res = true;
		res = rq.addMessage(message, type);
		return res;
	}
	
	public static long sendToAll(String[] jsonmsg,int score){
		Iterator<UserData> it = LiveCache.getAllUserIterator();
		long count =0;
		while(it.hasNext()){
			UserData user = it.next();
			logger.debug("one of sendToAll uid:"+user.getUserid() + " score:"+user.getScore());
			if( user.getScore() >= score ){
				MessageUtils.sendToUserAsync(user, jsonmsg);
				count++;
			} 
		}
		return count;
	}
}
