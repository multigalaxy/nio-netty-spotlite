package icblive.chatserver.model.json;
/*
 * 
 * 带request的消息都是客户端的请求消息，都用json解析进行初始化
 */
public class BaseRequestMessage extends BaseMessage {
	public String userid;

	public BaseRequestMessage(String type,String userid) {
		super(type);
		this.userid = userid;
	}
	public BaseRequestMessage() {}
	@Override
	public String toString() {
		return "BaseRequestMessage [userid=" + userid + "]";
	}
	
}
