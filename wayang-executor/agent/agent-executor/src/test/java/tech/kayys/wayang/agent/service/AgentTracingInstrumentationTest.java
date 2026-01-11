package tech.kayys.wayang.agent.service;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class AgentTracingInstrumentationTest {

    @Inject
    AgentTracingInstrumentation instrumentation;

    @Inject
    Tracer tracer;

    @Test
    void testStartSpans() {
        Span executionSpan = instrumentation.startExecutionSpan("run-1", "node-1", "tenant-1");
        assertNotNull(executionSpan);
        executionSpan.end();

        Span llmSpan = instrumentation.startLLMSpan("openai", "gpt-4", 2);
        assertNotNull(llmSpan);
        llmSpan.end();

        Span toolSpan = instrumentation.startToolSpan("weather", Map.of("loc", "JKT"));
        assertNotNull(toolSpan);
        toolSpan.end();

        Span memorySpan = instrumentation.startMemorySpan("load", "session-1");
        assertNotNull(memorySpan);
        memorySpan.end();
    }

    @Test
    void testTraceExecution() {
        instrumentation.traceExecution("test-span", Map.of("key", "val"), () -> Uni.createFrom().item("done"))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem("done");
    }
}
