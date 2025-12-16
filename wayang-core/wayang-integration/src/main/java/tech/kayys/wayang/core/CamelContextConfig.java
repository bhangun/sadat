


import io.quarkus.runtime.Startup;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.Registry;
import tech.kayys.wayang.integration.processors.AuditProcessor;
import tech.kayys.wayang.integration.processors.GuardrailProcessor;
import tech.kayys.wayang.integration.processors.ProvenanceProcessor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Camel Context configuration and customization
 */
@Slf4j
@ApplicationScoped
public class CamelContextConfig {
    
    @Inject
    CamelContext camelContext;
    
    @Inject
    AuditProcessor auditProcessor;
    
    @Inject
    GuardrailProcessor guardrailProcessor;
    
    @Inject
    ProvenanceProcessor provenanceProcessor;
    
    @Startup
    public void configure() {
        log.info("Configuring Camel Context for Wayang Integration");
        
        // Register custom processors in registry
        Registry registry = camelContext.getRegistry();
        registry.bind("auditProcessor", auditProcessor);
        registry.bind("guardrailProcessor", guardrailProcessor);
        registry.bind("provenanceProcessor", provenanceProcessor);
        
        // Configure context properties
        camelContext.setStreamCaching(true);
        camelContext.setTracing(true);
        camelContext.setMessageHistory(true);
        
        log.info("Camel Context configured successfully");
    }
}