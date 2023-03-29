package com.unboundTech.mpc.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ChildChannelHandler extends ChannelInitializer<SocketChannel> {

    @Autowired
    private DataChannelHandler dataChannelHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        //中断空闲连接(idleHandler必须放在最前面）
        ch.pipeline().addLast(new ReadTimeoutHandler(20, TimeUnit.SECONDS));

        //webSocket基于http协议，需要使用http编解码器
        ch.pipeline().addLast(new HttpServerCodec());
        //分块向客户端写数据，防止发送大文件时导致内存溢出，channel.write(new ChunkedFile(new File("bigFile.mkv")))
        ch.pipeline().addLast(new ChunkedWriteHandler());
        //将HttpMessage和HttpContents聚合到一个完整的FullHttpRequest或FullHttpResponse中，需要放到HttpServerCodec后面
        ch.pipeline().addLast(new HttpObjectAggregator(10240));
        //webSocket数据压缩拓展，当添加这个处理器时WebSocketServerProtocolHandler的第三个参数需要设置true
//        ch.pipeline().addLast(new WebSocketServerCompressionHandler());
        //WebSocketServerProtocolHandler处理了握手以及 Close,Ping,Pong控制帧的处理
        ch.pipeline().addLast(new WebSocketServerProtocolHandler("/live", null, true, 10485760, false, true, 10000L) {
            @Override
            protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
                if (frame instanceof PingWebSocketFrame) {
                    log.info("get Ping msg, channel={}", ctx.channel().id());
                }
                super.decode(ctx, frame, out);
            }
        });
        //自定义二进制消息处理器
        ch.pipeline().addLast(dataChannelHandler);
    }
}
