# Usage Guide

## Configuration Profiles
The application uses Quarkus profiles to manage environments.

### 1. Development (`dev`)
- **Activation**: Default when running `./run-dev.sh` or `mvn quarkus:dev`.
- **Database**: Connects to **Docker Compose** services.
  - Postgres: `localhost:5433`
  - Redis: `localhost:6380`
- **Authentication**: Uses credentials `quarkus`/`quarkus`.

### 2. Testing (`test`)
- **Activation**: Automated during `mvn test`.
- **Database**: Uses **Quarkus Dev Services**.
  - Automatically spins up a temporary, isolated Docker container for the duration of the tests.
  - **No manual setup required.**

### 3. Production (`prod`)
- **Activation**: When running the built JAR.
- **Database**: Expects environment variables:
  - `DATABASE_URL` (JDBC)
  - `DATABASE_REACTIVE_URL` (Reactive)
  - `REDIS_HOST`

## Database Management

We use **Hibernate Reactive** with PostgreSQL.

### Docker Compose
A `docker-compose.yml` is provided for local development and can be used as a reference for production.

**Commands:**
- Start: `docker-compose up -d`
- Stop: `docker-compose down`
- **Reset Data**: `docker-compose down -v` (Use this if you encounter authentication errors).

### Schema Migration
Schema is automatically managed in `dev` mode (`drop-and-create`). In production, use standard migration tools (Liquibase/Flyway) compatible with your deployment pipeline.

## Running Tests

Run the full test suite with Maven:
```bash
./mvnw test
```
*Note: Ensure Docker is running, as tests require it to spin up the database.*

## Troubleshooting

### "Password authentication failed"
This usually happens if the Docker volume contains old credentials.
**Fix**:
```bash
docker-compose down -v
docker-compose up -d
```

### "Connection refused" on start
Ensure Docker Compose is running (`docker ps`). The application depends on the database being available on ports `5433` and `6380`.
