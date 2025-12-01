```
wayang-codegen-templates/
├── pom.xml
└── src/
    └── main/
        └── resources/
            └── templates/
                │
                ├── java/
                │   ├── main-class.ftl
                │   ├── orchestrator.ftl
                │   ├── node-executor.ftl
                │   ├── rag-node-impl.ftl
                │   ├── agent-node-impl.ftl
                │   ├── tool-node-impl.ftl
                │   └── decision-node-impl.ftl
                │
                ├── python/
                │   ├── main.ftl
                │   ├── orchestrator.ftl
                │   ├── executor.ftl
                │   └── requirements.ftl
                │
                ├── build/
                │   ├── pom.ftl
                │   ├── Dockerfile.ftl
                │   ├── docker-compose.ftl
                │   └── build.sh.ftl
                │
                └── config/
                    ├── application.properties.ftl
                    ├── logback.xml.ftl
                    └── README.md.ftl
```