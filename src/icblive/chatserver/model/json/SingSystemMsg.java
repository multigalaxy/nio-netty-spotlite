/**
 * 
 */
package icblive.chatserver.model.json;

/**
 * @author jgao
 *
 */
public class SingSystemMsg extends BaseMessage{
	public String msgbody;
	public String type;
	public String color;
	public static SingSystemMsg makeSingSystemMsg(String body){
		SingSystemMsg  msg = new SingSystemMsg();
		msg.type = "systemmessage";
		msg.color="#ff4c4c";
		msg.msgbody = body;
		return msg; 
	}
}
