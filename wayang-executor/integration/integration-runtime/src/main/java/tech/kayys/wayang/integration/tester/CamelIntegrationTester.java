package tech.kayys.wayang.integration.tester;

import io.smallrye.mutiny.Uni;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.silat.core.domain.*;
import tech.kayys.silat.core.engine.NodeExecutionResult;
import tech.kayys.silat.core.engine.NodeExecutionTask;
import tech.kayys.silat.executor.camel.CamelIntegrationExecutor;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive tester for Camel Integration Executor
 * Tests all EIP patterns and integration capabilities
 */
public class CamelIntegrationTester {

    private static final Logger LOG = LoggerFactory.getLogger(CamelIntegrationTester.class);

    private final CamelIntegrationExecutor executor;
    private final CamelContext camelContext;
    private final ProducerTemplate producerTemplate;
    
    private static final String TEST_TENANT_ID = "test-tenant";
    private static final String TEST_RUN_PREFIX = "test-run-";
    private static final String TEST_NODE_PREFIX = "test-node-";

    public CamelIntegrationTester(CamelIntegrationExecutor executor, 
                                 CamelContext camelContext, 
                                 ProducerTemplate producerTemplate) {
        this.executor = executor;
        this.camelContext = camelContext;
        this.producerTemplate = producerTemplate;
    }

    /**
     * Run comprehensive test suite for all integration patterns
     */
    public TestResults runAllTests() {
        LOG.info("Starting comprehensive Camel Integration Executor tests...");
        
        TestResults results = new TestResults();
        
        // Test all EIP patterns
        results.addResult("Content-Based Router", testContentBasedRouter());
        results.addResult("Message Translator", testMessageTranslator());
        results.addResult("Splitter", testSplitter());
        results.addResult("Aggregator", testAggregator());
        results.addResult("Content Enricher", testContentEnricher());
        results.addResult("Message Filter", testMessageFilter());
        results.addResult("Recipient List", testRecipientList());
        results.addResult("Wire Tap", testWireTap());
        results.addResult("Multicast", testMulticast());
        results.addResult("Circuit Breaker", testCircuitBreaker());
        results.addResult("Saga Pattern", testSagaPattern());
        results.addResult("Resequencer", testResequencer());
        
        // Performance tests
        results.addResult("High Throughput", testHighThroughput());
        results.addResult("Concurrent Operations", testConcurrentOperations());
        
        // Error handling tests
        results.addResult("Error Handling", testErrorHandling());
        results.addResult("Timeout Handling", testTimeoutHandling());
        
        LOG.info("Test Summary - Passed: {}, Failed: {}, Total: {}", 
                 results.getPassedCount(), results.getFailedCount(), results.getTotalCount());
        
        return results;
    }

    /**
     * Test Content-Based Router pattern
     */
    public TestResult testContentBasedRouter() {
        LOG.info("Testing Content-Based Router pattern...");
        
        try {
            // Setup mock endpoints
            MockEndpoint mock1 = camelContext.getEndpoint("mock:router-dest1", MockEndpoint.class);
            MockEndpoint mock2 = camelContext.getEndpoint("mock:router-dest2", MockEndpoint.class);
            MockEndpoint mockDefault = camelContext.getEndpoint("mock:router-default", MockEndpoint.class);

            mock1.expectedMessageCount(1);
            mock2.expectedMessageCount(0);
            mockDefault.expectedMessageCount(0);

            // Create task for urgent message
            NodeExecutionTask task = createTask(
                "CONTENT_BASED_ROUTER",
                Map.of(
                    "payload", Map.of("type", "urgent", "priority", 1),
                    "targetEndpoints", Arrays.asList("mock:router-dest1", "mock:router-dest2"),
                    "routingRules", Map.of(
                        "rule1", "${body[type]} == 'urgent'",
                        "rule2", "${body[type]} == 'normal'"
                    ),
                    "defaultEndpoint", "mock:router-default",
                    "tenantId", TEST_TENANT_ID
                )
            );

            // Execute
            NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(10));

            // Validate
            mock1.assertIsSatisfied();
            mock2.assertIsSatisfied();
            mockDefault.assertIsSatisfied();

            if (result.status().equals(NodeExecutionStatus.COMPLETED) && result.output() != null) {
                LOG.info("Content-Based Router test PASSED");
                return TestResult.passed("Content-Based Router executed successfully");
            } else {
                LOG.error("Content-Based Router test FAILED - Status: {}, Error: {}", 
                         result.status(), result.error());
                return TestResult.failed("Execution failed: " + result.error());
            }

        } catch (Exception e) {
            LOG.error("Content-Based Router test FAILED with exception", e);
            return TestResult.failed("Exception: " + e.getMessage());
        }
    }

    /**
     * Test Message Translator pattern
     */
    public TestResult testMessageTranslator() {
        LOG.info("Testing Message Translator pattern...");
        
        try {
            NodeExecutionTask task = createTask(
                "MESSAGE_TRANSLATOR",
                Map.of(
                    "payload", Map.of("name", "John", "age", 30),
                    "transformationType", "json",
                    "tenantId", TEST_TENANT_ID
                )
            );

            NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(10));

            if (result.status().equals(NodeExecutionStatus.COMPLETED) && 
                result.output().containsKey("transformed")) {
                LOG.info("Message Translator test PASSED");
                return TestResult.passed("Message translation completed successfully");
            } else {
                LOG.error("Message Translator test FAILED - Status: {}, Error: {}", 
                         result.status(), result.error());
                return TestResult.failed("Execution failed: " + result.error());
            }

        } catch (Exception e) {
            LOG.error("Message Translator test FAILED with exception", e);
            return TestResult.failed("Exception: " + e.getMessage());
        }
    }

    /**
     * Test Splitter pattern
     */
    public TestResult testSplitter() {
        LOG.info("Testing Splitter pattern...");
        
        try {
            String csvData = "item1,item2,item3,item4,item5";

            NodeExecutionTask task = createTask(
                "SPLITTER",
                Map.of(
                    "payload", csvData,
                    "splitDelimiter", ",",
                    "parallelProcessing", false,
                    "targetEndpoints", Arrays.asList("mock:split-target"),
                    "tenantId", TEST_TENANT_ID
                )
            );

            NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(10));

            if (result.status().equals(NodeExecutionStatus.COMPLETED) && 
                result.output().containsKey("splits")) {
                
                List<?> splits = (List<?>) result.output().get("splits");
                if (splits.size() == 5) {
                    LOG.info("Splitter test PASSED - Split {} items correctly", splits.size());
                    return TestResult.passed("Splitter executed successfully, split " + splits.size() + " items");
                } else {
                    LOG.error("Splitter test FAILED - Expected 5 splits, got {}", splits.size());
                    return TestResult.failed("Incorrect split count: expected 5, got " + splits.size());
                }
            } else {
                LOG.error("Splitter test FAILED - Status: {}, Error: {}", 
                         result.status(), result.error());
                return TestResult.failed("Execution failed: " + result.error());
            }

        } catch (Exception e) {
            LOG.error("Splitter test FAILED with exception", e);
            return TestResult.failed("Exception: " + e.getMessage());
        }
    }

    /**
     * Test Aggregator pattern
     */
    public TestResult testAggregator() {
        LOG.info("Testing Aggregator pattern...");
        
        try {
            List<Object> messages = Arrays.asList(
                Map.of("id", 1, "value", "A"),
                Map.of("id", 2, "value", "B"),
                Map.of("id", 3, "value", "C")
            );

            NodeExecutionTask task = createTask(
                "AGGREGATOR",
                Map.of(
                    "payload", messages,
                    "batchSize", 3,
                    "aggregationTimeout", 5000L,
                    "aggregationStrategy", "collect",
                    "tenantId", TEST_TENANT_ID
                )
            );

            NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(10));

            if (result.status().equals(NodeExecutionStatus.COMPLETED) && 
                result.output().containsKey("aggregated")) {
                LOG.info("Aggregator test PASSED");
                return TestResult.passed("Aggregator executed successfully");
            } else {
                LOG.error("Aggregator test FAILED - Status: {}, Error: {}", 
                         result.status(), result.error());
                return TestResult.failed("Execution failed: " + result.error());
            }

        } catch (Exception e) {
            LOG.error("Aggregator test FAILED with exception", e);
            return TestResult.failed("Exception: " + e.getMessage());
        }
    }

    /**
     * Test Content Enricher pattern
     */
    public TestResult testContentEnricher() {
        LOG.info("Testing Content Enricher pattern...");
        
        try {
            // Setup enrichment endpoint
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:test-enrichment-source")
                        .setBody(constant(Map.of("additionalData", "enriched")));
                }
            });

            NodeExecutionTask task = createTask(
                "CONTENT_ENRICHER",
                Map.of(
                    "payload", Map.of("original", "data"),
                    "enrichmentEndpoint", "direct:test-enrichment-source",
                    "enrichmentStrategy", "merge",
                    "tenantId", TEST_TENANT_ID
                )
            );

            NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(10));

            if (result.status().equals(NodeExecutionStatus.COMPLETED) && 
                result.output().containsKey("enriched")) {
                
                Map<String, Object> enriched = (Map<String, Object>) result.output().get("enriched");
                if (enriched.containsKey("original") && enriched.containsKey("additionalData")) {
                    LOG.info("Content Enricher test PASSED");
                    return TestResult.passed("Content Enricher executed successfully");
                } else {
                    LOG.error("Content Enricher test FAILED - Missing expected keys in enriched data");
                    return TestResult.failed("Enrichment failed - missing expected keys");
                }
            } else {
                LOG.error("Content Enricher test FAILED - Status: {}, Error: {}", 
                         result.status(), result.error());
                return TestResult.failed("Execution failed: " + result.error());
            }

        } catch (Exception e) {
            LOG.error("Content Enricher test FAILED with exception", e);
            return TestResult.failed("Exception: " + e.getMessage());
        }
    }

    /**
     * Test Message Filter pattern
     */
    public TestResult testMessageFilter() {
        LOG.info("Testing Message Filter pattern...");
        
        try {
            NodeExecutionTask task = createTask(
                "MESSAGE_FILTER",
                Map.of(
                    "payload", Map.of("status", "active", "priority", 1),
                    "filterExpression", "${body[status]} == 'active'",
                    "tenantId", TEST_TENANT_ID
                )
            );

            NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(10));

            if (result.status().equals(NodeExecutionStatus.COMPLETED) && 
                result.output().containsKey("passed")) {
                
                Boolean passed = (Boolean) result.output().get("passed");
                if (passed) {
                    LOG.info("Message Filter test PASSED - Message passed filter");
                    return TestResult.passed("Message Filter executed successfully");
                } else {
                    LOG.error("Message Filter test FAILED - Message did not pass filter");
                    return TestResult.failed("Message did not pass filter when it should have");
                }
            } else {
                LOG.error("Message Filter test FAILED - Status: {}, Error: {}", 
                         result.status(), result.error());
                return TestResult.failed("Execution failed: " + result.error());
            }

        } catch (Exception e) {
            LOG.error("Message Filter test FAILED with exception", e);
            return TestResult.failed("Exception: " + e.getMessage());
        }
    }

    /**
     * Test Recipient List pattern
     */
    public TestResult testRecipientList() {
        LOG.info("Testing Recipient List pattern...");
        
        try {
            MockEndpoint mock1 = camelContext.getEndpoint("mock:recipient1", MockEndpoint.class);
            MockEndpoint mock2 = camelContext.getEndpoint("mock:recipient2", MockEndpoint.class);
            MockEndpoint mock3 = camelContext.getEndpoint("mock:recipient3", MockEndpoint.class);

            mock1.expectedMessageCount(1);
            mock2.expectedMessageCount(1);
            mock3.expectedMessageCount(1);

            NodeExecutionTask task = createTask(
                "RECIPIENT_LIST",
                Map.of(
                    "payload", Map.of("message", "broadcast"),
                    "targetEndpoints", Arrays.asList(
                        "mock:recipient1",
                        "mock:recipient2", 
                        "mock:recipient3"
                    ),
                    "parallelProcessing", true,
                    "tenantId", TEST_TENANT_ID
                )
            );

            NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(10));

            mock1.assertIsSatisfied();
            mock2.assertIsSatisfied();
            mock3.assertIsSatisfied();

            if (result.status().equals(NodeExecutionStatus.COMPLETED) && 
                result.output().containsKey("responses")) {
                
                List<?> responses = (List<?>) result.output().get("responses");
                if (responses.size() == 3) {
                    LOG.info("Recipient List test PASSED - Sent to {} recipients", responses.size());
                    return TestResult.passed("Recipient List executed successfully");
                } else {
                    LOG.error("Recipient List test FAILED - Expected 3 responses, got {}", responses.size());
                    return TestResult.failed("Incorrect response count: expected 3, got " + responses.size());
                }
            } else {
                LOG.error("Recipient List test FAILED - Status: {}, Error: {}", 
                         result.status(), result.error());
                return TestResult.failed("Execution failed: " + result.error());
            }

        } catch (Exception e) {
            LOG.error("Recipient List test FAILED with exception", e);
            return TestResult.failed("Exception: " + e.getMessage());
        }
    }

    /**
     * Test Wire Tap pattern
     */
    public TestResult testWireTap() {
        LOG.info("Testing Wire Tap pattern...");
        
        try {
            MockEndpoint wireTap = camelContext.getEndpoint("mock:wiresource", MockEndpoint.class);
            MockEndpoint target = camelContext.getEndpoint("mock:wiredest", MockEndpoint.class);

            wireTap.expectedMessageCount(1);
            target.expectedMessageCount(1);

            NodeExecutionTask task = createTask(
                "WIRE_TAP",
                Map.of(
                    "payload", Map.of("data", "test"),
                    "wireTapEndpoint", "mock:wiresource",
                    "targetEndpoints", Arrays.asList("mock:wiredest"),
                    "tenantId", TEST_TENANT_ID
                )
            );

            NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(10));

            wireTap.assertIsSatisfied();
            target.assertIsSatisfied();

            if (result.status().equals(NodeExecutionStatus.COMPLETED)) {
                LOG.info("Wire Tap test PASSED");
                return TestResult.passed("Wire Tap executed successfully");
            } else {
                LOG.error("Wire Tap test FAILED - Status: {}, Error: {}", 
                         result.status(), result.error());
                return TestResult.failed("Execution failed: " + result.error());
            }

        } catch (Exception e) {
            LOG.error("Wire Tap test FAILED with exception", e);
            return TestResult.failed("Exception: " + e.getMessage());
        }
    }

    /**
     * Test Multicast pattern
     */
    public TestResult testMulticast() {
        LOG.info("Testing Multicast pattern...");
        
        try {
            MockEndpoint mock1 = camelContext.getEndpoint("mock:multi1", MockEndpoint.class);
            MockEndpoint mock2 = camelContext.getEndpoint("mock:multi2", MockEndpoint.class);

            mock1.expectedMessageCount(1);
            mock2.expectedMessageCount(1);

            NodeExecutionTask task = createTask(
                "MULTICAST",
                Map.of(
                    "payload", Map.of("broadcast", "message"),
                    "targetEndpoints", Arrays.asList("mock:multi1", "mock:multi2"),
                    "tenantId", TEST_TENANT_ID
                )
            );

            NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(10));

            mock1.assertIsSatisfied();
            mock2.assertIsSatisfied();

            if (result.status().equals(NodeExecutionStatus.COMPLETED)) {
                LOG.info("Multicast test PASSED");
                return TestResult.passed("Multicast executed successfully");
            } else {
                LOG.error("Multicast test FAILED - Status: {}, Error: {}", 
                         result.status(), result.error());
                return TestResult.failed("Execution failed: " + result.error());
            }

        } catch (Exception e) {
            LOG.error("Multicast test FAILED with exception", e);
            return TestResult.failed("Exception: " + e.getMessage());
        }
    }

    /**
     * Test Circuit Breaker pattern
     */
    public TestResult testCircuitBreaker() {
        LOG.info("Testing Circuit Breaker pattern...");
        
        try {
            // Setup target endpoint
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:cb-target")
                        .setBody(constant(Map.of("result", "success")));
                }
            });

            NodeExecutionTask task = createTask(
                "CIRCUIT_BREAKER",
                Map.of(
                    "payload", Map.of("request", "test"),
                    "targetEndpoints", Arrays.asList("direct:cb-target"),
                    "circuitBreakerThreshold", 5,
                    "halfOpenAfter", 30000L,
                    "fallbackResponse", Map.of("result", "fallback"),
                    "tenantId", TEST_TENANT_ID
                )
            );

            NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(10));

            if (result.status().equals(NodeExecutionStatus.COMPLETED) && 
                result.output().containsKey("result")) {
                LOG.info("Circuit Breaker test PASSED");
                return TestResult.passed("Circuit Breaker executed successfully");
            } else {
                LOG.error("Circuit Breaker test FAILED - Status: {}, Error: {}", 
                         result.status(), result.error());
                return TestResult.failed("Execution failed: " + result.error());
            }

        } catch (Exception e) {
            LOG.error("Circuit Breaker test FAILED with exception", e);
            return TestResult.failed("Exception: " + e.getMessage());
        }
    }

    /**
     * Test Saga Pattern
     */
    public TestResult testSagaPattern() {
        LOG.info("Testing Saga Pattern...");
        
        try {
            // Setup saga endpoints
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:saga-step1")
                        .setBody(constant(Map.of("step1", "completed")));
                    from("direct:saga-step2")
                        .setBody(constant(Map.of("step2", "completed")));
                    from("direct:saga-step3")
                        .setBody(constant(Map.of("step3", "completed")));
                    from("direct:saga-compensate1")
                        .setBody(constant(Map.of("compensate1", "executed")));
                    from("direct:saga-compensate2")
                        .setBody(constant(Map.of("compensate2", "executed")));
                }
            });

            NodeExecutionTask task = createTask(
                "SAGA",
                Map.of(
                    "payload", Map.of("initial", "data"),
                    "targetEndpoints", Arrays.asList("direct:saga-step1", "direct:saga-step2", "direct:saga-step3"),
                    "compensationEndpoints", Arrays.asList("direct:saga-compensate1", "direct:saga-compensate2"),
                    "sagaTimeout", 300L,
                    "tenantId", TEST_TENANT_ID
                )
            );

            NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(15));

            if (result.status().equals(NodeExecutionStatus.COMPLETED) && 
                result.output().containsKey("sagaCompleted")) {
                LOG.info("Saga Pattern test PASSED");
                return TestResult.passed("Saga Pattern executed successfully");
            } else {
                LOG.error("Saga Pattern test FAILED - Status: {}, Error: {}", 
                         result.status(), result.error());
                return TestResult.failed("Execution failed: " + result.error());
            }

        } catch (Exception e) {
            LOG.error("Saga Pattern test FAILED with exception", e);
            return TestResult.failed("Exception: " + e.getMessage());
        }
    }

    /**
     * Test Resequencer pattern
     */
    public TestResult testResequencer() {
        LOG.info("Testing Resequencer pattern...");
        
        try {
            List<Object> messages = Arrays.asList(
                Map.of("id", 3, "data", "third"),
                Map.of("id", 1, "data", "first"), 
                Map.of("id", 2, "data", "second")
            );

            NodeExecutionTask task = createTask(
                "RESEQUENCER",
                Map.of(
                    "payload", messages,
                    "targetEndpoints", Arrays.asList("mock:reseq-target"),
                    "tenantId", TEST_TENANT_ID
                )
            );

            NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(10));

            if (result.status().equals(NodeExecutionStatus.COMPLETED)) {
                LOG.info("Resequencer test PASSED");
                return TestResult.passed("Resequencer executed successfully");
            } else {
                LOG.error("Resequencer test FAILED - Status: {}, Error: {}", 
                         result.status(), result.error());
                return TestResult.failed("Execution failed: " + result.error());
            }

        } catch (Exception e) {
            LOG.error("Resequencer test FAILED with exception", e);
            return TestResult.failed("Exception: " + e.getMessage());
        }
    }

    /**
     * Test high throughput scenario
     */
    public TestResult testHighThroughput() {
        LOG.info("Testing high throughput scenario...");
        
        try {
            int messageCount = 50;
            long startTime = System.currentTimeMillis();

            List<Uni<NodeExecutionResult>> futures = new ArrayList<>();

            for (int i = 0; i < messageCount; i++) {
                NodeExecutionTask task = createTask(
                    "MESSAGE_TRANSLATOR",
                    Map.of(
                        "payload", Map.of("messageId", i, "data", "test-data-" + i),
                        "transformationType", "json",
                        "tenantId", TEST_TENANT_ID + "-ht-" + i
                    )
                );

                Uni<NodeExecutionResult> future = executor.execute(task);
                futures.add(future);
            }

            // Wait for all to complete
            for (Uni<NodeExecutionResult> future : futures) {
                NodeExecutionResult result = future.await().atMost(Duration.ofSeconds(30));
                if (!result.status().equals(NodeExecutionStatus.COMPLETED)) {
                    LOG.error("High throughput test FAILED - One or more executions failed");
                    return TestResult.failed("One or more executions failed");
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            double throughput = (messageCount * 1000.0) / duration;

            LOG.info("High throughput test PASSED - {} messages in {} ms ({:.2f} msg/sec)", 
                     messageCount, duration, throughput);
            
            return TestResult.passed(String.format("High throughput: %.2f msg/sec", throughput));

        } catch (Exception e) {
            LOG.error("High throughput test FAILED with exception", e);
            return TestResult.failed("Exception: " + e.getMessage());
        }
    }

    /**
     * Test concurrent operations
     */
    public TestResult testConcurrentOperations() {
        LOG.info("Testing concurrent operations...");
        
        try {
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<TestResult> results = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                Thread t = new Thread(() -> {
                    try {
                        TestResult result = runSingleOperationTest("thread-" + threadIndex);
                        results.add(result);
                    } catch (Exception e) {
                        results.add(TestResult.failed("Thread " + threadIndex + " failed: " + e.getMessage()));
                    } finally {
                        latch.countDown();
                    }
                });
                t.start();
            }

            // Wait for all threads to complete
            boolean completed = latch.await(60, TimeUnit.SECONDS);
            if (!completed) {
                LOG.error("Concurrent operations test TIMEOUT - Not all threads completed");
                return TestResult.failed("Timeout waiting for threads to complete");
            }

            long passedCount = results.stream().filter(TestResult::isPassed).count();
            if (passedCount == threadCount) {
                LOG.info("Concurrent operations test PASSED - All {} threads succeeded", threadCount);
                return TestResult.passed("Concurrent operations: " + passedCount + "/" + threadCount + " passed");
            } else {
                LOG.error("Concurrent operations test PARTIAL - {} out of {} threads passed", 
                         passedCount, threadCount);
                return TestResult.failed("Only " + passedCount + "/" + threadCount + " threads passed");
            }

        } catch (Exception e) {
            LOG.error("Concurrent operations test FAILED with exception", e);
            return TestResult.failed("Exception: " + e.getMessage());
        }
    }

    /**
     * Helper method for concurrent test
     */
    private TestResult runSingleOperationTest(String threadId) {
        try {
            NodeExecutionTask task = createTask(
                "MESSAGE_TRANSLATOR",
                Map.of(
                    "payload", Map.of("threadId", threadId, "data", "concurrent-test"),
                    "transformationType", "json",
                    "tenantId", TEST_TENANT_ID + "-" + threadId
                )
            );

            NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(10));

            if (result.status().equals(NodeExecutionStatus.COMPLETED)) {
                return TestResult.passed("Thread " + threadId + " succeeded");
            } else {
                return TestResult.failed("Thread " + threadId + " failed: " + result.error());
            }
        } catch (Exception e) {
            return TestResult.failed("Thread " + threadId + " exception: " + e.getMessage());
        }
    }

    /**
     * Test error handling
     */
    public TestResult testErrorHandling() {
        LOG.info("Testing error handling...");
        
        try {
            NodeExecutionTask task = createTask(
                "INVALID_PATTERN_TYPE",
                Map.of(
                    "payload", Map.of("data", "test"),
                    "tenantId", TEST_TENANT_ID
                )
            );

            NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(10));

            // Should fail gracefully
            if (result.status().equals(NodeExecutionStatus.FAILED) && result.error() != null) {
                LOG.info("Error handling test PASSED - Error handled gracefully");
                return TestResult.passed("Error handling worked correctly");
            } else {
                LOG.error("Error handling test FAILED - Expected failure but got success");
                return TestResult.failed("Expected failure but operation succeeded");
            }

        } catch (Exception e) {
            LOG.error("Error handling test FAILED with unexpected exception", e);
            return TestResult.failed("Unexpected exception: " + e.getMessage());
        }
    }

    /**
     * Test timeout handling
     */
    public TestResult testTimeoutHandling() {
        LOG.info("Testing timeout handling...");
        
        try {
            // Create a slow endpoint that will timeout
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:slow-endpoint")
                        .delay(60000) // 60 seconds delay
                        .setBody(constant("slow-response"));
                }
            });

            NodeExecutionTask task = createTask(
                "GENERIC",
                Map.of(
                    "payload", Map.of("data", "test"),
                    "targetEndpoints", Arrays.asList("direct:slow-endpoint"),
                    "tenantId", TEST_TENANT_ID
                )
            );

            // This should timeout
            NodeExecutionResult result = executor.execute(task)
                .await().atMost(Duration.ofSeconds(5)); // Short timeout

            // Check if operation timed out appropriately
            if (result != null) {
                LOG.info("Timeout handling test PASSED - Operation handled timeout appropriately");
                return TestResult.passed("Timeout handling worked correctly");
            } else {
                LOG.error("Timeout handling test FAILED - Unexpected result");
                return TestResult.failed("Unexpected result for timeout test");
            }

        } catch (Exception e) {
            LOG.error("Timeout handling test FAILED with exception", e);
            return TestResult.failed("Exception: " + e.getMessage());
        }
    }

    /**
     * Create a test task
     */
    private NodeExecutionTask createTask(String patternType, Map<String, Object> context) {
        Map<String, Object> fullContext = new HashMap<>(context);
        fullContext.put("patternType", patternType);

        return new NodeExecutionTask(
            WorkflowRunId.of(TEST_RUN_PREFIX + UUID.randomUUID()),
            NodeId.of(TEST_NODE_PREFIX + patternType.toLowerCase().replace("_", "-") + "-" + System.currentTimeMillis()),
            1,
            new ExecutionToken(
                UUID.randomUUID().toString(),
                WorkflowRunId.of(TEST_RUN_PREFIX + "main"),
                NodeId.of(TEST_NODE_PREFIX + "main"),
                1,
                Instant.now().plusSeconds(3600)
            ),
            fullContext
        );
    }

    /**
     * Test Result class
     */
    public static class TestResult {
        private final boolean passed;
        private final String message;
        private final long timestamp;

        private TestResult(boolean passed, String message) {
            this.passed = passed;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public static TestResult passed(String message) {
            return new TestResult(true, message);
        }

        public static TestResult failed(String message) {
            return new TestResult(false, message);
        }

        public boolean isPassed() {
            return passed;
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s", 
                new Date(timestamp), 
                passed ? "PASS" : "FAIL", 
                message);
        }
    }

    /**
     * Test Results container
     */
    public static class TestResults {
        private final Map<String, TestResult> results = new LinkedHashMap<>();
        private final long startTime = System.currentTimeMillis();

        public void addResult(String testName, TestResult result) {
            results.put(testName, result);
            LOG.info("Test [{}] - {}", testName, result);
        }

        public int getTotalCount() {
            return results.size();
        }

        public int getPassedCount() {
            return (int) results.values().stream().filter(TestResult::isPassed).count();
        }

        public int getFailedCount() {
            return (int) results.values().stream().filter(r -> !r.isPassed()).count();
        }

        public boolean isAllPassed() {
            return getFailedCount() == 0;
        }

        public Map<String, TestResult> getResults() {
            return new LinkedHashMap<>(results);
        }

        public long getDuration() {
            return System.currentTimeMillis() - startTime;
        }

        @Override
        public String toString() {
            return String.format("TestResults{total=%d, passed=%d, failed=%d, duration=%dms}", 
                getTotalCount(), getPassedCount(), getFailedCount(), getDuration());
        }
    }
}