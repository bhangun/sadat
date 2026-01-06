# Silat Agent Executor - Production Deployment Guide

## 📦 Complete Production Setup

### 1. Database Schema (PostgreSQL)

```sql
-- ============================================================================
-- SILAT AGENT DATABASE SCHEMA
-- ============================================================================

-- Agent Configurations Table
CREATE TABLE agent_configurations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    llm_provider VARCHAR(100),
    llm_model VARCHAR(100),
    temperature DOUBLE PRECISION,
    max_tokens INTEGER,
    memory_enabled BOOLEAN DEFAULT true,
    memory_type VARCHAR(50) DEFAULT 'buffer',
    memory_window_size INTEGER DEFAULT 10,
    enabled_tools TEXT, -- JSON array
    allow_tool_calls BOOLEAN DEFAULT true,
    system_prompt TEXT,
    streaming BOOLEAN DEFAULT false,
    max_iterations INTEGER DEFAULT 5,
    additional_config TEXT, -- JSON object
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(agent_id, tenant_id)
);

CREATE INDEX idx_agent_tenant ON agent_configurations(agent_id, tenant_id);
CREATE INDEX idx_agent_configurations_tenant ON agent_configurations(tenant_id);

-- Conversation Sessions Table
CREATE TABLE conversation_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    agent_id VARCHAR(255),
    user_id VARCHAR(255),
    message_count INTEGER DEFAULT 0,
    total_tokens BIGINT DEFAULT 0,
    session_metadata TEXT, -- JSON
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP
);

CREATE INDEX idx_session_tenant ON conversation_sessions(session_id, tenant_id);
CREATE INDEX idx_session_created ON conversation_sessions(created_at);
CREATE INDEX idx_session_updated ON conversation_sessions(updated_at);

-- Conversation Messages Table
CREATE TABLE conversation_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    sequence_number INTEGER NOT NULL,
    role VARCHAR(50) NOT NULL,
    content TEXT,
    tool_calls TEXT, -- JSON array
    tool_call_id VARCHAR(255),
    timestamp TIMESTAMP NOT NULL,
    token_count INTEGER
);

CREATE INDEX idx_message_session ON conversation_messages(session_id, sequence_number);
CREATE INDEX idx_message_tenant ON conversation_messages(tenant_id);
CREATE INDEX idx_message_timestamp ON conversation_messages(timestamp);

-- Agent Executions (Audit) Table
CREATE TABLE agent_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id VARCHAR(255) NOT NULL,
    node_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    session_id VARCHAR(255),
    agent_id VARCHAR(255),
    status VARCHAR(50),
    iterations INTEGER,
    tool_calls_count INTEGER,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,
    duration_ms BIGINT,
    llm_provider VARCHAR(100),
    llm_model VARCHAR(100),
    error_message TEXT,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE INDEX idx_exec_run ON agent_executions(run_id);
CREATE INDEX idx_exec_tenant ON agent_executions(tenant_id);
CREATE INDEX idx_exec_status ON agent_executions(status);
CREATE INDEX idx_exec_started ON agent_executions(started_at);

-- API Keys Table
CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    tenant_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    roles TEXT, -- JSON array
    permissions TEXT, -- JSON array
    active BOOLEAN DEFAULT true,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_key_hash ON api_keys(key_hash);
CREATE INDEX idx_api_key_tenant ON api_keys(tenant_id);

-- Secrets Table (encrypted at rest)
CREATE TABLE secrets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    encrypted_value TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(key, tenant_id)
);

CREATE INDEX idx_secret_tenant ON secrets(tenant_id);

-- Audit Logs Table
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    user_id VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    outcome VARCHAR(50),
    details TEXT, -- JSON
    ip_address VARCHAR(45),
    user_agent TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_tenant ON audit_logs(tenant_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_event_type ON audit_logs(event_type);

-- Partitioning for high-volume tables
CREATE TABLE conversation_messages_2024_01 PARTITION OF conversation_messages
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

-- Add more partitions as needed
```

### 2. Complete Application Configuration

```properties
# ============================================================================
# SILAT AGENT EXECUTOR - PRODUCTION CONFIGURATION
# ============================================================================

# Application
quarkus.application.name=silat-agent-executor
quarkus.application.version=1.0.0

# ==================== DATABASE ====================
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=${DB_USERNAME:silat}
quarkus.datasource.password=${DB_PASSWORD}
quarkus.datasource.jdbc.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:silat}
quarkus.datasource.jdbc.max-size=20
quarkus.datasource.jdbc.min-size=5

# Hibernate
quarkus.hibernate-orm.database.generation=validate
quarkus.hibernate-orm.log.sql=false
quarkus.hibernate-orm.dialect=org.hibernate.dialect.PostgreSQLDialect

# Flyway migrations
quarkus.flyway.migrate-at-start=true
quarkus.flyway.baseline-on-migrate=true

# ==================== REDIS (CACHING) ====================
quarkus.redis.hosts=redis://${REDIS_HOST:localhost}:${REDIS_PORT:6379}
quarkus.redis.password=${REDIS_PASSWORD:}
quarkus.redis.database=0
quarkus.redis.max-pool-size=20

# ==================== LLM PROVIDERS ====================
silat.agent.llm.openai.api-key=${OPENAI_API_KEY}
silat.agent.llm.openai.base-url=https://api.openai.com/v1
silat.agent.llm.openai.timeout=60000
silat.agent.llm.openai.max-retries=3

silat.agent.llm.anthropic.api-key=${ANTHROPIC_API_KEY}
silat.agent.llm.anthropic.base-url=https://api.anthropic.com/v1
silat.agent.llm.anthropic.timeout=60000

silat.agent.llm.azure.api-key=${AZURE_OPENAI_API_KEY:}
silat.agent.llm.azure.endpoint=${AZURE_OPENAI_ENDPOINT:}
silat.agent.llm.azure.deployment=${AZURE_OPENAI_DEPLOYMENT:}

# ==================== MEMORY ====================
silat.agent.memory.cache-size=1000
silat.agent.memory.cache-ttl-ms=3600000
silat.agent.memory.default-type=buffer
silat.agent.memory.default-window-size=10

# Vector Database (for semantic memory)
silat.agent.memory.vector.enabled=false
silat.agent.memory.vector.provider=pinecone
silat.agent.memory.vector.api-key=${PINECONE_API_KEY:}
silat.agent.memory.vector.environment=${PINECONE_ENVIRONMENT:}
silat.agent.memory.vector.index-name=agent-memory

# ==================== TOOLS ====================
silat.agent.tools.enabled=calculator,web_search,current_time
silat.agent.tools.web-search.provider=google
silat.agent.tools.web-search.api-key=${GOOGLE_SEARCH_API_KEY:}
silat.agent.tools.web-search.cx=${GOOGLE_SEARCH_CX:}

# Tool Execution
silat.agent.tools.execution.timeout-seconds=30
silat.agent.tools.execution.max-concurrent=10

# ==================== SECURITY ====================
silat.security.api-key.header=X-API-Key
silat.security.tenant-header=X-Tenant-ID
silat.security.rate-limit.capacity=100
silat.security.rate-limit.refill-rate=10

# Encryption
silat.security.encryption.algorithm=AES/GCM/NoPadding
silat.security.encryption.key=${ENCRYPTION_KEY}

# JWT (optional)
mp.jwt.verify.publickey.location=${JWT_PUBLIC_KEY_URL:}
mp.jwt.verify.issuer=${JWT_ISSUER:}

# ==================== EXECUTOR ====================
silat.executor.type=common-agent
silat.executor.transport=GRPC
silat.executor.max-concurrent-tasks=10

# gRPC Configuration
quarkus.grpc.server.port=9090
quarkus.grpc.server.host=0.0.0.0
quarkus.grpc.server.enable-reflection-service=true

# Kafka Configuration (if using Kafka transport)
kafka.bootstrap.servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
mp.messaging.incoming.workflow-tasks.connector=smallrye-kafka
mp.messaging.incoming.workflow-tasks.topic=silat.workflow.tasks
mp.messaging.incoming.workflow-tasks.group.id=agent-executor
mp.messaging.outgoing.workflow-results.connector=smallrye-kafka
mp.messaging.outgoing.workflow-results.topic=silat.workflow.results

# ==================== OBSERVABILITY ====================

# Metrics
quarkus.micrometer.enabled=true
quarkus.micrometer.registry-enabled-default=true
quarkus.micrometer.binder.jvm=true
quarkus.micrometer.binder.system=true
quarkus.micrometer.export.prometheus.enabled=true
quarkus.micrometer.export.prometheus.path=/metrics

# Tracing
quarkus.otel.enabled=true
quarkus.otel.traces.enabled=true
quarkus.otel.exporter.otlp.traces.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4317}
quarkus.otel.service.name=silat-agent-executor
quarkus.otel.resource.attributes=deployment.environment=${ENVIRONMENT:dev}

# Logging
quarkus.log.level=INFO
quarkus.log.category."tech.kayys.silat".level=DEBUG
quarkus.log.console.json=true
quarkus.log.console.json.additional-field."service".value=silat-agent-executor
quarkus.log.console.json.additional-field."environment".value=${ENVIRONMENT:dev}

# ==================== RESILIENCE ====================

# Circuit Breaker
fault-tolerance.circuit-breaker.llm-provider.delay=5000
fault-tolerance.circuit-breaker.llm-provider.requestVolumeThreshold=10
fault-tolerance.circuit-breaker.llm-provider.failureRatio=0.5

# Bulkhead
fault-tolerance.bulkhead.llm-provider.value=10
fault-tolerance.bulkhead.llm-provider.waitingTaskQueue=20

# Timeout
fault-tolerance.timeout.llm-provider=30000
fault-tolerance.timeout.tool-execution=15000

# Retry
fault-tolerance.retry.llm-provider.maxRetries=3
fault-tolerance.retry.llm-provider.delay=1000
fault-tolerance.retry.llm-provider.maxDuration=60000

# ==================== HEALTH CHECKS ====================
quarkus.smallrye-health.root-path=/health
quarkus.smallrye-health.liveness-path=/health/live
quarkus.smallrye-health.readiness-path=/health/ready

# ==================== PERFORMANCE ====================
quarkus.thread-pool.core-threads=8
quarkus.thread-pool.max-threads=100
quarkus.vertx.event-loops-pool-size=4
quarkus.vertx.worker-pool-size=20

# Virtual Threads (Java 21+)
quarkus.virtual-threads.enabled=true

# ==================== DEVELOPMENT ====================
%dev.quarkus.log.console.json=false
%dev.quarkus.hibernate-orm.log.sql=true
%dev.quarkus.datasource.jdbc.max-size=5

# ==================== PRODUCTION ====================
%prod.quarkus.log.level=WARN
%prod.quarkus.log.category."tech.kayys.silat".level=INFO
%prod.quarkus.hibernate-orm.log.sql=false
%prod.quarkus.datasource.jdbc.max-size=50
```

### 3. Docker Compose for Local Development

```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:16-alpine
    container_name: silat-postgres
    environment:
      POSTGRES_DB: silat
      POSTGRES_USER: silat
      POSTGRES_PASSWORD: silat_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U silat"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis Cache
  redis:
    image: redis:7-alpine
    container_name: silat-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

  # Kafka (Optional)
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    container_name: silat-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:latest
    container_name: silat-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  # OpenTelemetry Collector
  otel-collector:
    image: otel/opentelemetry-collector:latest
    container_name: silat-otel-collector
    command: ["--config=/etc/otel-collector-config.yaml"]
    volumes:
      - ./otel-collector-config.yaml:/etc/otel-collector-config.yaml
    ports:
      - "4317:4317" # OTLP gRPC
      - "4318:4318" # OTLP HTTP

  # Prometheus
  prometheus:
    image: prom/prometheus:latest
    container_name: silat-prometheus
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    ports:
      - "9090:9090"
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'

  # Grafana
  grafana:
    image: grafana/grafana:latest
    container_name: silat-grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    volumes:
      - grafana_data:/var/lib/grafana

volumes:
  postgres_data:
  redis_data:
  prometheus_data:
  grafana_data:
```

### 4. Kubernetes Deployment

```yaml
# ============================================================================
# KUBERNETES DEPLOYMENT
# ============================================================================

apiVersion: v1
kind: Namespace
metadata:
  name: silat

---
# ConfigMap for Application Configuration
apiVersion: v1
kind: ConfigMap
metadata:
  name: silat-agent-config
  namespace: silat
data:
  application.properties: |
    # See application.properties above

---
# Secret for Sensitive Configuration
apiVersion: v1
kind: Secret
metadata:
  name: silat-agent-secrets
  namespace: silat
type: Opaque
stringData:
  OPENAI_API_KEY: "your-openai-key"
  ANTHROPIC_API_KEY: "your-anthropic-key"
  DB_PASSWORD: "your-db-password"
  ENCRYPTION_KEY: "your-encryption-key"

---
# Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: silat-agent-executor
  namespace: silat
spec:
  replicas: 3
  selector:
    matchLabels:
      app: silat-agent-executor
  template:
    metadata:
      labels:
        app: silat-agent-executor
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/metrics"
    spec:
      containers:
      - name: agent-executor
        image: your-registry/silat-agent-executor:1.0.0
        ports:
        - containerPort: 8080
          name: http
        - containerPort: 9090
          name: grpc
        env:
        - name: ENVIRONMENT
          value: "production"
        envFrom:
        - secretRef:
            name: silat-agent-secrets
        - configMapRef:
            name: silat-agent-config
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /health/live
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health/ready
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5

---
# Service
apiVersion: v1
kind: Service
metadata:
  name: silat-agent-executor
  namespace: silat
spec:
  selector:
    app: silat-agent-executor
  ports:
  - name: http
    port: 80
    targetPort: 8080
  - name: grpc
    port: 9090
    targetPort: 9090
  type: ClusterIP

---
# Horizontal Pod Autoscaler
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: silat-agent-executor-hpa
  namespace: silat
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: silat-agent-executor
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80

---
# Network Policy
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: silat-agent-executor-netpol
  namespace: silat
spec:
  podSelector:
    matchLabels:
      app: silat-agent-executor
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: silat-workflow-engine
    ports:
    - protocol: TCP
      port: 8080
    - protocol: TCP
      port: 9090
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - podSelector:
        matchLabels:
          app: redis
    ports:
    - protocol: TCP
      port: 6379
```

### 5. Monitoring Dashboards

```yaml
# Grafana Dashboard JSON
{
  "dashboard": {
    "title": "Silat Agent Executor",
    "panels": [
      {
        "title": "Agent Executions per Second",
        "targets": [{
          "expr": "rate(agent_executions_total[5m])"
        }]
      },
      {
        "title": "Success Rate",
        "targets": [{
          "expr": "rate(agent_executions_success[5m]) / rate(agent_executions_total[5m]) * 100"
        }]
      },
      {
        "title": "Average Execution Duration",
        "targets": [{
          "expr": "rate(agent_execution_duration_sum[5m]) / rate(agent_execution_duration_count[5m])"
        }]
      },
      {
        "title": "Token Usage per Minute",
        "targets": [{
          "expr": "rate(agent_tokens_used_total[1m]) * 60"
        }]
      },
      {
        "title": "Active Executions",
        "targets": [{
          "expr": "agent_executions_active"
        }]
      },
      {
        "title": "Circuit Breaker Status",
        "targets": [{
          "expr": "fault_tolerance_circuitbreaker_opened_total"
        }]
      }
    ]
  }
}
```

### 6. Production Checklist

#### Pre-Deployment
- [ ] Database migrations tested
- [ ] API keys and secrets configured
- [ ] Rate limits configured per tenant
- [ ] Circuit breaker thresholds tuned
- [ ] Resource limits set (CPU, memory)
- [ ] Backup strategy implemented
- [ ] Monitoring dashboards created
- [ ] Alerts configured
- [ ] Load testing completed
- [ ] Security audit passed

#### Post-Deployment
- [ ] Health checks passing
- [ ] Metrics being collected
- [ ] Traces visible in APM
- [ ] Logs structured and searchable
- [ ] Rate limiting working
- [ ] Circuit breakers functional
- [ ] Database connections pooled
- [ ] Cache hit ratio acceptable
- [ ] API response times < 2s (p95)
- [ ] Error rate < 1%

### 7. Performance Tuning

```properties
# High-throughput configuration
quarkus.datasource.jdbc.max-size=100
quarkus.thread-pool.max-threads=200
quarkus.vertx.event-loops-pool-size=8
silat.executor.max-concurrent-tasks=50

# Low-latency configuration
quarkus.datasource.jdbc.max-size=20
quarkus.thread-pool.max-threads=50
quarkus.vertx.event-loops-pool-size=4
silat.executor.max-concurrent-tasks=10
silat.agent.memory.cache-size=5000
```

### 8. Troubleshooting

```bash
# Check logs
kubectl logs -f deployment/silat-agent-executor -n silat

# Check metrics
curl http://localhost:8080/metrics

# Check health
curl http://localhost:8080/health

# Database connections
kubectl exec -it postgres-pod -n silat -- psql -U silat -c "SELECT count(*) FROM pg_stat_activity;"

# Redis cache stats
kubectl exec -it redis-pod -n silat -- redis-cli INFO stats

# Circuit breaker status
curl http://localhost:8080/q/metrics | grep circuitbreaker
```

## 🎯 Production Best Practices

1. **Always use connection pooling**
2. **Enable circuit breakers for external services**
3. **Implement rate limiting per tenant**
4. **Use structured logging (JSON)**
5. **Monitor token usage and costs**
6. **Set appropriate timeouts**
7. **Implement graceful shutdown**
8. **Use health checks**
9. **Enable distributed tracing**
10. **Regular security audits**

## 📚 Additional Resources

- [Quarkus Documentation](https://quarkus.io/guides/)
- [Microprofile Fault Tolerance](https://microprofile.io/project/eclipse/microprofile-fault-tolerance)
- [OpenTelemetry](https://opentelemetry.io/)
- [Prometheus Best Practices](https://prometheus.io/docs/practices/)

---

**You're now ready for production deployment! 🚀**