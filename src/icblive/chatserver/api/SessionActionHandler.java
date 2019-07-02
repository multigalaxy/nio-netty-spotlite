/**
 * 
 */
package icblive.chatserver.api;

import java.util.ArrayList;
import java.util.List;

import icblive.chatserver.model.json.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

import icblive.chatserver.data.LiveCache;
import icblive.chatserver.model.RoomData;
import icblive.chatserver.model.UserData;
import icblive.chatserver.utils.LanguageUtils;
import icblive.chatserver.utils.RedisHelper;
import icblive.chatserver.utils.SupportMultiLangClientMsg;

/**
 * @author jgao
 *
 */
public class SessionActionHandler {
	private static Logger logger = LogManager.getLogger(SessionActionHandler.class);
			
	public static void exitSession(String jsonMessage, UserData curUser) {
		// TODO Auto-generated method stub
		final ExitRoomMessage exitRoomMsg = JSON.parseObject(jsonMessage, ExitRoomMessage.class);
		SessionActionHandler.exitroom(Integer.parseInt(exitRoomMsg.sessionid),curUser);
		return;
	}
	
	public static void exitroom(int sessionid, UserData curUser) {
		// TODO Auto-generated method stub
		if (curUser!=null){
			RedisHelper.removeUserfromSession(curUser.getUserid(),sessionid+"");
			curUser.exitSession();
		}
		return;
	}

	// 加入直播
	public static void joinSession(String jsonMessage, UserData curUser) {
		// TODO Auto-generated method stub
		if (curUser == null) {
			return;
		}
		String sessionid = curUser.sessionid;
		RoomData sessioninfo = LiveCache.getRoomBySessionid(sessionid);
		if (sessioninfo == null || !sessioninfo.isLive()) {
			String errStr = LanguageUtils.getInstance().getContents(curUser.lang, "ERROR_ANCHORNOTLIVE");
			ErrorResponseMessage err = ErrorResponseMessage.makeErrorMessage(errStr);
			MessageUtils.sendToUser(curUser, new String[] { JSON.toJSONString(err) });
			curUser.exitSession();
			return;
		}
		logger.info("joinSession roominfo" + sessioninfo + " userinfo " + curUser);
		// score将来可能有用，先留着
		curUser.score = 10;
		curUser.joinSession();
		AudienceListMessage joinSessionReplyMsg = RedisHelper.joinSession(curUser.getUserid(), sessioninfo.getRoomSession(),
				curUser.getUserid().equals(sessioninfo.userid));
		if (!LiveCache.addUserData(curUser.getUserid(), curUser)) {
			String errStr = "请不要在网页和客户端同时进入同一个房间";
			ErrorResponseMessage err = ErrorResponseMessage.makeErrorMessage(errStr);
			MessageUtils.sendToUser(curUser, new String[] { JSON.toJSONString(err) });
			logger.info("joinSessoin userid=" + curUser.getUserid() + "err=" + errStr);
			curUser.exitSession();
			return;
		}
		sessioninfo.addUser(curUser);

		List<String> msg = new ArrayList<String>();
		msg.add(JSON.toJSONString(joinSessionReplyMsg));
		for (SystemWsMessage notice : SystemMessageManager.getNoticeMessages()) {
			msg.add(JSON.toJSONString(notice));
		}
		if (sessioninfo.isPause()) {
			msg.add(JSON.toJSONString(new PauseMessage("pause", sessioninfo.pauseOrResume("", "0"))));
		}
		if (curUser.getUserid().equals(sessioninfo.userid)) {
			sessioninfo.ownerJoined();
		}
		if (!msg.isEmpty()) {
			MessageUtils.sendToUser(curUser, msg.toArray(new String[1]));
		}
		// 给当前用户发送公告
//		String infomsg = LanguageUtils.getInstance().getContents(curUser.lang, "INFO_ANNOUNCE");
//		infomsg = "Welcome to SPOTLITE. We will work together to create a better living environment.";
		String infomsg = RedisHelper.getLiveSystemMsg(curUser.userinfo.country, "annouce");  // 获取配置消息
		logger.info("joinSessoin userid=" + curUser.getUserid() + ", infomsg=" + infomsg);
		if(!Strings.isNullOrEmpty(infomsg)) {
			SystemResponseMessage userMsg = new SystemResponseMessage(infomsg);  // 客户端按照systemmessage处理，指定app只收到一条消息
			MessageUtils.sendToUser(curUser, new String[]{JSON.toJSONString(userMsg)});
		}
		// 发进入房间的消息
		ArriveResponseMessage tmp = new ArriveResponseMessage(curUser.getUserid(), curUser.getUserInfo().nickname, curUser.getUserInfo().level,"just arrived.");
		tmp.setUserMedalInfo(curUser.getUserMedalInfo());
		MessageUtils.sendToRoom(sessioninfo, JSON.toJSONString(tmp), "arrive");
		return;
	}

	public static void getAudienceList(String jsonMessage, UserData curUser) {
		final GetAudienceListMessage getAudienceListMsg = JSON.parseObject(jsonMessage, GetAudienceListMessage.class);
		if (curUser!=null){
			int start = getAudienceListMsg.start>0?getAudienceListMsg.start:0;
			int count = getAudienceListMsg.count>0?getAudienceListMsg.count:0;
			AudienceListMessage audienceListMsg = RedisHelper.getAudienceList(curUser.getUserid(), curUser.getRoomid(),start,count);
			MessageUtils.sendToUser(curUser, new String[]{JSON.toJSONString(audienceListMsg)});
		}
		return;	
	}

	public static void disableMsg(String requestMesage, UserData curUser) {
		DisableChatRequestMessage disablemsg = JSON.parseObject(requestMesage, DisableChatRequestMessage.class);
		if (curUser == null || Strings.isNullOrEmpty(disablemsg.targetuserid)) {
			return;
		}
		String sessionid = curUser.sessionid;
		RoomData roomdata = LiveCache.getRoomBySessionid(sessionid);
		if (roomdata == null) {
			return;
		}
		if (!Objects.equal(roomdata.userid,curUser.getUserid())) {
			String errStr = LanguageUtils.getInstance().getContents(curUser.lang, "ERROR_DISABLE_AUTHORITY");
			ErrorResponseMessage err = ErrorResponseMessage.makeErrorMessage(errStr);
			MessageUtils.sendToUser(curUser, new String[]{JSON.toJSONString(err)});
			return;
		}
		//判断权限
		logger.debug( "disablemsg roominfo:user:"+roomdata.userid +"curUserinfo:"+curUser.getUserid());
		//现在只有主播有这个权限
		LiveCache.addDisableMsgUser(disablemsg.targetuserid, roomdata.userid);
		UserData targetuser = LiveCache.getUserData(disablemsg.targetuserid);
		if (targetuser != null && Objects.equal(targetuser.sessionid,curUser.sessionid)) {
			long banchattime = LiveCache.disableMsgUserTimeStamp(disablemsg.targetuserid, roomdata.userid);
			DisableChatResponseMessage msg = new DisableChatResponseMessage(disablemsg.targetuserid, banchattime);
			MessageUtils.sendToUser(targetuser, JSON.toJSONString(msg));
			//TODO：在这里退出能不能保证收到？
		}
		//发一条系统消息？
		return;	
	}
	public static void disableMsgFromAdmin(String jsonMessage, String userid, String sessionid) {
		DisableChatRequestMessage disablemsg = JSON.parseObject(jsonMessage, DisableChatRequestMessage.class);
		logger.debug( "disablemsgfromadmin roominfo:user:"+userid +"targetuserid:"+disablemsg.targetuserid);
		// 现在只有主播有这个权限
		LiveCache.addDisableMsgUser(disablemsg.targetuserid, userid);
		UserData targetuser = LiveCache.getUserData(disablemsg.targetuserid);
		if (targetuser != null && Objects.equal(targetuser.sessionid, sessionid)) {
			long banchattime = LiveCache.disableMsgUserTimeStamp(disablemsg.targetuserid, userid);
			DisableChatResponseMessage msg = new DisableChatResponseMessage(disablemsg.targetuserid, banchattime);
			MessageUtils.sendToUser(targetuser, JSON.toJSONString(msg));
			// TODO：在这里退出能不能保证收到？
		}
	}
	public static void kickOffFromAdmin(String jsonMessage, String userid, String sessionid) {
		final KickOffUserMessage kickoffMsg = JSON.parseObject(jsonMessage, KickOffUserMessage.class);
		logger.debug( "kickofffromadmin roominfo:user:"+userid +"targetuserid:"+kickoffMsg.targetuserid);
		// 现在只有主播有这个权限
		LiveCache.addKickOffUser(kickoffMsg.targetuserid, userid);
		// 放在redis里，让PHP也知道
		// 告诉被踢的人
		UserData targetuser = LiveCache.getUserData(kickoffMsg.targetuserid);
		if (targetuser != null && Objects.equal(targetuser.sessionid, sessionid)) {
			MessageUtils.sendToUser(targetuser, new String[] { jsonMessage });
			// TODO：在这里退出能不能保证收到？
			targetuser.exitSession();
		}
	}
	
	public static void finishMic( EndLiveResponseMessage finishMsg ) {
		RoomData roomdata = LiveCache.getRoomBySessionid(finishMsg.sessionid);
		if (roomdata!=null){
			//给所有人发一条消息，告诉大家直播结束了
			finishMsg.usercnt = RedisHelper.getSessionInfo(finishMsg.sessionid, "usercnt");
			roomdata.islive = false;
			MessageUtils.sendToRoom(roomdata, JSON.toJSONString(finishMsg),finishMsg.type);
		}
		return;	
	}

	public static void pauseOrResume(String jsonMessage, UserData curUser) {
		if (curUser!=null){
			final PauseMessage pauseMsg = JSON.parseObject(jsonMessage, PauseMessage.class);
			RoomData roomdata = curUser.getRoom();
			if( roomdata !=null && Objects.equal( curUser.getUserid() , roomdata.userid)){
				logger.debug("pauseOrResume type:"+pauseMsg.type+" owner:"+curUser.userinfo);
				roomdata.pauseOrResume(pauseMsg.type,pauseMsg.pauseid);
				MessageUtils.sendToRoom(roomdata, JSON.toJSONString(pauseMsg),pauseMsg.type);
			}else{
				logger.error("pauseOrResume catch bad guy:"+curUser.userinfo);
			}
		}
		return;
	}
	
	public static void pauseOrResumeSendToRoom(String jsonMessage, RoomData roomdata) {
		if( roomdata !=null ){
			final PauseMessage pauseMsg = JSON.parseObject(jsonMessage, PauseMessage.class);
			roomdata.pauseOrResume(pauseMsg.type,pauseMsg.pauseid);
			MessageUtils.sendToRoom(roomdata, JSON.toJSONString(pauseMsg),pauseMsg.type);
			logger.debug("pauseOrResumeSendToRoom pauseinfo"+jsonMessage+"roominfo"+roomdata);
		}
		return;
	}

	public static void kickOff(String jsonMessage, UserData curUser) {
		final KickOffUserMessage kickoffMsg = JSON.parseObject(jsonMessage, KickOffUserMessage.class);
		if (curUser!=null){
			RoomData roomdata = LiveCache.getRoomBySessionid(curUser.sessionid);
			if( roomdata != null){
				//判断权限
				logger.debug( "kickOff roominfo:user:"+roomdata.userid +"curUserinfo:"+curUser.getUserid());
				if( Objects.equal(roomdata.userid,curUser.getUserid()) ){
					//现在只有主播有这个权限
					LiveCache.addKickOffUser(kickoffMsg.targetuserid, roomdata.userid);
					//放在redis里，让PHP也知道
					//告诉被踢的人
					UserData targetuser = LiveCache.getUserData(kickoffMsg.targetuserid);
					if( targetuser != null && Objects.equal(targetuser.sessionid,curUser.sessionid)){
						MessageUtils.sendToUser(targetuser, new String[]{jsonMessage});
						//TODO：在这里退出能不能保证收到？
						targetuser.exitSession();
					}
				}else{
					String errStr = "对不起你没有权限";
					ErrorResponseMessage err = ErrorResponseMessage.makeErrorMessage(errStr);
					MessageUtils.sendToUser(curUser, new String[]{JSON.toJSONString(err)});
				}
			}
		}
		return;	
	}

	public static void share(String jsonMessage, UserData curUser) {
		// TODO Auto-generated method stub
		BaseResponseMessage shareMsg = JSON.parseObject(jsonMessage, BaseResponseMessage.class);
		shareMsg.nickname = curUser.getNickName();
		shareMsg.userid = curUser.getUserid();
		shareMsg.level = curUser.userinfo.level;
		shareMsg.setUserMedalInfo(curUser.getUserMedalInfo());
		RoomData roomdata = LiveCache.getRoomBySessionid(curUser.sessionid);
		if( roomdata != null && roomdata.islive){
			MessageUtils.sendToRoom(roomdata, JSON.toJSONString(shareMsg),shareMsg.type);
		}
	}
}
