/**
 * 
 */
package icblive.chatserver.api;

import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import icblive.chatserver.api.MessageUtils;
import icblive.chatserver.model.RoomData;
import icblive.chatserver.model.UserData;
import icblive.chatserver.utils.ChangbaConfig;
import icblive.chatserver.utils.GzipUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.ReferenceCountUtil;

public class RoomMessageQueue {
	private RoomData roomdata;
	private List<String> messages = Lists.newArrayList();
	public static int MAX_MSG_PER_SECOND = 100; //50Mbps
	public static int WS_FLOW_LIMIT = 50; //50Mbps
	public static final int DELAY_MILLISECONDS = 100; //消息最长排队时间 单位 毫秒
	public static final int BEGIN_DELAY_USER_COUNT = 100; //房间人数超过多少时开始delay
	public static final int BEGIN_LIMIT_PUBLICCHAT_USER_COUNT = 500; //房间人数超过多少时开始限制公聊
	public static final int PUBLICCHAT_DELAY_MILLISECONDS = 300; //超过1000公聊延迟1s
	public static final int MAX_LIMIT_PUBLICCHAT_COUNT = ChangbaConfig.getConfig().max_chat_per_second; //最高的房间公聊数
	public  static Map<RoomData,RoomMessageQueue> queueMap = Maps.newConcurrentMap();
	public static final ExecutorService sendmsgexecutor = Executors.newFixedThreadPool(ChangbaConfig.getConfig().sendmsgthreads);
	
	private static Logger logger = LogManager.getLogger(RoomMessageQueue.class);
	
	private Lock msglock = new ReentrantLock();
	private long lastsendtimestamp = 0;
	private Lock issend = new ReentrantLock();
	// 各种类型的消息限制
	private static Map<String, Integer> msgLimit = new Hashtable<String, Integer>();
	// 当前已经积攒的各种类型的消息数量
	private Map<String, Integer> msgTypeCount = new Hashtable<String, Integer>();
	private int NOT_LIMIT_COUNT = 10000;
	
	static {
		// 100毫秒的各种消息的限制数量
		msgLimit.put("publicchat", 1);
		msgLimit.put("freegift", 1);
		msgLimit.put("arrive", 2);
		msgLimit.put("share", 2);
		msgLimit.put("adminlist", 1);
		msgLimit.put("follow", 1);
	}
		private RoomMessageQueue(RoomData roomdata){
			this.roomdata = roomdata;
		}
		public RoomData getRoomData() {
			return this.roomdata;
		}
		
		
		public boolean addMessage(String message, String type) {
			boolean ret = true;
			int delay = roomdata.count() > BEGIN_LIMIT_PUBLICCHAT_USER_COUNT ? PUBLICCHAT_DELAY_MILLISECONDS : DELAY_MILLISECONDS;
			int maxcount = MAX_MSG_PER_SECOND*delay/1000;
			// checksendmessage 检查发送消息的定时器
			if (!Objects.equal(type, "checksendmessage") && !Strings.isNullOrEmpty(message)) {
				// 100毫秒应该不会有任何消息超过10000条
				int limit = msgLimit.get(type) == null ? NOT_LIMIT_COUNT : msgLimit.get(type)*1000/delay;
				this.msglock.lock();
				if (limit == NOT_LIMIT_COUNT) {
					this.messages.add(message);
				} else if (this.messages.size() > maxcount) {
					logger.debug(" reject level maxtotalcount=>>send the message: curcount[" + this.messages.size() + "],msg[" + message + "]");
					ret = false;
				} else {
					int curcount = msgTypeCount.get("type") == null ? 0 : msgTypeCount.get("type");
					if (curcount >= limit) {
						logger.debug(" reject level maxtypecount=>>send the message: curcount[" + curcount + "],msg[" + message + "]");
						ret = false;
					} else {
						msgTypeCount.put(type, curcount + 1);
						this.messages.add(message);
					}
				}
				this.msglock.unlock();
			}
			if (this.messages.isEmpty()) {
				return ret;
			}
			this.issend.lock();
			long curtimestamp = new Date().getTime();
			if (curtimestamp - this.lastsendtimestamp >= delay) {
				// 对于变量messages在这里的访问无需线程安全
			    // 可能的请求就是messages不为空 下一次会得到机会发送
				if (!this.messages.isEmpty()) {
					sendmsgexecutor.submit(getSendMsgTask());
				}
				this.lastsendtimestamp = curtimestamp;
			}
			this.issend.unlock();
			
			return ret;
		}
		private Runnable getSendMsgTask(){
			return new Runnable() {
				@Override
				public void run() {
					logger.debug("start to send msg for data==> " + roomdata);
					msglock.lock();
					String [] msgs = messages.toArray(new String[]{});
					messages = Lists.newArrayList();
					msgTypeCount.clear();
					msglock.unlock();
					if (msgs.length == 0) {
						logger.debug("the msag is empty finish to send msg for data==> " + roomdata);
						return;
					}
					//复用消息数据， 发送.  下面这个才是正确的不重复构建ByteBuf的方法
					String msgData = MessageUtils.makeMessageString(msgs);

					final ByteBuf tmp = Unpooled.wrappedBuffer(GzipUtils.encode(msgData));
					final BinaryWebSocketFrame binframe = new BinaryWebSocketFrame(tmp);
					final TextWebSocketFrame textframe = new TextWebSocketFrame(msgData);
					Iterator<UserData> it = roomdata.getUserIterator();
					while(it.hasNext()){
						UserData user = it.next();
						try {
							if (Objects.equal("0.0.1", user.getVersion())) {
								//只给后台用户发，数量不多，所以这么写
								MessageUtils.sendFrameToUser(user,  textframe.duplicate().retain());
							}else{
								MessageUtils.sendFrameToUser(user,  binframe.duplicate().retain());
							}
						} catch(Exception e) {
							logger.debug("error in RoomMessageQueue" + e.getMessage());
						}
					}
					/* 测试知道不加下面的代码似乎也没有内存泄露， 但是如果使用releaseLater() 就有泄露。*/
					ReferenceCountUtil.release(binframe);
					ReferenceCountUtil.release(textframe);
					logger.debug("finish to send msg for data==> " + roomdata);
					/* 检测引用的测试代码
					new Thread(){
						public void run(){
							for(int i=0; i< 10; i++){
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
								}
								System.out.println("binframe cnt: " +binframe.refCnt() + "  buf cnt:" +tmp.refCnt());
							}
							
						}
					}.start();
					*/
				}
			};
		}
		
		public static void removeMessageQueue(RoomData roomdata) {
			if (queueMap.containsKey(roomdata)) {
				queueMap.remove(roomdata);
			}
		}
		public static RoomMessageQueue getRoomMessageQueue(RoomData roomdata){
			//这段代码有初始化的同步问题， 但是实际上不用care
			if (! queueMap.containsKey(roomdata)){
				RoomMessageQueue queue = new RoomMessageQueue(roomdata);
				queueMap.put(roomdata,queue);
				return queue;
			}
			return queueMap.get(roomdata);
		}
	}