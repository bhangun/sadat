package enterprise

import (
	"context"
	"fmt"
	"net/http"
	"time"

	"github.com/bhangun/golek/pkg/core"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracegrpc"
	"go.opentelemetry.io/otel/sdk/trace"
	"go.uber.org/zap"
	"tech.kayys.golek/pkg/plugins"
)

// ============================================================================
// ADVANCED OBSERVABILITY PLUGIN
// ============================================================================

type AdvancedObservabilityPlugin struct {
	*plugins.BasePlugin
	metricsRegistry *prometheus.Registry
	tracerProvider  *trace.TracerProvider
	metricsServer   *http.Server

	// Metrics
	workflowsActive    *prometheus.GaugeVec
	workflowsCompleted *prometheus.CounterVec
	workflowsFailed    *prometheus.CounterVec
	workflowDuration   *prometheus.HistogramVec
	nodesExecuted      *prometheus.CounterVec
	nodesFailed        *prometheus.CounterVec
	nodeDuration       *prometheus.HistogramVec
}

func NewAdvancedObservabilityPlugin(config map[string]any, logger *zap.Logger) *AdvancedObservabilityPlugin {
	plugin := &AdvancedObservabilityPlugin{
		BasePlugin:      plugins.NewBasePlugin("advanced-observability", "1.0.0", core.PluginTypeObservability, logger),
		metricsRegistry: prometheus.NewRegistry(),
	}

	plugin.initializeMetrics()
	plugin.Initialize(config)

	return plugin
}

func (p *AdvancedObservabilityPlugin) Initialize(config map[string]any) error {
	p.BasePlugin.Initialize(config)

	// Initialize tracing
	if tracingEnabled, ok := config["tracing_enabled"].(bool); ok && tracingEnabled {
		endpoint := config["tracing_endpoint"].(string)
		p.initializeTracing(endpoint)
	}

	return nil
}

func (p *AdvancedObservabilityPlugin) Start(ctx context.Context) error {
	p.BasePlugin.Start(ctx)

	// Start metrics server
	metricsPort := p.GetConfig()["metrics_port"].(int)
	p.startMetricsServer(metricsPort)

	return nil
}

func (p *AdvancedObservabilityPlugin) Stop(ctx context.Context) error {
	if p.metricsServer != nil {
		p.metricsServer.Shutdown(ctx)
	}

	if p.tracerProvider != nil {
		p.tracerProvider.Shutdown(ctx)
	}

	return p.BasePlugin.Stop(ctx)
}

func (p *AdvancedObservabilityPlugin) initializeMetrics() {
	// Workflow metrics
	p.workflowsActive = prometheus.NewGaugeVec(
		prometheus.GaugeOpts{
			Name: "golek_workflows_active_total",
			Help: "Number of currently active workflows",
		},
		[]string{"tenant_id", "definition_id"},
	)

	p.workflowsCompleted = prometheus.NewCounterVec(
		prometheus.CounterOpts{
			Name: "golek_workflows_completed_total",
			Help: "Total number of completed workflows",
		},
		[]string{"tenant_id", "definition_id", "status"},
	)

	p.workflowsFailed = prometheus.NewCounterVec(
		prometheus.CounterOpts{
			Name: "golek_workflows_failed_total",
			Help: "Total number of failed workflows",
		},
		[]string{"tenant_id", "definition_id", "error_code"},
	)

	p.workflowDuration = prometheus.NewHistogramVec(
		prometheus.HistogramOpts{
			Name:    "golek_workflow_duration_seconds",
			Help:    "Workflow execution duration in seconds",
			Buckets: prometheus.ExponentialBuckets(1, 2, 10), // 1s, 2s, 4s, ..., 512s
		},
		[]string{"tenant_id", "definition_id"},
	)

	// Node metrics
	p.nodesExecuted = prometheus.NewCounterVec(
		prometheus.CounterOpts{
			Name: "golek_nodes_executed_total",
			Help: "Total number of executed nodes",
		},
		[]string{"tenant_id", "definition_id", "node_id", "executor_type"},
	)

	p.nodesFailed = prometheus.NewCounterVec(
		prometheus.CounterOpts{
			Name: "golek_nodes_failed_total",
			Help: "Total number of failed nodes",
		},
		[]string{"tenant_id", "definition_id", "node_id", "error_code"},
	)

	p.nodeDuration = prometheus.NewHistogramVec(
		prometheus.HistogramOpts{
			Name:    "golek_node_execution_duration_seconds",
			Help:    "Node execution duration in seconds",
			Buckets: prometheus.ExponentialBuckets(0.1, 2, 10), // 0.1s, 0.2s, 0.4s, ..., 51.2s
		},
		[]string{"tenant_id", "definition_id", "node_id"},
	)

	// Register metrics
	p.metricsRegistry.MustRegister(
		p.workflowsActive,
		p.workflowsCompleted,
		p.workflowsFailed,
		p.workflowDuration,
		p.nodesExecuted,
		p.nodesFailed,
		p.nodeDuration,
	)
}

func (p *AdvancedObservabilityPlugin) initializeTracing(endpoint string) {
	ctx := context.Background()

	exporter, err := otlptracegrpc.New(ctx,
		otlptracegrpc.WithEndpoint(endpoint),
		otlptracegrpc.WithInsecure(),
	)

	if err != nil {
		p.logger.Error("Failed to create trace exporter", zap.Error(err))
		return
	}

	p.tracerProvider = trace.NewTracerProvider(
		trace.WithBatcher(exporter),
		trace.WithSampler(trace.AlwaysSample()),
	)

	otel.SetTracerProvider(p.tracerProvider)

	p.logger.Info("Distributed tracing initialized", zap.String("endpoint", endpoint))
}

func (p *AdvancedObservabilityPlugin) startMetricsServer(port int) {
	mux := http.NewServeMux()
	mux.Handle("/metrics", promhttp.HandlerFor(p.metricsRegistry, promhttp.HandlerOpts{}))

	p.metricsServer = &http.Server{
		Addr:    fmt.Sprintf(":%d", port),
		Handler: mux,
	}

	go func() {
		p.logger.Info("Metrics server started", zap.Int("port", port))
		if err := p.metricsServer.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			p.logger.Error("Metrics server error", zap.Error(err))
		}
	}()
}

// Metric recording methods
func (p *AdvancedObservabilityPlugin) RecordWorkflowStarted(tenantID core.TenantID, definitionID core.WorkflowDefinitionID) {
	p.workflowsActive.WithLabelValues(tenantID.String(), definitionID.String()).Inc()
}

func (p *AdvancedObservabilityPlugin) RecordWorkflowCompleted(tenantID core.TenantID, definitionID core.WorkflowDefinitionID, status string, duration time.Duration) {
	p.workflowsActive.WithLabelValues(tenantID.String(), definitionID.String()).Dec()
	p.workflowsCompleted.WithLabelValues(tenantID.String(), definitionID.String(), status).Inc()
	p.workflowDuration.WithLabelValues(tenantID.String(), definitionID.String()).Observe(duration.Seconds())
}

func (p *AdvancedObservabilityPlugin) RecordWorkflowFailed(tenantID core.TenantID, definitionID core.WorkflowDefinitionID, errorCode string) {
	p.workflowsActive.WithLabelValues(tenantID.String(), definitionID.String()).Dec()
	p.workflowsFailed.WithLabelValues(tenantID.String(), definitionID.String(), errorCode).Inc()
}

func (p *AdvancedObservabilityPlugin) RecordNodeExecuted(tenantID core.TenantID, definitionID core.WorkflowDefinitionID, nodeID core.NodeID, executorType string, duration time.Duration) {
	p.nodesExecuted.WithLabelValues(tenantID.String(), definitionID.String(), nodeID.String(), executorType).Inc()
	p.nodeDuration.WithLabelValues(tenantID.String(), definitionID.String(), nodeID.String()).Observe(duration.Seconds())
}

func (p *AdvancedObservabilityPlugin) RecordNodeFailed(tenantID core.TenantID, definitionID core.WorkflowDefinitionID, nodeID core.NodeID, errorCode string) {
	p.nodesFailed.WithLabelValues(tenantID.String(), definitionID.String(), nodeID.String(), errorCode).Inc()
}
