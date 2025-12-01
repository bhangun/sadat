# Wayang Agent SDK - Complete Module Skeleton and Dependencies

# Wayang Agent SDK

Core SDK for building AI agents in the Wayang platform.

## Features

- **Modular Agent Architecture**: Build agents with composable capabilities
- **A2A Communication**: Agent-to-agent messaging and coordination
- **Plugin System**: Extend functionality with custom plugins
- **Lifecycle Management**: Automatic health monitoring and recovery
- **Observability**: Built-in metrics, tracing, and logging
- **Memory System**: Episodic, semantic, and procedural memory
- **Tool Integration**: MCP-compatible tool execution

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker (optional, for dev services)

### Build

```bash
mvn clean install
```

### Run Tests

```bash
mvn test
```

### Usage Example

```java
// Create an agent
AgentRegistry registry = new AgentRegistry();
PlannerAgent agent = AgentBuilder.create()
    .planner("my-planner")
    .planningEngine(planningEngine)
    .taskDecomposer(taskDecomposer)
    .build();

// Execute agent
AgentContext context = AgentContext.builder()
    .input("goal", "Plan a multi-step workflow")
    .build();

AgentResult result = agent.execute(context);
```

## Documentation

- [API Documentation](docs/API.md)
- [Architecture Guide](docs/ARCHITECTURE.md)
- [Quick Start Guide](docs/QUICKSTART.md)
- [Examples](docs/EXAMPLES.md)

## License

Apache License 2.0
```


## Project Structure

```
wayang-agent-sdk/
├── pom.xml
├── README.md
├── .gitignore
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── io/
│   │   │       └── wayang/
│   │   │           └── agent/
│   │   │               ├── sdk/
│   │   │               │   ├── AgentSDK.java
│   │   │               │   ├── AgentFactory.java
│   │   │               │   ├── AgentRegistry.java
│   │   │               │   └── AgentBuilder.java
│   │   │               ├── core/
│   │   │               │   ├── Agent.java
│   │   │               │   ├── AbstractAgent.java
│   │   │               │   ├── AutonomousAgent.java
│   │   │               │   ├── SpecialistAgent.java
│   │   │               │   ├── AgentContext.java
│   │   │               │   ├── AgentResult.java
│   │   │               │   ├── AgentConfig.java
│   │   │               │   ├── AgentDescriptor.java
│   │   │               │   ├── AgentType.java
│   │   │               │   ├── Capability.java
│   │   │               │   ├── AgentException.java
│   │   │               │   └── AgentMetrics.java
│   │   │               ├── lifecycle/
│   │   ││   │   ├── AgentLifecycleManager.java
│   │   │               │   ├── ManagedAgent.java
│   │   │               │   ├── AgentHealthMonitor.java
│   │   │               │   ├── AgentInfo.java
│   │   │               │   ├── HealthStatus.java
│   │   │               │   └── LifecycleEvent.java
│   │   │               ├── impl/
│   │   │               │   ├── PlannerAgent.java
│   │   │               │   ├── EvaluatorAgent.java
│   │   │               │   ├── CriticAgent.java
│   │   │               │   ├── GuardrailsAgent.java
│   │   │               │   ├── RAGAgent.java
│   │   │               │   └── ToolExecutorAgent.java
│   │   │               ├── a2a/
│   │   │               │   ├── A2AMessage.java
│   │   │               │   ├── A2AResponse.java
│   │   │               │   ├── A2ARouter.java
│   │   │               │   ├── A2AMessageBus.java
│   │   │               │   ├── A2AMessageHandler.java
│   │   │               │   ├── A2APolicyEngine.java
│   │   │               │   ├── ContextHandoverManager.java
│   │   │               │   ├── MessageType.java
│   │   │               │   ├── MessagePurpose.java
│   │   │               │   └── A2AMetrics.java
│   │   │               ├── node/
│   │   │               │   ├── Node.java
│   │   │               │   ├── AbstractNode.java
│   │   │               │   ├── NodeDescriptor.java
│   │   │               │   ├── NodeContext.java
│   │   │               │   ├── NodeConfig.java
│   │   │               │   ├── ExecutionResult.java
│   │   │               │   ├── ExecutionStatus.java
│   │   │               │   ├── NodeFactory.java
│   │   │               │   ├── NodeException.java
│   │   │               │   ├── ValidationResult.java
│   │   │               │   └── NodeMetrics.java
│   │   │               ├── node/
│   │   │               │   └── types/
│   │   │               │       ├── AgentNode.java
│   │   │               │       ├── ToolNode.java
│   │   │               │       ├── RAGNode.java
│   │   │               │       ├── GuardrailsNode.java
│   │   │               │       ├── EvaluatorNode.java
│   │   │               │       ├── CriticNode.java
│   │   │               │       ├── StartNode.java
│   │   │               │       ├── EndNode.java
│   │   │               │       └── DecisionNode.java
│   │   │               ├── plugin/
│   │   │               │   ├── Plugin.java
│   │   │               │   ├── NodePlugin.java
│   │   │               │   ├── ToolPlugin.java
│   │   │               │   ├── PluginManager.java
│   │   │               │   ├── PluginDescriptor.java
│   │   │               │   ├── PluginContext.java
│   │   │               │   ├── PluginLoader.java
│   │   │               │   ├── PluginValidator.java
│   │   │               │   ├── PluginRegistry.java
│   │   │               │   ├── PluginArtifact.java
│   │   │               │   └── PluginException.java
│   │   │               ├── tool/
│   │   │               │   ├── Tool.java
│   │   │               │   ├── ToolDescriptor.java
│   │   │               │   ├── ToolRequest.java
│   │   │               │   ├── ToolResponse.java
│   │   │               │   ├── ToolExecutor.java
│   │   │               │   ├── ToolGateway.java
│   │   │               │   ├── ToolReasoner.java
│   │   │               │   ├── ToolRegistry.java
│   │   │               │   ├── ToolValidator.java
│   │   │               │   ├── ToolSelection.java
│   │   │               │   └── ToolException.java
│   │   │               ├── memory/
│   │   │               │   ├── MemoryService.java
│   │   │               │   ├── EpisodicMemory.java
│   │   │               │   ├── SemanticMemory.java
│   │   │               │   ├── ProceduralMemory.java
│   │   │               │   ├── MemoryEntry.java
│   │   │               │   ├── MemoryScorer.java
│   │   │               │   ├── MemoryConsolidator.java
│   │   │               │   ├── Episode.java
│   │   │               │   └── ScoredMemory.java
│   │   │               ├── model/
│   │   │               │   ├── Goal.java
│   │   │               │   ├── Task.java
│   │   │               │   ├── ExecutionPlan.java
│   │   │               │   ├── PlanNode.java
│   │   │               │   ├── ExecutionGraph.java
│   │   │               │   ├── ExecutionMetadata.java
│   │   │               │   ├── ResourceBindings.java
│   │   │               │   ├── ResourceProfile.java
│   │   │               │   ├── SecurityContext.java
│   │   │               │   ├── SecurityProfile.java
│   │   │               │   ├── PersonaConfig.java
│   │   │               │   ├── ResourceLimits.java
│   │   │               │   └── SecurityConfig.java
│   │   │               ├── util/
│   │   │               │   ├── JsonUtils.java
│   │   │               │   ├── ValidationUtils.java
│   │   │               │   ├── SerializationUtils.java
│   │   │               │   └── MetricsUtils.java
│   │   │               └── config/
│   │   │                   ├── AgentSDKConfig.java
│   │   │                   └── package-info.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── META-INF/
│   │       │   ├── beans.xml
│   │       │   └── microprofile-config.properties
│   │       └── logback.xml
│   └── test/
│       ├── java/
│       │   └── io/
│       │       └── wayang/
│       │           └── agent/
│       │               ├── sdk/
│       │               │   ├── AgentSDKTest.java
│       │               │   └── AgentBuilderTest.java
│       │               ├── core/
│       │               │   ├── AgentTest.java
│       │               │   └── AgentContextTest.java
│       │               ├── lifecycle/
│       │               │   └── AgentLifecycleManagerTest.java
│       │               ├── impl/
│       │               │   ├── PlannerAgentTest.java
│       │               │   ├── EvaluatorAgentTest.java
│       │               │   └── CriticAgentTest.java
│       │               ├── a2a/
│       │               │   ├── A2ARouterTest.java
│       │               │   └── A2AMessageBusTest.java
│       │               ├── node/
│       │               │   └── NodeTest.java
│       │               └── plugin/
│       │                   └── PluginManagerTest.java
│       └── resources/
│           ├── application-test.properties
│           └── test-data/
└── docs/
    ├── API.md
    ├── ARCHITECTURE.md
    ├── QUICKSTART.md
    └── EXAMPLES.md
```
