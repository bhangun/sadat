
## 5. WAYANG-NODE-CORE MODULE

```
wayang-node-core/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── tech/kayys/wayang/node/core/
    │   │       ├── factory/
    │   │       │   ├── NodeFactory.java
    │   │       │   ├── NodeFactoryRegistry.java
    │   │       │   └── DefaultNodeFactory.java
    │   │       │
    │   │       ├── loader/
    │   │       │   ├── NodeLoader.java
    │   │       │   ├── ClassLoaderStrategy.java
    │   │       │   ├── WasmLoaderStrategy.java
    │   │       │   └── ContainerLoaderStrategy.java
    │   │       │
    │   │       ├── isolation/
    │   │       │   ├── IsolationManager.java
    │   │       │   ├── SandboxController.java
    │   │       │   └── ResourceQuotaController.java
    │   │       │
    │   │       ├── validation/
    │   │       │   ├── NodeValidator.java
    │   │       │   ├── SchemaValidator.java
    │   │       │   └── CapabilityValidator.java
    │   │       │
    │   │       └── lifecycle/
    │   │           ├── NodeLifecycleManager.java
    │   │           ├── NodeInitializer.java
    │   │           └── NodeDestructor.java
    │   │
    │   └── resources/
    │       └── application.properties
    │
    └── test/
        └── java/
            └── tech/kayys/wayang/node/core/
                └── factory/
                    └── NodeFactoryTest.java