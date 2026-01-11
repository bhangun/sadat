package tech.kayys.silat.ui;

import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UI Schema Registry
 */
@ApplicationScoped
@Startup
public class NodeUISchemaRegistry {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(NodeUISchemaRegistry.class);

    private final Map<String, NodeUISchema> schemas = new ConcurrentHashMap<>();

    @PostConstruct
    void initialize() {
        LOG.info("Initializing Node UI Schema Registry");

        // Register all built-in node types
        registerBuiltInSchemas();

        LOG.info("Registered {} node type schemas", schemas.size());
    }

    /**
     * Register a node schema
     */
    public void register(NodeUISchema schema) {
        schemas.put(schema.nodeType(), schema);
        LOG.info("Registered schema for node type: {}", schema.nodeType());
    }

    /**
     * Get schema by node type
     */
    public Optional<NodeUISchema> getSchema(String nodeType) {
        return Optional.ofNullable(schemas.get(nodeType));
    }

    /**
     * Get all schemas
     */
    public Map<String, NodeUISchema> getAllSchemas() {
        return new HashMap<>(schemas);
    }

    /**
     * Get schemas by category
     */
    public Map<String, NodeUISchema> getSchemasByCategory(String category) {
        return schemas.values().stream()
            .filter(schema -> schema.metadata().category().equals(category))
            .collect(java.util.stream.Collectors.toMap(
                NodeUISchema::nodeType,
                schema -> schema
            ));
    }

    /**
     * Search schemas
     */
    public List<NodeUISchema> searchSchemas(String query) {
        String lowerQuery = query.toLowerCase();
        return schemas.values().stream()
            .filter(schema ->
                schema.metadata().displayName().toLowerCase().contains(lowerQuery) ||
                schema.metadata().description().toLowerCase().contains(lowerQuery) ||
                schema.metadata().tags().stream()
                    .anyMatch(tag -> tag.toLowerCase().contains(lowerQuery))
            )
            .toList();
    }

    /**
     * Register all built-in node types
     */
    private void registerBuiltInSchemas() {
        // Core node types
        register(TaskNodeSchema.create());
        register(DecisionNodeSchema.create());
        register(ParallelNodeSchema.create());
        register(AggregateNodeSchema.create());
        register(SubWorkflowNodeSchema.create());

        // Human interaction
        register(HumanTaskNodeSchema.create());
        register(ApprovalNodeSchema.create());

        // Integration
        register(HttpRequestNodeSchema.create());
        register(RestAPINodeSchema.create());
        register(GraphQLNodeSchema.create());
        register(GrpcCallNodeSchema.create());

        // Data
        register(DatabaseQueryNodeSchema.create());
        register(DataTransformNodeSchema.create());
        register(JsonPathNodeSchema.create());

        // Messaging
        register(KafkaProducerNodeSchema.create());
        register(KafkaConsumerNodeSchema.create());
        register(RabbitMQNodeSchema.create());

        // Cloud
        register(AwsLambdaNodeSchema.create());
        register(S3OperationNodeSchema.create());
        register(GcpStorageNodeSchema.create());

        // AI/ML
        register(LLMCallNodeSchema.create());
        register(EmbeddingNodeSchema.create());
        register(VectorSearchNodeSchema.create());

        // Utilities
        register(TimerNodeSchema.create());
        register(EventWaitNodeSchema.create());
        register(ScriptNodeSchema.create());
        register(EmailNodeSchema.create());
    }
}