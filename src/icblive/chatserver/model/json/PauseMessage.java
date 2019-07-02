package icblive.chatserver.model.json;

public class PauseMessage extends BaseMessage{
	public String pauseid;
	public PauseMessage(){
	}
	public PauseMessage(String msgtype){
		super(msgtype);
	}
	public PauseMessage(String msgtype,String pause){
		super(msgtype);
		pauseid = pause;
	}
}
