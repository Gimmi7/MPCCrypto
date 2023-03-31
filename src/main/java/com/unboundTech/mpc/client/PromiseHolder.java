package com.unboundTech.mpc.client;

import com.unboundTech.mpc.socketmsg.MsgWrapper;

import java.util.concurrent.*;

public class PromiseHolder {

    private static final ConcurrentMap<Integer, CompletableFuture<MsgWrapper>> seq2promise = new ConcurrentHashMap<>();

    public static void add(int seq, CompletableFuture<MsgWrapper> promise) {
        // remove promise from map when timeout
        ClientExecutorUtil.schedulePool.schedule(() -> {
            if (promise != null && !promise.isDone()) {
                promise.completeExceptionally(new TimeoutException("promise timeout"));
            }

            seq2promise.remove(seq);
        }, 10, TimeUnit.SECONDS);

        seq2promise.put(seq, promise);
    }

    public static void resolve(int seq, MsgWrapper msgWrapper) {
        CompletableFuture<MsgWrapper> promise = seq2promise.get(seq);
        if (promise != null) {
            promise.complete(msgWrapper);

            // remove promise from map
            seq2promise.remove(seq);
        }
    }
}
