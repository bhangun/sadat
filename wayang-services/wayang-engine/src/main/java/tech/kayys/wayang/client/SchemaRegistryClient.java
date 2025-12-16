package tech.kayys.wayang.client;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.model.OutputChannel;
import tech.kayys.wayang.model.PortDescriptor;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import java.util.List;
import java.util.Map;

/**
 * SchemaRegistryClient - Client for Node Schema Registry service
 */
@RegisterRestClient(configKey = "schema-registry")
@Path("/api/v1/schemas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SchemaRegistryClient {

    /**
     * Get node schema by type
     */
    @GET
    @Path("/nodes/{nodeType}")
    @Retry(maxRetries = 3, delay = 500)
    @Timeout(value = 5000)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 10000)
    Uni<NodeSchema> getNodeSchema(@PathParam("nodeType") String nodeType);

    /**
     * List all available node schemas
     */
    @GET
    @Path("/nodes")
    @Retry(maxRetries = 2)
    @Timeout(value = 10000)
    Uni<List<NodeSchema>> listNodeSchemas();

    /**
     * Validate node configuration against schema
     */
    @POST
    @Path("/nodes/validate")
    @Timeout(value = 5000)
    Uni<SchemaValidationResult> validateNode(NodeValidationRequest request);

    // DTOs
    record NodeSchema(
            String id,
            String name,
            String version,
            String description,
            List<PortDescriptor> inputs,
            List<OutputChannel> outputs,
            List<PropertyDescriptor> properties,
            List<String> capabilities,
            Map<String, Object> metadata) {
    }

    record NodeValidationRequest(
            String nodeType,
            Map<String, Object> config) {
    }

    record SchemaValidationResult(
            boolean valid,
            List<ValidationError> errors) {
    }

    record ValidationError(
            String code,
            String message,
            String path) {
    }

    record PropertyDescriptor(
            String name,
            String type,
            Object defaultValue,
            boolean required,
            String description) {
    }
}
