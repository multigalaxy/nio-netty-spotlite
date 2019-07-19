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
 * @author xiaol
 *
 */
public class LiveChatServer2 {

	// 设置日志文件格式
	static{
		System.setProperty("log4j.configurationFile", "log4j2.xml");
	}
	
	// 初始化日志引擎
	private static Logger logger = LogManager.getLogger(LiveChatServer2.class.getName());
	
	
	 public static void main(String[] args) throws Exception {
		 	logger.error("server start!");

		 	// 初始化参数配置对象
		 	ChangbaConfig config = ChangbaConfig.getConfig();
		 	// 初始化语言环境
		 	LanguageUtils.getInstance().initLanguageCont();
        	//新建线程，启动一些Task
		 	new Thread(new InternalMsgHandler()).start(); //1、处理来自服务端触发的redis的系统消息，发布订阅模式
		 	ChangbaMetrics.startReport(); //统计线程
		 	
	        final ServerBootstrap sb = new ServerBootstrap();
	        // Configure the server.
	        EventLoopGroup bossGroup = new NioEventLoopGroup();  // 接受请求：Acceptor线程池，只监听一个端口，所以设置默认1个线程
	        EventLoopGroup workerGroup = new NioEventLoopGroup();  // 处理：io线程池，此处量不大，设置默认1个，用于绑定channel
	        try {
	            sb.option(ChannelOption.SO_BACKLOG, 1024);
	            sb.group(bossGroup, workerGroup)
	             .channel(NioServerSocketChannel.class)
	             .childOption(ChannelOption.TCP_NODELAY, true)	
	             .childOption(ChannelOption.SO_REUSEADDR, true)
	             .childOption(ChannelOption.SO_KEEPALIVE, true)
	             .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
	             .childHandler(new ChatInitializer());  //2、 每个客户端连接成功后执行该handler：构造消息socket，初始化webscoketmessagehandler类（app调用joinSession、exitSession、publicchat、kickoff等）

				// 绑定并服务端
	            Channel ch = sb.bind(new InetSocketAddress("0.0.0.0" , config.ws_port)).sync().channel();  // sync阻塞执行绑定操作完成，然后获取channel

	            // 等待服务端关闭
	            ch.closeFuture().sync();  // sync阻塞执行关闭服务操作
	        } finally {
	            bossGroup.shutdownGracefully();  // 优雅关闭接受线程组
	            workerGroup.shutdownGracefully();  // 优雅关闭处理线程组
	        }
	 }
	 
}






