package icblive.chatserver.model.json;

import java.util.List;
import com.google.common.collect.Lists;

public class GetAudienceListMessage extends BaseMessage{
	public int start = 0;
	public int count = 0;
	public GetAudienceListMessage(){
		super("getaudiencelist");
	}
}
