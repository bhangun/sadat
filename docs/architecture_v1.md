# Wayang Platform - Architecture Diagrams

## 1. High-Level Service Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        UI[Web UI / Mobile]
        CLI[CLI Tools]
    end
    
    subgraph "Designer Service :8080"
        DS_API[Designer REST API]
        DS_GQL[GraphQL Client]
        DS_WS[WebSocket Client]
        DS_SVC[Designer Services]
    end
    
    subgraph "Workflow Service :8081"
        WF_GQL[GraphQL API]
        WF_REST[REST API]
        WF_WS[WebSocket Server]
        WF_CMD[Command Service]
        WF_QRY[Query Service]
        WF_COLLAB[Collaboration Service]
    end
    
    subgraph "Shared Infrastructure"
        AUDIT[Audit Service]
        ERROR[Error Handler]
        METRICS[Metrics Collector]
    end
    
    subgraph "Data Layer"
        PG[(PostgreSQL + pgvector)]
        KAFKA[Kafka Event Bus]
        REDIS[(Redis Cache)]
    end
    
    UI -->|HTTP/WS| DS_API
    CLI -->|HTTP| DS_API
    
    DS_API --> DS_SVC
    DS_SVC --> DS_GQL
    DS_SVC --> DS_WS
    
    DS_GQL -->|GraphQL| WF_GQL
    DS_API -->|REST| WF_REST
    DS_WS -->|WebSocket| WF_WS
    
    WF_GQL --> WF_CMD
    WF_GQL --> WF_QRY
    WF_REST --> WF_CMD
    WF_WS --> WF_COLLAB
    
    WF_CMD --> AUDIT
    WF_CMD --> ERROR
    WF_QRY --> METRICS
    
    WF_CMD --> PG
    WF_QRY --> PG
    WF_QRY --> REDIS
    WF_COLLAB --> KAFKA
    
    AUDIT --> PG
    
    style Designer Service fill:#e1f5ff
    style Workflow Service fill:#fff4e1
    style Shared Infrastructure fill:#f0f0f0
    style Data Layer fill:#e8f5e9
```

## 2. API Communication Flow

```mermaid
sequenceDiagram
    participant UI as Web UI
    participant DS as Designer Service
    participant GQL as GraphQL API
    participant REST as REST API
    participant WS as WebSocket
    participant DB as Database
    participant AUDIT as Audit Service
    
    Note over UI,AUDIT: Workflow Query (GraphQL)
    UI->>DS: Get Workflow
    DS->>GQL: query { workflow(id) }
    GQL->>DB: SELECT workflow
    DB-->>GQL: Workflow Data
    GQL-->>DS: WorkflowDTO
    DS-->>UI: Workflow JSON
    
    Note over UI,AUDIT: Workflow Execution (REST)
    UI->>DS: Execute Workflow
    DS->>REST: POST /workflows/{id}/execute
    REST->>DB: Create Execution
    REST->>AUDIT: Log Execution Start
    REST-->>DS: ExecutionResponse (202 Accepted)
    DS-->>UI: Execution Started
    
    Note over UI,AUDIT: Real-time Collaboration (WebSocket)
    UI->>DS: Connect Collaboration
    DS->>WS: WS Connect /ws/workflows/{id}
    WS-->>DS: Connected
    DS-->>UI: Connected
    
    UI->>DS: Move Node
    DS->>WS: CURSOR_MOVE message
    WS->>WS: Broadcast to others
    WS-->>DS: NODE_MOVED event
    DS-->>UI: Update UI
```

## 3. Designer Service Internal Architecture

```mermaid
graph LR
    subgraph "Designer Service"
        API[REST Controllers]
        
        subgraph "Service Layer"
            WF_SVC[Workflow Service]
            NODE_SVC[Node Service]
            VAL_SVC[Validation Service]
            DIFF_SVC[Diff Service]
        end
        
        subgraph "Client Layer"
            GQL_CLIENT[GraphQL Client]
            REST_CLIENT[REST Client]
            WS_CLIENT[WebSocket Client]
        end
        
        subgraph "Support"
            MAPPER[DTO Mapper]
            CACHE[Local Cache]
            ERROR[Error Handler]
        end
    end
    
    API --> WF_SVC
    API --> NODE_SVC
    API --> VAL_SVC
    API --> DIFF_SVC
    
    WF_SVC --> GQL_CLIENT
    WF_SVC --> REST_CLIENT
    WF_SVC --> WS_CLIENT
    
    NODE_SVC --> GQL_CLIENT
    VAL_SVC --> GQL_CLIENT
    DIFF_SVC --> GQL_CLIENT
    
    WF_SVC --> MAPPER
    WF_SVC --> CACHE
    WF_SVC --> ERROR
    
    GQL_CLIENT -.->|GraphQL| WORKFLOW_SERVICE
    REST_CLIENT -.->|REST| WORKFLOW_SERVICE
    WS_CLIENT -.->|WebSocket| WORKFLOW_SERVICE
    
    style "Designer Service" fill:#e1f5ff
```

## 4. Workflow Service Internal Architecture

```mermaid
graph TB
    subgraph "Workflow Service"
        subgraph "API Layer"
            GQL_API[GraphQL API<br/>Queries & Mutations]
            REST_API[REST API<br/>Operations]
            WS_API[WebSocket API<br/>Collaboration]
        end
        
        subgraph "Service Layer"
            CMD[Command Service<br/>Write Operations]
            QRY[Query Service<br/>Read Operations]
            COLLAB[Collaboration Service<br/>Real-time]
            VAL[Validation Service]
        end
        
        subgraph "Domain Layer"
            MAPPER[Entity Mapper]
            VALIDATOR[Workflow Validator]
            DIFF[Diff Engine]
        end
        
        subgraph "Infrastructure"
            REPO[Repository Layer]
            AUDIT[Audit Service]
            ERROR[Error Handler]
            LOCK[Lock Manager]
        end
        
        subgraph "Data Store"
            PG[(PostgreSQL)]
            REDIS[(Redis Cache)]
            KAFKA[Kafka Events]
        end
    end
    
    GQL_API --> CMD
    GQL_API --> QRY
    REST_API --> CMD
    WS_API --> COLLAB
    
    CMD --> VAL
    CMD --> MAPPER
    CMD --> REPO
    CMD --> AUDIT
    CMD --> ERROR
    CMD --> LOCK
    
    QRY --> MAPPER
    QRY --> REPO
    QRY --> ERROR
    
    COLLAB --> CMD
    COLLAB --> LOCK
    COLLAB --> KAFKA
    
    VAL --> VALIDATOR
    QRY --> DIFF
    
    REPO --> PG
    QRY --> REDIS
    AUDIT --> PG
    
    style "Workflow Service" fill:#fff4e1
```

## 5. GraphQL API Structure

```mermaid
graph LR
    subgraph "GraphQL Schema"
        QUERY[Query Type]
        MUTATION[Mutation Type]
        TYPES[Domain Types]
        INPUTS[Input Types]
    end
    
    subgraph "Resolvers"
        QUERY_RES[Query Resolver]
        MUTATION_RES[Mutation Resolver]
        FIELD_RES[Field Resolvers]
    end
    
    subgraph "Services"
        QUERY_SVC[Query Service]
        COMMAND_SVC[Command Service]
    end
    
    QUERY --> QUERY_RES
    MUTATION --> MUTATION_RES
    TYPES --> FIELD_RES
    
    QUERY_RES --> QUERY_SVC
    MUTATION_RES --> COMMAND_SVC
    FIELD_RES --> QUERY_SVC
    
    QUERY_SVC -.-> DB[(Database)]
    COMMAND_SVC -.-> DB
    COMMAND_SVC -.-> AUDIT[Audit]
```

## 6. WebSocket Collaboration Flow

```mermaid
sequenceDiagram
    participant U1 as User 1 (Designer)
    participant WS1 as WS Client 1
    participant WS_SRV as WS Server
    participant COLLAB as Collaboration Service
    participant WS2 as WS Client 2
    participant U2 as User 2 (Designer)
    
    Note over U1,U2: Connection Establishment
    U1->>WS1: Connect to workflow
    WS1->>WS_SRV: WS Connect /ws/workflows/{id}
    WS_SRV->>COLLAB: User joined event
    WS_SRV-->>WS1: Connected
    WS1-->>U1: Connected
    
    U2->>WS2: Connect to workflow
    WS2->>WS_SRV: WS Connect /ws/workflows/{id}
    WS_SRV->>WS1: USER_JOINED event
    WS_SRV-->>WS2: Connected
    WS1-->>U1: User 2 joined
    
    Note over U1,U2: Real-time Collaboration
    U1->>WS1: Move node
    WS1->>WS_SRV: NODE_MOVE message
    WS_SRV->>COLLAB: Process move
    COLLAB->>WS_SRV: Broadcast event
    WS_SRV->>WS2: NODE_MOVED event
    WS2->>U2: Update node position
    
    U1->>WS1: Lock node
    WS1->>WS_SRV: NODE_LOCK message
    WS_SRV->>COLLAB: Lock node
    COLLAB->>DB: Update lock
    COLLAB->>WS_SRV: Lock success
    WS_SRV->>WS2: NODE_LOCKED event
    WS2->>U2: Show locked node
    
    Note over U1,U2: Disconnection
    U1->>WS1: Disconnect
    WS1->>WS_SRV: WS Close
    WS_SRV->>COLLAB: User left
    COLLAB->>DB: Release locks
    WS_SRV->>WS2: USER_LEFT event
    WS2->>U2: User 1 left
```

## 7. Error Handling & Audit Flow

```mermaid
graph TB
    subgraph "Request Flow"
        REQ[Client Request]
        SVC[Service Method]
        REPO[Repository]
    end
    
    subgraph "Error Handling"
        CATCH[Try-Catch Block]
        TRANSFORM[Exception Transformer]
        HANDLER[Error Handler Service]
        RECOVERY[Recovery Strategy]
    end
    
    subgraph "Audit Trail"
        PRE_AUDIT[Pre-Operation Audit]
        POST_AUDIT[Post-Operation Audit]
        ERROR_AUDIT[Error Audit]
        AUDIT_DB[(Audit Database)]
    end
    
    subgraph "Response"
        SUCCESS[Success Response]
        ERROR_RESP[Error Response]
    end
    
    REQ --> SVC
    SVC --> PRE_AUDIT
    PRE_AUDIT --> REPO
    
    REPO -->|Success| POST_AUDIT
    POST_AUDIT --> SUCCESS
    
    REPO -->|Error| CATCH
    CATCH --> TRANSFORM
    TRANSFORM --> HANDLER
    HANDLER --> ERROR_AUDIT
    ERROR_AUDIT --> RECOVERY
    RECOVERY --> ERROR_RESP
    
    PRE_AUDIT --> AUDIT_DB
    POST_AUDIT --> AUDIT_DB
    ERROR_AUDIT --> AUDIT_DB
    
    style "Error Handling" fill:#ffebee
    style "Audit Trail" fill:#e8f5e9
```

## 8. Data Model Relationships

```mermaid
erDiagram
    WORKFLOW ||--o{ NODE : contains
    WORKFLOW ||--o{ CONNECTION : contains
    WORKFLOW ||--|| UI_DEFINITION : has
    WORKFLOW ||--|| RUNTIME_CONFIG : has
    WORKFLOW ||--o{ VERSION : has
    WORKFLOW ||--o{ EXECUTION : executes
    
    NODE ||--o{ PORT_DESCRIPTOR : has
    NODE ||--o{ NODE_LOCK : locked_by
    
    CONNECTION }o--|| NODE : from
    CONNECTION }o--|| NODE : to
    
    EXECUTION ||--o{ NODE_EXECUTION : contains
    EXECUTION ||--o{ AUDIT_EVENT : logs
    
    WORKFLOW {
        uuid id PK
        string version
        string name
        string tenant_id
        enum status
        timestamp created_at
        jsonb logic
        jsonb ui
        jsonb runtime
    }
    
    NODE {
        string id PK
        string type
        string name
        jsonb properties
        boolean locked
        string locked_by
    }
    
    CONNECTION {
        string id PK
        string from FK
        string to FK
        string from_port
        string to_port
        string condition
    }
    
    EXECUTION {
        uuid id PK
        uuid workflow_id FK
        enum status
        timestamp started_at
        timestamp completed_at
        jsonb outputs
    }
    
    AUDIT_EVENT {
        uuid id PK
        string event_type
        string entity_id
        string user_id
        string tenant_id
        jsonb metadata
        timestamp created_at
    }
```

## 9. Deployment Architecture

```mermaid
graph TB
    subgraph "Load Balancer"
        LB[Nginx / ALB]
    end
    
    subgraph "Designer Service Cluster"
        DS1[Designer Pod 1]
        DS2[Designer Pod 2]
        DS3[Designer Pod 3]
    end
    
    subgraph "Workflow Service Cluster"
        WF1[Workflow Pod 1]
        WF2[Workflow Pod 2]
        WF3[Workflow Pod 3]
    end
    
    subgraph "Data Layer"
        PG_PRIMARY[(PostgreSQL Primary)]
        PG_REPLICA[(PostgreSQL Replica)]
        REDIS_CLUSTER[(Redis Cluster)]
        KAFKA_CLUSTER[Kafka Cluster]
    end
    
    subgraph "Observability"
        PROMETHEUS[Prometheus]
        GRAFANA[Grafana]
        JAEGER[Jaeger Tracing]
    end
    
    LB --> DS1
    LB --> DS2
    LB --> DS3
    
    DS1 --> WF1
    DS1 --> WF2
    DS1 --> WF3
    
    DS2 --> WF1
    DS2 --> WF2
    DS2 --> WF3
    
    DS3 --> WF1
    DS3 --> WF2
    DS3 --> WF3
    
    WF1 --> PG_PRIMARY
    WF2 --> PG_REPLICA
    WF3 --> PG_REPLICA
    
    WF1 --> REDIS_CLUSTER
    WF2 --> REDIS_CLUSTER
    WF3 --> REDIS_CLUSTER
    
    WF1 --> KAFKA_CLUSTER
    WF2 --> KAFKA_CLUSTER
    WF3 --> KAFKA_CLUSTER
    
    DS1 -.->|Metrics| PROMETHEUS
    DS2 -.->|Metrics| PROMETHEUS
    DS3 -.->|Metrics| PROMETHEUS
    
    WF1 -.->|Metrics| PROMETHEUS
    WF2 -.->|Metrics| PROMETHEUS
    WF3 -.->|Metrics| PROMETHEUS
    
    PROMETHEUS --> GRAFANA
    
    WF1 -.->|Traces| JAEGER
    WF2 -.->|Traces| JAEGER
    WF3 -.->|Traces| JAEGER
    
    style "Designer Service Cluster" fill:#e1f5ff
    style "Workflow Service Cluster" fill:#fff4e1
    style "Data Layer" fill:#e8f5e9
    style "Observability" fill:#f3e5f5
```

## 10. Security & Multi-tenancy Flow

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Auth as Auth Service
    participant Designer as Designer Service
    participant Workflow as Workflow Service
    participant DB as Database
    
    Note over Client,DB: Authentication
    Client->>Gateway: Request with JWT
    Gateway->>Auth: Validate JWT
    Auth->>Auth: Extract tenant_id & user_id
    Auth-->>Gateway: Valid + Context
    
    Note over Client,DB: Authorization
    Gateway->>Designer: Request + Context Headers
    Designer->>Designer: Check RBAC
    Designer->>Workflow: GraphQL/REST + tenant_id
    Workflow->>Workflow: Validate tenant access
    
    Note over Client,DB: Data Isolation
    Workflow->>DB: Query with tenant_id filter
    DB->>DB: Row-level security
    DB-->>Workflow: Tenant-scoped data
    Workflow-->>Designer: Response
    Designer-->>Gateway: Response
    Gateway-->>Client: Response
    
    Note over Client,DB: Audit Trail
    Workflow->>DB: Insert audit_event
    DB-->>Workflow: Audit logged
```

## 11. Complete Request Lifecycle

```mermaid
stateDiagram-v2
    [*] --> ClientRequest
    
    ClientRequest --> DesignerService: HTTP/GraphQL/WebSocket
    
    DesignerService --> ValidateRequest: Validate Input
    ValidateRequest --> RouteRequest: Valid
    ValidateRequest --> ErrorResponse: Invalid
    
    RouteRequest --> GraphQLClient: Metadata Query
    RouteRequest --> RESTClient: Heavy Operation
    RouteRequest --> WebSocketClient: Real-time
    
    GraphQLClient --> WorkflowService: GraphQL Query
    RESTClient --> WorkflowService: REST POST/GET
    WebSocketClient --> WorkflowService: WS Message
    
    WorkflowService --> AuthorizeRequest: Check Tenant
    AuthorizeRequest --> ProcessRequest: Authorized
    AuthorizeRequest --> ErrorResponse: Unauthorized
    
    ProcessRequest --> PreAudit: Log Start
    PreAudit --> ExecuteOperation: Continue
    
    ExecuteOperation --> Database: Query/Mutate
    Database --> PostAudit: Success
    Database --> ErrorHandler: Failure
    
    PostAudit --> SuccessResponse: Return Result
    
    ErrorHandler --> ErrorAudit: Log Error
    ErrorAudit --> RecoveryStrategy: Apply Recovery
    RecoveryStrategy --> ErrorResponse: Return Error
    
    SuccessResponse --> ClientResponse
    ErrorResponse --> ClientResponse
    
    ClientResponse --> [*]
```

## 12. Node Execution with Error Handling

```mermaid
flowchart TD
    START([Node Execution Start]) --> VALIDATE{Validate Input}
    
    VALIDATE -->|Valid| PRE_GUARD[Pre-Guardrails Check]
    VALIDATE -->|Invalid| ERROR_HANDLER[Error Handler]
    
    PRE_GUARD -->|Pass| EXECUTE[Execute Node Logic]
    PRE_GUARD -->|Fail| ERROR_HANDLER
    
    EXECUTE -->|Success| POST_GUARD[Post-Guardrails Check]
    EXECUTE -->|Error| RETRY{Retry Policy?}
    
    RETRY -->|Yes, Attempts < Max| BACKOFF[Exponential Backoff]
    RETRY -->|No, Max Reached| ERROR_HANDLER
    BACKOFF --> EXECUTE
    
    POST_GUARD -->|Pass| AUDIT[Audit Success]
    POST_GUARD -->|Fail| ERROR_HANDLER
    
    ERROR_HANDLER --> ERROR_AUDIT[Audit Error]
    ERROR_AUDIT --> RECOVERY{Recovery Strategy}
    
    RECOVERY -->|Fallback Node| FALLBACK[Execute Fallback]
    RECOVERY -->|Self-Heal| SELF_HEAL[Auto-Fix & Retry]
    RECOVERY -->|HITL| HUMAN[Human Review]
    RECOVERY -->|Abort| FAIL([Fail])
    
    FALLBACK --> AUDIT
    SELF_HEAL --> EXECUTE
    HUMAN --> WAIT[Wait for Decision]
    WAIT --> EXECUTE
    
    AUDIT --> SUCCESS([Success])
    
    style START fill:#e8f5e9
    style SUCCESS fill:#e8f5e9
    style FAIL fill:#ffebee
    style ERROR_HANDLER fill:#fff3e0
    style HUMAN fill:#e3f2fd
```

---

## Summary

These diagrams cover:

1. **High-level architecture** - Service boundaries and communication
2. **API flows** - GraphQL, REST, WebSocket interactions
3. **Internal architectures** - Designer and Workflow service internals
4. **Collaboration** - Real-time WebSocket messaging
5. **Error handling** - Comprehensive error flow with audit
6. **Data model** - Entity relationships
7. **Deployment** - Kubernetes cluster architecture
8. **Security** - Multi-tenancy and authentication flow
9. **Request lifecycle** - Complete end-to-end state machine
10. **Node execution** - Error handling and recovery patterns

Each diagram provides a different view of the system for different stakeholders (developers, architects, DevOps, security teams).