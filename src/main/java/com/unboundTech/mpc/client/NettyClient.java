package com.unboundTech.mpc.client;

import com.unboundTech.mpc.client.handler.ClientChildChannelHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.URI;

public class NettyClient {

    private Bootstrap bootstrap;
    private EventLoopGroup group;

    public NettyClient() {
        //创建reactor线程组, 一个线程监听服务端消息
        group = ClientExecutorUtil.eventLoopGroup;
        //设置reactor线程组
        bootstrap = new Bootstrap();
        bootstrap.group(group);
        //设置nio类型的channel
        bootstrap.channel(NioSocketChannel.class);
        //设置监听地址和端口,webSocket需要在处理器设置
//        bootstrap.remoteAddress(serverHost, serverPort);
        //设置通道的参数
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
    }

    public Channel runClient(ClientChildChannelHandler handler, URI uri) {
        //装配通道流水线
        handler.setWsUri(uri);
        bootstrap.handler(handler);
        try {
            //连接server端，阻塞直到成功
            ChannelFuture future = bootstrap.connect(uri.getHost(), uri.getPort()).sync();
            //获取channel
            Channel channel = future.channel();
            for (; ; ) {
                //自旋等待webSocket握手完成
                boolean flag = handler.getWsHandler().handshaker().isHandshakeComplete();
                if (flag) {
                    break;
                }
            }
            return channel;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void closeClient() {
        if (group != null) {
            group.shutdownGracefully();
        }
    }
}
