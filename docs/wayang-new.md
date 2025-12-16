# AI Agent Workflow Platform - Project Structure

## Overview
Multi-tenant, microservices-based AI agent workflow platform built with Quarkus, following clean architecture and domain-driven design principles.

---

## Module Architecture

```
agentic-platform/
├── platform-parent/                    # Parent POM with dependency management
│   └── pom.xml
│
├── platform-api/                       # Public API contracts (shared)
│   ├── src/main/java/io/agentic/platform/
│   │   ├── schema/                     # Core schema definitions
│   │   ├── error/                      # Error types and payloads
│   │   └── api/                        # REST API contracts
│   └── pom.xml
│
├── platform-core/                      # Core workflow engine (this module)
│   ├── src/main/java/io/agentic/platform/engine/
│   │   ├── WorkflowEngine.java         # Main orchestrator
│   │   ├── execution/                  # Execution components
│   │   │   ├── ExecutionContext.java
│   │   │   ├── NodeExecutor.java
│   │   │   └── NodeContext.java
│   │   ├── state/                      # State management
│   │   │   ├── StateStore.java
│   │   │   ├── WorkflowRunManager.java
│   │   │   └── WorkflowRun.java
│   │   └── error/                      # Error handling
│   │       └── ErrorOrchestrator.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/               # Flyway migrations
│   └── pom.xml
│
├── platform-nodes/                     # Node implementations
│   ├── platform-node-base/             # Base abstractions
│   │   ├── AbstractNode.java
│   │   ├── IntegrationNode.java
│   │   └── AgentNode.java
│   ├── platform-node-llm/              # LLM nodes
│   ├── platform-node-rag/              # RAG nodes
│   ├── platform-node-mcp/              # MCP integration
│   └── platform-node-tools/            # Tool nodes
│
├── platform-guardrails/                # Safety & policy enforcement
│   ├── GuardrailsEngine.java
│   ├── PolicyEngine.java
│   └── ContentFilter.java
│
├── platform-audit/                     # Provenance & audit
│   ├── ProvenanceService.java
│   ├── AuditNode.java
│   └── EventStore.java
│
├── platform-hitl/                      # Human-in-the-loop
│   ├── HTILService.java
│   ├── HumanTaskManager.java
│   └── TaskUI/                         # UI components
│
├── platform-integration/               # Camel & connectors
│   ├── CamelRuntime.java
│   ├── ConnectorRegistry.java
│   └── adapters/
│
├── platform-plugin/                    # Plugin system
│   ├── PluginRegistry.java
│   ├── PluginLoader.java
│   └── PluginSandbox.java
│
├── platform-telemetry/                 # Observability
│   ├── TelemetryService.java
│   ├── MetricsCollector.java
│   └── TracingService.java
│
├── platform-standalone/                # Standalone runtime generator
│   ├── CodeGenerator.java
│   ├── DependencyResolver.java
│   └── templates/
│
├── platform-services/                  # Microservices
│   ├── workflow-service/               # Main workflow API
│   ├── execution-service/              # Execution workers
│   ├── designer-service/               # Visual editor backend
│   └── api-gateway/                    # API Gateway
│
└── platform-deployment/                # Deployment configs
    ├── docker/
    ├── kubernetes/
    └── terraform/
```

---

## Key Design Decisions

### 1. Modular Architecture
Each module is independently deployable and can be included/excluded based on deployment needs:
- **Core modules**: Always required (engine, state, error)
- **Optional modules**: HITL, plugins, advanced guardrails
- **Standalone**: Minimal subset for embedded deployments

### 2. Shared vs Isolated Dependencies
- `platform-api`: Shared across all modules (schemas, contracts)
- `platform-core`: Heavy dependencies (Quarkus, Hibernate Reactive)
- `platform-standalone`: Stripped dependencies for minimal footprint

### 3. Multi-Tenancy Strategy
- **Database-level**: Tenant ID in all tables with row-level security
- **Runtime-level**: Tenant context propagation through MDC
- **Resource isolation**: Separate resource pools per tenant (optional)

### 4. Error Handling Architecture
Following blueprint requirements:
- Every node emits `success` and `error` outputs
- `ErrorOrchestrator` routes errors deterministically
- CEL-based policy evaluation for error routing
- Built-in retry, self-heal, and HITL escalation

### 5. Audit Trail
- All state transitions logged to `ProvenanceService`
- Cryptographic hashing for tamper detection
- Append-only event store for compliance
- Separate audit database (optional)

---

## Technology Stack

### Core Framework
- **Quarkus 3.8+**: Reactive, cloud-native Java
- **Mutiny**: Reactive programming (non-blocking)
- **Hibernate Reactive + Panache**: Reactive ORM
- **PostgreSQL 15+**: Main database with JSONB support

### AI/LLM Integration
- **LangChain4j**: LLM orchestration and agents
- **Ollama/OpenAI/Anthropic**: LLM providers
- **Qdrant/Weaviate**: Vector databases for RAG

### Integration
- **Apache Camel 4.x**: Integration framework
- **SmallRye Reactive Messaging**: Event streaming
- **Quarkus REST Client**: HTTP integrations

### Observability
- **OpenTelemetry**: Distributed tracing
- **Micrometer**: Metrics collection
- **Quarkus Logging**: Structured logging

### Security
- **Quarkus Security**: Authentication/authorization
- **JWT/OAuth2**: Token-based auth
- **Vault**: Secret management

### Testing
- **JUnit 5**: Unit testing
- **RestAssured**: API testing
- **Testcontainers**: Integration testing
- **WireMock**: Mock external services

---

## Database Schema

### Core Tables

```sql
-- Workflow Runs (main state table)
CREATE TABLE workflow_runs (
    id VARCHAR(36) PRIMARY KEY,
    workflow_id VARCHAR(100) NOT NULL,
    workflow_version VARCHAR(20) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    inputs JSONB,
    outputs JSONB,
    checkpoint JSONB,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    updated_at TIMESTAMP NOT NULL,
    initiated_by VARCHAR(100),
    error TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_workflow FOREIGN KEY (workflow_id, tenant_id) 
        REFERENCES workflows(id, tenant_id)
);

CREATE INDEX idx_workflow_tenant ON workflow_runs(workflow_id, tenant_id);
CREATE INDEX idx_status_tenant ON workflow_runs(status, tenant_id);
CREATE INDEX idx_start_time ON workflow_runs(start_time DESC);

-- Workflows
CREATE TABLE workflows (
    id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    name VARCHAR(200) NOT NULL,
    version VARCHAR(20) NOT NULL,
    definition JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    status VARCHAR(20) DEFAULT 'active',
    PRIMARY KEY (id, tenant_id)
);

-- Audit Events (append-only)
CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id VARCHAR(36) NOT NULL,
    node_id VARCHAR(100),
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB NOT NULL,
    actor_type VARCHAR(20) NOT NULL,
    actor_id VARCHAR(100),
    timestamp TIMESTAMP NOT NULL,
    hash VARCHAR(64),
    CONSTRAINT fk_run FOREIGN KEY (run_id) 
        REFERENCES workflow_runs(id) ON DELETE CASCADE
);

CREATE INDEX idx_run_events ON audit_events(run_id, timestamp);
CREATE INDEX idx_event_type ON audit_events(event_type);

-- Human Tasks (HITL)
CREATE TABLE human_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id VARCHAR(36) NOT NULL,
    node_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    context JSONB NOT NULL,
    error JSONB,
    assigned_to VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    decision JSONB,
    ttl_minutes INTEGER DEFAULT 60,
    CONSTRAINT fk_task_run FOREIGN KEY (run_id) 
        REFERENCES workflow_runs(id) ON DELETE CASCADE
);

CREATE INDEX idx_task_status ON human_tasks(status, created_at);
CREATE INDEX idx_task_assignee ON human_tasks(assigned_to);

-- Plugins
CREATE TABLE plugins (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    version VARCHAR(20) NOT NULL,
    descriptor JSONB NOT NULL,
    implementation JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    published_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    checksum VARCHAR(128) NOT NULL
);

-- Node Registry (cached metadata)
CREATE TABLE node_registry (
    node_type VARCHAR(100) PRIMARY KEY,
    descriptor JSONB NOT NULL,
    plugin_id VARCHAR(100),
    capabilities TEXT[],
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_plugin FOREIGN KEY (plugin_id) 
        REFERENCES plugins(id) ON DELETE SET NULL
);
```

---

## Configuration

### application.yml (platform-core)

```yaml
quarkus:
  application:
    name: agentic-platform-core
  
  # Database Configuration
  datasource:
    db-kind: postgresql
    username: ${DB_USER:platform}
    password: ${DB_PASSWORD:platform}
    reactive:
      url: postgresql://${DB_HOST:localhost}:5432/${DB_NAME:agentic_platform}
      max-size: 20
      idle-timeout: PT10M
  
  hibernate-orm:
    database:
      generation: validate
    log:
      sql: ${LOG_SQL:false}
  
  # Flyway Migration
  flyway:
    migrate-at-start: true
    baseline-on-migrate: true
    locations: classpath:db/migration
  
  # REST Configuration
  rest:
    path: /api
  
  http:
    port: 8080
    cors:
      origins: "*"
      methods: GET,POST,PUT,DELETE,OPTIONS
  
  # Security
  security:
    jdbc:
      enabled: true
    auth:
      enabled-in-dev-mode: false
  
  # OpenTelemetry
  otel:
    enabled: true
    exporter:
      otlp:
        endpoint: ${OTEL_ENDPOINT:http://localhost:4317}
  
  # Logging
  log:
    level: INFO
    category:
      "io.agentic.platform":
        level: DEBUG
    console:
      format: "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n"

# Platform Configuration
platform:
  engine:
    # Workflow execution settings
    max-concurrent-workflows: 100
    max-concurrent-nodes: 10
    default-timeout-ms: 30000
    
    # Error handling
    error:
      max-retry-attempts: 3
      retry-backoff: exponential
      circuit-breaker-enabled: true
    
    # HITL settings
    hitl:
      default-ttl-minutes: 60
      escalation-enabled: true
  
  # Node registry
  nodes:
    cache-enabled: true
    hot-reload-enabled: ${HOT_RELOAD:true}
  
  # Plugin system
  plugins:
    enabled: true
    repository-url: ${PLUGIN_REPO:https://plugins.agentic.io}
    sandbox-level: semi-trusted
  
  # Multi-tenancy
  tenant:
    isolation: database-row
    resource-pools: false
  
  # Audit
  audit:
    enabled: true
    hashing-enabled: true
    retention-days: 90
```

---

## Dependency Management (Parent POM)

```xml
<properties>
    <quarkus.version>3.8.0</quarkus.version>
    <langchain4j.version>0.27.0</langchain4j.version>
    <camel.version>4.3.0</camel.version>
    <lombok.version>1.18.30</lombok.version>
    <postgresql.version>42.7.1</postgresql.version>
    <testcontainers.version>1.19.3</testcontainers.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Quarkus BOM -->
        <dependency>
            <groupId>io.quarkus.platform</groupId>
            <artifactId>quarkus-bom</artifactId>
            <version>${quarkus.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        
        <!-- LangChain4j -->
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-bom</artifactId>
            <version>${langchain4j.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        
        <!-- Apache Camel -->
        <dependency>
            <groupId>org.apache.camel.quarkus</groupId>
            <artifactId>camel-quarkus-bom</artifactId>
            <version>${camel.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## Build & Deployment

### Maven Build
```bash
# Build all modules
mvn clean install

# Build specific module
mvn clean package -pl platform-core

# Build native image
mvn clean package -Pnative

# Run tests
mvn test

# Integration tests
mvn verify -Pintegration-test
```

### Docker
```bash
# Build image
docker build -f src/main/docker/Dockerfile.jvm -t agentic-platform-core .

# Run with Docker Compose
docker-compose up -d
```

### Kubernetes
```bash
# Deploy to k8s
kubectl apply -f platform-deployment/kubernetes/

# Scale workers
kubectl scale deployment workflow-executor --replicas=5
```

---

## Development Workflow

### 1. Local Development
```bash
# Start dependencies
docker-compose up -d postgres redis

# Run in dev mode (hot reload)
mvn quarkus:dev

# Access dev UI
open http://localhost:8080/q/dev
```

### 2. Testing Strategy
- **Unit tests**: Mock external dependencies
- **Integration tests**: Use Testcontainers for DB
- **E2E tests**: Full workflow execution scenarios
- **Performance tests**: JMeter/Gatling for load testing

### 3. Code Quality
- **Checkstyle**: Code formatting
- **SpotBugs**: Static analysis
- **JaCoCo**: Code coverage (target: 80%)
- **SonarQube**: Quality gate

---

## Next Steps

1. **Implement remaining services**:
   - ProvenanceService
   - HTILService
   - GuardrailsEngine
   - SelfHealingService

2. **Add Camel integration layer**

3. **Build visual designer UI** (React/Vue)

4. **Create plugin SDK** for third-party extensions

5. **Add API Gateway** with rate limiting

6. **Implement agent orchestration** (MCP/A2A)

7. **Build standalone runtime generator**

8. **Add comprehensive tests**

---

## References

- Blueprint documents provided
- Quarkus guides: https://quarkus.io/guides/
- LangChain4j: https://github.com/langchain4j/langchain4j
- Apache Camel: https://camel.apache.org/