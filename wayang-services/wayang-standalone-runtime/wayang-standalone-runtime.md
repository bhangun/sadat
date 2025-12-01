
### 14. WAYANG-STANDALONE-RUNTIME


## WAYANG-STANDALONE-RUNTIME MODULE

```
wayang-standalone-runtime/
├── pom.xml
├── README.md
├── RUNTIME-GUIDE.md
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── tech/kayys/wayang/standalone/
    │   │       │
    │   │       ├── core/
    │   │       │   ├── StandaloneAgentRuntime.java
    │   │       │   ├── AgentBootstrap.java
    │   │       │   ├── RuntimeContext.java
    │   │       │   ├── RuntimeConfig.java
    │   │       │   └── ShutdownHook.java
    │   │       │
    │   │       ├── orchestrator/
    │   │       │   ├── EmbeddedOrchestrator.java
    │   │       │   ├── SimpleDAGExecutor.java
    │   │       │   ├── NodeExecutionQueue.java
    │   │       │   └── ExecutionCoordinator.java
    │   │       │
    │   │       ├── executor/
    │   │       │   ├── LocalNodeExecutor.java
    │   │       │   ├── InlineExecutor.java
    │   │       │   └── ExecutionContext.java
    │   │       │
    │   │       ├── adapter/
    │   │       │   ├── local/
    │   │       │   │   ├── LocalLLMAdapter.java
    │   │       │   │   ├── LocalRAGAdapter.java
    │   │       │   │   ├── LocalToolAdapter.java
    │   │       │   │   └── LocalMemoryAdapter.java
    │   │       │   │
    │   │       │   └── remote/
    │   │       │       ├── RemoteLLMAdapter.java
    │   │       │       ├── RemoteRAGAdapter.java
    │   │       │       ├── RemoteToolAdapter.java
    │   │       │       └── RemoteMemoryAdapter.java
    │   │       │
    │   │       ├── state/
    │   │       │   ├── InMemoryStateStore.java
    │   │       │   ├── FileBasedStateStore.java
    │   │       │   ├── CheckpointManager.java
    │   │       │   └── StateSnapshot.java
    │   │       │
    │   │       ├── config/
    │   │       │   ├── ConfigLoader.java
    │   │       │   ├── EnvironmentConfigProvider.java
    │   │       │   ├── FileConfigProvider.java
    │   │       │   └── AgentConfiguration.java
    │   │       │
    │   │       ├── telemetry/
    │   │       │   ├── LightTelemetryClient.java
    │   │       │   ├── MetricsCollector.java
    │   │       │   ├── LoggingService.java
    │   │       │   └── HealthChecker.java
    │   │       │
    │   │       ├── cache/
    │   │       │   ├── LocalCache.java
    │   │       │   ├── CacheStrategy.java
    │   │       │   └── CacheEntry.java
    │   │       │
    │   │       ├── security/
    │   │       │   ├── SecretProvider.java
    │   │       │   ├── EnvironmentSecretProvider.java
    │   │       │   ├── FileSecretProvider.java
    │   │       │   └── SecretEncryption.java
    │   │       │
    │   │       └── util/
    │   │           ├── JsonUtil.java
    │   │           ├── FileUtil.java
    │   │           ├── NetworkUtil.java
    │   │           └── RetryUtil.java
    │   │
    │   └── resources/
    │       ├── application.properties
    │       ├── logback.xml
    │       └── banner.txt
    │
    └── test/
        ├── java/
        │   └── tech/kayys/wayang/standalone/
        │       ├── core/
        │       │   └── StandaloneAgentRuntimeTest.java
        │       ├── orchestrator/
        │       │   └── EmbeddedOrchestratorTest.java
        │       └── adapter/
        │           └── LocalLLMAdapterTest.java
        │
        └── resources/
            ├── test-workflow.json
            └── test-config.properties
```



## Complete Dependency Tree for Standalone Runtime

```
wayang-standalone-runtime
│
├── wayang-core (REQUIRED)
│   └── No external dependencies (pure domain)
│
├── wayang-common (REQUIRED)
│   ├── jackson-databind
│   ├── slf4j-api
│   └── jakarta.validation-api
│
├── wayang-adapter-common (REQUIRED for adapters)
│   ├── okhttp3
│   └── resilience4j-retry
│
├── jackson-databind (REQUIRED for JSON)
├── logback-classic (REQUIRED for logging)
├── okhttp (REQUIRED for HTTP calls)
├── typesafe-config (REQUIRED for configuration)
│
└── OPTIONAL dependencies:
    ├── caffeine (for caching)
    ├── langchain4j-core (for local LLM)
    └── jvector (for local vector store)
```