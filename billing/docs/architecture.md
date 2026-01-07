# 🚀 Silat Complete Platform - Production Implementation

## Overview

**Silat** is a comprehensive, enterprise-grade workflow orchestration platform combining:
- **Workflow Engine**: Execute complex workflows with AI agents, integrations, and automation
- **Control Plane**: Low-code visual workflow builder with AI orchestration
- **Management Platform**: Complete SaaS management, billing, and provisioning system

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Silat Platform Architecture                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │   Control Plane  │  │  Workflow Engine │  │  Management  │  │
│  │   (Low-Code UI)  │  │   (Execution)    │  │  (Billing)   │  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
│           │                     │                     │          │
│           └─────────────────────┴─────────────────────┘          │
│                                │                                  │
│  ┌─────────────────────────────┴──────────────────────────────┐ │
│  │                    Core Services Layer                       │ │
│  ├──────────────────────────────────────────────────────────────┤ │
│  │  • Multi-tenant Isolation  • API Gateway  • Service Discovery│ │
│  │  • Event Bus (Kafka)       • Caching (Redis)  • Monitoring  │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                │                                  │
│  ┌─────────────────────────────┴──────────────────────────────┐ │
│  │                     Data Layer                               │ │
│  ├──────────────────────────────────────────────────────────────┤ │
│  │  • PostgreSQL (Multi-tenant)  • Redis (Cache/Quota)         │ │
│  │  • S3 (Storage)               • Kafka (Events)              │ │
│  └──────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## 📦 Complete Components

### 1. **Workflow Engine** (Core Domain)
- **Reactive workflow execution** with Mutiny
- **Event sourcing** for auditability
- **Multi-communication strategies**: REST, gRPC, Kafka
- **Distributed locking** for concurrency control
- **Retry policies** with exponential backoff
- **Compensation/Saga pattern** support
- **Human-in-the-loop** tasks with signals

**Key Classes**:
- `WorkflowRunManager`: Orchestrates workflow lifecycle
- `WorkflowRun`: Aggregate root with business logic
- `WorkflowScheduler`: Task distribution to executors
- `ExecutorSDK`: Framework for building executors

### 2. **Control Plane** (Low-Code Platform)
- **Visual workflow designer** (canvas-based)
- **AI agent orchestration** with LLM integration
- **Enterprise Integration Patterns** (EIP) support
- **Template catalog** with pre-built workflows
- **Project management** for organizing workflows

**Key Features**:
- Canvas-to-workflow conversion
- AI agent lifecycle management (GPT-4, Claude, etc.)
- Integration pattern execution (router, splitter, aggregator)
- Real-time collaboration via WebSocket

### 3. **Management Platform** (SaaS Backend)

#### Organization Management
- Multi-tenant organization lifecycle
- Feature flags per tier
- Settings and preferences
- User management

#### Subscription & Billing
- **Flexible plans**: Free → Enterprise
- **Multiple billing cycles**: Monthly, Quarterly, Annual
- **Usage-based billing** with tiered pricing
- **Add-ons** for capacity expansion
- **Trial management**
- **Automated invoicing**
- **Dunning management** (failed payment retry)

#### Resource Provisioning
- **Automated onboarding**:
  - Database schema creation
  - Redis namespace allocation
  - Kafka topic provisioning
  - S3 bucket creation
  - Service discovery registration
- **Health monitoring** with scheduled checks
- **Auto-scaling** capabilities
- **Graceful deprovisioning**

#### Usage Tracking & Quotas
- **Real-time quota enforcement** (Redis-based)
- **High-throughput metering** with batching
- **Cost calculation** with tiered pricing
- **Usage aggregation** for billing
- **Alert thresholds** (80%, 90%, 100%)
- **Quota status** dashboards

#### Event System
- **Domain event publishing** via Kafka
- **Event-driven workflows**
- **Multi-channel notifications**:
  - Email (SendGrid/SES)
  - Slack webhooks
  - Custom webhooks
- **Real-time updates** via WebSocket

#### Admin Dashboard
- **Platform overview**: MRR, ARR, ARPU, churn
- **Revenue analytics** with forecasting
- **Usage metrics** across all tenants
- **Tenant health** monitoring
- **Audit logging** for compliance

## 🚀 Getting Started

### Prerequisites
```bash
- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 15+
- Redis 7+
- Kafka 3.5+
```

### Quick Start (Docker Compose)

```bash
# Clone repository
git clone https://github.com/kayys/silat.git
cd silat

# Start infrastructure
docker-compose up -d

# Wait for services to be healthy
docker-compose ps

# Build and run application
./mvnw clean package -Dquarkus.profile=dev
./mvnw quarkus:dev
```

Access:
- **Management API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui
- **gRPC**: localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9091

### Production Deployment (Kubernetes)

```bash
# Apply configurations
kubectl apply -f k8s-deployment.yml

# Check status
kubectl get pods -n silat-management

# Access service
kubectl port-forward svc/management-service 8080:80 -n silat-management
```

## 🔧 Configuration

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_USERNAME=silat
DB_PASSWORD=your_password
DB_NAME=silat_management

# Redis
REDIS_HOST=redis://localhost:6379
REDIS_PASSWORD=

# Kafka
KAFKA_BOOTSTRAP=localhost:9092

# Payment Gateway
PAYMENT_PROVIDER=stripe
STRIPE_API_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Email
EMAIL_PROVIDER=sendgrid
SENDGRID_API_KEY=SG...

# Features
OIDC_ENABLED=false
LOG_LEVEL=INFO
ENVIRONMENT=production
```

### Subscription Plans Configuration

```sql
-- Insert default plans
INSERT INTO mgmt_subscription_plans (
  plan_code, name, tier, monthly_price, annual_price, currency
) VALUES
  ('free', 'Free', 'FREE', 0, 0, 'USD'),
  ('starter', 'Starter', 'STARTER', 29, 290, 'USD'),
  ('professional', 'Professional', 'PROFESSIONAL', 99, 990, 'USD'),
  ('business', 'Business', 'BUSINESS', 299, 2990, 'USD'),
  ('enterprise', 'Enterprise', 'ENTERPRISE', 999, 9990, 'USD');
```

## 📊 API Examples

### Create Organization
```bash
curl -X POST http://localhost:8080/api/v1/management/organizations \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corp",
    "slug": "acme-corp",
    "billingEmail": "billing@acme.com",
    "orgType": "BUSINESS"
  }'
```

### Create Subscription
```bash
curl -X POST http://localhost:8080/api/v1/management/subscriptions \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "uuid-here",
    "planId": "uuid-here",
    "billingCycle": "MONTHLY",
    "startTrial": true
  }'
```

### Record Usage
```bash
curl -X POST http://localhost:8080/api/v1/management/usage/record \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "tenant_xyz",
    "usageType": "WORKFLOW_EXECUTION",
    "quantity": 1,
    "unit": "execution",
    "resourceId": "workflow-run-123"
  }'
```

### Execute Workflow
```bash
curl -X POST http://localhost:8080/api/v1/workflows/runs \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant_xyz" \
  -d '{
    "workflowDefinitionId": "order-processing",
    "inputs": {
      "orderId": "ORDER-123",
      "customerId": "CUST-456",
      "totalAmount": 99.99
    }
  }'
```

## 🔐 Security

### Multi-Tenancy Isolation
- **Database**: Tenant-specific schemas/tables with tenant_id filtering
- **Cache**: Prefixed keys per tenant
- **Kafka**: Tenant-specific topic prefixes
- **Storage**: Tenant-specific S3 buckets/prefixes
- **API**: Tenant validation on every request

### Authentication & Authorization
- **API Keys**: Per-tenant API keys with scopes
- **JWT**: OIDC integration ready
- **RBAC**: Role-based access control
- **Audit Logging**: All actions logged

### Data Protection
- **Encryption at rest**: Database and S3
- **Encryption in transit**: TLS/SSL
- **PII handling**: Configurable data retention
- **GDPR compliance**: Soft deletes, data export

## 📈 Monitoring & Observability

### Metrics (Prometheus)
- Request rates and latencies
- Workflow execution metrics
- Database connection pool stats
- Cache hit rates
- Quota usage metrics
- Business metrics (MRR, churn, etc.)

### Logging
- Structured JSON logging
- Trace ID propagation
- Log aggregation (ELK/Loki)
- Error tracking (Sentry)

### Health Checks
- Liveness: `/q/health/live`
- Readiness: `/q/health/ready`
- Database connectivity
- Redis connectivity
- Kafka connectivity

### Dashboards
- **Operations**: System health, performance
- **Business**: Revenue, usage, churn
- **Tenant**: Individual tenant metrics

## 🧪 Testing

```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw verify -Pit

# Load testing
./mvnw gatling:test
```

## 📚 Additional Features

### Advanced Capabilities
- **Rate Limiting**: Per-tenant API rate limits
- **Webhooks**: Custom webhook delivery
- **Data Export**: Tenant data export for compliance
- **Multi-Currency**: Support for multiple currencies
- **Tax Calculation**: Integrated tax calculation
- **Invoicing**: PDF invoice generation
- **Payment Methods**: Multiple payment methods per tenant
- **Credits**: Account credits and refunds
- **Discounts**: Coupon codes and volume discounts

### Upcoming Features
- **Self-Service Portal**: Customer dashboard
- **Advanced Analytics**: Predictive churn analysis
- **Workflow Marketplace**: Share and sell workflows
- **White-Label**: Custom branding
- **Reseller Program**: Partner management
- **Advanced Governance**: Policy enforcement

## 🤝 Contributing

Contributions welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md)

## 📄 License

Apache License 2.0 - see [LICENSE](LICENSE)

## 🆘 Support

- **Documentation**: https://docs.silat.io
- **Community**: https://community.silat.io
- **Issues**: https://github.com/kayys/silat/issues
- **Email**: support@silat.io

---

**Built with ❤️ using Quarkus, Mutiny, and modern cloud-native technologies**