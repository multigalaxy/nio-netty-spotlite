/**
 * 
 */
package icblive.chatserver.api;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;

import icblive.chatserver.model.json.NoticeMessage;
import icblive.chatserver.model.json.ResponseMessage.ResponseEntry;
import icblive.chatserver.utils.ChangbaConfig;
import icblive.chatserver.utils.KTVRedisConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * @author jgao
 *
 */
public class InternalMsgHandler implements Runnable{

	private static Logger logger = LogManager.getLogger(InternalMsgHandler.class.getName());

	private static JedisPoolConfig config = KTVRedisConfig.getJedisPoolConfig();
	
	private static JedisPool pool = new JedisPool(config, ChangbaConfig.getConfig().main_redis_ip, ChangbaConfig.getConfig().main_redis_port);
	
	private static String SYS_NOTICE_KEY = "sysnotice";

	public static final String SUB_QUEUE_NAME = ChangbaConfig.getConfig().rediskey_brodcastmsg;
	public static final String SUB_QUEUE_NAME2 = SUB_QUEUE_NAME +":"+ ChangbaConfig.getConfig().ws_ip+":"+ChangbaConfig.getConfig().ws_port;
	@Override
	public void run() {
		try{
			initVaribleFromRedis();
		}catch(Exception e){
			logger.error(e);
		}
		
		int retries = 0;
		while(true){
			
			logger.error("InternalMsgWorker: start: "+ ( ++retries) +" times.");
			
			Jedis redis = null; 
			try{
				redis = pool.getResource();
				redis.subscribe(new JedisPubSub() {
					@Override
					public void onUnsubscribe(String channel, int subscribedChannels) {
						logger.info("onUnsubscribe");
					}

					@Override
					public void onSubscribe(String channel, int subscribedChannels) {
						logger.info("onSubscribe");
					}

					@Override
					public void onPUnsubscribe(String pattern, int subscribedChannels) {
					}

					@Override
					public void onPSubscribe(String pattern, int subscribedChannels) {
					}

					@Override
					public void onPMessage(String pattern, String channel, String message) {
					}

					@Override
					public void onMessage(String channel, String message) {
						String msg = message;
						logger.info(msg);
						if (Strings.isNullOrEmpty(msg)){
							return;
						}
						List<ResponseEntry> entries = JSON.parseArray(msg, ResponseEntry.class);
						for (ResponseEntry entry: entries ){
							APICallbackHandler callback = new APICallbackHandler(null);
							callback.dealWithResponseEntry(entry);
						}
					}
				}, new String[]{SUB_QUEUE_NAME,SUB_QUEUE_NAME2});
			
			}catch( JedisConnectionException e){
				if (redis!=null){
					pool.returnBrokenResource(redis);
					redis = null;
				}
			}catch(Exception e){
				logger.error("InternalMsg:" , e);
			}finally {	
				if (redis!=null){
					pool.returnResource(redis);
				}
				if (retries > 10){
					try {
						TimeUnit.SECONDS.sleep(1);
					} catch (InterruptedException e) {}
				}
			}
		}
	}
	//从redis中获取初始的系统消息
	private void initVaribleFromRedis(){
		Jedis redis = null;
		try {
			redis = pool.getResource();
			Set<String> noticesFromRedis = redis.zrange(SYS_NOTICE_KEY , 0, -1);
			for (String notice : noticesFromRedis) {
				logger.info("init noticesFromRedis: " + notice);
				NoticeMessage entry = JSON.parseObject(notice, NoticeMessage.class);
				SystemMessageManager.addNoticeMessage(entry);
			}
		} catch( JedisConnectionException e){
			logger.error("InternalMsg:" , e);
			if (redis!=null){
				pool.returnBrokenResource(redis);
				redis = null;
			}
		}catch(Exception e){
			logger.error("InternalMsg:" , e);
		}finally {	
			if (redis!=null){
				pool.returnResource(redis);
			}
		}
	}
	
	public static void  main(String s[]){
		new Thread(new InternalMsgHandler()).start();
	}
}
