package icblive.chatserver.service;
import icblive.chatserver.WebSocketMessageHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;


/**
 * @author jgao
 *
 */
public class ChatInitializer extends ChannelInitializer<SocketChannel> {
    public ChatInitializer( ) {
		// TODO Auto-generated constructor stub
	}

	@Override
    public void initChannel(SocketChannel ch) throws Exception {
 //       ChannelPipeline p = ch.pipeline();

//        p.addLast("codec-http", new HttpServerCodec());
//        p.addLast("aggregator", new HttpObjectAggregator(65536));
//        p.addLast("handler", new WebSocketServerHandler());
		
        ch.pipeline().addLast(
                new HttpRequestDecoder(),
                new HttpObjectAggregator(65536),
                new HttpResponseEncoder(),
 //               new IdleStateHandler(60, 60, 0,TimeUnit.SECONDS),
//                new WebSocketServerProtocolHandler("/websocket"),
//                new WebSocketTextandler()
                //new WebSocketServerHandlerTest()
                new WebSocketMessageHandler(  )
        		);
    }
}
