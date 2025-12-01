## 🧠 **6. Planner Service (wayang-planner)**

### **Purpose**
Strategic and tactical planning engine that decomposes high-level goals into executable workflows.

### **Project Structure**

```
wayang-planner/
├── pom.xml
└── src/main/java/tech/kayys/wayang/planner/
    ├── resource/
    │   └── PlannerResource.java
    ├── service/
    │   ├── PlanningService.java
    │   ├── StrategicPlanner.java
    │   └── TacticalPlanner.java
    ├── engine/
    │   ├── GoalParser.java
    │   ├── TaskDecomposer.java
    │   ├── NodeMapper.java
    │   └── PlanValidator.java
    ├── strategy/
    │   ├── PlanningStrategy.java
    │   ├── ChainOfThoughtStrategy.java
    │   ├── ReActStrategy.java
    │   └── TreeOfThoughtStrategy.java
    └── domain/
        ├── Goal.java
        ├── Plan.java
        └── PlanningContext.java
```