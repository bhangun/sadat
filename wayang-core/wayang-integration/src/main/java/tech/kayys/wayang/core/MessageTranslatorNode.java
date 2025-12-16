
/**
 * Transforms message structure/format using processors, templating, or scripts.
 */
@ApplicationScoped
public class MessageTranslatorNode extends AbstractIntegrationNode {
    
    @Inject
    CelExpressionEvaluator celEvaluator; // For CEL support
    
    @Override
    public void configureRoute(RouteBuilder builder, NodeContext ctx) {
        builder.from(getFromEndpoint())
            .routeId(getRouteId())
            .errorHandler(getErrorHandler())
            .process(exchange -> {
                String input = exchange.getIn().getBody(String.class);
                String output = transform(input, config.getTransformation());
                exchange.getIn().setBody(output);
            })
            .to("direct:success-output");
    }
    
    private String transform(String input, TransformConfig config) {
        return switch (config.getType()) {
            case "velocity" -> applyVelocity(input, config.getTemplate());
            case "jq" -> applyJq(input, config.getTemplate());
            case "cel" -> celEvaluator.evaluate(config.getTemplate(), input);
            // ... other transformers
        };
    }
}