package icblive.chatserver.model.json;

public class ArriveResponseMessage extends BaseResponseMessage {
	public String msgbody;
	public ArriveResponseMessage(String userid, String nickname, int level, String msgbody) {
		super("arrive", userid, nickname, level);
		this.msgbody = msgbody;
	}
}
