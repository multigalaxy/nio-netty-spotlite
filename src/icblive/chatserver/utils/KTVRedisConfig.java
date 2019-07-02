/**
 * 
 */
package icblive.chatserver.utils;

import redis.clients.jedis.JedisPoolConfig;

/**
 * @author jgao
 *
 */
public class KTVRedisConfig {
	
	public static JedisPoolConfig getJedisPoolConfig(){
		
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxActive(20);
		config.setMaxIdle(20);
		config.setMaxWait(1000 * 3);
		config.setTestOnBorrow(true);
		config.setTestOnReturn(true);
		config.setTestWhileIdle(true);
		config.timeBetweenEvictionRunsMillis= 10000;
		config.minEvictableIdleTimeMillis = 20000;
		config.numTestsPerEvictionRun=-1;
		
		return config;
	}

}
