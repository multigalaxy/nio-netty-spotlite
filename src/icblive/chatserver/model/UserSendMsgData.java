package icblive.chatserver.model;

import java.util.Date;

public class UserSendMsgData {
	private String roomid;
	private String userid;
	private long timestamp;

	
	@Override
	public String toString() {
		return "DisableMsgUserData [roomid=" + roomid + ", userid=" + userid + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((roomid == null) ? 0 : roomid.hashCode());
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
		UserSendMsgData other = (UserSendMsgData) obj;
		if (roomid == null) {
			if (other.roomid != null)
				return false;
		} else if (!roomid.equals(other.roomid))
			return false;
		if (userid == null) {
			if (other.userid != null)
				return false;
		} else if (!userid.equals(other.userid))
			return false;
		return true;
	}
	public UserSendMsgData(String userid, String roomid) {
		super();
		this.roomid = roomid;
		this.userid = userid;
		this.timestamp = (new Date().getTime());
		
	}
	public String getRoomid() {
		return roomid;
	}
	public void setRoomid(String roomid) {
		this.roomid = roomid;
	}
	public String getUserid() {
		return userid;
	}
	public void setUserid(String userid) {
		this.userid = userid;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
}
