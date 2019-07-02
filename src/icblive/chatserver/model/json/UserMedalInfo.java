/**
 * 
 */
package icblive.chatserver.model.json;

import icblive.chatserver.utils.UtilFunc;

import java.util.Map;

/**
 * @author caoliang
 * 用户勋章json
 *
 */
public class UserMedalInfo {

	public int medaltype = 0;
	public int isexpire = 0;
	public int cost = 0;

	public UserMedalInfo(Map<String,String> medalinfo) {
		medaltype = UtilFunc.parseInt(medalinfo.get("medaltype"), 0);
		isexpire = UtilFunc.parseInt(medalinfo.get("isexpire"), 0);
		cost = UtilFunc.parseInt(medalinfo.get("cost"), 0);
	}

	public UserMedalInfo() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String toString() {
		return "UserMedalInfo [medaltype=" + medaltype + ", isexpire=" + isexpire +  ", cost=" + cost + "]";
	}
	
}
