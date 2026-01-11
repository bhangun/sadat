package tech.kayys.silat.executor.camel.modern;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * gRPC service integration
 */
@ApplicationScoped
public class GRPCIntegrationService {

    private static final Logger LOG = LoggerFactory.getLogger(GRPCIntegrationService.class);

    @Inject
    CamelContext camelContext;

    /**
     * Call unary gRPC method
     */
    public Uni<GRPCResponse> callUnary(
            String serviceName,
            String methodName,
            Map<String, Object> request,
            GRPCConfig config,
            String tenantId) {

        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<GRPCResponse> future = new CompletableFuture<>();

            try {
                String routeId = "grpc-unary-" + UUID.randomUUID();

                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))

                            // Use Camel gRPC component
                            .toD("grpc://" + config.host() + ":" + config.port() +
                                 "/" + serviceName + "/" + methodName +
                                 "?method=" + methodName +
                                 "&synchronous=true" +
                                 "&usePlainText=" + !config.useTLS())

                            .process(exchange -> {
                                Object response = exchange.getIn().getBody();

                                GRPCResponse grpcResponse = new GRPCResponse(
                                    serviceName,
                                    methodName,
                                    response,
                                    Map.of(),
                                    Instant.now()
                                );

                                future.complete(grpcResponse);
                            });
                    }
                });

                camelContext.getRouteController().startRoute(routeId);
                camelContext.createProducerTemplate().sendBody("direct:" + routeId, request);

            } catch (Exception e) {
                LOG.error("gRPC call failed", e);
                future.completeExceptionally(e);
            }

            return future;
        });
    }

    /**
     * Call server-streaming gRPC method
     */
    public Multi<GRPCResponse> callServerStreaming(
            String serviceName,
            String methodName,
            Map<String, Object> request,
            GRPCConfig config,
            String tenantId) {

        return Multi.createFrom().emitter(emitter -> {
            try {
                String routeId = "grpc-stream-" + UUID.randomUUID();

                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)

                            .toD("grpc://" + config.host() + ":" + config.port() +
                                 "/" + serviceName + "/" + methodName +
                                 "?method=" + methodName +
                                 "&streamReplies=true")

                            .split(body())
                                .process(exchange -> {
                                    Object response = exchange.getIn().getBody();

                                    GRPCResponse grpcResponse = new GRPCResponse(
                                        serviceName,
                                        methodName,
                                        response,
                                        Map.of(),
                                        Instant.now()
                                    );

                                    emitter.emit(grpcResponse);
                                })
                            .end()

                            .process(exchange -> emitter.complete());
                    }
                });

                camelContext.getRouteController().startRoute(routeId);
                camelContext.createProducerTemplate().sendBody("direct:" + routeId, request);

            } catch (Exception e) {
                LOG.error("gRPC streaming call failed", e);
                emitter.fail(e);
            }
        });
    }
}