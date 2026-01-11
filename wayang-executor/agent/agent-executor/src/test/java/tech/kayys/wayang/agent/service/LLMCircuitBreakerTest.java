package tech.kayys.wayang.agent.service;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class LLMCircuitBreakerTest {

    @Inject
    LLMCircuitBreaker circuitBreaker;

    @Test
    void testCallWithProtectionSuccess() {
        AtomicInteger callCount = new AtomicInteger(0);
        Uni<String> call = Uni.createFrom().item(() -> {
            callCount.incrementAndGet();
            return "success";
        });

        circuitBreaker.callWithProtection(() -> call)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem("success");

        assertEquals(1, callCount.get());
    }

    @Test
    void testFallback() {
        circuitBreaker.fallbackLLMCall()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure();
    }
}
