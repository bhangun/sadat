
/**
 * Plugin Audit Service - Immutable audit logging
 */
@ApplicationScoped
public class PluginAuditService {

    private static final Logger LOG = Logger.getLogger(PluginAuditService.class);

    @Inject
    AuditRepository auditRepository;

    @Inject
    KafkaProducer<String, AuditEvent> auditProducer;

    @Inject
    ProvenanceService provenanceService;

    /**
     * Log plugin registration event
     */
    public Uni<Void> logPluginRegistered(PluginEntity plugin) {
        return logEvent(AuditEventType.PLUGIN_REGISTERED, plugin, null);
    }

    /**
     * Log plugin load event
     */
    public Uni<Void> logPluginLoad(
            String pluginId, 
            String version, 
            boolean success) {
        
        AuditEvent event = AuditEvent.builder()
            .auditId(UUID.randomUUID())
            .eventType(AuditEventType.PLUGIN_LOADED)
            .pluginId(pluginId)
            .version(version)
            .timestamp(Instant.now())
            .actor(getCurrentActor())
            .details(Map.of("success", success))
            .build();

        return persistAuditEvent(event);
    }

    /**
     * Log plugin unload event
     */
    public Uni<Void> logPluginUnload(String pluginId, String version) {
        AuditEvent event = AuditEvent.builder()
            .auditId(UUID.randomUUID())
            .eventType(AuditEventType.PLUGIN_UNLOADED)
            .pluginId(pluginId)
            .version(version)
            .timestamp(Instant.now())
            .actor(getCurrentActor())
            .build();

        return persistAuditEvent(event);
    }

    /**
     * Log error handling event
     */
    public Uni<Void> logErrorHandling(
            ErrorPayload error, 
            ErrorHandlingDecision decision) {
        
        AuditEvent event = AuditEvent.builder()
            .auditId(UUID.randomUUID())
            .eventType(AuditEventType.PLUGIN_ERROR)
            .pluginId(error.getOriginNode())
            .timestamp(Instant.now())
            .actor(getCurrentActor())
            .error(error)
            .details(Map.of(
                "action", decision.getAction(),
                "reason", decision.getReason()
            ))
            .build();

        return persistAuditEvent(event);
    }

    /**
     * Persist audit event with integrity hash
     */
    private Uni<Void> persistAuditEvent(AuditEvent event) {
        // Calculate content hash for tamper detection
        String hash = calculateHash(event);
        event.setHash(hash);

        // Sign event if required
        if (requiresSignature(event)) {
            String signature = signEvent(event);
            event.setSignature(signature);
        }

        // Persist to database
        return auditRepository.persist(AuditEventEntity.from(event))
            .onItem().transformToUni(persisted -> 
                // Emit to Kafka for real-time processing
                emitAuditEvent(event)
            )
            .onItem().transformToUni(sent -> 
                // Store in provenance chain
                provenanceService.recordAudit(event)
            )
            .onFailure().invoke(throwable -> 
                LOG.error("Failed to persist audit event", throwable)
            );
    }

    /**
     * Calculate Blake3 hash of event
     */
    private String calculateHash(AuditEvent event) {
        String content = String.format("%s|%s|%s|%s",
            event.getEventType(),
            event.getPluginId(),
            event.getTimestamp(),
            event.getActor().getId()
        );
        
        return "blake3:" + Blake3.hash(content);
    }

    /**
     * Sign event for non-repudiation
     */
    private String signEvent(AuditEvent event) {
        // Use JCA to sign event
        return "signature"; // Placeholder
    }

    /**
     * Emit event to Kafka
     */
    private Uni<Void> emitAuditEvent(AuditEvent event) {
        return auditProducer.send(
            ProducerRecord.builder()
                .topic("plugin-audit-events")
                .key(event.getPluginId())
                .value(event)
                .build()
        ).replaceWithVoid();
    }

    private boolean requiresSignature(AuditEvent event) {
        // Sign security-critical events
        return event.getEventType() == AuditEventType.PLUGIN_APPROVED
            || event.getEventType() == AuditEventType.PLUGIN_REVOKED;
    }

    private Actor getCurrentActor() {
        // Get current user/system actor from security context
        return Actor.builder()
            .type(ActorType.SYSTEM)
            .id("plugin-loader-service")
            .name("Plugin Loader")
            .build();
    }

    private Uni<Void> logEvent(
            AuditEventType eventType,
            PluginEntity plugin,
            Map<String, Object> additionalDetails) {
        
        AuditEvent event = AuditEvent.builder()
            .auditId(UUID.randomUUID())
            .eventType(eventType)
            .pluginId(plugin.getPluginId())
            .version(plugin.getVersion())
            .timestamp(Instant.now())
            .actor(getCurrentActor())
            .details(additionalDetails != null ? additionalDetails : Map.of())
            .build();

        return persistAuditEvent(event);
    }
}
