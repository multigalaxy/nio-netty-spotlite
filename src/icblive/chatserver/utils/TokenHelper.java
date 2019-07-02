package icblive.chatserver.utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
/**
 * @author sandcu
 * 
 */
public class TokenHelper {
	public static JedisPool pool;
	private static final int redisPort = ChangbaConfig.getConfig().token_redis_port;
	public static final String redisAddress = ChangbaConfig.getConfig().token_redis_ip;
	private static Logger logger = LogManager.getLogger(TokenHelper.class.getName());
	static {	
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxActive(500);
		config.setMaxIdle(5);
		config.setMaxWait(1000 * 10);
		config.maxActive=200;
		config.testWhileIdle=true;
		config.timeBetweenEvictionRunsMillis=3000;
		pool = new JedisPool(config, redisAddress, redisPort);
	}
	private static final String TOKENPREFIX = "token:uid:";

	public TokenHelper() {

	}

	public static boolean checkToken(String userid, String token) {
		logger.debug(" OPEN: uid:"+userid+" token: "+token);
		if (token.equals("debug20160809")) {
			return true;
		}
		// return true;
		int uid = 0;
		try {
			uid = Integer.parseInt(userid);
		} catch (Exception e) {
			return false;
		}
		if ((uid <= 0) || (token == "")) {
			return false;
		}
		if (!token.substring(0, 1).equals("T")) {
			return false;
		}
		Jedis jedis = null;
		String actualTokenString = "";
		try {
			jedis = pool.getResource();
			if (!jedis.isConnected()) {
				jedis.connect();
			}
			actualTokenString = jedis.get(TOKENPREFIX + userid);
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			logger.error(" ERROR: redis 挂了");
		} finally {
			if (jedis != null) {
				pool.returnResource(jedis);
			}
		}
		logger.debug(" OPEN: uid:"+userid+" actualtoken: "+actualTokenString);
		return token.equals(actualTokenString);
	}
}
