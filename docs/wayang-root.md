```
wayang-platform/
├── pom.xml (parent)
├── README.md
├── LICENSE
├── .gitignore
├── docker-compose.yml
├── kubernetes/
│   ├── namespace.yaml
│   ├── configmap.yaml
│   └── deployments/
├── docs/
│   ├── architecture.md
│   ├── api-reference.md
│   └── deployment-guide.md
│
├── wayang-core/                    # Core domain models and interfaces
├── wayang-common/                  # Shared utilities
├── wayang-api/                     # API contracts and DTOs
│
├── wayang-node-core/               # Node system foundation
├── wayang-node-types/              # Built-in node implementations
├── wayang-node-registry/           # Node schema registry
│
├── wayang-orchestrator/            # Workflow orchestration engine
├── wayang-planner/                 # Planning engine
├── wayang-executor/                # Node execution engine
├── wayang-runtime-hub/             # Distributed runtime management
│
├── wayang-rag/                     # RAG service
├── wayang-memory/                  # Memory service
├── wayang-knowledge-graph/         # Knowledge graph service
│
├── wayang-llm/                     # LLM runtime abstraction
├── wayang-tools/                   # Tool gateway (MCP)
├── wayang-guardrails/              # Guardrails engine
│
├── wayang-a2a/                     # Agent-to-agent communication
├── wayang-designer/                # Workflow designer service
├── wayang-versioning/              # Versioning service
├── wayang-plugins/                 # Plugin manager
│
├── wayang-linter/                  # Workflow linter & optimizer
├── wayang-debugger/                # Debugger & visualizer
├── wayang-authoring/               # Node authoring assistant
├── wayang-codegen/                 # Standalone agent generator
│
├── wayang-gateway/                 # API gateway
├── wayang-auth/                    # Authentication & authorization
├── wayang-audit/                   # Audit service
│
├── wayang-observability/           # Observability & telemetry
├── wayang-storage/                 # Storage layer (State, Checkpoints)
│
├── wayang-standalone-runtime/      # Minimal runtime for standalone agents
└── wayang-integration-tests/       # End-to-end tests
```



```
wayang-platform/
│
├── 🟦 PLATFORM MODULES (Model 1 - Full Runtime)
│   ├── wayang-core/                    # Core abstractions
│   ├── wayang-common/                  # Shared utilities
│   ├── wayang-api/                     # API contracts
│   │
│   ├── wayang-runtime-platform/        # Full platform runtime
│   │   ├── Dynamic component loader
│   │   ├── Plugin system
│   │   └── Hot-reload support
│   │
│   └── All service modules (orchestrator, planner, etc.)
│
├── 🟩 CODEGEN MODULES (Model 2 - Standalone Generation)
│   ├── wayang-codegen-engine/          # Code generation engine
│   │   ├── Workflow analyzer
│   │   ├── Dependency resolver
│   │   ├── Code synthesizer
│   │   └── Build optimizer
│   │
│   ├── wayang-codegen-templates/       # Code templates
│   │   ├── Java templates
│   │   ├── Python templates
│   │   └── Configuration templates
│   │
│   └── wayang-codegen-optimizer/       # Tree-shaking optimizer
│       ├── Dependency pruner
│       ├── Dead code eliminator
│       └── Size optimizer
│
├── 🟨 RUNTIME LIBRARIES (Minimal, composable)
│   ├── wayang-runtime-minimal/         # Absolute minimum
│   │   ├── Node execution
│   │   └── Basic context
│   │
│   ├── wayang-runtime-llm/             # LLM support (optional)
│   ├── wayang-runtime-rag/             # RAG support (optional)
│   ├── wayang-runtime-tools/           # Tool execution (optional)
│   ├── wayang-runtime-memory/          # Memory support (optional)
│   ├── wayang-runtime-guardrails/      # Guardrails (optional)
│   └── ... (each component is separate)
│
└── 🟪 NODE LIBRARIES (Fine-grained, composable)
    ├── wayang-node-agent/              # Agent node only
    ├── wayang-node-rag/                # RAG node only
    ├── wayang-node-tool/               # Tool node only
    ├── wayang-node-decision/           # Decision node only
    └── ... (each node type is separate)
```


