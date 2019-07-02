package icblive.chatserver.model.json;

public class GiveGiftMessage extends BaseMessage{
	//赠送礼物的用户的信息
	public String userid ="";
	public String nickname ="";
	public String headphoto ="";
	public int level = 0;
	//礼物的信息
	public String giftid; //礼物类型
	public String giftname; //礼物名称
	public int amount; //数量
	public String quantifier; //单位
	public String gift_image; //礼物图片
	public String gift_image_svga; //礼物svga图片
	public String is_luxurygift; //是否奢侈品
	public String showtime;
	public int incrpopular = 0; 
	public int showtype;
	public int comboamount = 0;
	public UserMedalInfo medalinfo;  // 勋章

	public void addUserinfo(UserInfo userinfo){
		if(userinfo != null){
			nickname = userinfo.nickname;
			headphoto = userinfo.headphoto;
			level = userinfo.level;
			medalinfo = userinfo.medalinfo;
		}else{
			nickname = "游客";
		}
	}
}
