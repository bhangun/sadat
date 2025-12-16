
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import tech.kayys.wayang.integration.config.ConnectorConfig;

import javax.enterprise.context.ApplicationScoped;

/**
 * Generic Connector Node - Wraps any Camel component
 */
@Slf4j
@ApplicationScoped
@RegisterForReflection
public class ConnectorNode extends AbstractIntegrationNode {
    
    private ConnectorConfig connectorConfig;
    
    @Override
    protected void onLoad() {
        super.onLoad();
        this.connectorConfig = config.getConfigAs(ConnectorNodeConfig.class).getConnector();
    }
    
    @Override
    protected void configureRoute(RouteBuilder builder) throws Exception {
        String uri = connectorConfig.getUri();
        String direction = connectorConfig.getDirection();
        
        if ("from".equals(direction)) {
            // Consumer - receives messages
            builder.from(uri)
                .routeId(getRouteId())
                .to("direct:connector-output");
                
        } else if ("to".equals(direction)) {
            // Producer - sends messages
            builder.from(getFromEndpoint())
                .routeId(getRouteId())
                .to(uri)
                .to("direct:connector-output");
                
        } else if ("both".equals(direction)) {
            // Bidirectional
            builder.from(uri)
                .routeId(getRouteId() + "-consumer")
                .to("direct:connector-processing");
            
            builder.from("direct:connector-processing")
                .routeId(getRouteId() + "-processor")
                .to(uri)
                .to("direct:connector-output");
        }
    }
}