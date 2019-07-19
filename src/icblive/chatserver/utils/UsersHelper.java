/**
 * 
 */
package icblive.chatserver.utils;

import com.google.common.base.Strings;

/**
 * @author xiaol
 *
 */
public class UsersHelper {
	private static final String ANONYMOUS_USER_PREFIX = "uid:";

	public static boolean isAnonymousUser(String userid) {
		if (Strings.isNullOrEmpty(userid)){
			return false;
		}
		if (userid.startsWith(ANONYMOUS_USER_PREFIX) && userid.length() > 4){
			return true;
		}
		return false;
	}
}
