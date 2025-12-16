


@ApplicationScoped
public class CamelAuditInterceptor implements Processor {
    
    @Inject
    AuditService auditService;
    
    @Override
    public void process(Exchange exchange) throws Exception {
        AuditEntry entry = AuditEntry.builder()
            .event("CAMEL_EXCHANGE")
            .actor(Actor.system())
            .target(Target.node(exchange.getFromRouteId()))
            .metadata(Map.of(
                "exchangeId", exchange.getExchangeId(),
                "endpoint", exchange.getFromEndpoint().getEndpointUri(),
                "bodyType", exchange.getIn().getBody().getClass().getName()
            ))
            .build();
        
        auditService.record(entry);
    }
}