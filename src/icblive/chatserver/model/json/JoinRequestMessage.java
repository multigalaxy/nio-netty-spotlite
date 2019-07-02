package icblive.chatserver.model.json;

public class JoinRequestMessage extends BaseMessage {
	public String source;
	public JoinRequestMessage(String source){
		type = "join";
		this.source = source;
	} 	
}
