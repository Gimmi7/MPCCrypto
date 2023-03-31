package com.unboundTech.mpc.client;

import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.*;

public class ClientExecutorUtil {
    /**
     * 一个线程监听服务端消息
     */
    public static NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);

//    /**
//     * 一个线程组处理命令消息和某些通知消息
//     */
//    private static LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(4);
//    public static ExecutorService discardOldestPool = new ThreadPoolExecutor(4, 4, 60, TimeUnit.SECONDS, queue, Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardOldestPolicy());

    /**
     * 一个线程组处理心跳消息,promise
     */
    public static ScheduledThreadPoolExecutor schedulePool = new ScheduledThreadPoolExecutor(4, new ThreadPoolExecutor.CallerRunsPolicy());

//    /**
//     * 处理机器人自发动作的延迟线程池
//     */
//    public static ScheduledExecutorService robotDelayPool = new ScheduledThreadPoolExecutor(4);

    public static ExecutorService backPressurePool() {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(4);
        ExecutorService executorService = new ThreadPoolExecutor(4, 4, 60, TimeUnit.SECONDS, queue, Executors.defaultThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
        return executorService;
    }
}
