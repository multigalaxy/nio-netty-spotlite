package icblive.chatserver.model.json;


public class KickOffUserMessage extends BaseMessage {
	public String targetuserid;
	public KickOffUserMessage(){
		super("kickoff");
	}
}
