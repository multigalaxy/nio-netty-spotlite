package icblive.chatserver.utils;

import java.util.HashMap;
import java.util.Map;

public class SupportMultiLangClientMsg extends AbstractClientMsg {
	private Map<String, String> msgs;
	public SupportMultiLangClientMsg() {
		super(AbstractClientMsg.NEEDTRANSLATE);
		this.msgs = new HashMap<String, String>();
		// TODO Auto-generated constructor stub
	}
	public void addMessage(String msg, String langtype) {
		this.msgs.put(langtype, msg);
	}
	@Override
	public String getMessage(String langtype) {
		// TODO Auto-generated method stub
		String msg = this.msgs.get(langtype);
		if (msg == null) {
			return "";
		}
		return msg;
	}

}
