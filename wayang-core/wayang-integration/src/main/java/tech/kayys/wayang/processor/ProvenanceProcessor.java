



import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import tech.kayys.wayang.audit.ProvenanceService;
import tech.kayys.wayang.nodes.NodeContext;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Processor for recording provenance information
 */
@Slf4j
@ApplicationScoped
@RegisterForReflection
public class ProvenanceProcessor implements Processor {
    
    @Inject
    ProvenanceService provenanceService;
    
    @Override
    public void process(Exchange exchange) throws Exception {
        NodeContext context = exchange.getProperty("nodeContext", NodeContext.class);
        
        if (context != null) {
            String eventType = exchange.getProperty("provenanceEvent", String.class);
            if (eventType != null) {
                provenanceService.record(context, eventType, exchange.getIn().getBody());
            }
        }
    }
}