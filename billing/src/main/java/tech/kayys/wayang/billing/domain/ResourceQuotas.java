package tech.kayys.wayang.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Resource quotas embedded in plan
 */
@Embeddable
public class ResourceQuotas {
    // Workflow execution quotas
    @Column(name = "max_workflow_runs_per_month")
    public int maxWorkflowRunsPerMonth = 1000;
    
    @Column(name = "max_concurrent_runs")
    public int maxConcurrentRuns = 10;
    
    @Column(name = "max_workflow_definitions")
    public int maxWorkflowDefinitions = 50;
    
    // AI Agent quotas
    @Column(name = "max_ai_agents")
    public int maxAiAgents = 5;
    
    @Column(name = "max_ai_tokens_per_month")
    public long maxAiTokensPerMonth = 100000;
    
    // Integration quotas
    @Column(name = "max_integrations")
    public int maxIntegrations = 10;
    
    @Column(name = "max_api_calls_per_month")
    public long maxApiCallsPerMonth = 100000;
    
    // Storage quotas
    @Column(name = "max_storage_gb")
    public int maxStorageGb = 10;
    
    @Column(name = "max_log_retention_days")
    public int maxLogRetentionDays = 30;
    
    // Team quotas
    @Column(name = "max_users")
    public int maxUsers = 5;
    
    @Column(name = "max_teams")
    public int maxTeams = 1;
    
    // Control plane quotas
    @Column(name = "max_projects")
    public int maxProjects = 10;
    
    @Column(name = "max_templates")
    public int maxTemplates = 50;
}

