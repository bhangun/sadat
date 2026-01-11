package tech.kayys.silat.executor.camel.ai;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Computer vision model integration
 */
@ApplicationScoped
public class ComputerVisionService {

    private static final Logger LOG = LoggerFactory.getLogger(ComputerVisionService.class);

    @Inject
    CamelContext camelContext;

    /**
     * Image classification
     */
    public Uni<ImageClassificationResult> classifyImage(
            byte[] imageData,
            String modelEndpoint,
            String tenantId) {

        return Uni.createFrom().completionStage(() -> {
            java.util.concurrent.CompletableFuture<ImageClassificationResult> future = new java.util.concurrent.CompletableFuture<>();

            try {
                String routeId = "image-classification-" + java.util.UUID.randomUUID();

                camelContext.addRoutes(new org.apache.camel.builder.RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader(org.apache.camel.Exchange.HTTP_METHOD, constant("POST"))
                            .setHeader(org.apache.camel.Exchange.CONTENT_TYPE, constant("application/octet-stream"))

                            .toD(modelEndpoint)

                            .unmarshal().json()
                            .process(exchange -> {
                                Map<String, Object> response = exchange.getIn().getBody(Map.class);

                                ImageClassificationResult result = new ImageClassificationResult(
                                    (String) response.get("label"),
                                    (double) response.get("confidence"),
                                    (List<Map<String, Object>>) response.get("top_predictions"),
                                    Instant.now()
                                );

                                future.complete(result);
                            });
                    }
                });

                camelContext.getRouteController().startRoute(routeId);
                camelContext.createProducerTemplate().sendBody("direct:" + routeId, imageData);

            } catch (Exception e) {
                future.completeExceptionally(e);
            }

            return future;
        });
    }

    /**
     * Object detection
     */
    public Uni<ObjectDetectionResult> detectObjects(
            byte[] imageData,
            String modelEndpoint,
            String tenantId) {

        return Uni.createFrom().completionStage(() -> {
            java.util.concurrent.CompletableFuture<ObjectDetectionResult> future = new java.util.concurrent.CompletableFuture<>();

            try {
                String routeId = "object-detection-" + java.util.UUID.randomUUID();

                camelContext.addRoutes(new org.apache.camel.builder.RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader(org.apache.camel.Exchange.HTTP_METHOD, constant("POST"))

                            .toD(modelEndpoint)

                            .unmarshal().json()
                            .process(exchange -> {
                                Map<String, Object> response = exchange.getIn().getBody(Map.class);

                                ObjectDetectionResult result = new ObjectDetectionResult(
                                    (List<Map<String, Object>>) response.get("detections"),
                                    (int) response.get("object_count"),
                                    Instant.now()
                                );

                                future.complete(result);
                            });
                    }
                });

                camelContext.getRouteController().startRoute(routeId);
                camelContext.createProducerTemplate().sendBody("direct:" + routeId, imageData);

            } catch (Exception e) {
                future.completeExceptionally(e);
            }

            return future;
        });
    }
}