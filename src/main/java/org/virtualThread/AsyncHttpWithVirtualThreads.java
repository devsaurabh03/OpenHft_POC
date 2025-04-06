package org.virtualThread;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncHttpWithVirtualThreads {
    public static void main(String[] args) {
        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

        HttpClient client = HttpClient.newBuilder()
                .executor(virtualExecutor)
                .build();

        long start = System.currentTimeMillis();

        CompletableFuture<?>[] futures = new CompletableFuture[10000];
        for (int i = 0; i < 10000; i++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://httpbin.org/delay/10"))
                    .GET()
                    .build();

            futures[i] = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                       // System.out.println("Status: " + response.statusCode() + ", Thread: " + Thread.currentThread());
                    });
        }

        CompletableFuture.allOf(futures).join();
        virtualExecutor.shutdown();

        System.out.println("Total Time: " + (System.currentTimeMillis() - start) + "ms");
    }
}

