package tech.kayys.wayang.client;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * PluginManagerClient - Client for Plugin Manager service
 */
@RegisterRestClient(configKey = "plugin-manager")
@Path("/api/v1/plugins")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PluginManagerClient {

        /**
         * List available plugins
         */
        @GET
        @Retry(maxRetries = 2)
        @Timeout(value = 5000)
        Uni<List<PluginDescriptor>> listPlugins();

        /**
         * Get plugin details
         */
        @GET
        @Path("/{pluginId}")
        @Timeout(value = 5000)
        Uni<PluginDescriptor> getPlugin(@PathParam("pluginId") String pluginId);

        /**
         * Check plugin compatibility
         */
        @POST
        @Path("/compatibility")
        @Timeout(value = 5000)
        Uni<CompatibilityResult> checkCompatibility(CompatibilityRequest request);

        // DTOs
        record PluginDescriptor(
                        String id,
                        String name,
                        String version,
                        String description,
                        PluginStatus status,
                        List<String> capabilities,
                        Map<String, Object> metadata) {
        }

        enum PluginStatus {
                ACTIVE, DEPRECATED, DISABLED
        }

        record CompatibilityRequest(
                        List<String> pluginIds,
                        String runtimeVersion) {
        }

        record CompatibilityResult(
                        boolean compatible,
                        List<String> incompatiblePlugins,
                        List<String> warnings) {
        }
}
