package tech.kayys.silat.examples;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.silat.agent.core.*;
import tech.kayys.silat.agent.executor.CommonAgentExecutor;
import tech.kayys.silat.agent.memory.*;
import tech.kayys.silat.agent.model.*;
import tech.kayys.silat.agent.tools.*;
import tech.kayys.silat.client.*;
import tech.kayys.silat.api.dto.*;

import java.util.*;

/**
 * ============================================================================
 * COMPLETE END-TO-END EXAMPLE
 * ============================================================================
 * 
 * This example demonstrates the full Silat Agent Executor system:
 * 1. Workflow definition with AI agent node
 * 2. Custom tool creation and registration
 * 3. Agent execution with memory and tools
 * 4. Multi-turn conversation
 * 5. Monitoring and metrics
 * 6. Error handling and resilience
 */

// ==================== EXAMPLE 1: CUSTOMER SUPPORT AGENT ====================

@ApplicationScoped
public class CustomerSupportAgentExample {
    
    @Inject
    SilatClient client;
    
    @Inject
    ToolRegistry toolRegistry;
    
    /**
     * Complete customer support workflow with AI agent
     */
    public void runExample() {
        System.out.println("=".repeat(80));
        System.out.println("SILAT AGENT EXECUTOR - CUSTOMER SUPPORT EXAMPLE");
        System.out.println("=".repeat(80));
        
        // Step 1: Register custom tools
        registerCustomerTools();
        
        // Step 2: Create workflow with agent
        String workflowId = createCustomerSupportWorkflow();
        
        // Step 3: Execute conversation
        String sessionId = "session-" + UUID.randomUUID();
        executeConversation(workflowId, sessionId);
        
        // Step 4: View metrics
        viewMetrics();
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("EXAMPLE COMPLETED SUCCESSFULLY");
        System.out.println("=".repeat(80));
    }
    
    /**
     * Step 1: Register custom tools for customer support
     */
    private void registerCustomerTools() {
        System.out.println("\n[Step 1] Registering custom tools...");
        
        // Register order lookup tool
        OrderLookupTool orderTool = new OrderLookupTool();
        toolRegistry.registerTool(orderTool, "acme-corp");
        
        // Register refund policy tool
        RefundPolicyTool refundTool = new RefundPolicyTool();
        toolRegistry.registerTool(refundTool, "acme-corp");
        
        System.out.println("✓ Registered: order_lookup");
        System.out.println("✓ Registered: refund_policy");
    }
    
    /**
     * Step 2: Create workflow with AI agent node
     */
    private String createCustomerSupportWorkflow() {
        System.out.println("\n[Step 2] Creating customer support workflow...");
        
        WorkflowDefinitionResponse workflow = client.workflows()
            .create("customer-support-ai")
            .version("1.0.0")
            .description("AI-powered customer support with tools")
            
            // Add AI agent node
            .addNode(new NodeDefinitionDto(
                "support-agent",
                "Customer Support Agent",
                "TASK",
                "common-agent",
                Map.of(
                    // LLM Configuration
                    "llm.provider", "openai",
                    "llm.model", "gpt-4",
                    "llm.temperature", 0.7,
                    "llm.maxTokens", 2000,
                    
                    // Memory Configuration
                    "memory.enabled", true,
                    "memory.type", "buffer",
                    "memory.windowSize", 10,
                    
                    // Tools Configuration
                    "tools.enabled", List.of(
                        "order_lookup",
                        "refund_policy",
                        "current_time"
                    ),
                    "tools.allowCalls", true,
                    
                    // System Prompt
                    "systemPrompt", """
                        You are a helpful customer support agent for ACME Corp.
                        
                        Your responsibilities:
                        - Help customers with orders and refunds
                        - Use the order_lookup tool to check order status
                        - Use the refund_policy tool for refund information
                        - Be polite, professional, and empathetic
                        
                        Guidelines:
                        - Always greet the customer warmly
                        - Ask clarifying questions if needed
                        - Use tools to get accurate information
                        - Provide clear, step-by-step instructions
                        - End with asking if there's anything else you can help with
                        """,
                    
                    "maxIterations", 5
                ),
                List.of(), // No dependencies
                List.of(), // No transitions
                new RetryPolicyDto(3, 1, 60, 2.0, List.of()),
                120L,
                false
            ))
            
            .addInput("input", new InputDefinitionDto(
                "input", "string", true, null, "Customer message"))
            .addInput("sessionId", new InputDefinitionDto(
                "sessionId", "string", true, null, "Session ID"))
            .addInput("customerId", new InputDefinitionDto(
                "customerId", "string", false, null, "Customer ID"))
            
            .addOutput("response", new OutputDefinitionDto(
                "response", "string", "Agent response"))
            .addOutput("iterations", new OutputDefinitionDto(
                "iterations", "integer", "Number of iterations"))
            
            .execute()
            .await().indefinitely();
        
        System.out.println("✓ Workflow created: " + workflow.definitionId());
        return workflow.definitionId();
    }
    
    /**
     * Step 3: Execute multi-turn conversation
     */
    private void executeConversation(String workflowId, String sessionId) {
        System.out.println("\n[Step 3] Executing conversation...");
        System.out.println("Session ID: " + sessionId);
        System.out.println();
        
        // Turn 1: Customer asks about order
        System.out.println("Turn 1:");
        System.out.println("Customer: Can you check the status of my order ORDER-12345?");
        
        RunResponse run1 = client.runs()
            .create(workflowId)
            .input("input", "Can you check the status of my order ORDER-12345?")
            .input("sessionId", sessionId)
            .input("customerId", "CUST-67890")
            .label("channel", "web")
            .label("priority", "normal")
            .executeAndStart()
            .await().indefinitely();
        
        waitForCompletion(run1.runId());
        
        run1 = client.runs().get(run1.runId()).await().indefinitely();
        System.out.println("Agent: " + run1.variables().get("response"));
        System.out.println("(Used " + run1.variables().get("iterations") + " iterations)");
        System.out.println();
        
        // Turn 2: Customer asks about refund
        System.out.println("Turn 2:");
        System.out.println("Customer: What's your refund policy?");
        
        RunResponse run2 = client.runs()
            .create(workflowId)
            .input("input", "What's your refund policy?")
            .input("sessionId", sessionId) // Same session - agent remembers context
            .input("customerId", "CUST-67890")
            .executeAndStart()
            .await().indefinitely();
        
        waitForCompletion(run2.runId());
        
        run2 = client.runs().get(run2.runId()).await().indefinitely();
        System.out.println("Agent: " + run2.variables().get("response"));
        System.out.println();
        
        // Turn 3: Customer thanks and ends conversation
        System.out.println("Turn 3:");
        System.out.println("Customer: Great, thank you!");
        
        RunResponse run3 = client.runs()
            .create(workflowId)
            .input("input", "Great, thank you!")
            .input("sessionId", sessionId)
            .executeAndStart()
            .await().indefinitely();
        
        waitForCompletion(run3.runId());
        
        run3 = client.runs().get(run3.runId()).await().indefinitely();
        System.out.println("Agent: " + run3.variables().get("response"));
    }
    
    /**
     * Step 4: View metrics and statistics
     */
    private void viewMetrics() {
        System.out.println("\n[Step 4] Viewing metrics...");
        
        // This would query your metrics endpoint
        System.out.println("\nMetrics Summary:");
        System.out.println("- Total Executions: 3");
        System.out.println("- Success Rate: 100%");
        System.out.println("- Average Duration: 2.5s");
        System.out.println("- Total Tokens Used: 1,234");
        System.out.println("- Tool Calls: 2 (order_lookup, refund_policy)");
    }
    
    private void waitForCompletion(String runId) {
        while (true) {
            RunResponse run = client.runs().get(runId).await().indefinitely();
            
            if (isTerminal(run.status())) {
                break;
            }
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private boolean isTerminal(String status) {
        return status.equals("COMPLETED") || 
               status.equals("FAILED") || 
               status.equals("CANCELLED");
    }
}

// ==================== CUSTOM TOOLS ====================

/**
 * Custom tool: Order Lookup
 */
@ApplicationScoped
class OrderLookupTool extends AbstractTool {
    
    public OrderLookupTool() {
        super(
            "order_lookup",
            "Look up order status and details by order ID"
        );
    }
    
    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "orderId", Map.of(
                    "type", "string",
                    "description", "The order ID to look up (e.g., ORDER-12345)"
                )
            ),
            "required", List.of("orderId")
        );
    }
    
    @Override
    public Uni<String> execute(Map<String, Object> arguments, AgentContext context) {
        String orderId = getParam(arguments, "orderId", String.class);
        
        // Simulate database lookup
        return Uni.createFrom().item(() -> {
            // Mock order data
            if (orderId.equals("ORDER-12345")) {
                return """
                    Order Status: Shipped
                    Order ID: ORDER-12345
                    Customer: John Doe
                    Items: 
                      - Product A (Qty: 2) - $29.99
                      - Product B (Qty: 1) - $49.99
                    Total: $109.97
                    Shipped Date: 2024-01-15
                    Tracking: TRACK-ABC123
                    Expected Delivery: 2024-01-20
                    """;
            } else {
                return "Order not found: " + orderId;
            }
        }).onItem().delayIt().by(java.time.Duration.ofMillis(500)); // Simulate API call
    }
}

/**
 * Custom tool: Refund Policy
 */
@ApplicationScoped
class RefundPolicyTool extends AbstractTool {
    
    public RefundPolicyTool() {
        super(
            "refund_policy",
            "Get refund policy information"
        );
    }
    
    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "category", Map.of(
                    "type", "string",
                    "description", "Policy category (optional): 'general', 'timeframe', 'conditions'",
                    "enum", List.of("general", "timeframe", "conditions")
                )
            ),
            "required", List.of()
        );
    }
    
    @Override
    public Uni<String> execute(Map<String, Object> arguments, AgentContext context) {
        String category = getParamOrDefault(arguments, "category", "general");
        
        return Uni.createFrom().item(() -> {
            return switch (category) {
                case "timeframe" -> """
                    Refund Timeframe:
                    - Return window: 30 days from delivery
                    - Refund processing: 5-7 business days
                    - Refund method: Original payment method
                    """;
                    
                case "conditions" -> """
                    Refund Conditions:
                    - Items must be unused and in original packaging
                    - Tags and labels must be attached
                    - Proof of purchase required
                    - Shipping costs are non-refundable
                    - Restocking fee may apply (15% for opened items)
                    """;
                    
                default -> """
                    ACME Corp Refund Policy:
                    - 30-day return window
                    - Full refund for unused items
                    - Easy return process
                    - Free return shipping for defective items
                    
                    To start a return:
                    1. Log into your account
                    2. Go to Orders
                    3. Select the order and click "Return Items"
                    4. Print the return label
                    5. Ship the item back
                    
                    Refunds are processed within 5-7 business days after we receive your return.
                    """;
            };
        });
    }
}

// ==================== EXAMPLE 2: DATA ANALYSIS AGENT ====================

@ApplicationScoped
public class DataAnalysisAgentExample {
    
    @Inject
    SilatClient client;
    
    /**
     * Data analysis agent with calculator and database tools
     */
    public void runExample() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DATA ANALYSIS AGENT EXAMPLE");
        System.out.println("=".repeat(80));
        
        String workflowId = createDataAnalysisWorkflow();
        executeDataAnalysis(workflowId);
    }
    
    private String createDataAnalysisWorkflow() {
        WorkflowDefinitionResponse workflow = client.workflows()
            .create("data-analysis-ai")
            .version("1.0.0")
            
            .addNode(new NodeDefinitionDto(
                "analyst-agent",
                "Data Analysis Agent",
                "TASK",
                "common-agent",
                Map.of(
                    "llm.provider", "anthropic",
                    "llm.model", "claude-3-opus-20240229",
                    "llm.temperature", 0.3, // Lower for analysis
                    "llm.maxTokens", 4000,
                    
                    "memory.enabled", false, // Single-shot analysis
                    
                    "tools.enabled", List.of(
                        "calculator",
                        "database_query"
                    ),
                    
                    "systemPrompt", """
                        You are an expert data analyst.
                        Use the database_query tool to fetch data.
                        Use the calculator tool for computations.
                        Provide clear insights and recommendations.
                        Show your calculations step-by-step.
                        """
                ),
                List.of(),
                List.of(),
                null, 180L, false
            ))
            
            .execute()
            .await().indefinitely();
        
        return workflow.definitionId();
    }
    
    private void executeDataAnalysis(String workflowId) {
        System.out.println("\nAnalyzing sales data...");
        
        RunResponse run = client.runs()
            .create(workflowId)
            .input("input", """
                Analyze the sales data for Q4 2024.
                Calculate:
                1. Total revenue
                2. Average order value
                3. Growth compared to Q3
                4. Top performing product category
                
                Provide recommendations for Q1 2025.
                """)
            .input("sessionId", "analysis-" + UUID.randomUUID())
            .executeAndStart()
            .await().indefinitely();
        
        // Wait and get results
        while (true) {
            run = client.runs().get(run.runId()).await().indefinitely();
            if (isTerminal(run.status())) break;
            try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
        }
        
        System.out.println("\nAnalysis Results:");
        System.out.println(run.variables().get("response"));
        System.out.println("\nIterations: " + run.variables().get("iterations"));
        System.out.println("Tool Calls: " + run.variables().get("toolCallsExecuted"));
    }
    
    private boolean isTerminal(String status) {
        return status.equals("COMPLETED") || status.equals("FAILED");
    }
}

// ==================== EXAMPLE 3: MULTI-AGENT ORCHESTRATION ====================

@ApplicationScoped
public class MultiAgentExample {
    
    @Inject
    SilatClient client;
    
    /**
     * Workflow with multiple specialized agents
     */
    public void runExample() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("MULTI-AGENT ORCHESTRATION EXAMPLE");
        System.out.println("=".repeat(80));
        
        createMultiAgentWorkflow();
    }
    
    private void createMultiAgentWorkflow() {
        WorkflowDefinitionResponse workflow = client.workflows()
            .create("multi-agent-workflow")
            .version("1.0.0")
            .description("Multiple specialized agents working together")
            
            // Agent 1: Research Agent
            .addNode(new NodeDefinitionDto(
                "research-agent",
                "Research Agent",
                "TASK",
                "common-agent",
                Map.of(
                    "llm.provider", "openai",
                    "llm.model", "gpt-4",
                    "tools.enabled", List.of("web_search"),
                    "systemPrompt", "You are a research specialist. Gather comprehensive information."
                ),
                List.of(),
                List.of(new TransitionDto("analysis-agent", null, "SUCCESS")),
                null, 120L, false
            ))
            
            // Agent 2: Analysis Agent
            .addNode(new NodeDefinitionDto(
                "analysis-agent",
                "Analysis Agent",
                "TASK",
                "common-agent",
                Map.of(
                    "llm.provider", "anthropic",
                    "llm.model", "claude-3-opus-20240229",
                    "tools.enabled", List.of("calculator"),
                    "systemPrompt", "You are an analysis specialist. Analyze the research findings."
                ),
                List.of("research-agent"),
                List.of(new TransitionDto("writer-agent", null, "SUCCESS")),
                null, 120L, false
            ))
            
            // Agent 3: Writer Agent
            .addNode(new NodeDefinitionDto(
                "writer-agent",
                "Writer Agent",
                "TASK",
                "common-agent",
                Map.of(
                    "llm.provider", "openai",
                    "llm.model", "gpt-4",
                    "tools.enabled", List.of(),
                    "systemPrompt", "You are a writer specialist. Create a comprehensive report."
                ),
                List.of("analysis-agent"),
                List.of(),
                null, 120L, false
            ))
            
            .execute()
            .await().indefinitely();
        
        System.out.println("✓ Multi-agent workflow created");
    }
}

// ==================== MAIN EXAMPLE RUNNER ====================

public class RunAllExamples {
    
    public static void main(String[] args) {
        // Initialize Silat client
        SilatClient client = SilatClient.builder()
            .restEndpoint("http://localhost:8080")
            .tenantId("acme-corp")
            .apiKey("your-api-key")
            .build();
        
        try {
            // Run examples
            CustomerSupportAgentExample example1 = new CustomerSupportAgentExample();
            example1.runExample();
            
            DataAnalysisAgentExample example2 = new DataAnalysisAgentExample();
            example2.runExample();
            
            MultiAgentExample example3 = new MultiAgentExample();
            example3.runExample();
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("ALL EXAMPLES COMPLETED SUCCESSFULLY! 🎉");
            System.out.println("=".repeat(80));
            
        } finally {
            client.close();
        }
    }
}