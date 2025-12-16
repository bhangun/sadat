



import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import tech.kayys.wayang.audit.AuditService;
import tech.kayys.wayang.nodes.NodeContext;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Processor for auditing Camel exchanges
 */
@Slf4j
@ApplicationScoped
@RegisterForReflection
public class AuditProcessor implements Processor {
    
    @Inject
    AuditService auditService;
    
    @Override
    public void process(Exchange exchange) throws Exception {
        // Extract node context if available
        NodeContext context = exchange.getProperty("nodeContext", NodeContext.class);
        
        if (context != null) {
            // Audit the exchange
            log.debug("Auditing exchange {} for node {}", 
                exchange.getExchangeId(), context.getNodeId());
            
            // Record in audit service
            // (Actual audit recording is done in node execution)
        }
    }
}