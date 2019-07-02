package icblive.chatserver.model.json;

public class DisableChatRequestMessage extends BaseRequestMessage {
	public String targetuserid;
	public DisableChatRequestMessage(String type, String userid, String targetuserid) {
		super(type, userid);
		this.targetuserid = targetuserid;
	}
    public DisableChatRequestMessage() {}
	@Override
	public String toString() {
		return "DisableChatRequestMessage []";
	}
}
