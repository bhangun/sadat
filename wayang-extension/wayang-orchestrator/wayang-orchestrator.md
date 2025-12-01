## 🎭 **4. Orchestrator Service (wayang-orchestrator)**

This is the **heart of the platform** - coordinates plan execution, manages state, handles errors, and integrates with all other services.

### **Project Structure**

```
wayang-orchestrator/
├── pom.xml
└── src/main/java/tech/kayys/wayang/orchestrator/
    ├── engine/
    │   ├── OrchestrationEngine.java
    │   ├── DAGWalker.java
    │   ├── StateManager.java
    │   └── ErrorHandler.java
    ├── resource/
    │   └── ExecutionResource.java
    ├── service/
    │   ├── ExecutionService.java
    │   └── CheckpointService.java
    ├── repository/
    │   ├── ExecutionRunRepository.java
    │   └── NodeStateRepository.java
    └── event/
        └── EventEmitter.java
```


## 7. WAYANG-ORCHESTRATOR MODULE

```
wayang-orchestrator/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── tech/kayys/wayang/orchestrator/
    │   │       ├── api/
    │   │       │   └── OrchestratorResource.java
    │   │       │
    │   │       ├── service/
    │   │       │   ├── WorkflowOrchestrator.java
    │   │       │   ├── AdaptiveOrchestrator.java
    │   │       │   └── OrchestratorConfig.java
    │   │       │
    │   │       ├── engine/
    │   │       │   ├── DAGExecutor.java
    │   │       │   ├── ExecutionGraph.java
    │   │       │   ├── NodeScheduler.java
    │   │       │   └── DependencyResolver.java
    │   │       │
    │   │       ├── dispatcher/
    │   │       │   ├── TaskDispatcher.java
    │   │       │   └── DispatchStrategy.java
    │   │       ││   │       ├── replanner/
    │   │       │   ├── Replanner.java
    │   │       │   └── ReplanStrategy.java
    │   │       │
    │   │       ├── compensation/
    │   │       │   ├── CompensationEngine.java
    │   │       │   └── CompensationHandler.java
    │   │       │
    │   │       ├── checkpoint/
    │   │       │   ├── Checkpointer.java
    │   │       │   └── CheckpointManager.java
    │   │       │
    │   │       └── policy/
    │   │           ├── PolicyEnforcer.java
    │   │           └── ExecutionPolicy.java
    │   │
    │   └── resources/
    │       ├── application.properties
    │       └── META-INF/
    │           └── microprofile-config.properties
    │
    └── test/
        ├── java/
        │   └── tech/kayys/wayang/orchestrator/
        │       ├── engine/
        │       │   └── DAGExecutorTest.java
        │       └── service/
        │           └── AdaptiveOrchestratorTest.java
        └── resources/
            └── test-workflows/
                └── simple-workflow.json
```