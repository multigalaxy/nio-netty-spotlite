package icblive.chatserver.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import icblive.chatserver.model.json.UserMedalInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import icblive.chatserver.data.LiveCache;
import icblive.chatserver.model.json.AudienceListMessage;
import icblive.chatserver.model.json.UserInfo;
import icblive.chatserver.model.json.AudienceListMessage.AudienceInfo;
import redis.clients.jedis.*;

/**
 * @author maxingxian
 * 
 */
public class RedisHelper {

	private static final String ICB_SESSION_AUDIENCE_LIST = "icb:live:prefix:uidlist";
	private static final String ICB_USERINFO_PREFIX = "userinfo:uid:";
	private static final String ICB_SESSIONID_USER_LIVING = "icb:sessionid:user:living:";
	private static final String ICB_SESSIONINFO = "icb:sessioninfo:";
	private static final String ICB_LIVE_SYSTEM_MSG = "icb:live:system:msg:";
	private static final String	ICB_USER_MEDAL_PREFIX = "medalresult:userid:";  // 勋章
	private static final String BEHAVIOR_GAG_LIST_PREFIX = "behavior_gag_list_";  // 禁言列表：behavior_gag_list_0是永久封号，1, 3, 7, 30

	public static JedisPool pool;
	private static final int redisPort = ChangbaConfig.getConfig().main_redis_port;
	public static final String redisAddress = ChangbaConfig.getConfig().main_redis_ip;
	private static Logger logger = LogManager.getLogger(RedisHelper.class.getName());

	static {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxActive(500);
		config.setMaxIdle(5);
		config.setMaxWait(1000 * 10);
		config.maxActive = 500;
		config.testWhileIdle = true;
		config.timeBetweenEvictionRunsMillis = 3000;
		pool = new JedisPool(config, redisAddress, redisPort);
	}

	public RedisHelper() {
	}

	public static boolean addUserToSession(String userid, String sessionid) {
		if (Strings.isNullOrEmpty(userid) || Strings.isNullOrEmpty(sessionid)) {
			return false;
		}
		int res = 0;
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			if (!jedis.isConnected()) {
				jedis.connect();
			}
			jedis.zadd(ICB_SESSION_AUDIENCE_LIST + sessionid, (int) System.currentTimeMillis() / 1000, userid);
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			logger.error("main_redis 挂了," + e.getMessage());
		} finally {
			if (jedis != null) {
				pool.returnResource(jedis);
			}
		}
		logger.debug("addUserToSession: uid:" + userid + " sessionid: " + sessionid + " res: " + res);
		return res == 1;
	}

	public static String getSessionidByAnchorid(String ownerid) {
		String res = "";
		if (Strings.isNullOrEmpty(ownerid)) {
			return "";
		}
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			if (!jedis.isConnected()) {
				jedis.connect();
			}
			res = jedis.get(ICB_SESSIONID_USER_LIVING + ownerid);
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			logger.error("main_redis 挂了" + e.getMessage());
		} finally {
			if (jedis != null) {
				pool.returnResource(jedis);
			}
		}
		logger.debug("getSessionidByAnchorid: onwerid: " + ownerid + " res: " + res);
		return res;
	}

	public static boolean removeUserfromSession(String userid, String sessionid) {
		if (Strings.isNullOrEmpty(userid) || Strings.isNullOrEmpty(sessionid)) {
			return false;
		}
		Jedis jedis = null;
		long res = 0;
		try {
			jedis = pool.getResource();
			if (!jedis.isConnected()) {
				jedis.connect();
			}
			res = jedis.zrem(ICB_SESSION_AUDIENCE_LIST + sessionid, userid);
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			logger.error("main_redis 挂了" + e.getMessage());
		} finally {
			if (jedis != null) {
				pool.returnResource(jedis);
			}
		}
		logger.debug("removeUserfromSession: uid:" + userid + " sessionid: " + sessionid + " res: " + res);
		return res == 1;
	}

	public static AudienceListMessage joinSession(String userid, String sessionid, boolean isowner) {
		logger.debug("joinSession: uid:" + userid + " sessionid: " + sessionid);
		AudienceListMessage joinSessionReplyMsg = new AudienceListMessage();
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			if (!jedis.isConnected()) {
				jedis.connect();
			}
			Pipeline p = jedis.pipelined();
			if (isowner != true) {
				p.zadd(ICB_SESSION_AUDIENCE_LIST + sessionid,(int) (System.currentTimeMillis() / 1000), userid);
			}
			p.hincrBy(ICB_SESSIONINFO + sessionid, "usercnt", 1);
			p.sync();
			joinSessionReplyMsg = getAudienceList(userid, sessionid, 0, 10);
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			logger.error(" main_redis 挂了" + e.getMessage());
		} finally {
			if (jedis != null) {
				pool.returnResource(jedis);
			}
		}
		return joinSessionReplyMsg;
	}

	public static AudienceListMessage getAudienceList(String userid, String sessionid, int start, int count) {
		if (Strings.isNullOrEmpty(sessionid) || Strings.isNullOrEmpty(userid)) {
			return null;
		}
		start = start < 500 ? start : 0;
		count = count < 200 ? count : 100;
		AudienceListMessage audienceListMsg = new AudienceListMessage();
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			if (!jedis.isConnected()) {
				jedis.connect();
			}
			Pipeline p = jedis.pipelined();
			Response<Set<String>> list = p.zrevrange(ICB_SESSION_AUDIENCE_LIST + sessionid, start, start + count);
			Response<Long> audiencenum = p.zcard(ICB_SESSION_AUDIENCE_LIST + sessionid);
			p.sync();
			Set<String> audiencelist = list.get();
			audienceListMsg.audiencelist = Lists.newArrayList();
			if (audiencelist != null && !audiencelist.isEmpty()) {
				for (String auid : audiencelist) {
					if (Strings.isNullOrEmpty(auid)) {
						continue;
					}
					UserInfo info = LiveCache.getUserInfo(auid);
					if (info == null) {
						continue;
					}
					audienceListMsg.audiencelist.add(new AudienceInfo(info));
				}
				audienceListMsg.start = start;
				audienceListMsg.audienceamount = audiencenum.get();
			}
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			logger.error(" main_redis 挂了");
			e.printStackTrace();
		} finally {
			if (jedis != null) {
				pool.returnResource(jedis);
			}
		}
		logger.debug("getAudienceList: uid:" + userid + " sessionid: " + sessionid + " start:" + start + " count:"
				+ count + " actnum:" + audienceListMsg.audiencelist.size());
		return audienceListMsg;
	}

	public static UserInfo getUserInfo(String userid) {
		if (Strings.isNullOrEmpty(userid)) {
			return null;
		}
		Jedis jedis = null;
		UserInfo userinfo = null;
		try {
			jedis = pool.getResource();
			if (!jedis.isConnected()) {
				jedis.connect();
			}
			Map<String, String> info = jedis.hgetAll(ICB_USERINFO_PREFIX + userid);
			if (info != null && info.containsKey("userid") && !Strings.isNullOrEmpty(info.get("userid"))) {
				userinfo = new UserInfo(info);
			}
			// 读取勋章
			Map<String, String> medal = jedis.hgetAll(ICB_USER_MEDAL_PREFIX + userid);
			UserMedalInfo userMedalInfo;
			logger.debug("getMedalInfo: uid=" + userid + ", medaltype=" + medal.get("medaltype") + ", isexpire=" + medal.get("isexpire") + ", cost=" + medal.get("cost") + ", is_key=" + medal.containsKey("medaltype"));
			if(userinfo != null && medal != null && medal.containsKey("medaltype")) {
				logger.debug("getMedalInfo: uid=" + userid + ", medal=" + medal.toString());
				userMedalInfo = new UserMedalInfo(medal);
				userinfo.setUserMedalInfo(userMedalInfo);
			}
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			logger.error("main_redis 挂了" + e.toString());
			e.printStackTrace();
		} finally {
			if (jedis != null) {
				pool.returnResource(jedis);
			}
		}
		return userinfo;
	}

	public static List<UserInfo> getUserInfo(Set<String> userids) {
		logger.debug(" getUserInfo: uid:" + userids);
		List<UserInfo> userinfos = Lists.newArrayList();
		Set<String> uids = Sets.newHashSet();
		Jedis jedis = null;
		try {
			for (String id : userids) {
				if (Strings.isNullOrEmpty(id)) {
					continue;
				}
				uids.add(id);
			}
			jedis = pool.getResource();
			if (!jedis.isConnected()) {
				jedis.connect();
			}
			Pipeline p = jedis.pipelined();
			Set<Response<Map<String, String>>> responses = Sets.newHashSet();
			for (String uid : uids) {
				responses.add(p.hgetAll(ICB_USERINFO_PREFIX + uid));
			}
			p.sync();
			for (Response<Map<String, String>> value : responses) {
				Map<String, String> info = value.get();
				if (info != null && info.containsKey("userid") && Integer.parseInt(info.get("userid")) > 0) {
					userinfos.add(new UserInfo(info));
				}
			}
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			logger.error(" main_redis 挂了" + e.getMessage());
		} finally {
			if (jedis != null) {
				pool.returnResource(jedis);
			}
		}
		return userinfos.isEmpty() ? null : userinfos;
	}

	public static String getSessionInfo(String sessionid, String field) {
		String res = "";
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			if (!jedis.isConnected()) {
				jedis.connect();
			}
			res = jedis.hget(ICB_SESSIONINFO + sessionid, field);
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			logger.error(" ERROR: main_redis 挂了");
		} finally {
			if (jedis != null) {
				pool.returnResource(jedis);
			}
		}
		logger.debug( "getSessionInfo"
				+ ": sessionid:"+sessionid+" field: "+field+" value:"+ res);
		return res;
	}

	/**
	 * 获取直播系统消息配置
	 * @param lang en ch es in
	 * @return String
	 */
	public static String getLiveSystemMsg(String lang, String type) {
		String res = "";
		Jedis jedis = null;
		try{
			jedis = pool.getResource();
			if(!jedis.isConnected()) {
				jedis.connect();
			}
			if(type.isEmpty()) {
				type = "annouce";
			}
			res = jedis.hget(ICB_LIVE_SYSTEM_MSG + lang, type);
		}catch (Exception e) {
			pool.returnBrokenResource(jedis);
			logger.error("ERROR: main_rdis 挂了");
		}finally {
			if(jedis != null) {
				pool.returnResource(jedis);
			}
		}
		logger.debug("getLiveSystemMsg" + ": lang:" + lang + " type:" + type);
		return res;
	}


	// 获取用户禁言信息，目前只返回是否被禁言
	public static boolean getUserGagInfo(String userid) {
		List<Response<String>> list = new ArrayList<>();  // redis事务item列表
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			if(!jedis.isConnected()) {
				jedis.connect();
			}
			if(!userid.isEmpty()) {
				Pipeline pipeline = jedis.pipelined();
				for(String i: ChatHelper.BEHAVIOR_GAG_DADYS) {
					list.add(pipeline.hget(BEHAVIOR_GAG_LIST_PREFIX + i, userid));
				}
				pipeline.sync();
				for(Response<String> res: list) {
					String val = res.get();
						if(val != null && !val.isEmpty()) {
						logger.info("user_gag_info real: " + val);
						return true;
					}
				}
			}
		}catch (Exception e) {
			pool.returnBrokenResource(jedis);
			e.printStackTrace();
		}finally {
			if(jedis != null) {
				pool.returnResource(jedis);
			}
		}
		return false;
	}
}
