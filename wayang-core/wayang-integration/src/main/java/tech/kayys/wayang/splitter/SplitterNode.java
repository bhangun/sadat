


import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.SplitDefinition;
import tech.kayys.wayang.integration.config.SplitterConfig;

import javax.enterprise.context.ApplicationScoped;

/**
 * Splitter Node - Splits messages into multiple parts
 */
@Slf4j
@ApplicationScoped
@RegisterForReflection
public class SplitterNode extends AbstractIntegrationNode {
    
    private SplitterConfig splitterConfig;
    
    @Override
    protected void onLoad() {
        super.onLoad();
        this.splitterConfig = config.getConfigAs(SplitterNodeConfig.class).getSplitter();
    }
    
    @Override
    protected void configureRoute(RouteBuilder builder) throws Exception {
        SplitDefinition split = builder.from(getFromEndpoint())
            .routeId(getRouteId())
            .split(builder.jsonpath(splitterConfig.getExpression()));
        
        // Configure splitting behavior
        if (Boolean.TRUE.equals(splitterConfig.getStreaming())) {
            split.streaming();
        }
        
        if (Boolean.TRUE.equals(splitterConfig.getParallel())) {
            split.parallelProcessing();
        }
        
        if (Boolean.TRUE.equals(splitterConfig.getStopOnException())) {
            split.stopOnException();
        }
        
        // Configure aggregation strategy
        String aggStrategy = splitterConfig.getAggregationStrategy();
        if ("groupedExchange".equals(aggStrategy)) {
            split.aggregationStrategy(new org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy());
        }
        
        split.to("direct:split-output")
            .end();
    }
}