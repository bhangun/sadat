
## 📦 **1. Common Module (wayang-common)**

### **Purpose**
Shared contracts, DTOs, utilities, and interfaces used across all microservices and standalone agents.

### **Project Structure**

```
wayang-common/
├── pom.xml
└── src/main/java/tech/kayys/wayang/common/
    ├── domain/
    │   ├── ErrorPayload.java
    │   ├── AuditPayload.java
    │   ├── NodeDescriptor.java
    │   ├── ExecutionPlan.java
    │   ├── ExecutionResult.java
    │   └── NodeState.java
    ├── contract/
    │   ├── Node.java              # Core node interface
    │   ├── NodeFactory.java
    │   └── NodeContext.java
    ├── event/
    │   ├── WorkflowEvent.java
    │   ├── NodeEvent.java
    │   └── ErrorEvent.java
    ├── exception/
    │   ├── NodeExecutionException.java
    │   └── ValidationException.java
    └── util/
        ├── JsonUtil.java
        ├── HashUtil.java
        └── TokenCounter.java
```
