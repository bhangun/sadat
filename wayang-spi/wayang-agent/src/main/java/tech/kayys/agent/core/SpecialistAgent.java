package tech.kayys.agent.core;

/**
 * Agent specialized for specific domain tasks
 */
public abstract class SpecialistAgent extends AbstractAgent {
    protected final String domain;

    protected SpecialistAgent(String id, String domain, Set<Capability> capabilities) {
        super(id, AgentType.SPECIALIST, capabilities);
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }
}