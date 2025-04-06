package org.virtualThread;

import java.util.concurrent.*;

public class VirtualThreadDemo {
    public static void main(String[] args) throws InterruptedException {
        int tasks = 10_000;
        ExecutorService vExecutor = Executors.newVirtualThreadPerTaskExecutor();

        long start = System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(tasks);

        for (int i = 0; i < tasks; i++) {
            vExecutor.submit(() -> {
                try {
                    Thread.sleep(2000);  // simulate blocking I/O
                } catch (InterruptedException ignored) {}
                latch.countDown();
            });
        }

        latch.await();
        long duration = System.currentTimeMillis() - start;
        vExecutor.shutdown();

        System.out.println("Virtual thread pool took: " + duration + " ms");
    }
}

