package tech.kayys.wayang.agent.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
//import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
//import org.eclipse.microprofile.faulttolerance.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AtomicDouble;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================================
 * OBSERVABILITY AND RESILIENCE LAYER
 * ============================================================================
 * 
 * Production-ready observability and resilience:
 * - Metrics (Micrometer/Prometheus)
 * - Distributed tracing (OpenTelemetry)
 * - Circuit breakers (Resilience4j)
 * - Bulkheads (isolation)
 * - Retries with backoff
 * - Timeouts
 * - Health checks
 * 
 * Architecture:
 * ┌────────────────────────────────────────────────────────┐
 * │           Observability & Resilience                    │
 * ├────────────────────────────────────────────────────────┤
 * │  ┌─────────┐  ┌─────────┐  ┌──────────┐  ┌─────────┐ │
 * │  │ Metrics │  │ Tracing │  │ Circuit  │  │ Health  │ │
 * │  │         │  │         │  │ Breaker  │  │ Check   │ │
 * │  └─────────┘  └─────────┘  └──────────┘  └─────────┘ │
 * └────────────────────────────────────────────────────────┘
 */

// ==================== METRICS INSTRUMENTATION ====================

/**
 * Metrics collector using Micrometer
 */
@ApplicationScoped
public class AgentMetricsInstrumentation {

        private static final Logger LOG = LoggerFactory.getLogger(AgentMetricsInstrumentation.class);

        private final MeterRegistry meterRegistry;

        // Counters
        private final Counter agentExecutionsTotal;
        private final Counter agentExecutionsSuccess;
        private final Counter agentExecutionsFailed;
        private final Counter tokensUsedTotal;
        private final Counter toolCallsTotal;

        // Gauges
        private final Map<String, AtomicDouble> activeExecutions = new ConcurrentHashMap<>();

        // Timers
        private final Map<String, Timer> executionTimers = new ConcurrentHashMap<>();
        private final Map<String, Timer> llmTimers = new ConcurrentHashMap<>();
        private final Map<String, Timer> toolTimers = new ConcurrentHashMap<>();

        @Inject
        public AgentMetricsInstrumentation(MeterRegistry meterRegistry) {
                this.meterRegistry = meterRegistry;

                // Initialize counters
                this.agentExecutionsTotal = Counter.builder("agent.executions.total")
                                .description("Total number of agent executions")
                                .register(meterRegistry);

                this.agentExecutionsSuccess = Counter.builder("agent.executions.success")
                                .description("Number of successful agent executions")
                                .register(meterRegistry);

                this.agentExecutionsFailed = Counter.builder("agent.executions.failed")
                                .description("Number of failed agent executions")
                                .register(meterRegistry);

                this.tokensUsedTotal = Counter.builder("agent.tokens.used.total")
                                .description("Total number of tokens used")
                                .register(meterRegistry);

                this.toolCallsTotal = Counter.builder("agent.toolcalls.total")
                                .description("Total number of tool calls")
                                .register(meterRegistry);

                LOG.info("Agent metrics instrumentation initialized");
        }

        /**
         * Record agent execution start
         */
        public void recordExecutionStart(String nodeId, String tenantId) {
                agentExecutionsTotal.increment();

                String key = makeKey(nodeId, tenantId);
                activeExecutions.computeIfAbsent(key, k -> {
                        AtomicDouble gauge = new AtomicDouble(0);
                        Gauge.builder("agent.executions.active", gauge, AtomicDouble::get)
                                        .tag("node", nodeId)
                                        .tag("tenant", tenantId)
                                        .description("Number of active agent executions")
                                        .register(meterRegistry);
                        return gauge;
                }).addAndGet(1.0);
        }

        /**
         * Record agent execution completion
         */
        public void recordExecutionComplete(
                        String nodeId,
                        String tenantId,
                        Duration duration,
                        boolean success) {

                if (success) {
                        agentExecutionsSuccess.increment();
                } else {
                        agentExecutionsFailed.increment();
                }

                // Decrement active executions
                String key = makeKey(nodeId, tenantId);
                AtomicDouble gauge = activeExecutions.get(key);
                if (gauge != null) {
                        gauge.addAndGet(-1.0);
                }

                // Record duration
                Timer timer = executionTimers.computeIfAbsent(key, k -> Timer.builder("agent.execution.duration")
                                .tag("node", nodeId)
                                .tag("tenant", tenantId)
                                .description("Agent execution duration")
                                .register(meterRegistry));

                timer.record(duration);
        }

        /**
         * Record token usage
         */
        public void recordTokenUsage(
                        String provider,
                        String model,
                        int promptTokens,
                        int completionTokens,
                        int totalTokens) {

                tokensUsedTotal.increment(totalTokens);

                Counter.builder("agent.tokens.prompt")
                                .tag("provider", provider)
                                .tag("model", model)
                                .register(meterRegistry)
                                .increment(promptTokens);

                Counter.builder("agent.tokens.completion")
                                .tag("provider", provider)
                                .tag("model", model)
                                .register(meterRegistry)
                                .increment(completionTokens);
        }

        /**
         * Record LLM call duration
         */
        public void recordLLMCall(String provider, String model, Duration duration) {
                String key = provider + ":" + model;

                Timer timer = llmTimers.computeIfAbsent(key, k -> Timer.builder("agent.llm.call.duration")
                                .tag("provider", provider)
                                .tag("model", model)
                                .description("LLM API call duration")
                                .register(meterRegistry));

                timer.record(duration);
        }

        /**
         * Record tool call
         */
        public void recordToolCall(String toolName, boolean success, Duration duration) {
                toolCallsTotal.increment();

                Counter.builder("agent.toolcalls." + (success ? "success" : "failed"))
                                .tag("tool", toolName)
                                .register(meterRegistry)
                                .increment();

                Timer timer = toolTimers.computeIfAbsent(toolName, k -> Timer.builder("agent.tool.call.duration")
                                .tag("tool", toolName)
                                .description("Tool call duration")
                                .register(meterRegistry));

                timer.record(duration);
        }

        private String makeKey(String nodeId, String tenantId) {
                return tenantId + ":" + nodeId;
        }
}