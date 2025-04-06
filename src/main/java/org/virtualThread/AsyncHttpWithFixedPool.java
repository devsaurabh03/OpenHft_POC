package org.virtualThread;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncHttpWithFixedPool {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newCachedThreadPool(); // adjust pool size
        HttpClient client = HttpClient.newBuilder()
                .executor(executor)
                .build();

        long start = System.currentTimeMillis();

        CompletableFuture<?>[] futures = new CompletableFuture[1000];
        for (int i = 0; i < 1000; i++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://httpbin.org/delay/5"))
                    .GET()
                    .build();

            futures[i] = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        //System.out.println("Status: " + response.statusCode() + ", Thread: " + Thread.currentThread());
                    });
        }

        CompletableFuture.allOf(futures).join();
        executor.shutdown();

        System.out.println("Total Time: " + (System.currentTimeMillis() - start) + "ms");
    }
}

