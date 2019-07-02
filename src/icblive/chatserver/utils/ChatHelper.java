package icblive.chatserver.utils;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.alibaba.fastjson.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import icblive.chatserver.WebSocketMessageHandler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


public class ChatHelper {
	public static JedisPool rdspool;
	public static final int CHAT_DB = 0;
	public static final int PUBLICCHAT_CONTENT_LENGTH = 100;
	private static final String LIVEROOM_KEY_PREFIX = "live:chatroom:key:";
	private static final String LIVEROOM_GUEST_KEY_PREFIX = "live:chatroom:guest:key:";
	private static final String LIVEROOM_UID_ROOM_PREFIX = "live:chatroom:uid:";
	private static String LIVEROOM_STATS= "live:chatroom:stats:";
	private static String GIFT_RUN_WAY_KEY = "curtoprunway";
	private static String SYS_NOTICE_KEY = "sysnotice";

	private static Set<String> forbiddenWords = new HashSet<String>();
	
	private static Set<String> emojilist = new HashSet<String>();
	private static Set<Character> emoji_prefix = new HashSet<Character>();
	
	public static Random random = new Random();
	public static int largeroom_roomid = 0 ;//这个房间需要做限制
	public static int largeroom_publicchat_simpleechomod = 1 ;//这么多分之1的公聊能通过，其他简单echo给发送者
	public static int largeroom_publicchat_partreceivemod = 1 ;//这么多分之1的人能收到公聊
	public static String BEHAVIOR_GAG_DADYS[] = {"0", "1", "2", "3", "7", "30"};  // 0是永久禁言[0, 1, 3, 7, 30]

	private static final Timer updateTimer = new Timer(true);
	//private static long forbiddenwordfile_time = 0 ;
	//private static ArrayList forbiddenwords = new ArrayList();
	private static Logger chatlogger = LogManager.getLogger( "chathelper");
	
	static {				
		updateTimer.schedule( new TimerTask() {
			@Override
			public void run() {//统计数据
				LoadEmojiFile() ;
				LoadForbiddenFile();
			}
		}, 0, 60*1000);

	}
	public static void LoadEmojiFile(){
		String str = null ;
		Set<String> tmpemojilist = new HashSet<String>();
		Set<String> tmp = UtilFunc.getFileLines( ChangbaConfig.getConfig().emoji_file ) ;
		Iterator<String> it = tmp.iterator() ;
		while( it.hasNext() ){
			  str = it.next() ;
			  if( str.charAt(0) == '#'){
				  continue ;
			  }
			  tmpemojilist.add( unescapeJava(str) ) ;
			  emoji_prefix.add( unescapeJava(str).charAt(0) ) ;
		}

		chatlogger.debug("finish LoadEmojiFile, oldsize:"+emojilist.size() + " newsize:" + tmpemojilist.size() ) ;
		emojilist = tmpemojilist ;//使用新的
	}
	public static void LoadForbiddenFile(){
		Set<String> tmpwords = UtilFunc.getFileLines( ChangbaConfig.getConfig().forbbidenword_file ) ;
		chatlogger.debug("finish LoadForbiddenFile, oldsize:"+forbiddenWords.size() + " newsize:" + tmpwords.size() ) ;
		tmpwords.remove("");  // 移除空串
		tmpwords.remove(null);
		forbiddenWords = tmpwords ;//使用新的
	}
	
	public static String unescapeJava(String escaped) {
	    if(escaped.indexOf("\\u")==-1)
	        return escaped;

	    String processed="";

	    int position=escaped.indexOf("\\u");
	    while(position!=-1) {
	        if(position!=0)
	            processed+=escaped.substring(0,position);
	        String token=escaped.substring(position+2,position+6);
	        escaped=escaped.substring(position+6);
	        processed+=(char)Integer.parseInt(token,16);
	        position=escaped.indexOf("\\u");
	    }
	    processed+=escaped;

	    return processed;
	}

	
	public static String makeReplyFrame_ori( String msg ) {
		//System.out.println("frist: "+msg) ;
		//msg = removeUnreadableChar(msg) ;
		//if( msg.indexOf("publicchat") > 0){
			/*msg = "{\"headphoto\":\"http:\\/\\/aliimg.changba.com\\/cache\\/photo\\/383944290_100_100.jpg\"" +
",\"msg_body\":\"\\u0675\\u0674\\u0673\\u82B1\\u82B1\\u8D70\\u8D77\\u6765\\uD83C\\uDF39\\uD83C\\uDF39\\uD83C\\uDF39\\uD83C\\uDF39\\u0672\\u0671\\u0670 \\u0957 \\u0957 \\u0957 \\u0957\"" +
					",\"msg_type\":\"0\",\"nickname\":\"kulv2012a\",\"richlevel\":0,\"role\":\"admin\",\"room_id\":\"184867\",\"target_nickname\":\"\",\"target_richlevel\":0,\"target_userid\":\"\",\"type\":\"publicchat\",\"userid\":\"28946468\"}";
		*/
		//",\"msg_body\":\"\\u067F\\u067E\\u067D\\u0306\\u0306\\u067C\\u067B\\u067A\\u0679\\u0678\\u0677\\u0676\\u0306\\u0675\\u0674\\u0673\\u82B1\\u82B1\\u8D70\\u8D77\\u6765\\uD83C\\uDF39\\uD83C\\uDF39\\uD83C\\uDF39\\uD83C\\uDF39\\u0672\\u0671\\u0670 \\u0957 \\u0957 \\u0957 \\u0957\"" +

			//msg = msg.replaceAll("[^0-9a-zA-Z\u4e00-\u9fa5.，,。？“”]+\",_\\:{}","") ;
		//}
		//System.out.println("makeReplyFrame_ori: "+msg+"\n") ;
		msg = msg.replaceAll("\u0000|\uffff", "") ;
		return "{\"result\":[" + msg + "]}"  ;
	}
	public static byte[] makeReplyFrame_gzip( String msg ) {
		byte[] gzipbytes = GzipUtils.encode (makeReplyFrame_ori(msg) );
		
		return gzipbytes ;
	}
	public static String FilterChatContent( String source){        
        /*System.out.println("frist: "+source+"\n") ;
        //source = source.replaceAll("[^0-9a-zA-Z\u4e00-\u9fa5.，,。？“”,_\\/:{}\n\"]+","") ;
        Iterator<String> it = emojilist.iterator() ;
		while( it.hasNext() ){
			System.out.println("Iterator emojilist:" + JSON.toJSONString(it.next() , SerializerFeature.BrowserCompatible) + " char1:"+ 
					JSON.toJSONString(emojilist.contains( ""+source.charAt(0) ), SerializerFeature.BrowserCompatible)  ) ;
		}
			
		String replyMsg = JSON.toJSONString(source, SerializerFeature.BrowserCompatible);
		System.out.println("toJSONString: "+replyMsg+ "----" + emojilist.contains(source.charAt(0))) ;
		*/
		
        StringBuilder buf = new StringBuilder(source.length());
        int len = source.length();
        for (int i = 0; i < len; i++) {
            char codePoint = source.charAt(i);
            if ( ( '0' <= codePoint && codePoint <= '9')
            		|| ( 'a' <= codePoint && codePoint <= 'Z')
            		|| ( ' ' <= codePoint && codePoint <= '~')
            		|| ( '\u4e00' <= codePoint && codePoint <= '\u9fa5')
            		|| ( emojilist.contains(""+codePoint) ) 
            ){
            	//System.out.println("ok:"+i+" ["+codePoint+"]" + emojilist.contains(codePoint)+"--");
                buf.append(codePoint);
            }else if( emoji_prefix.contains(codePoint) ) {
            	if( source.length() >= i+2 && emojilist.contains(source.substring( i, i+2 ))  ){
	            	//System.out.println("find i+2:"+source.substring( i, i+2 ) ) ;
	            	buf.append(source.substring( i, i+2 ));
	            	i = i +1 ;
	            }else if( source.length() >= i+3 && emojilist.contains(source.substring( i, i+3 ))  ){
	            	//System.out.println("find i+3:"+source.substring( i, i+3 ) ) ;
	            	buf.append(source.substring( i, i+3 ));
	            	i = i +2 ;
	            }else if(source.length() >= i+4 &&  emojilist.contains(source.substring( i, i+4 ))  ){
	            	//System.out.println("find i+4:"+source.substring( i, i+4 ) ) ;
	            	buf.append(source.substring( i, i+4 ));
	            	i = i +3 ;
	            } 
            }else {
	            	//System.out.println("--no:"+i+" ["+codePoint+"]" );
	        }
        }
 
        if (buf.length() == 0 ) {
            return "";//如果没有找到 emoji表情，则返回源字符串
        } else {
            if (buf.length() == len) {//这里的意义在于尽可能少的toString，因为会重新生成字符串
                buf = null;
               // System.out.println("buf.length() == len: "+source+"\n") ;
                return source;
            } else {
            	//System.out.println("else : "+ buf.toString() +"\n") ;
                return buf.toString();
            }
        }
	}
	
	public static String removeUnreadableChar(String str){
        if(str == null || str.equals("") ){
                return "";
        }
        StringBuilder appender = new StringBuilder();
        for (int i=0;i<str.length();i++){
                char c =  str.charAt(i);
                int ch = (int)c;
                if (ch < 0x20 && (ch != 0x9) && (ch != 0xA) && (ch != 0xD)){ 
                	continue;
                }
                appender.append(c);  
        }
        return appender.toString();
	}
	public static boolean hasForbiddenWord(String orgMsg) {
		if (orgMsg == null) {
			return false;
		}
		String filteredString = orgMsg.replaceAll("\\s", "");
		filteredString = filteredString.toLowerCase() ;
		for (String word : forbiddenWords) {
			if (filteredString.contains(word) && !word.equals("")) {
//				chatlogger.info("sensitive: filteredString=" + filteredString + ", word=" + word + ", words=" + JSON.toJSONString(forbiddenWords));
				return true;
			}
		}
		return false;
	}
	
	public static boolean needSimpleEchoPublicChat( String room_id, String userid ) {
		if( Integer.parseInt( room_id) == largeroom_roomid  ){
			int randnum = random.nextInt();
			if( randnum % largeroom_publicchat_simpleechomod != 0 ){
				return true ;
			}else {
				return false ;//只有取摸相等的消息，才能发出去
			}
		}else {//不是需要监控的房间
			return false ;
		}
	}
	public static boolean needPartRecivePublicChatRandom( String room_id, String userid ) {
		if( Integer.parseInt( room_id) == largeroom_roomid  ){
			int randnum = random.nextInt();
			if( randnum % largeroom_publicchat_partreceivemod  != 0 ){
				return true ;
			}else {
				return false ;//只有取摸相等的消息，才能发出去
			}
		}else {//不是需要监控的房间
			return false ;
		}
	}
	public static boolean setLargeRoomConfig(String roomid, String simpleechomod, String partreceivemod ) {
		largeroom_roomid = Integer.parseInt( roomid) ;
		largeroom_publicchat_simpleechomod = Integer.parseInt( simpleechomod ) ;
		largeroom_publicchat_partreceivemod = Integer.parseInt( partreceivemod) ;
		chatlogger.info("setLargeRoomConfig largeroom_roomid:" + largeroom_roomid+ " largeroom_publicchat_simpleechomod:" + largeroom_publicchat_simpleechomod + " largeroom_publicchat_partreceivemod:"+largeroom_publicchat_partreceivemod );
		return true;
	}

	// 用户是否被禁言
	public static boolean isUserGaged(String userid) {
		return RedisHelper.getUserGagInfo(userid);
	}
}
