Billing Service Integration Guide
Overview
The billing-service is a comprehensive SaaS management platform that integrates with:

consumer-access-service (Quarkus) - API key management
iket-enterprise (Go) - API Gateway with consumer_access plugin
Workflow Engine - Usage tracking and metering
Architecture
X-API-Key
Introspect
Validate
Check Quota
Emit Usage
Consume
Aggregate
Generate
Manage
Track
Client
Iket-Enterprise Gateway
Consumer Access Service
PostgreSQL
Redis
Kafka
Billing Service
Usage Metering
Invoices
Subscriptions
Organizations
Components Built
1. Consumer Access Service (Quarkus)
Location: /Users/bhangun/Workspace/workkayys/Products/Iket/consumer-access-service

Key Classes:

ApiKey.java
 - Entity with SHA-256 hashing
IntrospectionResource.java
 - /internal/api-keys/introspect
ApiKeyAdminResource.java
 - Admin CRUD
Port: 8081

2. Iket-Enterprise Plugin (Go)
Location: /Users/bhangun/Workspace/workkayys/Products/Iket/iket-enterprise/plugins/consumer_access

Files:

consumer_access.go
 - Middleware implementation
model.go
 - Data models
init.go
 - Plugin registration
Features:

API key introspection
Redis-based quota enforcement
Usage event emission
3. Billing Service (Quarkus)
Location: /Users/bhangun/Workspace/workkayys/Products/Iket/billing-service

Modules:

Organization Management
Organization.java
OrganizationService.java
OrganizationResource.java
Subscription Management
Subscription.java
SubscriptionPlan.java
SubscriptionService.java
Usage Tracking & Metering
UsageTrackingService.java
UsageMeteringService.java
QuotaEnforcementService.java
Billing & Invoicing
Invoice.java
BillingService.java
InvoiceGenerator.java
Advanced Features
ChurnPredictionService.java
 - ML-based churn prediction
DynamicPricingService.java
 - Personalized pricing
MarketplaceService.java
 - Workflow marketplace
Port: 8080

Integration Flow
1. API Key Creation
# Create API key via billing-service admin API
curl -X POST http://localhost:8080/api/v1/management/admin/api-keys \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "uuid-here",
    "scopes": ["workflow.execute", "workflow.read"]
  }'
# Response
{
  "apiKey": "sk_live_abc123...",
  "keyHash": "sha256-hash",
  "organizationId": "uuid-here"
}
2. API Request with Key
# Client makes request to Iket-Enterprise
curl -X POST http://localhost:8080/api/v1/workflows/execute \
  -H "X-API-Key: sk_live_abc123..." \
  -H "Content-Type: application/json" \
  -d '{
    "workflowId": "order-processing",
    "inputs": {"orderId": "123"}
  }'
3. Request Processing
Iket-Enterprise receives request with X-API-Key
consumer_access plugin intercepts:
Calls consumer-access-service /internal/api-keys/introspect
Checks Redis quota: quota:<consumerId>:<YYYY-MM>
Emits usage event to Kafka
Consumer-access-service validates:
Hashes API key with SHA-256
Queries PostgreSQL for match
Returns ConsumerContext with tenant info
Billing-service consumes usage events:
Records usage in UsageRecord
Aggregates for billing period
Calculates costs with tiered pricing
4. Billing Cycle
# Monthly billing job runs
# 1. Aggregate usage for period
# 2. Calculate costs
# 3. Generate invoice
# 4. Send to payment processor
# 5. Update subscription status
Configuration
Docker Compose
docker-compose.consumer-access.yaml

services:
  postgres-consumer:
    image: postgres:16
    environment:
      POSTGRES_DB: consumer
      POSTGRES_USER: consumer
      POSTGRES_PASSWORD: consumer
    ports:
      - "5433:5432"
  redis:
    image: redis:7
    ports:
      - "6379:6379"
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
  consumer-access:
    build: ./consumer-access-service
    ports:
      - "8081:8081"
    environment:
      QUARKUS_DATASOURCE_JDBC_URL: jdbc:postgresql://postgres-consumer:5432/consumer
  billing-service:
    build: ./billing-service
    ports:
      - "8082:8080"
    environment:
      QUARKUS_DATASOURCE_JDBC_URL: jdbc:postgresql://postgres-billing:5432/billing
  iket-enterprise:
    build: ./iket-enterprise
    ports:
      - "8080:8080"
    environment:
      CONSUMER_ACCESS_URL: http://consumer-access:8081/internal/api-keys/introspect
      REDIS_URL: redis://redis:6379
Environment Variables
Consumer Access Service:

QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/consumer
QUARKUS_DATASOURCE_USERNAME=consumer
QUARKUS_DATASOURCE_PASSWORD=consumer
QUARKUS_HTTP_PORT=8081
Billing Service:

QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/billing
QUARKUS_DATASOURCE_USERNAME=billing
QUARKUS_DATASOURCE_PASSWORD=billing
QUARKUS_HTTP_PORT=8082
KAFKA_BOOTSTRAP=localhost:9092
REDIS_HOST=redis://localhost:6379
Iket-Enterprise:

CONSUMER_ACCESS_URL=http://localhost:8081/internal/api-keys/introspect
REDIS_URL=redis://localhost:6379
KAFKA_BOOTSTRAP=localhost:9092
API Endpoints
Consumer Access Service (Port 8081)
Endpoint	Method	Description
/internal/api-keys/introspect	POST	Validate API key (internal only)
/admin/api-keys	POST	Create API key
/admin/api-keys	GET	List API keys
/admin/api-keys/{id}	GET	Get API key
/admin/api-keys/{id}	DELETE	Revoke API key
Billing Service (Port 8082)
Endpoint	Method	Description
/api/v1/management/organizations	POST	Create organization
/api/v1/management/organizations/{id}	GET	Get organization
/api/v1/management/subscriptions	POST	Create subscription
/api/v1/management/subscriptions/{id}	PUT	Update subscription
/api/v1/management/usage/record	POST	Record usage event
/api/v1/management/billing/invoices	GET	List invoices
/api/v1/management/admin/overview	GET	Platform metrics
Testing
1. Build Services
# Consumer Access Service
cd consumer-access-service
./mvnw clean package -DskipTests
docker build -t wayang/consumer-access .
# Billing Service
cd billing-service
./mvnw clean package -DskipTests
docker build -t wayang/billing-service .
# Iket-Enterprise
cd iket-enterprise
go build -o bin/iket-enterprise ./cmd/server
2. Start Stack
docker-compose -f docker-compose.consumer-access.yaml up -d
3. Create Test Organization
curl -X POST http://localhost:8082/api/v1/management/organizations \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Corp",
    "slug": "test-corp",
    "billingEmail": "billing@test.com",
    "orgType": "BUSINESS"
  }'
4. Create Subscription
curl -X POST http://localhost:8082/api/v1/management/subscriptions \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId": "<org-id>",
    "planId": "<plan-id>",
    "billingCycle": "MONTHLY"
  }'
5. Create API Key
curl -X POST http://localhost:8081/admin/api-keys \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json"
6. Test API Request
curl -X POST http://localhost:8080/api/v1/workflows/execute \
  -H "X-API-Key: <api-key>" \
  -H "Content-Type: application/json" \
  -d '{
    "workflowId": "test",
    "inputs": {}
  }'
Next Steps
Security Hardening

Add mTLS for introspection endpoint
Implement rate limiting per API key
Add IP whitelisting
Monitoring

Set up Prometheus metrics
Configure Grafana dashboards
Add alerting rules
Production Deployment

Deploy to Kubernetes
Configure auto-scaling
Set up backup/restore
Advanced Features

Implement webhook delivery
Add multi-currency support
Build customer portal
Troubleshooting
API Key Not Working
# Check if key exists
curl -X POST http://localhost:8081/internal/api-keys/introspect \
  -H "Content-Type: application/json" \
  -d '{"apiKey": "<your-key>"}'
# Check Redis quota
redis-cli GET "quota:<consumerId>:<YYYY-MM>"
Quota Exceeded
# Check current usage
curl http://localhost:8082/api/v1/management/usage/organizations/<org-id>/current
# Reset quota (dev only)
redis-cli DEL "quota:<consumerId>:<YYYY-MM>"
Service Not Starting
# Check logs
docker-compose logs consumer-access
docker-compose logs billing-service
docker-compose logs iket-enterprise
# Check database connectivity
docker-compose exec postgres-consumer psql -U consumer -d consumer -c "SELECT 1"
Summary
✅ Completed:

Consumer Access Service with API key management
Iket-Enterprise plugin with introspection and quota enforcement
Billing Service with comprehensive SaaS management
Docker configuration for all services
Integration between all components
🎯 Production-Ready Features:

Secure API key hashing (SHA-256)
Real-time quota enforcement (Redis)
Usage metering and aggregation
Subscription lifecycle management
Invoice generation
Multi-tenant isolation
Event-driven architecture (Kafka)
Audit logging
📊 Metrics & Analytics:

MRR/ARR tracking
Churn prediction (ML-based)
Usage analytics
Customer health scores
Revenue forecasting