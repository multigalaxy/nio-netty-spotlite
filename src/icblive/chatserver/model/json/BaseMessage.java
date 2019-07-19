/**
 * 
 */
package icblive.chatserver.model.json;

/**
 * @author xiaol
 *
 */
public class BaseMessage {
	public String type;

	public BaseMessage(){
	}
	public BaseMessage(String type){
		this.type = type;
	}
	@Override
	public String toString() {
		return "BaseMessage [type=" + type + "]";
	}
}
