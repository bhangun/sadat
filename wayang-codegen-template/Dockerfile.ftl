# Generated Dockerfile for: ${workflowName}
# Generated at: ${generationTimestamp}
# Estimated size: ${estimatedSizeMB} MB

FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL name="${workflowName}"
LABEL version="${version}"
LABEL description="Standalone agent for ${workflowName}"

# Create app directory
WORKDIR /app

# Copy the fat JAR
COPY target/${artifactId}.jar /app/agent.jar

# Create non-root user
RUN addgroup -S wayang && adduser -S wayang -G wayang
USER wayang

# Expose port if needed
<#if exposePort>
EXPOSE ${port}
</#if>

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD java -cp /app/agent.jar tech.kayys.generated.HealthCheck || exit 1

# Run the agent
ENTRYPOINT ["java", "-jar", "/app/agent.jar"]

# Default arguments
<#if defaultArgs?has_content>
CMD [<#list defaultArgs as arg>"${arg}"<#sep>, </#list>]
</#if>