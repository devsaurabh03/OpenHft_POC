package org.virtualThread;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

public class FixEngineVirtualThread {
    private static final int TOTAL_MESSAGES = 1_000_000;
    private static final LongAdder processedMessages = new LongAdder();

    public static void main(String[] args) {
        var processor = new FixMessageProcessor();
        var executor = Executors.newVirtualThreadPerTaskExecutor();

        List<String> fixMessages = FixMessageGenerator.generateFixMessages(TOTAL_MESSAGES);
        long startTime = System.nanoTime(); // Start timer

        fixMessages.forEach(fixMessage -> executor.submit(() -> {
            long messageStart = System.nanoTime();
            processor.processMessage(fixMessage);
            long latency = System.nanoTime() - messageStart;

            processedMessages.increment();
            if (processedMessages.sum() % 100_000 == 0) { // Report every 100K messages
                long elapsedTime = System.nanoTime() - startTime;
                double seconds = elapsedTime / 1_000_000_000.0;
                System.out.printf("Processed: %,d messages | Avg Latency: %.2f µs | Elapsed Time: %.2f sec%n",
                        processedMessages.sum(), (latency / 1_000.0), seconds);
            }
        }));

        executor.close(); // Ensures all tasks complete before exit

        long endTime = System.nanoTime();
        double totalSeconds = (endTime - startTime) / 1_000_000_000.0;
        System.out.printf("Total Execution Time: %.2f sec | Throughput: %.2f messages/sec%n",
                totalSeconds, TOTAL_MESSAGES / totalSeconds);
    }
}
