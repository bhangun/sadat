
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.AggregateDefinition;
import tech.kayys.wayang.integration.config.AggregatorConfig;
import tech.kayys.wayang.integration.strategies.CustomAggregationStrategy;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Aggregator Node - Aggregates split messages or correlates messages
 */
@Slf4j
@ApplicationScoped
@RegisterForReflection
public class AggregatorNode extends AbstractIntegrationNode {
    
    @Inject
    CustomAggregationStrategy customAggregationStrategy;
    
    private AggregatorConfig aggregatorConfig;
    
    @Override
    protected void onLoad() {
        super.onLoad();
        this.aggregatorConfig = config.getConfigAs(AggregatorNodeConfig.class).getAggregator();
    }
    
    @Override
    protected void configureRoute(RouteBuilder builder) throws Exception {
        AggregateDefinition aggregate = builder.from(getFromEndpoint())
            .routeId(getRouteId())
            .aggregate(builder.simple(aggregatorConfig.getCorrelationExpression()));
        
        // Set aggregation strategy
        String strategy = aggregatorConfig.getAggregationStrategy();
        if ("custom".equals(strategy)) {
            aggregate.aggregationStrategy(customAggregationStrategy);
        } else if ("flexible".equals(strategy)) {
            aggregate.aggregationStrategy(new org.apache.camel.processor.aggregate.UseLatestAggregationStrategy());
        }
        
        // Set completion criteria
        if (aggregatorConfig.getCompletionSize() != null) {
            aggregate.completionSize(aggregatorConfig.getCompletionSize());
        }
        
        if (aggregatorConfig.getCompletionTimeout() != null) {
            aggregate.completionTimeout(aggregatorConfig.getCompletionTimeout());
        }
        
        if (aggregatorConfig.getCompletionPredicate() != null) {
            aggregate.completionPredicate(builder.simple(aggregatorConfig.getCompletionPredicate()));
        }
        
        aggregate.to("direct:aggregated-output");
    }
}