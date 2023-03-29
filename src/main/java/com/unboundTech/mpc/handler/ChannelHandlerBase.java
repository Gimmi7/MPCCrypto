package com.unboundTech.mpc.handler;

import com.unboundTech.mpc.server.ConnectionHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class ChannelHandlerBase extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress inetSocket = (InetSocketAddress) ctx.channel().remoteAddress();
        String ip = inetSocket.getAddress().getHostAddress();
        System.out.println(ip);
        /**
         * register router at userEventTriggered(HandshakeComplete)
         */
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // remove channel
        ConnectionHolder.remove(ctx.channel());
        log.info("inactive channel={}", ctx.channel().id());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("channel exception, channel={}:", ctx.channel().id(), cause);
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            String requestUri = handshakeComplete.requestUri();
            String[] uriArr = requestUri.split("/");
//            ConnectionHolder.addConnection(ctx.channel());
//            String subProtocol = handshakeComplete.selectedSubprotocol();
//            handshakeComplete.requestHeaders().forEach(entry -> log.info("header key={}, value={}", entry.getKey(), entry.getValue()));
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, BinaryWebSocketFrame binaryWebSocketFrame) throws Exception {

    }


}
