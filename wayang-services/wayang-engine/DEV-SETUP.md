# Wayang Engine - Development Setup

## Prerequisites
- Docker and Docker Compose
- Java 21 (GraalVM recommended)
- Maven 3.9+

## Quick Start

### 1. Start Infrastructure Services
```bash
# Start all services (PostgreSQL, Redis, Kafka)
docker-compose up -d

# Check services are healthy
docker-compose ps

# View logs
docker-compose logs -f
```

### 2. Start Application
```bash
# Development mode with hot reload
./mvnw quarkus:dev

# Or compile and run tests
./mvnw clean test
```

### 3. Access Services

**Application:**
- GraphQL API: http://localhost:8080/graphql/
- GraphQL UI: http://localhost:8080/graphql-ui/
- Swagger UI: http://localhost:8080/swagger-ui/
- Health Check: http://localhost:8080/q/health

**Infrastructure:**
- PostgreSQL: localhost:5432
  - Database: `wayang_designer_dev`
  - User: `wayang`
  - Password: `dev123`
- Redis: localhost:6379
- Kafka: localhost:19092
- Kafka UI: http://localhost:8090
- Schema Registry: http://localhost:18081

## GraphQL Schema

The GraphQL schema has been fixed to handle complex types properly:

### Key Changes Made:
1. **Object/Map fields** are annotated with `@Ignore` to prevent schema generation issues
2. **Complex input types** (LogicDefinition, UIDefinition, RuntimeConfig) are passed as JSON strings
3. **ValidationResult** has a GraphQL-safe wrapper (`ValidationResultQL`)
4. **Hibernate ORM** configured with `mapping.format.global=ignore`

### Example GraphQL Mutations:

**Create Workflow:**
```graphql
mutation {
  createWorkflow(
    workspaceId: "uuid-here"
    input: {
      name: "My Workflow"
      description: "Test workflow"
      version: "1.0.0"
      logic: "{\"nodes\":[],\"connections\":[]}"
      ui: "{\"canvas\":{\"zoom\":1.0}}"
      runtime: "{\"mode\":\"SYNC\"}"
      metadata: "{\"author\":\"test\"}"
    }
  ) {
    id
    name
    status
  }
}
```

**Query Workflows:**
```graphql
query {
  workflows(workspaceId: "uuid-here") {
    id
    name
    version
    status
    createdAt
  }
}
```

## Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

## Troubleshooting

### Application won't start
1. Ensure all Docker services are healthy: `docker-compose ps`
2. Check PostgreSQL is accessible: `docker-compose logs postgres`
3. Check Kafka is ready: `docker-compose logs kafka`

### GraphQL Schema Errors
The "Object must define one or more fields" error has been resolved by:
- Adding `@Ignore` annotations to Map/Object fields
- Converting complex inputs to JSON strings
- Creating GraphQL-safe wrapper types

### Tests Failing
Run tests with: `mvn test`
- Test configuration disables reactive messaging
- Hibernate ORM format mapping is set to `ignore`

## Development Notes

- **Hot Reload**: Quarkus dev mode supports hot reload for most changes
- **Database**: Schema is auto-created in dev mode (`generation: drop-and-create`)
- **Messaging**: Kafka topics are auto-created when needed
- **GraphQL**: Schema is validated on startup

## Next Steps

1. Start the infrastructure: `docker-compose up -d`
2. Wait for services to be healthy (~30 seconds)
3. Start the application: `./mvnw quarkus:dev`
4. Access GraphQL UI: http://localhost:8080/graphql-ui/
