/**
 * 
 */
package icblive.chatserver.model.json;

import java.util.Map;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * @author xiaol
 *
 */
public class UserInfo {
	
	public String userid ="";
	public String nickname ="";
	public String headphoto ="";
	public String gender ="";
	public int level = 0;
	public int vip = 0;
	public String country = "US";
	public UserMedalInfo medalinfo;
	//public int member = 0;

	public UserInfo( Map<String, String> userinfo) {
		userid = userinfo.get("userid");
		nickname = userinfo.get("nickname_blob");
		headphoto = userinfo.get("headphoto");
		gender = userinfo.get("gender");
		vip = Integer.parseInt(userinfo.get("vip"));
		if(userinfo.get("userlevel") !=null){
			level = Integer.parseInt(userinfo.get("userlevel"));
		}else{
			level = 1;
		}
		if(userinfo.get("country") != null) {
			this.country = userinfo.get("country");
		}
		//member = Integer.parseInt(userinfo.get("member"));
	}

	// 设置勋章信息
	public void setUserMedalInfo(UserMedalInfo userMedalInfo) {
		this.medalinfo = userMedalInfo;
	}

	public UserInfo() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((userid == null) ? 0 : userid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserInfo other = (UserInfo) obj;
		if (userid == null) {
			if (other.userid != null)
				return false;
		} else if (!userid.equals(other.userid))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "UserInfo{" +
				"userid='" + userid + '\'' +
				", nickname='" + nickname + '\'' +
				", headphoto='" + headphoto + '\'' +
				", gender='" + gender + '\'' +
				", level=" + level +
				", vip=" + vip +
				", country='" + country + '\'' +
				", medalinfo=" + medalinfo +
				'}';
	}
}
