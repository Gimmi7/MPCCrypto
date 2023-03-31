package com.unboundTech.mpc.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyServer {

    private final int port;
    private EventLoopGroup parentGroup;
    private EventLoopGroup childGroup;
    private ServerBootstrap bootstrap;

    public NettyServer(int port) {
        this.port = port;

        //创建reactor线程组
        parentGroup = new NioEventLoopGroup(4);
        childGroup = new NioEventLoopGroup(8);

        //设置reactor线程组
        bootstrap = new ServerBootstrap();
        bootstrap.group(parentGroup, childGroup);
        //设置nio类型的channel
        bootstrap.channel(NioServerSocketChannel.class);
        //设置监听端口
        bootstrap.localAddress(port);
        //设置parent channel的参数
        bootstrap.option(ChannelOption.SO_BACKLOG, 1024); //半连接队列长度
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        //设置child channel的参数
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
    }

    public void runServer(ChannelHandler childHandler) {
        try {
            //装配子通道流水线
            bootstrap.childHandler(childHandler);
            //开始绑定server, 通过调用sync同步方法阻塞直到绑定成功
            ChannelFuture future = bootstrap.bind().sync();
            //等待通道关闭的异步任务结束
            ChannelFuture closeFuture = future.channel().closeFuture();
            closeFuture.sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //优雅关闭EventLoopGroup,释放掉所有资源包括创建的线程
            childGroup.shutdownGracefully();
            parentGroup.shutdownGracefully();
        }
    }
}
