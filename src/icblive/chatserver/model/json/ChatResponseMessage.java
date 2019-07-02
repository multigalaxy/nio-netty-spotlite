/**
 * 
 */
package icblive.chatserver.model.json;

public class ChatResponseMessage extends BaseResponseMessage {
	public String targetuserid = "";
	public String msgbody;
	public UserMedalInfo medalinfo;

	public ChatResponseMessage(String type, String userid, String nickname, int level, String targetuserid, String msgbody) {
		super(type,userid,nickname,level);
		this.targetuserid = targetuserid;
		this.msgbody = msgbody;
	}

	public ChatResponseMessage(String type, String msgbody) {
		super(type);
		this.msgbody = msgbody;
	}

	public void setUserMedalInfo(UserMedalInfo userMedalInfo) {
		this.medalinfo = userMedalInfo;
	}

	@Override
	public String toString() {
		return "ChatResponseMessage [targetuserid=" + targetuserid + ", msgbody=" + msgbody + "]";
	}
}