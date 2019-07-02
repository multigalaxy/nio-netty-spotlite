package icblive.chatserver.service;
import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import icblive.chatserver.api.InternalMsgHandler;
import icblive.chatserver.utils.ChangbaConfig;
import icblive.chatserver.utils.ChangbaMetrics;
import icblive.chatserver.utils.LanguageUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author jgao
 *
 */
public class LiveChatServer2 {

	static{
		System.setProperty("log4j.configurationFile", "log4j2.xml");
	}
	

	private static Logger logger = LogManager.getLogger(LiveChatServer2.class.getName());
	
	
	 public static void main(String[] args) throws Exception {
		 	logger.error("server start!");
		 	
		 	ChangbaConfig config = ChangbaConfig.getConfig();
		 	// 初始化语言环境
		 	LanguageUtils.getInstance().initLanguageCont();
        	//新建线程，启动一些Task
		 	new Thread(new InternalMsgHandler()).start(); //1、处理来自服务端触发的redis的系统消息
		 	ChangbaMetrics.startReport(); //统计线程
		 	
	        final ServerBootstrap sb = new ServerBootstrap();
	        // Configure the server.
	        EventLoopGroup bossGroup = new NioEventLoopGroup();
	        EventLoopGroup workerGroup = new NioEventLoopGroup();
	        try {
	            sb.option(ChannelOption.SO_BACKLOG, 1024);
	            sb.group(bossGroup, workerGroup)
	             .channel(NioServerSocketChannel.class)
	             .childOption(ChannelOption.TCP_NODELAY, true)	
	             .childOption(ChannelOption.SO_REUSEADDR, true)
	             .childOption(ChannelOption.SO_KEEPALIVE, true)
	             .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
	             .childHandler(new ChatInitializer(  ));  //2、 构造消息socket，初始化webscoketmessagehandler类（app调用joinSession、exitSession、publicchat、kickoff等）

	            Channel ch = sb.bind(new InetSocketAddress("0.0.0.0" , config.ws_port)).sync().channel();

	            ch.closeFuture().sync();
	        } finally {
	            bossGroup.shutdownGracefully();
	            workerGroup.shutdownGracefully();
	        }
	 }
	 
}






