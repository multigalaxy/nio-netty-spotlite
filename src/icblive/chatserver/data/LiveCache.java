package icblive.chatserver.data;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import icblive.chatserver.api.APIManager;
import icblive.chatserver.api.RoomMessageQueue;
import icblive.chatserver.model.DisableMsgUserData;
import icblive.chatserver.model.KickOffUserData;
import icblive.chatserver.model.RoomData;
import icblive.chatserver.model.UserData;
import icblive.chatserver.model.UserSendMsgData;
import icblive.chatserver.model.json.UserInfo;
import icblive.chatserver.utils.ChangbaMetrics;
import icblive.chatserver.utils.ChatCount;
import icblive.chatserver.utils.RedisHelper;

/**
 * @author jgao
 *
 */
/**
 * 
 * @author jgao
 *  有两种类型的数据, 一种是可以重启丢失的, 一种需要dump到文件,在重启的时候自动加载.
 */
public class LiveCache {

	private static Cache <String , UserData> userCache ;
	//private static Cache <String , UserInfo> userInfoCache ;
	private static Cache <String, DisableMsgUserData> disableMsgUserCache ;     // key : userid@ownerid
	private static Cache <String, KickOffUserData> kickOffUserCache ;     // key : userid@ownerid
	private static Cache <String, UserSendMsgData> userSendMsgCache ;
	private static Map<String, RoomData> roomCache = Maps.newConcurrentMap();
	private static final int LOCK_NUM = 100;
	private static long CLEANUP_ROOM_TIMEOUT = 90 * 1000;
	private static List<Lock> roomLocks = Lists.newArrayList();
	private static RemovalListener<String, UserData> removalListener ;
	static {
		removalListener = new RemovalListener<String , UserData>() {
			@Override
			public void onRemoval(RemovalNotification<String, UserData> userdataNotify) {
				// TODO 在这里处理过期用户的逻辑
				UserData userdata = userdataNotify.getValue();
				if (userdata==null || userdata.getUserid()==""){
					return;
				}
				userdata.recycle();
			}
		};
		userCache = CacheBuilder.newBuilder()
				.expireAfterAccess(3600, TimeUnit.SECONDS)
				.removalListener(removalListener)
				.build();
		
		disableMsgUserCache = CacheBuilder.newBuilder()
				.expireAfterWrite(30, TimeUnit.MINUTES)
				.build();
		kickOffUserCache = CacheBuilder.newBuilder()
				.expireAfterWrite(120, TimeUnit.MINUTES)
				.build();

		userSendMsgCache = CacheBuilder.newBuilder()
				.expireAfterWrite(120, TimeUnit.MINUTES)
				.build();

		for(int i=0; i< LOCK_NUM; i++){
			roomLocks.add(new ReentrantLock());
		}
	}
	private static Lock getRoomLock(String sessionid){
		int id = 0;
		try{
			id = Integer.parseInt(sessionid);
		}catch(NumberFormatException e){}
		int index = id % LOCK_NUM;
		return roomLocks.get(index);
	}
	/* ------------------------------------------------------------------------------------ */
	public static int getRoomCount() {
		return roomCache.size();
	}
	public static int getUserCount() {
		return (int) userCache.size();
	}
	
	public static RoomData getRoomBySessionid(String sessionid){
		if (sessionid==null){
			return null;
		}	
		RoomData roomdata = roomCache.get(sessionid);
		return roomdata;
	}
	
	public static RoomData getRoomByOwnerid(String ownerid){
		if (ownerid==null){
			return null;
		}
		RoomData roomdata = null;
		String sessionid = RedisHelper.getSessionidByAnchorid(ownerid);
		if( !Strings.isNullOrEmpty( sessionid) ){
			roomdata = getRoomBySessionid(sessionid);
			if( roomdata == null ){
				roomdata = new RoomData( ownerid , sessionid );
				roomCache.put(sessionid, roomdata);
				//统计
				ChangbaMetrics.roomCount.inc();
			}
		}
		return roomdata;
	}
	//这个函数似乎无论如何也不能完全确保数据的线程安全. 因为其他线程可能先调用getRoom()，然后使用这个返回的引用
	public static int validateRoom(RoomData room){
		long now = new Date().getTime();
		if( room.isOwnerJoined() && room.isLive() && now - room.timestamp > CLEANUP_ROOM_TIMEOUT){
			//房间是开播状态，但是主播已经长时间没有连接websocket
//			APIManager.onfinishmic(room.userid,room.getRoomSession());  // 关闭ws端检测，server端已经有检测
			return 0;
		}
		if ( !room.isLive() && room.isEmpty()){
			RoomMessageQueue.removeMessageQueue(room);
			roomCache.remove(room.sessionid);
			ChatCount.remove(room.sessionid);
			ChangbaMetrics.roomCount.dec();
			return 1;
		}
		return 0;
	}
	public static UserData getUserData(String userid){
		if(!Strings.isNullOrEmpty(userid)){
			UserData userdata = userCache.getIfPresent(userid);
			return userdata;
		}else{
			return null;
		}
	}
	public static void removeUser(String userid) {
		userCache.invalidate(userid);
	}
	public static UserInfo getUserInfo(String userid){
		//保证能获得用户的信息，除非redis中也没有
		if( userid == null || userid.equals("")){
			return null;
		}
		UserInfo userinfo;
		UserData userdata = userCache.getIfPresent(userid);
		if( userdata == null){
			//从redis取
			userinfo = RedisHelper.getUserInfo(userid);
			if( userinfo != null){
				userdata = new UserData(userinfo,true);
				userCache.put(userid, userdata);
			}
		}else{
			userinfo = userdata.getUserInfo();
		}
		return userinfo;
	}
	
	public static boolean addUserData(String userid, UserData userdata){
		UserData old = getUserData(userid);
		if (old!=null && old.ctx!=null){
			if( userdata.version.equals("0.0.1") )
			{
				return false;
			}
			//如果该用户之前在别的房间，断开连接就行了。不用管假不假人
			old.exitSession();
		}
		userCache.put(userid, userdata);
		//统计
		ChangbaMetrics.userCount.inc();
		if (userid.length() == 9) {
			ChangbaMetrics.cbUserCount.inc();
		}
		return true;
	}
	
	public static void addUserInfo(String userid, UserInfo userinfo){
		if( userid != null && !userid.equals("") && userinfo != null){
			
		}
	}
	
	//不知道有没有同步问题
	public static Iterator<UserData> getAllUserIterator(){
		return userCache.asMap().values().iterator();
	}
	
	public static Iterator<Entry<String,RoomData>> getAllRoomIterator(){
		return roomCache.entrySet().iterator();
	}
	public static void addDisableMsgUser(String userid, String roomid) {
		if (Strings.isNullOrEmpty(userid) || Strings.isNullOrEmpty(roomid))
			return;
		String key = userid + "@" + roomid;
		disableMsgUserCache.put(key, new DisableMsgUserData(userid, roomid));
	}
	public static boolean isDisableMsgUser(String userid, String roomid) {
		return disableMsgUserTimeStamp(userid, roomid) != 0;
	}
	public static long disableMsgUserTimeStamp(String userid, String roomid) {
		if (Strings.isNullOrEmpty(userid) || Strings.isNullOrEmpty(roomid))
			return 0;
		String key = userid + "@" + roomid;
		DisableMsgUserData value = disableMsgUserCache.getIfPresent(key);
		if (value != null) {
			DisableMsgUserData tmp = new DisableMsgUserData(userid, roomid);
			if (Objects.equals(tmp, value)) {
				return value.getTimestamp();
			} else {
				return 0;
			}
		}
		return 0;
	}

	//zhangming 2016-08-03 记录本次发言时间
	public static void addLastSendMsgTimeStamp(String userid, String roomid){
		if (Strings.isNullOrEmpty(userid) || Strings.isNullOrEmpty(roomid))
		{
			return;
		}
		String key = userid + "@" + roomid;
		userSendMsgCache.put(key, new UserSendMsgData(userid,roomid));

	}
	//zhangming 2016-08-03 取得上次发言时间
	public static long getLastSendMsgTimeStamp(String userid, String roomid){
		if (Strings.isNullOrEmpty(userid) || Strings.isNullOrEmpty(roomid))
		{
			return 0;
		}
		String key = userid + "@" + roomid;
		UserSendMsgData  value = userSendMsgCache.getIfPresent(key);

		if (value != null) {
			UserSendMsgData tmp = new UserSendMsgData(userid, roomid);
			if (Objects.equals(tmp, value)) {
				return value.getTimestamp();
			} else {
				return 0;
			}
		}
		return 0;
	}
	
	public static Iterator<Entry<String, DisableMsgUserData>> getDisableMsgUserIterator() {
		return disableMsgUserCache.asMap().entrySet().iterator();
	}
	// 这个接口只通过后台admin调用，用于解封一个用户
	public static void removeDisableMsgUser(String userid, String roomid) {
		if (Strings.isNullOrEmpty(userid) || Strings.isNullOrEmpty(roomid))
			return ;
		String key = userid + "@" + roomid;
		disableMsgUserCache.invalidate(key);
	}
	
	public static void addKickOffUser(String userid, String ownerid) {
		if (Strings.isNullOrEmpty(userid) || Strings.isNullOrEmpty(ownerid))
			return;
		String key = userid + "@" + ownerid;
		kickOffUserCache.put(key, new KickOffUserData(userid, ownerid));
	}
	public static boolean isKickOffUser(String userid, String ownerid) {
		return kickOffUserTimeStamp(userid, ownerid) != 0;
	}
	public static long kickOffUserTimeStamp(String userid, String ownerid) {
		if (Strings.isNullOrEmpty(userid) || Strings.isNullOrEmpty(ownerid))
			return 0;
		String key = userid + "@" + ownerid;
		KickOffUserData value = kickOffUserCache.getIfPresent(key);
		if (value != null) {
			KickOffUserData tmp = new KickOffUserData(userid, ownerid);
			if (Objects.equals(tmp, value)) {
				return value.getTimestamp();
			} else {
				return 0;
			}
		}
		return 0;
	}
	
	public static Iterator<Entry<String, KickOffUserData>> getKickOffUserIterator() {
		return kickOffUserCache.asMap().entrySet().iterator();
	}
	
	// 这个接口只通过后台admin调用，用于解封一个被踢用户
	public static void removeKickOffUser(String userid, String roomid) {
		if (Strings.isNullOrEmpty(userid) || Strings.isNullOrEmpty(roomid))
			return ;
		String key = userid + "@" + roomid;
		kickOffUserCache.invalidate(key);
	}
	
}
