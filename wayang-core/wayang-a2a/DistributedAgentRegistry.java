
// Agent Registry for A2A
@ApplicationScoped
public class DistributedAgentRegistry {
    @Inject EntityManager entityManager;
    
    public Optional<AgentDescriptor> getAgent(String agentId) {
        return Optional.ofNullable(
            entityManager.find(AgentEntity.class, agentId)
        ).map(this::toDescriptor);
    }
    
    public Optional<AgentDescriptor> getAgentByRole(
        String role, 
        Set<String> requiredCapabilities
    ) {
        return entityManager.createQuery(
            "SELECT a FROM AgentEntity a " +
            "WHERE a.role = :role " +
            "AND a.status = 'ACTIVE'",
            AgentEntity.class
        )
        .setParameter("role", role)
        .getResultStream()
        .filter(agent -> hasCapabilities(agent, requiredCapabilities))
        .findFirst()
        .map(this::toDescriptor);
    }
}
