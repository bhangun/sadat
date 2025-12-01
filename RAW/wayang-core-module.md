
## 2. WAYANG-CORE MODULE

```
wayang-core/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── tech/kayys/wayang/core/
    │   │       ├── node/
    │   │       │   ├── Node.java
    │   │       │   ├── AbstractNode.java
    │   │       │   ├── NodeDescriptor.java
    │   │       │   ├── NodeContext.java
    │   │       │   ├── NodeConfig.java
    │   │       │   ├── ExecutionResult.java
    │   │       │   └── NodeException.java
    │   │       │
    │   │       ├── workflow/
    │   │       │   ├── Workflow.java
    │   │       │   ├── WorkflowDefinition.java
    │   │       │   ├── NodeInstance.java
    │   │       │   ├── Edge.java
    │   │       │   ├── Binding.java
    │   │       │   └── WorkflowStatus.java
    │   │       │
    │   │       ├── execution/
    │   │       │   ├── ExecutionPlan.java
    │   │       │   ├── ExecutionRun.java
    │   │       │   ├── NodeState.java
    │   │       │   ├── ExecutionMetadata.java
    │   │       │   └── Status.java
    │   │       │
    │   │       ├── model/
    │   │       │   ├── DataType.java
    │   │       │   ├── InputPort.java
    │   │       │   ├── OutputPort.java
    │   │       │   ├── PropertyDescriptor.java
    │   │       │   ├── Capability.java
    │   │       │   └── ResourceProfile.java
    │   │       │
    │   │       └── security/
    │   │           ├── SecurityContext.java
    │   │           ├── Permission.java
    │   │           └── SandboxLevel.java
    │   │
    │   └── resources/
    │       └── META-INF/
    │           └── schemas/
    │               └── node-descriptor-schema.json
    │
    └── test/
        └── java/
            └── tech/kayys/wayang/core/
                └── node/
                    └── AbstractNodeTest.java
```