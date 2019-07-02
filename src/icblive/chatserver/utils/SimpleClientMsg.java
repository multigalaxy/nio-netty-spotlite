package icblive.chatserver.utils;
// 不需要支持多语言
public class SimpleClientMsg extends AbstractClientMsg {
	private String msg;
	public SimpleClientMsg(String msg) {
		super(AbstractClientMsg.NONEEDTRANSLATE);
		this.msg = msg;
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getMessage(String langtype) {
		// TODO Auto-generated method stub
		return this.msg;
	}
}
