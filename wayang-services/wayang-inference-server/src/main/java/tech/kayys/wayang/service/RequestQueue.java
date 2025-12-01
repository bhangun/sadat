package tech.kayys.wayang.service;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.TimeoutException;
import tech.kayys.wayang.model.GenerationResult;

public class RequestQueue {
    private static final Logger log = Logger.getLogger(RequestQueue.class);
    
    private final PriorityBlockingQueue<QueuedRequest> queue;
    private final ExecutorService executor;
    private final Semaphore concurrencyLimit;
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    
    public RequestQueue() {
        this.queue = new PriorityBlockingQueue<>(
            1000,
            (a, b) -> Integer.compare(b.priority, a.priority)
        );
        this.executor = Executors.newFixedThreadPool(4);
        this.concurrencyLimit = new Semaphore(4);
        
        startWorker();
    }
    
    private void startWorker() {
        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    QueuedRequest request = queue.poll(1, TimeUnit.SECONDS);
                    if (request != null) {
                        concurrencyLimit.acquire();
                        executor.submit(() -> {
                            try {
                                processRequest(request);
                            } finally {
                                concurrencyLimit.release();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    public CompletableFuture<GenerationResult> submit(
            Callable<GenerationResult> task, 
            int priority,
            long timeoutMs) {
        
        CompletableFuture<GenerationResult> future = new CompletableFuture<>();
        String requestId = "req-" + requestCounter.incrementAndGet();
        
        QueuedRequest request = new QueuedRequest(
            requestId,
            task,
            priority,
            System.currentTimeMillis(),
            timeoutMs,
            future
        );
        
        queue.offer(request);
        log.debugf("Request queued: %s (priority=%d, queue_size=%d)", 
            requestId, priority, queue.size());
        
        return future;
    }
    
    private void processRequest(QueuedRequest request) {
        long waitTime = System.currentTimeMillis() - request.submittedAt;
        
        if (waitTime > request.timeoutMs) {
            request.future.completeExceptionally(
                new TimeoutException());
            return;
        }
        
        try {
            GenerationResult result = request.task.call();
            request.future.complete(result);
            
            log.debugf("Request completed: %s (waited=%dms)", 
                request.id, waitTime);
            
        } catch (Exception e) {
            request.future.completeExceptionally(e);
            log.errorf(e, "Request failed: %s", request.id);
        }
    }
    
    public QueueStats getStats() {
        return new QueueStats(
            queue.size(),
            4 - concurrencyLimit.availablePermits(),
            4
        );
    }
    
    private record QueuedRequest(
        String id,
        Callable<GenerationResult> task,
        int priority,
        long submittedAt,
        long timeoutMs,
        CompletableFuture<GenerationResult> future
    ) {}
    
    public record QueueStats(
        int queuedRequests,
        int activeRequests,
        int maxConcurrency
    ) {}
}
