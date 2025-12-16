


import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import tech.kayys.wayang.integration.config.EnrichmentConfig;
import tech.kayys.wayang.integration.strategies.EnrichmentStrategy;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Content Enricher Node - Enriches message with data from external sources
 */
@Slf4j
@ApplicationScoped
@RegisterForReflection
public class ContentEnricherNode extends AbstractIntegrationNode {
    
    @Inject
    EnrichmentStrategy enrichmentStrategy;
    
    private EnrichmentConfig enrichmentConfig;
    
    @Override
    protected void onLoad() {
        super.onLoad();
        this.enrichmentConfig = config.getConfigAs(EnricherNodeConfig.class).getEnrichment();
    }
    
    @Override
    protected void configureRoute(RouteBuilder builder) throws Exception {
        builder.from(getFromEndpoint())
            .routeId(getRouteId())
            .enrich(enrichmentConfig.getResourceUri(), enrichmentStrategy)
            .to("direct:enriched-output");
        
        // If caching is enabled, add a cache wrapper
        if (Boolean.TRUE.equals(enrichmentConfig.getCacheEnabled())) {
            configureCaching(builder);
        }
    }
    
    private void configureCaching(RouteBuilder builder) {
        // TODO: Implement caching using Camel cache component
        // or integrate with external cache like Redis
    }
}