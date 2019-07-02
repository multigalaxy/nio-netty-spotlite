package icblive.chatserver.model.json;

public class EndLiveResponseMessage extends BaseMessage {
	public String usercnt = "";
	public String userid = "";
	public String sessionid = "";
	public EndLiveResponseMessage(){
		super("endlive");
	}
}
