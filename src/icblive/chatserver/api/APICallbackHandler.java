/**
 * 
 */
package icblive.chatserver.api;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;

import icblive.chatserver.data.LiveCache;
import icblive.chatserver.model.RoomData;
import icblive.chatserver.model.UserData;
import icblive.chatserver.model.json.BaseResponseMessage;
import icblive.chatserver.model.json.ChatRequestMessage;
import icblive.chatserver.model.json.ChatResponseMessage;
import icblive.chatserver.model.json.BaseMessage;
import icblive.chatserver.model.json.EndLiveResponseMessage;
import icblive.chatserver.model.json.GiveGiftMessage;
import icblive.chatserver.model.json.KickOffUserMessage;
import icblive.chatserver.model.json.NoticeMessage;
import icblive.chatserver.model.json.NoticeMessageList;
import icblive.chatserver.model.json.ResponseMessage;
import icblive.chatserver.model.json.SystemWsMessage;
import icblive.chatserver.model.json.UserInfo;
import icblive.chatserver.model.json.ResponseMessage.ResponseEntry;
import icblive.chatserver.utils.RedisHelper;

/**
 * @author jgao
 *
 */
public class APICallbackHandler implements FutureCallback<String>{

	private UserData curUser;
	private static Logger logger = LogManager.getLogger(APICallbackHandler.class);
	private static final ScheduledExecutorService scheduledExe = Executors.newScheduledThreadPool(6);
	
	public APICallbackHandler(UserData curUser){
		this.curUser = curUser;
	}
	@Override
	public void onFailure(Throwable e) {
		// TODO Auto-generated method stub
		
	}


	/**
	 * 处理server发过来的消息
	 * @param text 消息内容
	 */
	@Override
	public void onSuccess(String text) {
		logger.info( "API: "+text);
		ResponseMessage resp;
		if (Strings.isNullOrEmpty(text)){
			return;
		}
		try{
			resp = JSON.parseObject(text, ResponseMessage.class);
		}catch(Exception e){
			logger.error("Error response json: "+e);
			return;
		}
		if (! Objects.equal("ok", resp.errorcode) ){
			logger.error("Error API response: "+resp.errorcode);
		}
		if (resp.result==null || resp.result.isEmpty()){
			logger.debug("API empty message:" + text);
			return;
		}
		for( ResponseEntry entry: resp.result){
			dealWithResponseEntry(entry);
		}
	}


	public void dealWithResponseEntry(ResponseEntry entry) {
		BaseMessage baseMsg = JSON.parseObject(entry.jsondata, BaseMessage.class);
		if (Objects.equal("global", entry.sendtype)){//要发送全站通知，那么就发给所有用户
			switch(baseMsg.type){
				case "publicchat":
					ChatRequestMessage requestmsg = JSON.parseObject(entry.jsondata,ChatRequestMessage.class);
					UserInfo ud = LiveCache.getUserInfo(entry.userid);
					ChatResponseMessage msg = new ChatResponseMessage("publicchat", ud.userid, ud.nickname,ud.level, "", requestmsg.msgbody);
					msg.setUserMedalInfo(ud.medalinfo);
					entry.jsondata = JSON.toJSONString(msg);
					break;
				default:
					break;
			}
			MessageUtils.sendToAll(new String[]{entry.jsondata}, 1);
			return;
		}else if( Objects.equal("session", entry.sendtype)){
			RoomData room = LiveCache.getRoomBySessionid(entry.sessionid);
			UserData relateduser = null;
			switch(baseMsg.type){
				case "gift":
					GiveGiftMessage giftMsg = JSON.parseObject(entry.jsondata, GiveGiftMessage.class);
					relateduser = LiveCache.getUserData(giftMsg.userid);
					if( relateduser == null ){
						giftMsg.addUserinfo(LiveCache.getUserInfo(giftMsg.userid));
					}else{
						giftMsg.addUserinfo(relateduser.getUserInfo());
					}
					entry.jsondata = JSON.toJSONString(giftMsg);
					if( Objects.equal("4",giftMsg.giftid) ){
						giftMsg.type = "freegift";
						baseMsg.type = "freegift";
					}
					break;
				case "share":
				case "arrive":
					//分享
					BaseResponseMessage shareMsg = JSON.parseObject(entry.jsondata,BaseResponseMessage.class);
					UserInfo userinfo = LiveCache.getUserInfo(shareMsg.userid);
					if( userinfo != null){
						shareMsg.nickname = userinfo.nickname;
						shareMsg.level = userinfo.level;
						shareMsg.setUserMedalInfo(userinfo.medalinfo);
					}else{
						shareMsg.nickname = "观众";
						shareMsg.level = 1;
					}
					entry.jsondata = JSON.toJSONString(shareMsg);
					break;
				case "finishmic":
					EndLiveResponseMessage finishMsg = JSON.parseObject(entry.jsondata, EndLiveResponseMessage.class);
					SessionActionHandler.finishMic(finishMsg);
					//finishMic里发了消息，所以这里直接返回了
					return;
				case "publicchat":
					//公聊
					ChatRequestMessage requestmsg = JSON.parseObject(entry.jsondata,ChatRequestMessage.class);
					UserInfo ud = LiveCache.getUserInfo(entry.userid);
					ChatResponseMessage msg = new ChatResponseMessage("publicchat", ud.userid, ud.nickname,ud.level, "", requestmsg.msgbody);
					msg.setUserMedalInfo(ud.medalinfo);  // 设置勋章
					entry.jsondata = JSON.toJSONString(msg);
					break;
				case "pause":
				case "resume":
					SessionActionHandler.pauseOrResumeSendToRoom(entry.jsondata,room);
					return;
				case "blocksession":
					// 指定主播断流
					UserData user = LiveCache.getUserData(entry.userid);
					if(user != null) {
						EndLiveResponseMessage erm = JSON.parseObject(entry.jsondata, EndLiveResponseMessage.class);
						erm.type = "finishmic";  // 客户端按照finishmic处理，每个app只收到一条消息，所以此处blocksession就等价于给指定主播发送finishmic
						logger.info(JSON.toJSONString(erm));
						MessageUtils.sendToUser(user, new String[]{JSON.toJSONString(erm)});
						SessionActionHandler.finishMic(erm);
					}
					return;
				case "sendtoone":
					UserData user2 = LiveCache.getUserData(entry.userid);
					if(user2 != null) {
						MessageUtils.sendToUser(user2, new String[]{entry.jsondata});
					}
					return;
				default:
					break;
			}
			if( room != null){
				if (MessageUtils.sendToRoom(room, entry.jsondata, baseMsg.type) == false) {
					// 消息太多了只发一个人
					if(relateduser != null){
						MessageUtils.sendToUser(relateduser, new String[]{entry.jsondata});
					}
				}
			}
		}else if( Objects.equal("one", entry.sendtype)){
			UserData user = null;
			if ( !Strings.isNullOrEmpty(entry.userid)){
				user = LiveCache.getUserData(entry.userid);
			}
			switch(baseMsg.type){
				case "disablemsg":
					if (user == null) {
						SessionActionHandler.disableMsgFromAdmin(entry.jsondata, entry.userid, entry.sessionid);
					} else {
						SessionActionHandler.disableMsg(entry.jsondata, user);
					}
					return;
				case "kickoff":
					if (user == null) {
						SessionActionHandler.kickOffFromAdmin(entry.jsondata, entry.userid, entry.sessionid);
					} else {
						SessionActionHandler.kickOff(entry.jsondata, user);
					}
					
					return;
				case "updateuserinfo":
					if(user != null){
						UserInfo newinfo = RedisHelper.getUserInfo(entry.userid);
						if(newinfo != null){
							user.userinfo = newinfo;
						}
					}
					return;
				default:
					break;
			}
			if(user != null){
				MessageUtils.sendToUser(user, new String[]{entry.jsondata});
			}
		}else if( Objects.equal("node",entry.sendtype)){
			// 如果是setprivatenotice就不能发出去
			if (Objects.equal(baseMsg.type, "setprivatenotice")){
				List<NoticeMessage> mslist = Lists.newArrayList();
				NoticeMessageList list = JSON.parseObject(entry.jsondata, NoticeMessageList.class);
				for(String s:list.data) {
					mslist.add(JSON.parseObject(s, NoticeMessage.class));
				}
				if (list.data!=null && !list.data.isEmpty()){
					SystemMessageManager.updateNoticeMessage(mslist);
				}
				return;
			}else if(Objects.equal(baseMsg.type, "updateuserinfo")){
				
			}
		}
	}


}
