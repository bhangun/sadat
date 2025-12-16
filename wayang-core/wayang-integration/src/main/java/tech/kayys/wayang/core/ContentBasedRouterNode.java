
/**
 * Routes messages based on content inspection (headers, body, properties).
 */
@ApplicationScoped
public class ContentBasedRouterNode extends AbstractIntegrationNode {
    
    @Override
    public void configureRoute(RouteBuilder builder, NodeContext ctx) {
        ChoiceDefinition choice = builder.from(getFromEndpoint())
            .routeId(getRouteId())
            .errorHandler(getErrorHandler())
            .choice();
        
        for (RoutingRule rule : config.getRoutingRules()) {
            choice.when(simple(rule.getWhen()))
                  .to(rule.getTo());
        }
        
        if (config.getOtherwise() != null) {
            choice.otherwise().to(config.getOtherwise());
        }
        
        choice.end()
              .to("direct:success-output");
    }
}