
-- ============================================================================
-- SILAT CONTROL PLANE - DATABASE SCHEMA
-- ============================================================================
-- Migration: V2__control_plane_schema.sql

-- ==================== PROJECTS ====================

CREATE TABLE wayang_project (
    project_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id VARCHAR(64) NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    description TEXT,
    project_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(128),
    is_active BOOLEAN DEFAULT true,
    metadata JSONB,
    
    CONSTRAINT uk_project_name_tenant UNIQUE (tenant_id, project_name)
);

CREATE INDEX idx_wayang_project_tenant ON wayang_project(tenant_id);
CREATE INDEX idx_wayang_project_type ON wayang_project(project_type);
CREATE INDEX idx_wayang_project_active ON wayang_project(is_active) WHERE is_active = true;

COMMENT ON TABLE wayang_project IS 'Control plane projects - containers for workflows, agents, integrations';

-- ==================== WORKFLOW TEMPLATES ====================

CREATE TABLE cp_workflow_templates (
    template_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES wayang_project(project_id) ON DELETE CASCADE,
    template_name VARCHAR(255) NOT NULL,
    description TEXT,
    version VARCHAR(32) NOT NULL DEFAULT '1.0.0',
    template_type VARCHAR(50) NOT NULL,
    canvas_definition JSONB NOT NULL,
    workflow_definition_id VARCHAR(255),
    is_published BOOLEAN DEFAULT false,
    tags JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uk_template_name UNIQUE (project_id, template_name, version)
);

CREATE INDEX idx_cp_templates_project ON cp_workflow_templates(project_id);
CREATE INDEX idx_cp_templates_type ON cp_workflow_templates(template_type);
CREATE INDEX idx_cp_templates_published ON cp_workflow_templates(is_published) WHERE is_published = true;
CREATE INDEX idx_cp_templates_workflow_def ON cp_workflow_templates(workflow_definition_id);
CREATE INDEX idx_cp_templates_tags ON cp_workflow_templates USING gin(tags);

COMMENT ON TABLE cp_workflow_templates IS 'Visual workflow templates with canvas definitions';

-- ==================== AI AGENTS ====================

CREATE TABLE cp_ai_agents (
    agent_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES wayang_project(project_id) ON DELETE CASCADE,
    agent_name VARCHAR(255) NOT NULL,
    description TEXT,
    agent_type VARCHAR(50) NOT NULL,
    llm_config JSONB NOT NULL,
    capabilities JSONB,
    tools JSONB,
    memory_config JSONB,
    guardrails JSONB,
    status VARCHAR(50) DEFAULT 'INACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uk_agent_name UNIQUE (project_id, agent_name)
);

CREATE INDEX idx_cp_agents_project ON cp_ai_agents(project_id);
CREATE INDEX idx_cp_agents_type ON cp_ai_agents(agent_type);
CREATE INDEX idx_cp_agents_status ON cp_ai_agents(status);

COMMENT ON TABLE cp_ai_agents IS 'AI agent definitions for agentic workflows';

-- ==================== AGENT INTERACTIONS ====================

CREATE TABLE cp_agent_interactions (
    interaction_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    agent_id UUID NOT NULL REFERENCES cp_ai_agents(agent_id) ON DELETE CASCADE,
    task_id VARCHAR(255) NOT NULL,
    instruction TEXT NOT NULL,
    response TEXT,
    context JSONB,
    actions_taken JSONB,
    output JSONB,
    success BOOLEAN,
    errors JSONB,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_ms INTEGER,
    
    CONSTRAINT uk_interaction_task UNIQUE (agent_id, task_id)
);

CREATE INDEX idx_cp_interactions_agent ON cp_agent_interactions(agent_id);
CREATE INDEX idx_cp_interactions_started ON cp_agent_interactions(started_at DESC);
CREATE INDEX idx_cp_interactions_success ON cp_agent_interactions(success);

COMMENT ON TABLE cp_agent_interactions IS 'History of agent task executions';

-- ==================== AGENT MEMORY ====================

CREATE TABLE cp_agent_memory (
    memory_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    agent_id UUID NOT NULL REFERENCES cp_ai_agents(agent_id) ON DELETE CASCADE,
    memory_key VARCHAR(255) NOT NULL,
    memory_value TEXT NOT NULL,
    memory_type VARCHAR(50) DEFAULT 'SHORT_TERM',
    embedding VECTOR(1536), -- For vector search
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT uk_agent_memory_key UNIQUE (agent_id, memory_key)
);

CREATE INDEX idx_cp_memory_agent ON cp_agent_memory(agent_id);
CREATE INDEX idx_cp_memory_type ON cp_agent_memory(memory_type);
CREATE INDEX idx_cp_memory_expires ON cp_agent_memory(expires_at) 
    WHERE expires_at IS NOT NULL;

COMMENT ON TABLE cp_agent_memory IS 'Agent memory storage for context retention';

-- ==================== INTEGRATION PATTERNS ====================

CREATE TABLE cp_integration_patterns (
    pattern_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES wayang_project(project_id) ON DELETE CASCADE,
    pattern_name VARCHAR(255) NOT NULL,
    description TEXT,
    pattern_type VARCHAR(50) NOT NULL,
    source_config JSONB NOT NULL,
    target_config JSONB NOT NULL,
    transformation JSONB,
    error_handling JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uk_pattern_name UNIQUE (project_id, pattern_name)
);

CREATE INDEX idx_cp_patterns_project ON cp_integration_patterns(project_id);
CREATE INDEX idx_cp_patterns_type ON cp_integration_patterns(pattern_type);

COMMENT ON TABLE cp_integration_patterns IS 'Enterprise integration pattern definitions';

-- ==================== INTEGRATION EXECUTIONS ====================

CREATE TABLE cp_integration_executions (
    execution_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pattern_id UUID NOT NULL REFERENCES cp_integration_patterns(pattern_id) ON DELETE CASCADE,
    input_payload JSONB,
    output_payload JSONB,
    success BOOLEAN,
    errors JSONB,
    metadata JSONB,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_ms INTEGER
);

CREATE INDEX idx_cp_int_exec_pattern ON cp_integration_executions(pattern_id);
CREATE INDEX idx_cp_int_exec_started ON cp_integration_executions(started_at DESC);
CREATE INDEX idx_cp_int_exec_success ON cp_integration_executions(success);

COMMENT ON TABLE cp_integration_executions IS 'History of integration pattern executions';

-- ==================== TEMPLATE CATALOG ====================

CREATE TABLE cp_template_catalog (
    catalog_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_key VARCHAR(255) UNIQUE NOT NULL,
    template_name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    template_type VARCHAR(50) NOT NULL,
    canvas_definition JSONB NOT NULL,
    tags JSONB,
    difficulty VARCHAR(50),
    is_featured BOOLEAN DEFAULT false,
    usage_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_catalog_category ON cp_template_catalog(category);
CREATE INDEX idx_catalog_type ON cp_template_catalog(template_type);
CREATE INDEX idx_catalog_featured ON cp_template_catalog(is_featured) WHERE is_featured = true;
CREATE INDEX idx_catalog_tags ON cp_template_catalog USING gin(tags);
CREATE INDEX idx_catalog_usage ON cp_template_catalog(usage_count DESC);

COMMENT ON TABLE cp_template_catalog IS 'Catalog of reusable workflow templates';

-- ==================== PATTERN CATALOG ====================

CREATE TABLE cp_pattern_catalog (
    catalog_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pattern_key VARCHAR(255) UNIQUE NOT NULL,
    pattern_name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    pattern_type VARCHAR(50) NOT NULL,
    documentation TEXT,
    example_config JSONB,
    is_featured BOOLEAN DEFAULT false,
    usage_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pattern_catalog_category ON cp_pattern_catalog(category);
CREATE INDEX idx_pattern_catalog_type ON cp_pattern_catalog(pattern_type);
CREATE INDEX idx_pattern_catalog_featured ON cp_pattern_catalog(is_featured) WHERE is_featured = true;

COMMENT ON TABLE cp_pattern_catalog IS 'Catalog of EIP patterns';

-- ==================== COLLABORATION ====================

CREATE TABLE cp_project_members (
    member_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES wayang_project(project_id) ON DELETE CASCADE,
    user_id VARCHAR(128) NOT NULL,
    role VARCHAR(50) NOT NULL, -- OWNER, EDITOR, VIEWER
    permissions JSONB,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    added_by VARCHAR(128),
    
    CONSTRAINT uk_project_member UNIQUE (project_id, user_id)
);

CREATE INDEX idx_cp_members_project ON cp_project_members(project_id);
CREATE INDEX idx_cp_members_user ON cp_project_members(user_id);

COMMENT ON TABLE cp_project_members IS 'Project collaboration and access control';

-- ==================== AUDIT LOG ====================

CREATE TABLE cp_audit_log (
    audit_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id VARCHAR(64) NOT NULL,
    project_id UUID REFERENCES wayang_project(project_id) ON DELETE SET NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    performed_by VARCHAR(128) NOT NULL,
    performed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ip_address INET,
    user_agent TEXT
);

CREATE INDEX idx_cp_audit_tenant ON cp_audit_log(tenant_id);
CREATE INDEX idx_cp_audit_project ON cp_audit_log(project_id);
CREATE INDEX idx_cp_audit_entity ON cp_audit_log(entity_type, entity_id);
CREATE INDEX idx_cp_audit_performed ON cp_audit_log(performed_at DESC);
CREATE INDEX idx_cp_audit_user ON cp_audit_log(performed_by);

COMMENT ON TABLE cp_audit_log IS 'Audit trail for control plane operations';

-- ==================== VIEWS ====================

-- Active projects summary
CREATE VIEW v_cp_project_summary AS
SELECT 
    p.project_id,
    p.tenant_id,
    p.project_name,
    p.project_type,
    COUNT(DISTINCT t.template_id) as template_count,
    COUNT(DISTINCT a.agent_id) as agent_count,
    COUNT(DISTINCT i.pattern_id) as integration_count,
    COUNT(DISTINCT m.member_id) as member_count,
    p.created_at,
    p.updated_at
FROM wayang_project p
LEFT JOIN cp_workflow_templates t ON p.project_id = t.project_id
LEFT JOIN cp_ai_agents a ON p.project_id = a.project_id
LEFT JOIN cp_integration_patterns i ON p.project_id = i.project_id
LEFT JOIN cp_project_members m ON p.project_id = m.project_id
WHERE p.is_active = true
GROUP BY p.project_id;

-- Agent activity summary
CREATE VIEW v_cp_agent_activity AS
SELECT 
    a.agent_id,
    a.agent_name,
    a.agent_type,
    a.status,
    COUNT(i.interaction_id) as total_interactions,
    COUNT(CASE WHEN i.success = true THEN 1 END) as successful_interactions,
    COUNT(CASE WHEN i.success = false THEN 1 END) as failed_interactions,
    AVG(i.duration_ms) as avg_duration_ms,
    MAX(i.completed_at) as last_interaction
FROM cp_ai_agents a
LEFT JOIN cp_agent_interactions i ON a.agent_id = i.agent_id
GROUP BY a.agent_id;

-- Integration pattern usage
CREATE VIEW v_cp_integration_usage AS
SELECT 
    p.pattern_id,
    p.pattern_name,
    p.pattern_type,
    COUNT(e.execution_id) as execution_count,
    COUNT(CASE WHEN e.success = true THEN 1 END) as success_count,
    COUNT(CASE WHEN e.success = false THEN 1 END) as failure_count,
    AVG(e.duration_ms) as avg_duration_ms,
    MAX(e.completed_at) as last_execution
FROM cp_integration_patterns p
LEFT JOIN cp_integration_executions e ON p.pattern_id = e.pattern_id
GROUP BY p.pattern_id;

-- ==================== FUNCTIONS ====================

-- Function to increment catalog usage count
CREATE OR REPLACE FUNCTION increment_catalog_usage()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE cp_template_catalog
    SET usage_count = usage_count + 1,
        updated_at = NOW()
    WHERE template_key = NEW.template_name;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup expired memories
CREATE OR REPLACE FUNCTION cleanup_expired_memories()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM cp_agent_memory
    WHERE expires_at IS NOT NULL 
    AND expires_at < NOW();
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to archive old interactions
CREATE OR REPLACE FUNCTION archive_old_interactions(retention_days INTEGER)
RETURNS INTEGER AS $$
DECLARE
    archived_count INTEGER;
BEGIN
    -- In production, move to archive table
    DELETE FROM cp_agent_interactions
    WHERE completed_at < NOW() - (retention_days || ' days')::INTERVAL;
    
    GET DIAGNOSTICS archived_count = ROW_COUNT;
    RETURN archived_count;
END;
$$ LANGUAGE plpgsql;

-- ==================== INITIAL DATA ====================

-- Insert sample catalog templates
INSERT INTO cp_template_catalog (
    template_key, template_name, description, category, 
    template_type, canvas_definition, tags, difficulty, is_featured
) VALUES
(
    'ai-customer-support',
    'AI Customer Support Agent',
    'Autonomous agent for handling customer support inquiries',
    'AI Agents',
    'AI_AGENT_WORKFLOW',
    '{"nodes": [], "edges": [], "metadata": {}}',
    '["ai", "customer-support", "chatbot"]',
    'beginner',
    true
),
(
    'api-to-database',
    'API to Database Sync',
    'Sync data from REST API to database',
    'Integration',
    'EIP_PATTERN',
    '{"nodes": [], "edges": [], "metadata": {}}',
    '["integration", "api", "database"]',
    'beginner',
    true
),
(
    'approval-workflow',
    'Multi-Level Approval Workflow',
    'Hierarchical approval process with escalation',
    'Automation',
    'AUTOMATION',
    '{"nodes": [], "edges": [], "metadata": {}}',
    '["automation", "approval", "human-task"]',
    'beginner',
    true
);

-- Insert EIP pattern catalog
INSERT INTO cp_pattern_catalog (
    pattern_key, pattern_name, description, category,
    pattern_type, documentation, is_featured
) VALUES
(
    'content-router',
    'Content-Based Router',
    'Route messages based on content',
    'Routing',
    'CONTENT_BASED_ROUTER',
    'Route messages to different destinations based on message content criteria',
    true
),
(
    'message-translator',
    'Message Translator',
    'Transform message format',
    'Transformation',
    'MESSAGE_TRANSLATOR',
    'Convert message from one format to another',
    true
),
(
    'splitter-aggregator',
    'Splitter-Aggregator',
    'Split and aggregate messages',
    'Routing',
    'SPLITTER',
    'Split message into parts, process independently, and aggregate results',
    false
);

-- ==================== SCHEDULED JOBS ====================

-- Cleanup expired memories daily
-- (Use pg_cron or external scheduler in production)

-- Archive old interactions weekly
-- SELECT archive_old_interactions(90);

-- ==================== GRANTS ====================

-- Grant appropriate permissions
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO control_plane_service;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO control_plane_service;

---
# ============================================================================
# CONTROL PLANE CONFIGURATION
# ============================================================================
# File: application-control-plane.yml

silat:
  control-plane:
    # Enable control plane
    enabled: true
    
    # UI Configuration
    ui:
      # Visual workflow designer
      designer:
        canvas-size: 
          width: 2000
          height: 2000
        grid-size: 20
        zoom-levels: [0.25, 0.5, 0.75, 1.0, 1.5, 2.0]
        auto-layout: true
        
      # Theme
      theme:
        primary-color: "#2563eb"
        accent-color: "#10b981"
        dark-mode: true
    
    # AI Agent Configuration
    agents:
      # LLM Providers
      llm-providers:
        openai:
          enabled: ${OPENAI_ENABLED:false}
          api-key: ${OPENAI_API_KEY:}
          default-model: gpt-4-turbo-preview
          
        anthropic:
          enabled: ${ANTHROPIC_ENABLED:false}
          api-key: ${ANTHROPIC_API_KEY:}
          default-model: claude-3-sonnet-20240229
          
        azure-openai:
          enabled: ${AZURE_OPENAI_ENABLED:false}
          endpoint: ${AZURE_OPENAI_ENDPOINT:}
          api-key: ${AZURE_OPENAI_KEY:}
      
      # Memory Configuration
      memory:
        short-term-size: 10
        long-term-enabled: true
        vector-store: ${VECTOR_STORE:in-memory}
        
      # Guardrails
      guardrails:
        pii-detection: true
        toxicity-check: true
        content-filter: true
        
      # Rate Limits
      rate-limits:
        requests-per-minute: 60
        tokens-per-minute: 100000
    
    # Integration Configuration
    integrations:
      # Supported endpoint types
      endpoints:
        - rest
        - kafka
        - database
        - sftp
        - email
        
      # Transformation engines
      transformations:
        - jq
        - jsonata
        - javascript
        
      # Error handling
      error-handling:
        max-retries: 3
        dead-letter-enabled: true
        
    # Template Catalog
    catalog:
      # Built-in templates
      built-in-enabled: true
      
      # Custom template repository
      custom-repository: ${TEMPLATE_REPO_URL:}
      
      # Auto-update catalog
      auto-update: true
      update-interval: 24h
      
    # Collaboration
    collaboration:
      # Real-time editing
      real-time-enabled: true
      websocket-enabled: true
      
      # Version control
      version-control: true
      max-versions: 10
      
    # Security
    security:
      # Project access control
      rbac-enabled: true
      
      # Encryption
      encrypt-sensitive-data: true
      
      # Audit logging
      audit-log-enabled: true
      audit-retention-days: 90

