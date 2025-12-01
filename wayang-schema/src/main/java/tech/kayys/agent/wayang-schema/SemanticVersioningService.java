
@ApplicationScoped
public class SemanticVersioningService implements VersioningService {
    @Inject EntityManager entityManager;
    @Inject DiffEngine diffEngine;
    
    @Override
    @Transactional
    public WorkflowVersion createVersion(Workflow workflow) {
        // Get latest version
        Optional<WorkflowVersion> latestOpt = getLatestVersion(workflow.getId());
        
        String newVersion;
        if (latestOpt.isPresent()) {
            // Increment version
            SemanticVersion latest = SemanticVersion.parse(latestOpt.get().getVersion());
            newVersion = latest.incrementMinor().toString();
        } else {
            // First version
            newVersion = "1.0.0";
        }
        
        // Create snapshot
        WorkflowVersion version = WorkflowVersion.builder()
            .id(UUID.randomUUID())
            .workflowId(workflow.getId())
            .version(newVersion)
            .snapshot(workflow.getDefinition())
            .createdBy(workflow.getCreatedBy())
            .createdAt(Instant.now())
            .build();
        
        WorkflowVersionEntity entity = toEntity(version);
        entityManager.persist(entity);
        
        return version;
    }
    
    @Override
    public ComparisonResult compare(UUID workflowId, String version1, String version2) {
        WorkflowVersion v1 = getVersion(workflowId, version1);
        WorkflowVersion v2 = getVersion(workflowId, version2);
        
        return diffEngine.compare(v1.getSnapshot(), v2.getSnapshot());
    }
    
    private Optional<WorkflowVersion> getLatestVersion(UUID workflowId) {
        List<WorkflowVersionEntity> entities = entityManager.createQuery(
            "SELECT v FROM WorkflowVersionEntity v " +
            "WHERE v.workflowId = :workflowId " +
            "ORDER BY v.createdAt DESC",
            WorkflowVersionEntity.class
        )
        .setParameter("workflowId", workflowId)
        .setMaxResults(1)
        .getResultList();
        
        return entities.isEmpty() 
            ? Optional.empty() 
            : Optional.of(toVersion(entities.get(0)));
    }
}