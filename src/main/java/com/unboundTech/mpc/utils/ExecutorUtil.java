package com.unboundTech.mpc.utils;

import java.util.concurrent.*;

public class ExecutorUtil {

    public static ExecutorService backPressurePool() {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(4);
        ExecutorService executorService = new ThreadPoolExecutor(4, 4, 60, TimeUnit.SECONDS, queue, Executors.defaultThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
        return executorService;
    }
}
