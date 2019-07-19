package icblive.chatserver.model.json;

/**
 * @author xiaol
 * 
 */


public class ChatRequestMessage extends BaseMessage {
	public String msgbody;

	public ChatRequestMessage(String type, String msgbody) {
		super(type);
		this.msgbody = msgbody;
	}
	public ChatRequestMessage() {
		
	}

	@Override
	public String toString() {
		return "ChatRequestMessage [msgbody=" + msgbody + "]";
	}
	
}
class SErrorWsMessage  
{
	public String msgbody;
	public String type;
	public String displaytype;
	public SErrorWsMessage(String msg_body){
		this.msgbody=msg_body;
		this.type="errormessage";
		this.displaytype = "toast" ;
	}
	 
}