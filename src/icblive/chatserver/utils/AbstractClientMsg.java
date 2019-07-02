package icblive.chatserver.utils;

public abstract class AbstractClientMsg {
	private int msgtype;
	public static int NONEEDTRANSLATE = 1;
	public static int NEEDTRANSLATE = 2;
	@Override
	public String toString() {
		return "ClientMsg [msgtype=" + msgtype + "]";
	}
	public AbstractClientMsg(int msgtype) {
		super();
		this.msgtype = msgtype;
	}
	public abstract String getMessage(String langtype);
}
