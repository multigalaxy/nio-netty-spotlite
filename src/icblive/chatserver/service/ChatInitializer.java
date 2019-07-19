package icblive.chatserver.service;
import icblive.chatserver.WebSocketMessageHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;


/**
 * @author xiaol
 *
 */
public class ChatInitializer extends ChannelInitializer<SocketChannel> {
    public ChatInitializer( ) {
		// TODO Auto-generated constructor stub
	}

	// 每个客户端成功connect后才触发：初始化chat管道线，添加请求解码、消息体解码、响应编码和统一处理websocket消息的句柄
    // inbound是顺序查找执行，outbound是逆序查找执行；所以执行websockethandler要执行encode时，是从此处向前查找到httpresponseencoder作为第一个outhandler。
	@Override
    public void initChannel(SocketChannel ch) throws Exception {
 //       ChannelPipeline p = ch.pipeline();

//        p.addLast("codec-http", new HttpServerCodec());
//        p.addLast("aggregator", new HttpObjectAggregator(65536));
//        p.addLast("handler", new WebSocketServerHandler());
		
        ch.pipeline().addLast(
                new HttpRequestDecoder(),  // 1、bytebuf数据流转成http对象
                new HttpObjectAggregator(65536),  // 2、把bytebuf流转成单一的fullhttprequest或fullhttpresponse对象
                new HttpResponseEncoder(),  // 4、http对象转成bytebuf流
 //               new IdleStateHandler(60, 60, 0,TimeUnit.SECONDS),
//                new WebSocketServerProtocolHandler("/websocket"),
//                new WebSocketTextandler()
                //new WebSocketServerHandlerTest()
                new WebSocketMessageHandler(  )  // 3、业务逻辑
        		);
    }
}
