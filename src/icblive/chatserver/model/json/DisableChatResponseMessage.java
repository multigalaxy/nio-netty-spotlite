package icblive.chatserver.model.json;

public class DisableChatResponseMessage extends BaseMessage {
	public String targetuserid;
	public long banchattime = 0;
	public DisableChatResponseMessage() {
		super("disablemsg");
	}
	public DisableChatResponseMessage(String targetuserid, long banchattime) {
		super("disablemsg");
		this.targetuserid = targetuserid;
		this.banchattime = banchattime;
	}
	@Override
	public String toString() {
		return "DisableChatResponseMessage [targetuserid=" + targetuserid + ", banchattime=" + banchattime + "]";
	}
	
}
