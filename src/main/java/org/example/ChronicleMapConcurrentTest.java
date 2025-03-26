package org.example;

import net.openhft.chronicle.map.ChronicleMap;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChronicleMapConcurrentTest {
    private static final int NUM_THREADS = 10;
    private static final int NUM_ENTRIES = 1_000_000;
    private static final int OPERATIONS_PER_THREAD = 100_000;
    private static final Path CHRONICLE_MAP_FILE = Paths.get("chronicle-map.dat");

    public static void main(String[] args) throws IOException, InterruptedException {
        printHeapMemoryUsage("Before ChronicleMap creation");

        // Chronicle Map Initialization
        ChronicleMap<String, String> map = ChronicleMap
                .of(String.class, String.class)
                .entries(NUM_ENTRIES)
                .averageKey("SYM000001")
                .averageValue("VALUE000001")
                .createPersistedTo(CHRONICLE_MAP_FILE.toFile());

        printHeapMemoryUsage("After ChronicleMap creation");

        // Populate Chronicle Map with initial data
        System.out.println("Populating Chronicle Map...");
        long start = System.nanoTime();
        for (int i = 0; i < NUM_ENTRIES; i++) {
            map.put("SYM" + i, "VALUE" + i);
        }
        long end = System.nanoTime();
        System.out.println("Population Time: " + (end - start) / 1_000_000 + " ms");

        printHeapMemoryUsage("After inserting 1 million symbols");

        // Concurrent Read/Write Test
        System.out.println("Starting concurrent access test...");
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.execute(new ChronicleMapTask(map, latch));
        }

        latch.await(); // Wait for all threads to finish
        executor.shutdown();

        printHeapMemoryUsage("After concurrent access test");

        // Cleanup
        map.close();
    }

    // Measure heap memory usage
    private static void printHeapMemoryUsage(String message) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();

        System.out.println(message);
        System.out.println("Heap Memory Used: " + heapMemory.getUsed() / (1024 * 1024) + " MB");
        System.out.println("Heap Committed: " + heapMemory.getCommitted() / (1024 * 1024) + " MB");
        System.out.println("Heap Max: " + heapMemory.getMax() / (1024 * 1024) + " MB");
        System.out.println("-------------------------------------------------");
    }

    // Concurrent Read/Write Task
    static class ChronicleMapTask implements Runnable {
        private final ChronicleMap<String, String> map;
        private final CountDownLatch latch;
        private final Random random = new Random();

        public ChronicleMapTask(ChronicleMap<String, String> map, CountDownLatch latch) {
            this.map = map;
            this.latch = latch;
        }

        @Override
        public void run() {
            long start = System.nanoTime();
            for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                int keyIndex = random.nextInt(NUM_ENTRIES);
                String key = "SYM" + keyIndex;

                if (random.nextBoolean()) {
                    // 50% Read
                    map.get(key);
                } else {
                    // 50% Write
                    map.put(key, "VALUE" + keyIndex);
                }
            }
            long end = System.nanoTime();
            long duration = (end - start) / OPERATIONS_PER_THREAD; // Avg time per op

            System.out.println(Thread.currentThread().getName() + " completed. Avg time per op: " + duration + " ns");
            latch.countDown();
        }
    }
}
