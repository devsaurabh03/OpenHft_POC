package org.example;

import net.openhft.chronicle.map.ChronicleMap;
import org.zerogc.OptionSymbol;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.zerogc.ChronicleMapPerformanceTest.SYMBOL_LENGTH;
import static org.zerogc.ChronicleMapPerformanceTest.createAverageSymbol;

public class ChronicleMapPerformanceVirtualThreadTest {
    private static final Random random = new Random();
    private static final String FILE_PATH = "symbol_map.dat";
    private static final int NUM_SYMBOLS = 5_000_000;
    private static final int NUM_RANDOM_ACCESSES = 1_000_000;

    public static void main(String[] args) throws IOException, InterruptedException {
        File file = new File(FILE_PATH);
        if (file.exists()) file.delete();

        System.out.println("Creating and loading ChronicleMap...");
        long startLoadTime = System.nanoTime();

        ChronicleMap<OptionSymbol, Double> symbolMap = ChronicleMap
                .of(OptionSymbol.class, Double.class)
                .name("stock-symbol-map")
                .entries(NUM_SYMBOLS)
                .averageKey(createAverageSymbol(SYMBOL_LENGTH))
                .createPersistedTo(file);

        // Load symbols using Virtual Threads
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<?>[] futures = IntStream.range(0, NUM_SYMBOLS)
                    .mapToObj(i -> CompletableFuture.runAsync(() -> {
                        OptionSymbol symbol = generateRandomSymbol();
                        symbolMap.put(symbol, 100.0 + (Math.random() * 900.0));
                    }, executor))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(futures).join();
        }

        printHeapMemoryUsage("Memory Usage after loading symbols");
        System.out.printf("Map loading completed in %.2f seconds\n", elapsedTime(startLoadTime));

        // Perform random access using Virtual Threads
        OptionSymbol[] allSymbols = symbolMap.keySet().toArray(OptionSymbol[]::new);
        long startAccessTime = System.nanoTime();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<?>[] futures = IntStream.range(0, NUM_RANDOM_ACCESSES)
                    .mapToObj(i -> CompletableFuture.runAsync(() -> {
                        int index = random.nextInt(allSymbols.length);
                        OptionSymbol symbol = allSymbols[index];
                        symbolMap.put(symbol, symbolMap.get(symbol) + 1.0);
                    }, executor))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(futures).join();
        }

        printHeapMemoryUsage("Memory Usage after random access test");
        System.out.printf("Random access completed in %.2f seconds\n", elapsedTime(startAccessTime));

        symbolMap.close();
        System.out.println("Test completed and map closed.");
    }

    private static OptionSymbol generateRandomSymbol() {
        String ticker = "AAPL";
        OptionSymbol.OptionType type = random.nextBoolean() ? OptionSymbol.OptionType.CALL : OptionSymbol.OptionType.PUT;
        LocalDate expDate = LocalDate.now().plusDays(random.nextInt(730) + 1);
        BigDecimal strikePrice = BigDecimal.valueOf(100 + (Math.random() * 900.0));
        return new OptionSymbol(ticker, type, expDate, strikePrice);
    }

    private static void printHeapMemoryUsage(String message) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        System.out.printf("%s - Heap Used: %d MB, Heap Max: %d MB\n",
                message, heapMemory.getUsed() / (1024 * 1024), heapMemory.getMax() / (1024 * 1024));
    }

    private static double elapsedTime(long startTime) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) / 1000.0;
    }
}

