/**
 * 
 */
package icblive.chatserver.utils;

import redis.clients.jedis.JedisPoolConfig;

/**
 * @author xiaol
 *
 */
public class KTVRedisConfig {
	
	public static JedisPoolConfig getJedisPoolConfig(){
		
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxActive(20);  // 分配最多20个实例
		config.setMaxIdle(20);  // 分配最多20个空闲实例，超过回收
		config.setMaxWait(1000 * 3);  // 使用一个实例连接时，最大等待时间，3秒
		config.setTestOnBorrow(true);  // 是否提前确定实例连接可用
		config.setTestOnReturn(true);  // 是否检测连接返回时成功
		config.setTestWhileIdle(true);
		config.timeBetweenEvictionRunsMillis= 10000;
		config.minEvictableIdleTimeMillis = 20000;
		config.numTestsPerEvictionRun=-1;
		
		return config;
	}

}
