package icblive.chatserver.model.json;

public class SystemResponseMessage extends BaseResponseMessage {
	public String msgbody;
	public SystemResponseMessage(String msgbody) {
		super("systemmessage");
		this.msgbody = msgbody;
	}
}
