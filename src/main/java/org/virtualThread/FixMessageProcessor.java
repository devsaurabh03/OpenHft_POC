package org.virtualThread;

import java.util.concurrent.StructuredTaskScope;

public class FixMessageProcessor {
    private final RiskCheckService riskCheckService = new RiskCheckService();

    public void processMessage(String fixMessage) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // Run risk checks in parallel
            var marginCheck = scope.fork(() -> riskCheckService.checkMargin(fixMessage));
            var positionCheck = scope.fork(() -> riskCheckService.checkPosition(fixMessage));

            scope.join(); // Wait for both checks
            if (marginCheck.get() && positionCheck.get()) {
                //System.out.println(Thread.currentThread() + " - Order Passed Risk Checks: " + fixMessage);
            } else {
               // System.out.println(Thread.currentThread() + " - Order Rejected: " + fixMessage);
            }
        } catch (Exception e) {
            System.err.println("Error processing FIX message: " + e.getMessage());
        }
    }
}
