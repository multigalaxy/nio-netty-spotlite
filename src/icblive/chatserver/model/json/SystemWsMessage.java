package icblive.chatserver.model.json;

public class SystemWsMessage extends BaseMessage{
	public String msgbody;
	public int isspecial;
	public String color;
	public SystemWsMessage(){
		super("systemmessage");
		isspecial = 0;
		color = "";
	}
	public SystemWsMessage(String msg, int special , String msgcolor){
		super("systemmessage");
		msgbody = msg;
		isspecial = special;
		color = msgcolor;
	}
}
