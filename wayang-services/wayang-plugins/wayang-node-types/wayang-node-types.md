
## 6. WAYANG-NODE-TYPES MODULE

```
wayang-node-types/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── tech/kayys/wayang/node/types/
    │   │       ├── agent/
    │   │       │   ├── AgentNode.java
    │   │       │   └── AgentNodeConfig.java
    │   │       │
    │   │       ├── tool/
    │   │       │   ├── ToolNode.java
    │   │       │   └── ToolNodeConfig.java
    │   │       │
    │   │       ├── rag/
    │   │       │   ├── RAGNode.java
    │   │       │   └── RAGNodeConfig.java
    │   │       │
    │   │       ├── guardrails/
    │   │       │   ├── GuardrailNode.java
    │   │       │   └── GuardrailNodeConfig.java
    │   │       │
    │   │       ├── evaluator/
    │   │       │   ├── EvaluatorNode.java
    │   │       │   └── EvaluatorNodeConfig.java
    │   │       │
    │   │       ├── decision/
    │   │       │   ├── DecisionNode.java
    │   │       │   └── DecisionNodeConfig.java
    │   │       │
    │   │       ├── loop/
    │   │       │   ├── LoopNode.java
    │   │       │   └── LoopNodeConfig.java
    │   │       │
    │   │       ├── parallel/
    │   │       │   ├── ParallelNode.java
    │   │       │   └── ParallelNodeConfig.java
    │   │       │
    │   │       ├── start/
    │   │       │   └── StartNode.java
    │   │       │
    │   │       └── end/
    │   │           └── EndNode.java
    │   │
    │   └── resources/
    │       └── META-INF/
    │           └── node-descriptors/
    │               ├── agent-node.json
    │               ├── tool-node.json
    │               ├── rag-node.json
    │               └── guardrail-node.json
    │
    └── test/
        └── java/
            └── tech/kayys/wayang/node/types/
                ├── agent/
                │   └── AgentNodeTest.java
                └── tool/
                    └── ToolNodeTest.java