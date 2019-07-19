/**
 * 
 */
package icblive.chatserver.model.json;

import java.util.List;

/**
 * @author xiaol
 * HTTP(PHP) API 返回的数据结构 
 */

public class ResponseMessage {
	public String errorcode;
	public List<ResponseEntry> result;
	
	public static class ResponseEntry{
		//客户端发来的消息，默认是他所在房间的，所以不需要有roomid消息，但是这里是php发过来的所以得带上。
		public String sessionid;
		//同上，需要知道这个消息是谁发的
		public String userid;
		public String sendtype;
		public String jsondata;  // {sessionid:1234, type:finishmic, msgbody:hahaha, userid: 222}
	}
}
