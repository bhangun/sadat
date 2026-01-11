FROM registry.access.redhat.com/ubi8/openjdk-17:1.16

ENV LANGUAGE='en_US:en'

# Set application info
LABEL name="{model.title}" \
version="{model.version}" \
description="{model.description}" \
maintainer="Generated MCP Server"

# Copy the application
COPY --chown=185 target/quarkus-app/lib/ /deployments/lib/
COPY --chown=185 target/quarkus-app/*.jar /deployments/
COPY --chown=185 target/quarkus-app/app/ /deployments/app/
COPY --chown=185 target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 185

# Environment configuration
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0
-Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
CMD curl -f http://localhost:8080/health || exit 1

# Default configuration
ENV API_BASE_URL="{model.baseUrl}"
{#if model.includeAuth}
ENV API_TOKEN=""
ENV API_KEY=""
{/if}