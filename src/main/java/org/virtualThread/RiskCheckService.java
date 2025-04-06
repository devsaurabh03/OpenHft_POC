package org.virtualThread;


public class RiskCheckService {
    public boolean checkMargin(String order) {
        sleep(1); // Simulate latency
        return true;
    }

    public boolean checkPosition(String order) {
        sleep(1); // Simulate latency
        return true;
    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

