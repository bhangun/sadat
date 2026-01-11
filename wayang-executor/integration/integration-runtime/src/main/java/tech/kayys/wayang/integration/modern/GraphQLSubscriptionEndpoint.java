package tech.kayys.silat.executor.camel.modern;

import io.smallrye.mutiny.Multi;
import jakarta.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@ClientEndpoint
class GraphQLSubscriptionEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(GraphQLSubscriptionEndpoint.class);

    private final Multi.Emitter<GraphQLResponse> emitter;
    private final String subscription;
    private final Map<String, Object> variables;

    public GraphQLSubscriptionEndpoint(
            Multi.Emitter<GraphQLResponse> emitter,
            String subscription,
            Map<String, Object> variables) {
        this.emitter = emitter;
        this.subscription = subscription;
        this.variables = variables;
    }

    @OnOpen
    public void onOpen(Session session) {
        LOG.info("GraphQL subscription connected");

        // Send subscription request
        Map<String, Object> message = Map.of(
            "type", "start",
            "payload", Map.of(
                "query", subscription,
                "variables", variables
            )
        );

        try {
            session.getBasicRemote().sendText(
                new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(message)
            );
        } catch (Exception e) {
            LOG.error("Error sending subscription", e);
            emitter.fail(e);
        }
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            Map<String, Object> data = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(message, Map.class);

            String type = (String) data.get("type");

            if ("data".equals(type)) {
                Map<String, Object> payload = (Map<String, Object>) data.get("payload");

                GraphQLResponse response = new GraphQLResponse(
                    (Map<String, Object>) payload.get("data"),
                    (List<Map<String, Object>>) payload.get("errors"),
                    payload.containsKey("errors"),
                    Instant.now()
                );

                emitter.emit(response);
            }

        } catch (Exception e) {
            LOG.error("Error processing subscription message", e);
            emitter.fail(e);
        }
    }

    @OnClose
    public void onClose() {
        LOG.info("GraphQL subscription closed");
        emitter.complete();
    }

    @OnError
    public void onError(Throwable error) {
        LOG.error("GraphQL subscription error", error);
        emitter.fail(error);
    }
}