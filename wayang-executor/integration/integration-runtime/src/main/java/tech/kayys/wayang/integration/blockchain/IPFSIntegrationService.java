package tech.kayys.silat.executor.camel.blockchain;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * IPFS (InterPlanetary File System) integration
 */
@ApplicationScoped
public class IPFSIntegrationService {

    private static final Logger LOG = LoggerFactory.getLogger(IPFSIntegrationService.class);

    @Inject
    CamelContext camelContext;

    /**
     * Upload file to IPFS
     */
    public Uni<IPFSUploadResult> uploadFile(
            byte[] fileData,
            String fileName,
            IPFSConfig config,
            String tenantId) {

        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<IPFSUploadResult> future = new CompletableFuture<>();

            try {
                String routeId = "ipfs-upload-" + UUID.randomUUID();

                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)

                            // Use Camel IPFS component
                            .toD("ipfs:add?" +
                                 "ipfsHost=" + config.host() +
                                 "&ipfsPort=" + config.port())

                            .process(exchange -> {
                                String cid = exchange.getIn().getBody(String.class);

                                IPFSUploadResult result = new IPFSUploadResult(
                                    cid,
                                    fileName,
                                    fileData.length,
                                    config.gatewayUrl() + "/ipfs/" + cid,
                                    Instant.now()
                                );

                                future.complete(result);
                            });
                    }
                });

                camelContext.getRouteController().startRoute(routeId);
                camelContext.createProducerTemplate().sendBody("direct:" + routeId, fileData);

            } catch (Exception e) {
                LOG.error("IPFS upload failed", e);
                future.completeExceptionally(e);
            }

            return future;
        });
    }

    /**
     * Download file from IPFS
     */
    public Uni<IPFSDownloadResult> downloadFile(
            String cid,
            IPFSConfig config,
            String tenantId) {

        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<IPFSDownloadResult> future = new CompletableFuture<>();

            try {
                String routeId = "ipfs-download-" + UUID.randomUUID();

                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)

                            .toD("ipfs:get?" +
                                 "cid=" + cid +
                                 "&ipfsHost=" + config.host() +
                                 "&ipfsPort=" + config.port())

                            .process(exchange -> {
                                byte[] fileData = exchange.getIn().getBody(byte[].class);

                                IPFSDownloadResult result = new IPFSDownloadResult(
                                    cid,
                                    fileData,
                                    fileData.length,
                                    Instant.now()
                                );

                                future.complete(result);
                            });
                    }
                });

                camelContext.getRouteController().startRoute(routeId);
                camelContext.createProducerTemplate().sendBody("direct:" + routeId, null);

            } catch (Exception e) {
                LOG.error("IPFS download failed", e);
                future.completeExceptionally(e);
            }

            return future;
        });
    }
}