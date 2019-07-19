/**
 * 
 */
package icblive.chatserver.model;

import icblive.chatserver.WebSocketMessageHandler;
import icblive.chatserver.api.APICallbackHandler;
import icblive.chatserver.api.APIManager;
import icblive.chatserver.data.LiveCache;
import icblive.chatserver.model.json.UserInfo;
import icblive.chatserver.model.json.UserMedalInfo;
import icblive.chatserver.utils.ChangbaMetrics;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author xiaol
 *
 */
public class UserData {
	public UserInfo userinfo ; 
	public String version = "1"; //1 为初始版本, 2 为协议gzip版本 
	private boolean exitedroom = false; //用于标记用户是否发送过exitroom
	public String sessionid = "";
	public String ownerid = "";//所在房间的主播的userid
	public int score = 0;
	public String role = "";
	public String clienttype = "ios";
	public String lang = "en"; // 0表示英文 1表示中文 2 西班牙 3印地语

	public UserData(UserInfo uinfo,boolean isexitedroom){
		userinfo = uinfo;
		exitedroom = isexitedroom;
		sessionid = "";
	}
	public UserData() {
		// TODO Auto-generated constructor stub
	}
	
	public ChannelHandlerContext ctx;
	public WebSocketMessageHandler wshander;
	public String getUserid(){
		if(userinfo != null){
			return userinfo.userid;
		}
		return "";
	}
	public UserInfo getUserInfo(){
		return userinfo;
	}

	public UserMedalInfo getUserMedalInfo() {
		if(userinfo != null) {
			return userinfo.medalinfo;
		}
		return null;
	}

	public String getNickName(){
		if(userinfo != null){
			return userinfo.nickname;
		}
		return "";
	}
	public String getRoomid(){
		if( exitedroom == false){
			return sessionid;
		}
		return "";
	}
	public String getVersion(){
		return version;
	}
	public RoomData getRoom(){
		if( !sessionid.equals("")){
			return LiveCache.getRoomBySessionid(sessionid);
		}
		return null;
	}
	
	public void recycle(){
//		String userid = getUserid();
//		String roomid = userinfo.roomid;
		RoomData roomdata = getRoom();
		if (roomdata!=null){
			roomdata.removeUser(this);
		}
		
		//XXX 不太优雅. 应该在netty去处理网络, 在这里处理直播逻辑.
		if (this.wshander!=null && this.ctx!=null){
			WebSocketMessageHandler.clientCleanUp(this.ctx);
			//统计
			//ChangbaMetrics.userCount.dec();
			this.ctx = null;
			this.wshander = null;
		}
		//回收引用 
		this.userinfo = null;
		if (!exitedroom){
			//没有正常退出房间，需要走一遍退出房间的流程
			//一是房间列表。
			//二是如果是主播的话，需要告诉PHP他下播了。
			this.exitedroom = true;
		}
		
	}
	public void exitSession(){
		this.exitedroom = true;
		recycle();
	}
	
	public void joinSession() {
		this.exitedroom = false;
	}
	
	public boolean isExitedroom() {
		return exitedroom;
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((userinfo == null) ? 0 : userinfo.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserData other = (UserData) obj;
		if (userinfo == null) {
			if (other.userinfo != null)
				return false;
		} else if (!userinfo.equals(other.userinfo))
			return false;
		return true;
	}
	public int getScore() {
		return score;
	}
	@Override
	public String toString() {
		return "UserData [userinfo=" + userinfo + ", version=" + version + ", exitedroom=" + exitedroom + ", sessionid="
				+ sessionid + ", ownerid=" + ownerid + ", score=" + score + ", role=" + role + ", ctx=" + ctx
				+ ", wshander=" + wshander + "]";
	}
	
	
}
