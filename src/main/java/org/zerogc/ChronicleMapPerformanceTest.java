package org.zerogc;

import net.openhft.chronicle.map.ChronicleMap;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ChronicleMapPerformanceTest {

    private static final Random random = new Random();
    private static final String[] TICKERS = {"AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "AMD", "JPM", "BAC",
            "INTC", "CSCO", "PFE", "KO", "DIS", "NFLX", "V", "WMT", "PG", "XOM"};

    private static final int NUM_SYMBOLS = 5_000_000;
    public static final int SYMBOL_LENGTH = 200; // Average length of stock symbols
    private static final int NUM_RANDOM_ACCESSES = 1_000_000;
    private static final String FILE_PATH = "symbol_map.dat";

    public static void main(String[] args) throws IOException {
        File file = new File(FILE_PATH);

        // Delete file if it exists to start fresh
        if (file.exists()) {
            file.delete();
        }

        System.out.println("Creating and loading ChronicleMap with " + NUM_SYMBOLS + " stock symbols...");

        // 1. Create and load the map
        long startLoadTime = System.nanoTime();

        ChronicleMap<OptionSymbol, Double> symbolMap = ChronicleMap
                .of(OptionSymbol.class, Double.class)
                .name("stock-symbol-map")
                .entries(NUM_SYMBOLS)
                .averageKey(createAverageSymbol(SYMBOL_LENGTH)) // Set average key size
                .putReturnsNull(true)
                .createPersistedTo(file);

        loadMapWithStockSymbols(symbolMap);
        printHeapMemoryUsage("Memory Usages before random access test and load symbols");
        long loadTime = System.nanoTime() - startLoadTime;
        System.out.printf("Map loading completed in %.2f seconds\n",
                TimeUnit.NANOSECONDS.toMillis(loadTime) / 1000.0);
        System.out.printf("Map size: %.2f MB\n", file.length() / (1024.0 * 1024.0));

        // 2. Perform random access test
        System.out.println("\nPerforming " + NUM_RANDOM_ACCESSES + " random accesses...");

        // not needed in production just for testing purpose
        OptionSymbol[] allSymbols = symbolMap.keySet().toArray(OptionSymbol[]::new);

        long startAccessTime = System.nanoTime();
        long hitCount = 0;

        for (int i = 0; i < NUM_RANDOM_ACCESSES; i++) {
            int randomIndex = random.nextInt(allSymbols.length);
            OptionSymbol randomSymbol = allSymbols[randomIndex];
            Double price = symbolMap.get(randomSymbol);
            symbolMap.put(randomSymbol, price + 1.0); // Update price to simulate write operation
            if (price != null) {
                hitCount++;
            }
        }

        printHeapMemoryUsage("Memory Usages after random access test after loading symbols from file");
        long accessTime = System.nanoTime() - startAccessTime;

        // Print performance metrics
        System.out.printf("Random access test completed in %.2f seconds\n",
                TimeUnit.NANOSECONDS.toMillis(accessTime) / 1000.0);
        System.out.printf("Average access time: %.2f microseconds per operation\n",
                TimeUnit.NANOSECONDS.toMicros(accessTime) / (double) NUM_RANDOM_ACCESSES);
        System.out.printf("Operations per second: %.2f\n",
                (NUM_RANDOM_ACCESSES * 1_000_000_000.0) / accessTime);
        System.out.printf("Hit rate: %.2f%%\n", (hitCount * 100.0) / NUM_RANDOM_ACCESSES);

        // 3. Close the map
        symbolMap.close();
        System.out.println("\nTest completed and map closed.");


    }

    public static OptionSymbol generateRandomSymbol() {
        // Select a random ticker
        String ticker = TICKERS[random.nextInt(TICKERS.length)];

        // Select a random option type
        OptionSymbol.OptionType type = random.nextBoolean() ? OptionSymbol.OptionType.CALL : OptionSymbol.OptionType.PUT;

        // Generate a random expiration date (between now and 2 years in the future)
        LocalDate now = LocalDate.now();
        int daysToAdd = random.nextInt(730) + 1; // 1 to 730 days (roughly 2 years)
        LocalDate expDate = now.plusDays(daysToAdd);

        // Generate a random strike price
        int basePrice = getBaseStockPrice(ticker);
        double variation = basePrice * 0.4; // 40% variation above or below base price
        double priceOffset = (random.nextDouble() * variation * 2) - variation;

        // Round to nearest 5 or 0.25 depending on price range
        double strikeValue = basePrice + priceOffset;
        if (strikeValue < 100) {
            strikeValue = Math.round(strikeValue * 4) / 4.0; // Round to nearest 0.25
        } else {
            strikeValue = Math.round(strikeValue / 5) * 5.0; // Round to nearest 5
        }

        BigDecimal strikePrice = BigDecimal.valueOf(strikeValue);

        // Create and return the option symbol
        return new OptionSymbol(ticker, type, expDate, strikePrice);
    }

    private static void loadMapWithStockSymbols(ChronicleMap<OptionSymbol, Double> map) {
        for (int i = 0; i < NUM_SYMBOLS; i++) {
            OptionSymbol symbol = generateRandomSymbol();
            Double price = 100.0 + (Math.random() * 900.0); // Random price between 100 and 1000
            map.put(symbol, price);

            if (i % 100000 == 0) {
                System.out.println("Loaded " + i + " symbols...");
            }
        }
    }

    private static int getBaseStockPrice(String ticker) {
        switch (ticker) {
            case "AAPL": return 175;
            case "MSFT": return 350;
            case "GOOGL": return 140;
            case "AMZN": return 130;
            case "TSLA": return 250;
            case "META": return 300;
            case "NVDA": return 450;
            case "AMD": return 110;
            case "JPM": return 160;
            case "BAC": return 35;
            case "INTC": return 40;
            case "CSCO": return 55;
            case "PFE": return 30;
            case "KO": return 60;
            case "DIS": return 120;
            case "NFLX": return 400;
            case "V": return 240;
            case "WMT": return 80;
            case "PG": return 150;
            case "XOM": return 110;
            default: return 100;
        }
    }


    private static void printHeapMemoryUsage(String message) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();

        System.out.println(message);
        System.out.println("Heap Memory Used: " + heapMemory.getUsed() / (1024 * 1024) + " MB");
        System.out.println("Heap Committed: " + heapMemory.getCommitted() / (1024 * 1024) + " MB");
        System.out.println("Heap Max: " + heapMemory.getMax() / (1024 * 1024) + " MB");
        System.out.println("-------------------------------------------------");
    }

    public static OptionSymbol createAverageSymbol(int length) {
        OptionSymbol op = new OptionSymbol();
        op.setUnderlyingSymbol("AAPL");
        op.setOptionType(OptionSymbol.OptionType.CALL);
        op.setExpirationDate(LocalDate.now());
        op.setStrikePrice(BigDecimal.valueOf(150.0));
        return op;
    }
}