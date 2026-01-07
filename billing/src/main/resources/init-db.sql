
# ============================================================================
# Database Initialization Script (init-db.sql)
# ============================================================================
---
-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create indexes for performance
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_org_tenant_active 
  ON mgmt_organizations(tenant_id) 
  WHERE deleted_at IS NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_usage_composite 
  ON mgmt_usage_records(organization_id, usage_type, timestamp DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_invoice_composite 
  ON mgmt_invoices(organization_id, status, due_date);

-- Create views for analytics
CREATE OR REPLACE VIEW v_active_subscriptions AS
SELECT 
  s.*,
  o.tenant_id,
  o.name as organization_name,
  p.name as plan_name,
  p.tier as plan_tier
FROM mgmt_subscriptions s
JOIN mgmt_organizations o ON s.organization_id = o.organization_id
JOIN mgmt_subscription_plans p ON s.plan_id = p.plan_id
WHERE s.status = 'ACTIVE'
  AND o.deleted_at IS NULL;

CREATE OR REPLACE VIEW v_revenue_summary AS
SELECT 
  DATE_TRUNC('month', invoice_date) as month,
  COUNT(*) as invoice_count,
  SUM(total_amount) as total_revenue,
  SUM(CASE WHEN status = 'PAID' THEN total_amount ELSE 0 END) as paid_revenue,
  AVG(total_amount) as avg_invoice_amount
FROM mgmt_invoices
WHERE invoice_date >= CURRENT_DATE - INTERVAL '12 months'
GROUP BY DATE_TRUNC('month', invoice_date)
ORDER BY month DESC;

-- Grant permissions
GRANT SELECT ON v_active_subscriptions TO silat;
GRANT SELECT ON v_revenue_summary TO silat;