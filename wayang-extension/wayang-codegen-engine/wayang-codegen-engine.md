
## This is the **key module** that analyzes workflow and generates minimal code.

```

wayang-codegen-engine/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── tech/kayys/wayang/codegen/
    │   │       │
    │   │       ├── analyzer/
    │   │       │   ├── WorkflowAnalyzer.java
    │   │       │   ├── NodeDependencyAnalyzer.java
    │   │       │   ├── CapabilityAnalyzer.java
    │   │       │   └── DependencyGraph.java
    │   │       │
    │   │       ├── resolver/
    │   │       │   ├── DependencyResolver.java
    │   │       │   ├── MavenDependencyResolver.java
    │   │       │   └── LibraryVersionResolver.java
    │   │       │
    │   │       ├── optimizer/
    │   │       │   ├── DependencyOptimizer.java
    │   │       │   ├── TreeShaker.java
    │   │       │   └── CodeMinimizer.java
    │   │       │
    │   │       ├── synthesizer/
    │   │       │   ├── CodeSynthesizer.java
    │   │       │   ├── JavaCodeGenerator.java
    │   │       │   ├── PythonCodeGenerator.java
    │   │       │   └── PomGenerator.java
    │   │       │
    │   │       ├── template/
    │   │       │   ├── TemplateEngine.java
    │   │       │   ├── TemplateContext.java
    │   │       │   └── TemplateRegistry.java
    │   │       │
    │   │       └── model/
    │   │           ├── GenerationRequest.java
    │   │           ├── GenerationResult.java
    │   │           ├── DependencySpec.java
    │   │           └── CodeArtifact.java
    │   │
    │   └── resources/
    │       └── templates/
    │           ├── java/
    │           │   ├── main-class.ftl
    │           │   ├── node-executor.ftl
    │           │   ├── orchestrator.ftl
    │           │   └── pom.ftl
    │           │
    │           ├── python/
    │           │   ├── main.ftl
    │           │   ├── executor.ftl
    │           │   └── requirements.ftl
    │           │
    │           └── config/
    │               ├── application.properties.ftl
    │               ├── Dockerfile.ftl
    │               └── README.md.ftl
    │
    └── test/
        └── java/
            └── tech/kayys/wayang/codegen/
                ├── analyzer/
                │   └── WorkflowAnalyzerTest.java
                └── synthesizer/
                    └── JavaCodeGeneratorTest.java
```
