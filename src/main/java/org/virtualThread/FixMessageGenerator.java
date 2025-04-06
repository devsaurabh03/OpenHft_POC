package org.virtualThread;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FixMessageGenerator {
    private static final String[] SYMBOLS = {"AAPL", "GOOG", "MSFT", "TSLA", "AMZN"};
    private static final Random RANDOM = new Random();

    public static List<String> generateFixMessages(int count) {
        List<String> messages = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String fixMessage = String.format("8=FIX.4.2|35=D|55=%s|44=%.2f|10=%03d",
                    SYMBOLS[RANDOM.nextInt(SYMBOLS.length)],
                    100 + RANDOM.nextDouble() * 1000,  // Random price
                    RANDOM.nextInt(900) + 100);  // Random checksum
            messages.add(fixMessage);
        }
        return messages;
    }
}
