import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class TestPromise {

    public static void main(String[] args) throws Exception {
        CompletableFuture<String> promise = new CompletableFuture<>();
        log.info("init promise");

        ScheduledThreadPoolExecutor schedule = new ScheduledThreadPoolExecutor(1, new ThreadPoolExecutor.CallerRunsPolicy());
        schedule.schedule(() -> promise.completeExceptionally(new TimeoutException("timeout promise")), 2, TimeUnit.SECONDS);


        new Thread(new Runnable() {
            @Override
            public void run() {
                log.info("await.....");
                String rsp = null;
                try {
                    rsp = promise.get();
                } catch (Exception e) {
//                    throw new RuntimeException(e);
                }
                log.info("rsp={}", rsp);
            }
        }).start();


        promise.cancel(true);
        Thread.sleep(5000);

        System.out.printf("isCancel: %b, isDone: %b, isExp: %b \n", promise.isCancelled(), promise.isDone(), promise.isCompletedExceptionally());
        promise.complete("ok");
        System.out.printf("isCancel: %b, isDone: %b, isExp: %b \n", promise.isCancelled(), promise.isDone(), promise.isCompletedExceptionally());


    }
}
