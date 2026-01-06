# Silat Agent Executor - Configuration and Usage Guide

## 📋 Overview

The Silat Agent Executor provides a comprehensive, production-ready agent system for the Silat Workflow Engine. It supports:

- **Multi-Provider LLM Integration**: OpenAI, Anthropic Claude, Azure, local models
- **Pluggable Memory Systems**: Buffer, Summary, Vector, Entity memory
- **Tool/Function Calling**: Built-in and custom tools
- **Multi-tenancy**: Complete tenant isolation
- **Multiple Communication Strategies**: gRPC, Kafka, REST

## 🏗️ Architecture

```
┌────────────────────────────────────────────────────────────┐
│                  Common Agent Executor                     │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │   Memory     │  │     LLM      │  │    Tools     │   │
│  │   Manager    │  │   Provider   │  │   Registry   │   │
│  └──────────────┘  └──────────────┘  └──────────────┘   │
│         │                 │                  │            │
│  ┌──────▼─────────────────▼──────────────────▼────────┐  │
│  │          Agent Execution Engine                    │  │
│  │   (Context, Configuration, Metrics, Storage)       │  │
│  └───────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### 1. Add Dependencies

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Quarkus Core -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-core</artifactId>
    </dependency>
    
    <!-- Reactive -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-mutiny</artifactId>
    </dependency>
    
    <!-- REST Client -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-rest-client-reactive</artifactId>
    </dependency>
    
    <!-- gRPC (optional) -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-grpc</artifactId>
    </dependency>
    
    <!-- Kafka (optional) -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-kafka-client</artifactId>
    </dependency>
</dependencies>
```

### 2. Configuration

```properties
# application.properties

# LLM Provider Configuration
silat.agent.llm.openai.api-key=${OPENAI_API_KEY}
silat.agent.llm.openai.base-url=https://api.openai.com/v1
silat.agent.llm.anthropic.api-key=${ANTHROPIC_API_KEY}
silat.agent.llm.anthropic.base-url=https://api.anthropic.com/v1

# Memory Configuration
silat.agent.memory.cache-size=1000
silat.agent.memory.cache-ttl=3600000
silat.agent.memory.default-type=buffer
silat.agent.memory.default-window-size=10

# Tool Configuration
silat.agent.tools.enabled=calculator,web_search,current_time
silat.agent.tools.web-search.api-key=${WEB_SEARCH_API_KEY}

# Executor Configuration
silat.executor.transport=GRPC
silat.executor.max-concurrent-tasks=10
silat.executor.grpc.port=9090
silat.executor.kafka.bootstrap-servers=localhost:9092

# Metrics
silat.metrics.enabled=true
silat.metrics.export-interval=60000
```

### 3. Define Workflow with Agent Node

```java
import tech.kayys.silat.client.*;
import tech.kayys.silat.api.dto.*;

public class AIWorkflowExample {
    
    public static void main(String[] args) {
        SilatClient client = SilatClient.builder()
            .restEndpoint("http://localhost:8080")
            .tenantId("acme-corp")
            .apiKey("your-api-key")
            .build();
        
        // Create workflow with AI agent node
        WorkflowDefinitionResponse workflow = client.workflows()
            .create("ai-customer-support")
            .version("1.0.0")
            .description("AI-powered customer support workflow")
            
            // Add AI agent node
            .addNode(new NodeDefinitionDto(
                "ai-agent",
                "AI Customer Support Agent",
                "TASK",
                "common-agent",  // Executor type
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
                        "web_search",
                        "database_query",
                        "current_time"
                    ),
                    "tools.allowCalls", true,
                    
                    // System Prompt
                    "systemPrompt", """
                        You are a helpful customer support agent.
                        You have access to tools to search information and query databases.
                        Always be polite and professional.
                        If you don't know something, use the web_search tool to find the answer.
                        """,
                    
                    // Agent Settings
                    "maxIterations", 5,
                    "streaming", false
                ),
                List.of(), // No dependencies
                List.of(), // No transitions yet
                new RetryPolicyDto(3, 1, 60, 2.0, List.of()),
                120L,
                false
            ))
            
            .execute()
            .await().indefinitely();
        
        System.out.println("Workflow created: " + workflow.definitionId());
        
        client.close();
    }
}
```

### 4. Execute Workflow with Agent

```java
public class ExecuteAIWorkflow {
    
    public static void main(String[] args) {
        SilatClient client = SilatClient.builder()
            .restEndpoint("http://localhost:8080")
            .tenantId("acme-corp")
            .apiKey("your-api-key")
            .build();
        
        // Execute workflow
        RunResponse run = client.runs()
            .create("ai-customer-support")
            .input("input", "What are your business hours?")
            .input("sessionId", "session-12345")
            .input("customerId", "CUST-67890")
            .executeAndStart()
            .await().indefinitely();
        
        System.out.println("Run ID: " + run.runId());
        
        // Wait for completion
        while (true) {
            run = client.runs()
                .get(run.runId())
                .await().indefinitely();
            
            System.out.println("Status: " + run.status());
            
            if (isTerminal(run.status())) {
                break;
            }
            
            Thread.sleep(2000);
        }
        
        // Get AI response
        Map<String, Object> variables = run.variables();
        System.out.println("AI Response: " + variables.get("response"));
        System.out.println("Iterations: " + variables.get("iterations"));
        System.out.println("Tokens Used: " + variables.get("tokenUsage"));
        
        client.close();
    }
    
    private static boolean isTerminal(String status) {
        return status.equals("COMPLETED") || 
               status.equals("FAILED") || 
               status.equals("CANCELLED");
    }
}
```

## 🔧 Advanced Configuration

### Custom Agent with Specific Configuration

```java
// Create workflow with advanced agent configuration
.addNode(new NodeDefinitionDto(
    "advanced-agent",
    "Advanced AI Agent",
    "TASK",
    "common-agent",
    Map.of(
        // Use Claude instead of GPT
        "llm.provider", "anthropic",
        "llm.model", "claude-3-opus-20240229",
        "llm.temperature", 0.5,
        "llm.maxTokens", 4000,
        
        // Use summary memory for long conversations
        "memory.enabled", true,
        "memory.type", "summary",
        "memory.windowSize", 20,
        
        // Enable specific tools
        "tools.enabled", List.of(
            "calculator",
            "web_search",
            "database_query",
            "api_call"
        ),
        
        // Custom system prompt
        "systemPrompt", """
            You are an expert financial analyst AI.
            You have access to:
            - Calculator for computations
            - Web search for latest financial news
            - Database for historical data
            - API calls for real-time market data
            
            Always cite your sources and show your calculations.
            Be conservative in your recommendations.
            """,
        
        "maxIterations", 10
    ),
    // ... other node configuration
))
```

### Multi-Turn Conversation

```java
// First turn
RunResponse run1 = client.runs()
    .create("ai-customer-support")
    .input("input", "What is your refund policy?")
    .input("sessionId", "session-123")
    .executeAndStart()
    .await().indefinitely();

// Wait for completion...

// Second turn - agent will remember context
RunResponse run2 = client.runs()
    .create("ai-customer-support")
    .input("input", "How long does a refund take?")
    .input("sessionId", "session-123")  // Same session
    .executeAndStart()
    .await().indefinitely();

// Agent will have memory of previous conversation
```

## 🛠️ Custom Tools

### Creating a Custom Tool

```java
package com.acme.tools;

import tech.kayys.silat.agent.tools.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CustomerLookupTool extends AbstractTool {
    
    @Inject
    CustomerService customerService;
    
    public CustomerLookupTool() {
        super(
            "customer_lookup",
            "Looks up customer information by customer ID or email"
        );
    }
    
    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "customerId", Map.of(
                    "type", "string",
                    "description", "Customer ID to look up"
                ),
                "email", Map.of(
                    "type", "string",
                    "description", "Customer email to look up"
                )
            ),
            "oneOf", List.of("customerId", "email")
        );
    }
    
    @Override
    public Uni<String> execute(
            Map<String, Object> arguments,
            AgentContext context) {
        
        String customerId = getParam(arguments, "customerId", String.class);
        String email = getParam(arguments, "email", String.class);
        
        if (customerId != null) {
            return customerService.findById(customerId)
                .map(this::formatCustomer);
        } else if (email != null) {
            return customerService.findByEmail(email)
                .map(this::formatCustomer);
        } else {
            return Uni.createFrom().item("Error: Must provide customerId or email");
        }
    }
    
    private String formatCustomer(Customer customer) {
        return String.format(
            "Customer: %s\nEmail: %s\nStatus: %s\nJoined: %s",
            customer.getName(),
            customer.getEmail(),
            customer.getStatus(),
            customer.getJoinedDate()
        );
    }
    
    @Override
    public boolean requiresAuth() {
        return true; // Requires authentication
    }
}
```

### Register Custom Tool

```java
@ApplicationScoped
public class ToolRegistration {
    
    @Inject
    ToolRegistry toolRegistry;
    
    @Inject
    CustomerLookupTool customerLookupTool;
    
    @PostConstruct
    void registerTools() {
        // Register for specific tenant
        toolRegistry.registerTool(customerLookupTool, "acme-corp");
        
        // Or register globally
        toolRegistry.registerTool(customerLookupTool, "_global");
    }
}
```

## 📊 Monitoring and Metrics

### Get Agent Metrics

```java
@Inject
AgentMetricsCollector metricsCollector;

public void printMetrics() {
    // Get statistics
    AgentStatistics stats = metricsCollector.getStatistics();
    System.out.println("Total Executions: " + stats.totalExecutions());
    System.out.println("Success Rate: " + stats.successRate() + "%");
    System.out.println("Total Tokens: " + stats.totalTokens());
    System.out.println("Avg Duration: " + stats.averageDurationMs() + "ms");
    
    // Get node-specific metrics
    NodeMetrics nodeMetrics = metricsCollector.getNodeMetrics("ai-agent");
    if (nodeMetrics != null) {
        System.out.println("\nNode Metrics:");
        System.out.println(nodeMetrics.toMap());
    }
    
    // Get provider metrics
    ProviderMetrics providerMetrics = 
        metricsCollector.getProviderMetrics("openai", "gpt-4");
    if (providerMetrics != null) {
        System.out.println("\nProvider Metrics:");
        System.out.println(providerMetrics.toMap());
    }
}
```

## 🔒 Multi-Tenancy

### Tenant Isolation

```java
// Each tenant has isolated:
// - Configurations
// - Memory sessions
// - Custom tools
// - Metrics

// Tenant A
client1 = SilatClient.builder()
    .tenantId("tenant-a")
    .apiKey("tenant-a-key")
    .build();

// Tenant B
client2 = SilatClient.builder()
    .tenantId("tenant-b")
    .apiKey("tenant-b-key")
    .build();

// They can have different:
// - LLM providers
// - Available tools
// - Memory settings
// - Agent configurations
```

## 🎯 Best Practices

### 1. System Prompts

```java
// Good system prompt
"""
You are a customer support agent for ACME Corp.

Your responsibilities:
- Answer customer questions about products and policies
- Look up order information using the order_lookup tool
- Escalate complex issues to human agents

Guidelines:
- Be polite and professional
- Cite sources when providing information
- If unsure, search for information or ask for human help
- Keep responses concise but complete

Available tools:
- order_lookup: Look up order status
- refund_policy: Get refund policy details
- product_search: Search product catalog
"""
```

### 2. Memory Management

```java
// For short conversations: use buffer memory
"memory.type", "buffer"
"memory.windowSize", 10

// For long conversations: use summary memory
"memory.type", "summary"
"memory.windowSize", 20

// For semantic retrieval: use vector memory
"memory.type", "vector"

// For entity tracking: use entity memory
"memory.type", "entity"
```

### 3. Error Handling

```java
// Configure retries
new RetryPolicyDto(
    3,              // max attempts
    1,              // initial delay seconds
    60,             // max delay seconds
    2.0,            // backoff multiplier
    List.of()       // retryable errors
)
```

### 4. Cost Management

```java
// Monitor token usage
ProviderMetrics metrics = metricsCollector
    .getProviderMetrics("openai", "gpt-4");

long totalTokens = metrics.getTotalTokens();
double estimatedCost = totalTokens * 0.00003; // $0.03 per 1K tokens

// Set token limits
"llm.maxTokens", 1000  // Limit response length

// Use cheaper models for simple tasks
"llm.model", "gpt-3.5-turbo"  // vs "gpt-4"
```

## 📚 Examples Repository

Complete working examples are available in the `examples/` directory:

- `CustomerSupportAgent.java` - Full customer support workflow
- `DataAnalysisAgent.java` - Data analysis with tools
- `MultiAgentOrchestration.java` - Multiple agents working together
- `CustomToolExample.java` - Creating and using custom tools
- `StreamingResponseExample.java` - Real-time streaming responses

## 🐛 Troubleshooting

### Common Issues

**Issue**: Agent not executing
```
Solution: Check executor is registered and running
- Verify EXECUTOR_TRANSPORT configuration
- Check executor logs for registration
```

**Issue**: Tool calls failing
```
Solution: Verify tool registration
- Check tool is registered for tenant
- Validate tool parameter schema
- Check tool execution logs
```

**Issue**: Memory not persisting
```
Solution: Check storage configuration
- Verify database connection
- Check MessageRepository implementation
- Review memory save logs
```

**Issue**: High token usage
```
Solution: Optimize configuration
- Reduce memory.windowSize
- Use summary memory instead of buffer
- Lower llm.maxTokens
- Use cheaper models for simple tasks
```

## 📖 API Reference

Full API documentation available at: `https://docs.silat.dev/agent-executor`

## 🤝 Contributing

Contributions welcome! Please see `CONTRIBUTING.md` for guidelines.

## 📄 License

Apache License 2.0 - See `LICENSE` file for details.