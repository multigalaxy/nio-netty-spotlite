package icblive.chatserver.utils;

import com.google.common.util.concurrent.AtomicLongMap;

public class ChatCount {
	public static AtomicLongMap<String> map = AtomicLongMap.create();//key是session
	
	public ChatCount(){
	}

	public static long increKey(String key){
		return map.incrementAndGet(key);
	}
	public static long decKey(String key){
		return map.decrementAndGet(key);
	}
	
	public static long getAndput(String key,long value){
		return map.put(key,value);
	}
	
	public static long remove(String key){
		return map.remove(key);
	}
	
	public static void clearUnusedCount(){
		//定时跑一跑
		map.removeAllZeros();
	}
}
