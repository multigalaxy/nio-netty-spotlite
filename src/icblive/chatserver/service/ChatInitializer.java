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

        // 添加处理句柄：每个socket链接一个线程，只实例化一次所有对象，即所有句柄都处在同一个线程里，防止发生线程争锁
        ch.pipeline().addLast(
                // 1、http协议解析：bytebuf数据流转成http消息对象，包含（httpcontent、lasthttpcontent、httpmessage等）【httpmessage=httprequest/httpresponse，取决于处理请求还是响应，此处是请求】
                // HttpObjectDecoder：如果是chunked编码的消息，将生成3个对象：httprequest、第一个httpcontent、最后一个lasthttpcontent。
                new HttpRequestDecoder(),
                // 2、把上面的httpmessage、httpcontents消息聚合成单一的fullhttprequest(http)对象，尤其chunked编码很有效
                new HttpObjectAggregator(65536),
                // 4、http对象转成bytebuf流
                new HttpResponseEncoder(),
 //               new IdleStateHandler(60, 60, 0,TimeUnit.SECONDS),
//                new WebSocketServerProtocolHandler("/websocket"),
//                new WebSocketTextandler()
                //new WebSocketServerHandlerTest()
                // 3、业务逻辑
                new WebSocketMessageHandler(  )
        		);
    }
}
