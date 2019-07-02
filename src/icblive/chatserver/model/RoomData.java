/**
 * 
 */
package icblive.chatserver.model;

import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import icblive.chatserver.utils.RedisHelper;

/**
 * @author jgao
 *
 */
public class RoomData {
	public String sessionid = "";
	public String userid = "";//当前麦上的主播id
	public long timestamp = new Date().getTime();
	private Set<UserData> usersSet = Sets.newConcurrentHashSet();
	//只要能创建出来，说明一定是在播的
	public boolean islive = true;
	private int pause = -1;
	private boolean ownerjoined = false;

	//public List<UserData> users = Lists.newLinkedList();
	
	//TODO BenchStruct
	public RoomData( String ownerid ,String session){
		userid = ownerid;
		sessionid = session;
	}
	
	public RoomData(){
	}
	
	public boolean removeUser(UserData userdata){
		return usersSet.remove(userdata);
	}
	public Iterator<UserData> getUserIterator(){
		return usersSet.iterator();
	}
	public boolean isEmpty(){
		return usersSet.isEmpty();
	}
	public int count(){
		return usersSet.size();
	}
	
	public boolean isLive(){
		return islive;
	}
	public boolean isPause(){
		return pause >= 0;
	}
	public String pauseOrResume(String type,String pid) {
		int pauseid = Integer.parseInt(pid);
		switch(type){
		case "pause":
			if(pause < pauseid){
				pause = pauseid;
			}
			break;
		case "resume":
			if(pause <= pauseid){
				pause = -1;
			}
			break;
		default:
			break;
		}
		return pause+"";
	}
	public String getRoomSession(){
		return sessionid;
	}
	
	//TODO 去重?
	public boolean addUser(UserData userdata){
		return usersSet.add(userdata);
	}
	@Override
	public String toString(){
		String buff = "{sessionid:"+sessionid + "/anchor:"+ userid+"/timestamp:"+timestamp + "/usercount:" + usersSet.size() +"/ispause:"+ pause +"/ownerjoined:"+ownerjoined+"}";
		return buff;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((userid == null) ? 0 : userid.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RoomData other = (RoomData) obj;
		if (sessionid == null) {
			if (other.sessionid != null)
				return false;
		} else if (!sessionid.equals(other.sessionid))
			return false;
		return true;
	}

	public void ownerJoined() {
		// TODO Auto-generated method stub
		ownerjoined  = true;
	}
	
	public boolean isOwnerJoined(){
		return ownerjoined;
	}
}
