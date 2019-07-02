/**
 * 
 */
package icblive.chatserver.model.json;

import java.util.Date;

// redis 中获取的系统消息的数据结构
public class NoticeMessage{
	public Date beginTime;
	public Date endTime;
	public SystemWsMessage sysmsg;
}