package icblive.chatserver.model.json;

import java.util.List;

public class AudienceListMessage extends BaseMessage{
	public long audienceamount = -1;
	public int start = 0;

	public List<AudienceInfo> audiencelist;
	public AudienceListMessage(){
		super("audiencelist");
	}
	public static class AudienceInfo{
        public String userid = "";
        public String headphoto = "";
        public int level = 1;
        public int vip = 0;
		public UserMedalInfo medalinfo;

		public AudienceInfo(){
        }

        public AudienceInfo(UserInfo userinfo){
			userid = userinfo.userid;
			headphoto = userinfo.headphoto;
			level = userinfo.level;
			vip = userinfo.vip;
			medalinfo = userinfo.medalinfo;
        }
    }
}
