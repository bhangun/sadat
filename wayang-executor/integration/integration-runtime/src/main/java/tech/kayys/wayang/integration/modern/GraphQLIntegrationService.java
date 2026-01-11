package tech.kayys.silat.executor.camel.modern;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Comprehensive GraphQL API integration
 */
@ApplicationScoped
public class GraphQLIntegrationService {

    private static final Logger LOG = LoggerFactory.getLogger(GraphQLIntegrationService.class);

    @Inject
    CamelContext camelContext;

    @Inject
    ProducerTemplate producerTemplate;

    /**
     * Execute GraphQL query
     */
    public Uni<GraphQLResponse> executeQuery(
            String query,
            Map<String, Object> variables,
            GraphQLConfig config,
            String tenantId) {

        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<GraphQLResponse> future = new CompletableFuture<>();

            try {
                String routeId = "graphql-query-" + UUID.randomUUID();

                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))
                            .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))

                            // Add authentication if configured
                            .choice()
                                .when(simple("${exchangeProperty.authToken} != null"))
                                    .setHeader("Authorization",
                                        simple("Bearer ${exchangeProperty.authToken}"))
                            .end()

                            // Build GraphQL request
                            .process(exchange -> {
                                Map<String, Object> request = Map.of(
                                    "query", query,
                                    "variables", variables
                                );
                                exchange.getIn().setBody(request);
                            })

                            // Execute query
                            .marshal().json()
                            .toD(config.endpoint())

                            // Parse response
                            .unmarshal().json()
                            .process(exchange -> {
                                Map<String, Object> response = exchange.getIn().getBody(Map.class);

                                GraphQLResponse graphQLResponse = new GraphQLResponse(
                                    (Map<String, Object>) response.get("data"),
                                    (List<Map<String, Object>>) response.get("errors"),
                                    response.containsKey("errors"),
                                    Instant.now()
                                );

                                future.complete(graphQLResponse);
                            })

                            // Error handling
                            .onException(Exception.class)
                                .handled(true)
                                .process(exchange -> {
                                    Exception cause = exchange.getProperty(
                                        Exchange.EXCEPTION_CAUGHT, Exception.class
                                    );

                                    GraphQLResponse errorResponse = new GraphQLResponse(
                                        null,
                                        List.of(Map.of(
                                            "message", cause.getMessage(),
                                            "extensions", Map.of("code", "INTERNAL_ERROR")
                                        )),
                                        Instant.now()
                                    );

                                    future.complete(errorResponse);
                                })
                            .end();
                    }
                });

                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBodyAndProperty(
                    "direct:" + routeId,
                    null,
                    "authToken",
                    config.authToken()
                );

            } catch (Exception e) {
                LOG.error("GraphQL query failed", e);
                future.completeExceptionally(e);
            }

            return future;
        });
    }

    /**
     * Execute GraphQL mutation
     */
    public Uni<GraphQLResponse> executeMutation(
            String mutation,
            Map<String, Object> variables,
            GraphQLConfig config,
            String tenantId) {

        return executeQuery(mutation, variables, config, tenantId);
    }

    /**
     * Subscribe to GraphQL subscription (WebSocket-based)
     */
    public Multi<GraphQLResponse> subscribe(
            String subscription,
            Map<String, Object> variables,
            GraphQLConfig config,
            String tenantId) {

        return Multi.createFrom().emitter(emitter -> {
            try {
                // Create WebSocket connection for GraphQL subscription
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();

                Session session = container.connectToServer(
                    new GraphQLSubscriptionEndpoint(emitter, subscription, variables),
                    URI.create(config.websocketEndpoint())
                );

                // Keep connection alive
                emitter.onTermination(() -> {
                    try {
                        session.close();
                    } catch (Exception e) {
                        LOG.error("Error closing GraphQL subscription", e);
                    }
                });

            } catch (Exception e) {
                LOG.error("GraphQL subscription failed", e);
                emitter.fail(e);
            }
        });
    }
}