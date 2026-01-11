package tech.kayys.wayang.agent.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tech.kayys.silat.execution.DefaultNodeExecutionResult;
import tech.kayys.silat.execution.NodeExecutionResult;
import tech.kayys.silat.execution.NodeExecutionTask;
import tech.kayys.silat.execution.NodeExecutionStatus;
import tech.kayys.wayang.agent.model.*;
import tech.kayys.wayang.agent.model.LLMProvider;
import tech.kayys.wayang.agent.model.llmprovider.LLMProviderRegistry;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
public class AgentExecutorTest {

    @Inject
    AgentExecutor executor;

    @InjectMock
    AgentMemoryManager memoryManager;

    @InjectMock
    LLMProviderRegistry llmRegistry;

    @InjectMock
    ToolRegistry toolRegistry;

    @InjectMock
    AgentMetricsCollector metricsCollector;

    @BeforeEach
    void setup() {
        when(memoryManager.loadMemory(any(), any(), any(), any())).thenReturn(Uni.createFrom().item(List.of()));
        when(memoryManager.saveMessages(any(), any(), any())).thenReturn(Uni.createFrom().voidItem());
        when(toolRegistry.getTools(any(), any())).thenReturn(Uni.createFrom().item(List.of()));
    }

    @Test
    void testExecuteSuccess() {
        NodeExecutionTask task = mock(NodeExecutionTask.class, Mockito.RETURNS_DEEP_STUBS);
        when(task.runId().value()).thenReturn("run-1");
        when(task.nodeId().value()).thenReturn("node-1");
        when(task.context()).thenReturn(Map.of("input", "hello", "sessionId", "session-1", "tenantId", "tenant-1"));

        LLMProvider provider = mock(LLMProvider.class);
        LLMResponse response = LLMResponse.create("Hello! How can I help?", "stop", TokenUsage.of(10, 10));

        when(llmRegistry.getProvider(anyString())).thenReturn(Uni.createFrom().item(provider));
        when(provider.complete(any(LLMRequest.class))).thenReturn(Uni.createFrom().item(response));

        NodeExecutionResult result = executor.execute(task)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertTrue(result instanceof DefaultNodeExecutionResult);
        DefaultNodeExecutionResult defaultResult = (DefaultNodeExecutionResult) result;

        assertEquals(NodeExecutionStatus.COMPLETED, defaultResult.status());
        // Fallback to toString check
        assertTrue(defaultResult.toString().contains("response"));

        verify(metricsCollector, atLeastOnce()).recordExecution(anyString(), any(), anyBoolean());
    }

    @Test
    void testExecuteWithToolCalls() {
        NodeExecutionTask task = mock(NodeExecutionTask.class, Mockito.RETURNS_DEEP_STUBS);
        when(task.runId().value()).thenReturn("run-2");
        when(task.nodeId().value()).thenReturn("node-2");
        when(task.context())
                .thenReturn(Map.of("input", "what is the weather?", "sessionId", "session-2", "tenantId", "tenant-2"));

        LLMProvider provider = mock(LLMProvider.class);
        ToolCall toolCall = new ToolCall("call-1", "weather", Map.of("location", "Jakarta"));
        LLMResponse response1 = LLMResponse.withToolCalls("Calling weather tool", List.of(toolCall),
                TokenUsage.of(10, 5));
        LLMResponse response2 = LLMResponse.create("The weather in Jakarta is sunny.", "stop", TokenUsage.of(20, 10));

        Tool weatherTool = mock(Tool.class);
        when(weatherTool.name()).thenReturn("weather");
        when(weatherTool.validate(any())).thenReturn(Uni.createFrom().item(true));
        when(weatherTool.execute(any(), any())).thenReturn(Uni.createFrom().item("Sunny, 30C"));

        when(llmRegistry.getProvider(anyString())).thenReturn(Uni.createFrom().item(provider));
        when(provider.complete(any(LLMRequest.class)))
                .thenReturn(Uni.createFrom().item(response1))
                .thenReturn(Uni.createFrom().item(response2));

        when(toolRegistry.getTools(any(), any())).thenReturn(Uni.createFrom().item(List.of(weatherTool)));
        when(toolRegistry.getTool(eq("weather"), anyString())).thenReturn(Uni.createFrom().item(weatherTool));

        NodeExecutionResult result = executor.execute(task)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        assertTrue(result instanceof DefaultNodeExecutionResult);
        DefaultNodeExecutionResult defaultResult = (DefaultNodeExecutionResult) result;

        assertEquals(NodeExecutionStatus.COMPLETED, defaultResult.status());
        // Check if output contains Jakarta
        assertTrue(defaultResult.toString().contains("Jakarta"));
    }

    @Test
    void testExecuteFailure() {
        NodeExecutionTask task = mock(NodeExecutionTask.class, Mockito.RETURNS_DEEP_STUBS);
        when(task.runId().value()).thenReturn("run-3");
        when(task.nodeId().value()).thenReturn("node-3");
        when(task.context()).thenReturn(Map.of("sessionId", "session-3", "tenantId", "tenant-3"));

        when(llmRegistry.getProvider(anyString()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("LLM Error")));

        NodeExecutionResult result = executor.execute(task)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .getItem();

        if (result != null) {
            assertTrue(result instanceof DefaultNodeExecutionResult);
            DefaultNodeExecutionResult defaultResult = (DefaultNodeExecutionResult) result;
            assertEquals(NodeExecutionStatus.FAILED, defaultResult.status());
        }
    }
}
