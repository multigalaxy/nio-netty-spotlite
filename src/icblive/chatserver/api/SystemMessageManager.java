/**
 * 
 */
package icblive.chatserver.api;

import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import icblive.chatserver.model.json.NoticeMessage;
import icblive.chatserver.model.json.SystemWsMessage;

/**
 * @author xiaol
 *
 */
public class SystemMessageManager {

	private static List<NoticeMessage> notices = Lists.newArrayList();
	private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	public static List<SystemWsMessage> getNoticeMessages(){
		long now  = new Date().getTime();
		lock.readLock().lock();
		List<SystemWsMessage> validNotices = Lists.newArrayList();
		lock.readLock().unlock();
		for(NoticeMessage notice : notices){
			if (now > notice.beginTime.getTime() && now < notice.endTime.getTime()){
				validNotices.add(notice.sysmsg);
			}
		}
		return ImmutableList.copyOf(validNotices);
	}
	public static void updateNoticeMessage(List<NoticeMessage> list){
		List<NoticeMessage> validNotices = Lists.newArrayList();
		long now  = new Date().getTime();
		for(NoticeMessage notice: list){
			if (now > notice.beginTime.getTime() && now < notice.endTime.getTime()) {
				validNotices.add(notice);
			}
		}
		lock.writeLock().lock();
		try{
			notices = validNotices;
		}finally{
			lock.writeLock().unlock();
		}
	}
	public static void addNoticeMessage(NoticeMessage n){
		validateNoticeMessage();
		lock.writeLock().lock();
		try{
			notices.add(n);
		}finally{
			lock.writeLock().unlock();
		}
	}
	
	/** -------------------------------------   */
	
	//去掉过期的通知
	private static void validateNoticeMessage(){
		lock.writeLock().lock();
		try{
			long now  = new Date().getTime();
			List<NoticeMessage> validNotices = Lists.newArrayList();
			for(NoticeMessage notice : notices){
				if (now > notice.beginTime.getTime() && now < notice.endTime.getTime()){
					validNotices.add(notice);
				}
			}
			notices = validNotices;
		}finally{
			lock.writeLock().unlock();
		}
	}
}
