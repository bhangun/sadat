#!/bin/bash

echo "🚀 Starting Wayang Engine Development Environment"
echo "=================================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

echo "✅ Docker is running"

# Start infrastructure services
echo ""
echo "📦 Starting infrastructure services (PostgreSQL, Redis, Kafka)..."
docker-compose up -d

# Wait for services to be healthy
echo ""
echo "⏳ Waiting for services to be healthy..."
sleep 10

# Check service health
echo ""
echo "🔍 Checking service health..."
docker-compose ps

# Start Quarkus dev mode
echo ""
echo "🎯 Starting Quarkus dev mode..."
echo "   This may take 1-2 minutes on first run..."
echo ""
echo "   Once started, access:"
echo "   - GraphQL UI: http://localhost:8080/graphql-ui/"
echo "   - Swagger UI: http://localhost:8080/swagger-ui/"
echo "   - Health Check: http://localhost:8080/q/health"
echo "   - Kafka UI: http://localhost:8090"
echo ""
echo "   Press Ctrl+C to stop"
echo ""

./mvnw quarkus:dev
