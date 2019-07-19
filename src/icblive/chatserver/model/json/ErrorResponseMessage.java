/**
 * 
 */
package icblive.chatserver.model.json;

/**
 * @author xiaol
 *
 */
public class ErrorResponseMessage extends BaseMessage{
	public String displaytype;
	public String msgbody;
	public static ErrorResponseMessage makeErrorMessage(String body){
		ErrorResponseMessage  msg = new ErrorResponseMessage();
		msg.displaytype = "toast";
		msg.type = "errormessage";
		msg.msgbody = body;
		return msg;
	}
}
