package icblive.chatserver.model.json;

public class BaseResponseMessage extends BaseMessage {
	public String userid = "";
	public String nickname = "";
	public int level = 0;
	public UserMedalInfo medalinfo;

	public BaseResponseMessage(String type){
		super(type);
	}
	public BaseResponseMessage(){
	}
	public BaseResponseMessage(String type, String userid, String nickname, int level) {
		super(type);
		this.userid = userid;
		this.nickname = nickname;
		this.level = level;
	}

	public void setUserMedalInfo(UserMedalInfo userMedalInfo) {
		this.medalinfo = userMedalInfo;
	}

	@Override
	public String toString() {
		return "BaseResponseMessage [userid=" + userid + ", nickname=" + nickname + ", level=" + level + ", medalinfo=" + medalinfo + "]";
	}
	
}
