
## 9. WAYANG-LLM MODULE

```
wayang-llm/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── tech/kayys/wayang/llm/
    │   │       ├── api/
    │   │       │   └── LLMResource.java
    │   │       │
    │   │       ├── runtime/
    │   │       │   ├── LLMRuntime.java
    │   │       │   ├── UnifiedLLMRuntime.java
    │   │       │   └── LLMConfig.java
    │   │       │
    │   │       ├── router/
    │   │       │   ├── ModelRouter.java
    │   │       │   ├── PolicyBasedModelRouter.java
    │   │       │   └── RoutingPolicy.java
    │   │       │
    │   │       ├── provider/
    │   │       │   ├── LLMProvider.java
    │   │       │   ├── OllamaProvider.java
    │   │       │   ├── OpenAIProvider.java
    │   │       │   ├── TritonProvider.java
    │   │       │   └── ProviderRegistry.java
    │   │       │
    │   │       ├── prompt/
    │   │       │   ├── PromptShaper.java
    │   │       │   ├── PromptTemplate.java
    │   │       │   └── TemplateEngine.java
    │   │       │
    │   │       ├── cache/
    │   │       │   ├── ResponseCache.java
    │   │       │   └── CacheStrategy.java
    │   │       │
    │   │       └── cost/
    │   │           ├── CostCalculator.java
    │   │           └── BillingService.java
    │   │
    │   └── resources/
    │       ├── application.properties
    │       └── templates/
    │           └── prompt-templates.yaml
    │
    └── test/
        └── java/
            └── tech/kayys/wayang/llm/
                ├── provider/
                │   └── OllamaProviderTest.java
                └── router/
                    └── ModelRouterTest.java
```