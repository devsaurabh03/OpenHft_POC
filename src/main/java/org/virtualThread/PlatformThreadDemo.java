package org.virtualThread;

import java.util.concurrent.*;

public class PlatformThreadDemo {
    public static void main(String[] args) throws InterruptedException {
        int tasks = 10_000;
        ExecutorService pool = Executors.newCachedThreadPool();  // limited real threads

        long start = System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(tasks);

        for (int i = 0; i < tasks; i++) {
            pool.submit(() -> {
                try {
                    Thread.sleep(2000);  // simulate blocking I/O
                } catch (InterruptedException ignored) {}
                latch.countDown();
            });
        }

        latch.await();  // wait for all tasks
        long duration = System.currentTimeMillis() - start;
        pool.shutdown();

        System.out.println("Platform thread pool took: " + duration + " ms");
    }
}

