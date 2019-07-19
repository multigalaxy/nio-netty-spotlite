/**
 * 
 */
package icblive.chatserver.api;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;

import icblive.chatserver.data.LiveCache;
import icblive.chatserver.model.UserData;
import icblive.chatserver.utils.ChangbaConfig;
import icblive.chatserver.utils.ChangbaMetrics;
import io.dropwizard.metrics.Meter;
import io.dropwizard.metrics.Timer;

/**
 * @author xiaol
 *
 */
public class APIManager {
	private static HttpClient _httpClient;
	private static final ListeningExecutorService exe = MoreExecutors.listeningDecorator(
			Executors.newCachedThreadPool());
	private static final String API_PREFIX = ChangbaConfig.getConfig().liveapi_url ;
	private static final String API_DEBUG = ChangbaConfig.getConfig().liveapi_debug;
	private static Logger logger = LogManager.getLogger(APIManager.class);
	
	public static HttpClient getDefaultHttpClient(){
		if (_httpClient==null){
			_httpClient = initHttpClient();
		}
		return _httpClient;
	}

	 private  static HttpClient initHttpClient(){
		PoolingHttpClientConnectionManager httpConnPoolMngr = new PoolingHttpClientConnectionManager() ;
		httpConnPoolMngr.setMaxTotal(500) ;
		httpConnPoolMngr.setDefaultMaxPerRoute(ChangbaConfig.getConfig().max_concurrent_http_conn);
		
		RequestConfig requestConfitg = RequestConfig.custom().setSocketTimeout(5000).setConnectTimeout(5000).build();
		_httpClient = HttpClients.custom()
				.setDefaultRequestConfig(requestConfitg)
				.setConnectionManager(httpConnPoolMngr)
				.build();
		return _httpClient;
	 }
	 
	 private static String getUrlParams(String proxyJsonString, UserData curUser){
		JSONObject jsonObj = JSON.parseObject(proxyJsonString);
		if (curUser.userinfo != null && !Strings.isNullOrEmpty(curUser.getUserid())) {
			jsonObj.put("userid", curUser.getUserid());
			jsonObj.put("sessionid", curUser.sessionid);
		}
		String json;
		try {
			json = URLEncoder.encode(new Gson().toJson(jsonObj), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			json = "";
		}
		return "&jsonmsg=" + json;
	 }
	 
	 private static ListenableFuture<String> executeRequest(final String type, final String apiUrl){
		 ListenableFuture<String> f = exe.submit(new Callable<String>() {
				@Override
				public String call() throws Exception {
					return _executeRequest(type,apiUrl);
				}
			});
		 return f;
	 }
	 private static String _executeRequest(final String type, final String apiUrl) throws Exception{
		Timer.Context t = ChangbaMetrics.getTimer(type).time();
		
		String text = "";
		try{
			HttpGet request = new HttpGet(apiUrl);
			HttpClient httpClient = getDefaultHttpClient();
			HttpResponse response = httpClient.execute(request);
			if (response.getStatusLine().hashCode()>400){
				//throw new RuntimeException();
			}
			text = EntityUtils.toString(response.getEntity(), Charset.forName("UTF-8"));
		}catch(Exception e){
			throw e;
		}finally {
			long apicost = t.stop();
			logger.info("API url:  "+ apiUrl + " text:"+ text +" cost:" + apicost/1000000 + "ms");
		}
		
		return text;
				
	 }
	 /* ------------------------------------------------------------------------------------ */
	 
	 //同步执行
	 public static String wsmessagedispachAync(String type, String proxyJsonMessage, UserData curUser){
		 String url = API_PREFIX  + "&ac=wsmessagedispach";
		 final String apiUrl = url + getUrlParams(proxyJsonMessage, curUser);
		 String result = "";
		 try{
			 result = _executeRequest(type,apiUrl);
		 }catch(Exception e){
			 logger.error("API error:",e);
		 }
		 return result;
	 }
	
	 /* proxyJsonString 是转发客户端传来的json字符串, 由于接口设计的原因, 无法解析这个json */
	 public static void wsmessagedispach(String type, String proxyJsonMessage, UserData curUser, FutureCallback<String> callback) {
		ChangbaMetrics.incrMeter("all_api_request");
		/*
		 * TODO
		 * 这里应该做一个压力检测，如果一分钟内的压力请求频率超过压力应该直接返回给客户端错误
		 */
		String url = API_PREFIX  + "&ac=wsmessagedispach";
		
		final String apiUrl = url + getUrlParams(proxyJsonMessage, curUser);
		ListenableFuture<String> f = executeRequest(type, apiUrl);
		if (callback!=null){
			Futures.addCallback(f, callback);
		}
	}
	public static void onfinishmic(String userid,String sessionid){
		String url = API_PREFIX + "endmylive?";
		final String apiUrl = url +"sessionid="+sessionid+"&curuserid="+userid + "&debug=" + API_DEBUG + "&client_from_origin=ws_server";
		logger.debug("finishmic" + apiUrl);
		executeRequest("onfinishmic",apiUrl);
	}
	
}
