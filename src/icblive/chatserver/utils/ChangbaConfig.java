package icblive.chatserver.utils;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
//import com.sun.swing.internal.plaf.synth.resources.synth;

import icblive.chatserver.service.LiveChatServer2;



/**
 * @author xiaol
 *
 */
public class ChangbaConfig {
	public String token_redis_ip = "127.0.0.1";
	public int token_redis_port = 6379; 
	public String main_redis_ip = "127.0.0.1";
	public int main_redis_port = 6379; 
	public String ws_ip = "";//自动生成
	public int ws_port = 8086; 
	public String ws_server_name = "";
	public String liveapi_url ;  // https://api.spotliteapi.com/liveapi/
	public String liveapi_debug;  // kulv201315
	public String forbbidenword_file = "forbiddenwords.txt";
	public String emoji_file = "emoji_unicode.data";
	public int bencheventpoolsize = 10 ;
	public int max_concurrent_http_conn = 48 ;
	public String rediskey_brodcastmsg = "icblivebrodcastmsg" ;
    // max_msg_per_second这个数至少要大于max_chat_per_second这个
	public int max_chat_per_second = 6;
	public int max_msg_per_second = 25;
	public String graphite_host = "192.168.32.103";
	public int sendmsgthreads = Runtime.getRuntime().availableProcessors()*2;
	
	private static Logger logger = LogManager.getLogger(LiveChatServer2.class.getName());
	
	private static ChangbaConfig config;
	
	
	public ChangbaConfig(){
		try {
			InetAddress addr = InetAddress.getLocalHost();
			this.ws_ip = addr.getHostAddress().toString();
			this.ws_server_name = addr.getHostName().toString();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public  synchronized static ChangbaConfig getConfig(){
		if (config==null){
			config = new ChangbaConfig();
			File file = new File("./config.json");
			if (file.exists()){
				logger.info("Load config from file: "+file.getAbsolutePath());
				try {
					config = JSON.parseObject(Files.toString(file, Charset.forName("UTF-8")), ChangbaConfig.class);
	
				} catch (JsonSyntaxException e) {
					logger.error("Load config error.  "+e);
				} catch (IOException e) {
					logger.error("Load config error.  "+e);
				}
			}else{
				logger.info("no config file found, use default settings.");
			}
			logger.info("Load config is :\n" + new Gson().toJson(config));
		}
		return config;

	}
}
