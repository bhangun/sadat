// Package core provides the core domain models and interfaces for the golek workflow engine.
// This is a polyglot, cloud-native workflow orchestration system designed for:
// - Agentic AI orchestration
// - Enterprise Integration Patterns (EIP)
// - Business Process Automation
// - Human-in-the-Loop workflows
package core

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/google/uuid"
)

// ============================================================================
// CORE DOMAIN TYPES
// ============================================================================

// TenantID represents a unique tenant identifier for multi-tenancy support
type TenantID string

func (t TenantID) String() string { return string(t) }
func (t TenantID) IsValid() bool  { return len(t) > 0 }

// WorkflowDefinitionID uniquely identifies a workflow definition
type WorkflowDefinitionID string

func (w WorkflowDefinitionID) String() string { return string(w) }

// WorkflowRunID uniquely identifies a workflow execution instance
type WorkflowRunID string

func (w WorkflowRunID) String() string { return string(w) }

func NewWorkflowRunID() WorkflowRunID {
	return WorkflowRunID(uuid.New().String())
}

// NodeID uniquely identifies a node within a workflow
type NodeID string

func (n NodeID) String() string { return string(n) }

// ExecutorID uniquely identifies an executor service
type ExecutorID string

func (e ExecutorID) String() string { return string(e) }

// ============================================================================
// WORKFLOW DEFINITION
// ============================================================================

// WorkflowDefinition defines the structure and behavior of a workflow
type WorkflowDefinition struct {
	ID           WorkflowDefinitionID `json:"id"`
	TenantID     TenantID             `json:"tenant_id"`
	Name         string               `json:"name"`
	Version      string               `json:"version"`
	Description  string               `json:"description,omitempty"`
	Nodes        []NodeDefinition     `json:"nodes"`
	Inputs       map[string]InputDef  `json:"inputs,omitempty"`
	Outputs      map[string]OutputDef `json:"outputs,omitempty"`
	Metadata     map[string]string    `json:"metadata,omitempty"`
	RetryPolicy  *RetryPolicy         `json:"retry_policy,omitempty"`
	Compensation *CompensationPolicy  `json:"compensation,omitempty"`
	CreatedAt    time.Time            `json:"created_at"`
	UpdatedAt    time.Time            `json:"updated_at"`
	IsActive     bool                 `json:"is_active"`
}

// NodeDefinition defines a single node in the workflow
type NodeDefinition struct {
	ID           NodeID         `json:"id"`
	Name         string         `json:"name"`
	Type         NodeType       `json:"type"`
	ExecutorType string         `json:"executor_type"`
	Config       map[string]any `json:"config,omitempty"`
	DependsOn    []NodeID       `json:"depends_on,omitempty"`
	Transitions  []Transition   `json:"transitions,omitempty"`
	RetryPolicy  *RetryPolicy   `json:"retry_policy,omitempty"`
	Timeout      *time.Duration `json:"timeout,omitempty"`
	Critical     bool           `json:"critical"`
}

// NodeType represents the type of workflow node
type NodeType string

const (
	NodeTypeTask        NodeType = "TASK"         // Standard task execution
	NodeTypeDecision    NodeType = "DECISION"     // Conditional branching
	NodeTypeParallel    NodeType = "PARALLEL"     // Parallel execution
	NodeTypeAggregate   NodeType = "AGGREGATE"    // Aggregate results
	NodeTypeHumanTask   NodeType = "HUMAN_TASK"   // Human-in-the-loop
	NodeTypeSubWorkflow NodeType = "SUB_WORKFLOW" // Nested workflow
	NodeTypeEventWait   NodeType = "EVENT_WAIT"   // Wait for external event
	NodeTypeTimer       NodeType = "TIMER"        // Time-based delay
	NodeTypeScript      NodeType = "SCRIPT"       // Script execution
	NodeTypeAIAgent     NodeType = "AI_AGENT"     // AI agent task
)

// Transition defines how to move from one node to another
type Transition struct {
	TargetNode NodeID         `json:"target_node"`
	Condition  string         `json:"condition,omitempty"` // Expression for conditional transition
	Type       TransitionType `json:"type"`
}

// TransitionType specifies the type of transition
type TransitionType string

const (
	TransitionSuccess   TransitionType = "SUCCESS"   // Execute on success
	TransitionFailure   TransitionType = "FAILURE"   // Execute on failure
	TransitionCondition TransitionType = "CONDITION" // Conditional execution
	TransitionDefault   TransitionType = "DEFAULT"   // Default path
)

// InputDef defines an input parameter for the workflow
type InputDef struct {
	Name         string `json:"name"`
	Type         string `json:"type"`
	Required     bool   `json:"required"`
	DefaultValue any    `json:"default_value,omitempty"`
	Description  string `json:"description,omitempty"`
}

// OutputDef defines an output parameter for the workflow
type OutputDef struct {
	Name        string `json:"name"`
	Type        string `json:"type"`
	Description string `json:"description,omitempty"`
}

// RetryPolicy defines retry behavior for failed executions
type RetryPolicy struct {
	MaxAttempts         int           `json:"max_attempts"`
	InitialDelay        time.Duration `json:"initial_delay"`
	MaxDelay            time.Duration `json:"max_delay"`
	BackoffMultiplier   float64       `json:"backoff_multiplier"`
	RetryableExceptions []string      `json:"retryable_exceptions,omitempty"`
}

// DefaultRetryPolicy provides sensible defaults
var DefaultRetryPolicy = &RetryPolicy{
	MaxAttempts:       3,
	InitialDelay:      time.Second,
	MaxDelay:          time.Minute * 5,
	BackoffMultiplier: 2.0,
}

// CompensationPolicy defines how to compensate failed workflows
type CompensationPolicy struct {
	Strategy       CompensationStrategy `json:"strategy"`
	TimeoutSeconds int                  `json:"timeout_seconds"`
	RetryOnFailure bool                 `json:"retry_on_failure"`
}

// CompensationStrategy defines compensation execution order
type CompensationStrategy string

const (
	CompensationSequential CompensationStrategy = "SEQUENTIAL" // Compensate in reverse order
	CompensationParallel   CompensationStrategy = "PARALLEL"   // Compensate in parallel
)

// ============================================================================
// WORKFLOW RUN (AGGREGATE ROOT)
// ============================================================================

// WorkflowRun represents a single execution instance of a workflow
type WorkflowRun struct {
	ID             WorkflowRunID             `json:"id"`
	TenantID       TenantID                  `json:"tenant_id"`
	DefinitionID   WorkflowDefinitionID      `json:"definition_id"`
	Status         RunStatus                 `json:"status"`
	Variables      map[string]any            `json:"variables"`
	NodeExecutions map[NodeID]*NodeExecution `json:"node_executions"`
	ExecutionPath  []string                  `json:"execution_path"`
	PendingNodes   []NodeID                  `json:"pending_nodes"`
	CreatedAt      time.Time                 `json:"created_at"`
	StartedAt      *time.Time                `json:"started_at,omitempty"`
	CompletedAt    *time.Time                `json:"completed_at,omitempty"`
	LastUpdatedAt  time.Time                 `json:"last_updated_at"`
	Error          *ErrorInfo                `json:"error,omitempty"`
	Labels         map[string]string         `json:"labels,omitempty"`

	// Event sourcing
	UncommittedEvents []Event `json:"-"`
	Version           int64   `json:"version"`
}

// RunStatus represents the current state of a workflow run
type RunStatus string

const (
	RunStatusCreated      RunStatus = "CREATED"
	RunStatusPending      RunStatus = "PENDING"
	RunStatusRunning      RunStatus = "RUNNING"
	RunStatusSuspended    RunStatus = "SUSPENDED"
	RunStatusCompleted    RunStatus = "COMPLETED"
	RunStatusFailed       RunStatus = "FAILED"
	RunStatusCancelled    RunStatus = "CANCELLED"
	RunStatusCompensating RunStatus = "COMPENSATING"
	RunStatusCompensated  RunStatus = "COMPENSATED"
)

// IsTerminal returns true if the status is a terminal state
func (s RunStatus) IsTerminal() bool {
	return s == RunStatusCompleted || s == RunStatusFailed ||
		s == RunStatusCancelled || s == RunStatusCompensated
}

// CanTransitionTo checks if transition to target status is valid
func (s RunStatus) CanTransitionTo(target RunStatus) bool {
	validTransitions := map[RunStatus][]RunStatus{
		RunStatusCreated:      {RunStatusPending, RunStatusCancelled},
		RunStatusPending:      {RunStatusRunning, RunStatusCancelled},
		RunStatusRunning:      {RunStatusSuspended, RunStatusCompleted, RunStatusFailed, RunStatusCancelled, RunStatusCompensating},
		RunStatusSuspended:    {RunStatusRunning, RunStatusCancelled},
		RunStatusCompensating: {RunStatusCompensated, RunStatusFailed},
	}

	allowed, exists := validTransitions[s]
	if !exists {
		return false
	}

	for _, status := range allowed {
		if status == target {
			return true
		}
	}
	return false
}

// NodeExecution tracks the execution state of a single node
type NodeExecution struct {
	NodeID      NodeID              `json:"node_id"`
	NodeName    string              `json:"node_name"`
	Status      NodeExecutionStatus `json:"status"`
	Attempt     int                 `json:"attempt"`
	StartedAt   *time.Time          `json:"started_at,omitempty"`
	CompletedAt *time.Time          `json:"completed_at,omitempty"`
	Output      map[string]any      `json:"output,omitempty"`
	Error       *ErrorInfo          `json:"error,omitempty"`
}

// NodeExecutionStatus represents the execution state of a node
type NodeExecutionStatus string

const (
	NodeStatusPending   NodeExecutionStatus = "PENDING"
	NodeStatusRunning   NodeExecutionStatus = "RUNNING"
	NodeStatusCompleted NodeExecutionStatus = "COMPLETED"
	NodeStatusFailed    NodeExecutionStatus = "FAILED"
	NodeStatusRetrying  NodeExecutionStatus = "RETRYING"
	NodeStatusSkipped   NodeExecutionStatus = "SKIPPED"
)

// ErrorInfo contains error details
type ErrorInfo struct {
	Code       string         `json:"code"`
	Message    string         `json:"message"`
	StackTrace string         `json:"stack_trace,omitempty"`
	Context    map[string]any `json:"context,omitempty"`
}

// ============================================================================
// EXECUTOR INTERFACE
// ============================================================================

// ExecutorInfo contains metadata about a registered executor
type ExecutorInfo struct {
	ID                ExecutorID        `json:"id"`
	ExecutorType      string            `json:"executor_type"`
	CommunicationType CommunicationType `json:"communication_type"`
	Endpoint          string            `json:"endpoint"`
	Metadata          map[string]string `json:"metadata,omitempty"`
	Capabilities      []string          `json:"capabilities,omitempty"`
	MaxConcurrent     int               `json:"max_concurrent"`
	Status            ExecutorStatus    `json:"status"`
	LastHeartbeat     time.Time         `json:"last_heartbeat"`
	RegisteredAt      time.Time         `json:"registered_at"`
}

// CommunicationType defines how to communicate with executor
type CommunicationType string

const (
	CommunicationGRPC  CommunicationType = "GRPC"
	CommunicationREST  CommunicationType = "REST"
	CommunicationKafka CommunicationType = "KAFKA"
	CommunicationNATS  CommunicationType = "NATS"
)

// ExecutorStatus represents the health status of an executor
type ExecutorStatus string

const (
	ExecutorStatusActive   ExecutorStatus = "ACTIVE"
	ExecutorStatusInactive ExecutorStatus = "INACTIVE"
	ExecutorStatusDraining ExecutorStatus = "DRAINING"
	ExecutorStatusFailed   ExecutorStatus = "FAILED"
)

// NodeExecutionTask represents a task sent to an executor
type NodeExecutionTask struct {
	RunID   WorkflowRunID  `json:"run_id"`
	NodeID  NodeID         `json:"node_id"`
	Attempt int            `json:"attempt"`
	Token   ExecutionToken `json:"token"`
	Context map[string]any `json:"context"`
	Config  map[string]any `json:"config,omitempty"`
	Timeout *time.Duration `json:"timeout,omitempty"`
}

// NodeExecutionResult represents the result from an executor
type NodeExecutionResult struct {
	RunID   WorkflowRunID       `json:"run_id"`
	NodeID  NodeID              `json:"node_id"`
	Attempt int                 `json:"attempt"`
	Status  NodeExecutionStatus `json:"status"`
	Output  map[string]any      `json:"output,omitempty"`
	Error   *ErrorInfo          `json:"error,omitempty"`
	Token   ExecutionToken      `json:"token"`
}

// ExecutionToken is used for security and idempotency
type ExecutionToken struct {
	Value     string        `json:"value"`
	RunID     WorkflowRunID `json:"run_id"`
	NodeID    NodeID        `json:"node_id"`
	Attempt   int           `json:"attempt"`
	ExpiresAt time.Time     `json:"expires_at"`
}

// IsValid checks if token is still valid
func (t *ExecutionToken) IsValid() bool {
	return time.Now().Before(t.ExpiresAt)
}

// ============================================================================
// EVENT SOURCING
// ============================================================================

// Event represents a domain event in the workflow
type Event interface {
	EventID() string
	EventType() string
	OccurredAt() time.Time
	AggregateID() WorkflowRunID
	Payload() map[string]any
}

// BaseEvent provides common event fields
type BaseEvent struct {
	ID          string        `json:"event_id"`
	Type        string        `json:"event_type"`
	Timestamp   time.Time     `json:"occurred_at"`
	RunID       WorkflowRunID `json:"run_id"`
	SequenceNum int64         `json:"sequence_num"`
}

func (e BaseEvent) EventID() string            { return e.ID }
func (e BaseEvent) EventType() string          { return e.Type }
func (e BaseEvent) OccurredAt() time.Time      { return e.Timestamp }
func (e BaseEvent) AggregateID() WorkflowRunID { return e.RunID }

// Specific event types
type WorkflowStartedEvent struct {
	BaseEvent
	DefinitionID WorkflowDefinitionID `json:"definition_id"`
	TenantID     TenantID             `json:"tenant_id"`
	Inputs       map[string]any       `json:"inputs"`
}

func (e WorkflowStartedEvent) Payload() map[string]any {
	return map[string]any{
		"definition_id": e.DefinitionID,
		"tenant_id":     e.TenantID,
		"inputs":        e.Inputs,
	}
}

type NodeScheduledEvent struct {
	BaseEvent
	NodeID  NodeID `json:"node_id"`
	Attempt int    `json:"attempt"`
}

func (e NodeScheduledEvent) Payload() map[string]any {
	return map[string]any{
		"node_id": e.NodeID,
		"attempt": e.Attempt,
	}
}

type NodeCompletedEvent struct {
	BaseEvent
	NodeID  NodeID         `json:"node_id"`
	Attempt int            `json:"attempt"`
	Output  map[string]any `json:"output"`
}

func (e NodeCompletedEvent) Payload() map[string]any {
	return map[string]any{
		"node_id": e.NodeID,
		"attempt": e.Attempt,
		"output":  e.Output,
	}
}

type NodeFailedEvent struct {
	BaseEvent
	NodeID    NodeID     `json:"node_id"`
	Attempt   int        `json:"attempt"`
	Error     *ErrorInfo `json:"error"`
	WillRetry bool       `json:"will_retry"`
}

func (e NodeFailedEvent) Payload() map[string]any {
	return map[string]any{
		"node_id":    e.NodeID,
		"attempt":    e.Attempt,
		"error":      e.Error,
		"will_retry": e.WillRetry,
	}
}

type WorkflowCompletedEvent struct {
	BaseEvent
	Outputs map[string]any `json:"outputs"`
}

func (e WorkflowCompletedEvent) Payload() map[string]any {
	return map[string]any{
		"outputs": e.Outputs,
	}
}

type WorkflowFailedEvent struct {
	BaseEvent
	Error *ErrorInfo `json:"error"`
}

func (e WorkflowFailedEvent) Payload() map[string]any {
	return map[string]any{
		"error": e.Error,
	}
}

// ============================================================================
// REPOSITORY INTERFACES
// ============================================================================

// WorkflowDefinitionRepository manages workflow definitions
type WorkflowDefinitionRepository interface {
	Save(ctx context.Context, def *WorkflowDefinition) error
	FindByID(ctx context.Context, id WorkflowDefinitionID, tenantID TenantID) (*WorkflowDefinition, error)
	FindByName(ctx context.Context, name, version string, tenantID TenantID) (*WorkflowDefinition, error)
	List(ctx context.Context, tenantID TenantID, activeOnly bool) ([]*WorkflowDefinition, error)
	Delete(ctx context.Context, id WorkflowDefinitionID, tenantID TenantID) error
}

// WorkflowRunRepository manages workflow run state
type WorkflowRunRepository interface {
	Save(ctx context.Context, run *WorkflowRun) error
	FindByID(ctx context.Context, id WorkflowRunID, tenantID TenantID) (*WorkflowRun, error)
	Query(ctx context.Context, query *RunQuery) ([]*WorkflowRun, error)
	CountActive(ctx context.Context, tenantID TenantID) (int64, error)
}

// RunQuery represents search criteria for workflow runs
type RunQuery struct {
	TenantID     TenantID
	DefinitionID *WorkflowDefinitionID
	Status       *RunStatus
	Labels       map[string]string
	CreatedAfter *time.Time
	Limit        int
	Offset       int
}

// EventStore manages event persistence
type EventStore interface {
	AppendEvents(ctx context.Context, runID WorkflowRunID, events []Event, expectedVersion int64) error
	GetEvents(ctx context.Context, runID WorkflowRunID) ([]Event, error)
	GetEventsSince(ctx context.Context, runID WorkflowRunID, version int64) ([]Event, error)
}

// ExecutorRegistry manages executor registration and discovery
type ExecutorRegistry interface {
	Register(ctx context.Context, info *ExecutorInfo) error
	Unregister(ctx context.Context, id ExecutorID) error
	FindByType(ctx context.Context, executorType string) ([]*ExecutorInfo, error)
	FindByID(ctx context.Context, id ExecutorID) (*ExecutorInfo, error)
	UpdateHeartbeat(ctx context.Context, id ExecutorID) error
	ListActive(ctx context.Context) ([]*ExecutorInfo, error)
}

// ============================================================================
// WORKFLOW ENGINE INTERFACE
// ============================================================================

// WorkflowEngine is the main orchestration engine
type WorkflowEngine interface {
	// Lifecycle operations
	CreateRun(ctx context.Context, req *CreateRunRequest) (*WorkflowRun, error)
	StartRun(ctx context.Context, runID WorkflowRunID, tenantID TenantID) (*WorkflowRun, error)
	SuspendRun(ctx context.Context, runID WorkflowRunID, tenantID TenantID, reason string) (*WorkflowRun, error)
	ResumeRun(ctx context.Context, runID WorkflowRunID, tenantID TenantID, data map[string]any) (*WorkflowRun, error)
	CancelRun(ctx context.Context, runID WorkflowRunID, tenantID TenantID, reason string) error

	// Query operations
	GetRun(ctx context.Context, runID WorkflowRunID, tenantID TenantID) (*WorkflowRun, error)
	QueryRuns(ctx context.Context, query *RunQuery) ([]*WorkflowRun, error)
	GetExecutionHistory(ctx context.Context, runID WorkflowRunID, tenantID TenantID) ([]Event, error)

	// Node result handling
	HandleNodeResult(ctx context.Context, result *NodeExecutionResult) error

	// Signal handling
	Signal(ctx context.Context, runID WorkflowRunID, signal *Signal) error
}

// CreateRunRequest contains parameters for creating a workflow run
type CreateRunRequest struct {
	DefinitionID WorkflowDefinitionID `json:"definition_id"`
	TenantID     TenantID             `json:"tenant_id"`
	Inputs       map[string]any       `json:"inputs,omitempty"`
	Labels       map[string]string    `json:"labels,omitempty"`
}

// Signal represents an external signal to a workflow
type Signal struct {
	Name         string         `json:"name"`
	TargetNodeID NodeID         `json:"target_node_id,omitempty"`
	Payload      map[string]any `json:"payload,omitempty"`
	Timestamp    time.Time      `json:"timestamp"`
}

// ============================================================================
// PLUGIN SYSTEM INTERFACES
// ============================================================================

// Plugin represents a pluggable component in the system
type Plugin interface {
	// Metadata
	Name() string
	Version() string
	Type() PluginType

	// Lifecycle
	Initialize(config map[string]any) error
	Start(ctx context.Context) error
	Stop(ctx context.Context) error
	HealthCheck(ctx context.Context) error

	// Configuration
	DefaultConfig() map[string]any
	ValidateConfig(config map[string]any) error
}

// PluginType categorizes plugins
type PluginType string

const (
	PluginTypeCore          PluginType = "CORE"
	PluginTypeMiddleware    PluginType = "MIDDLEWARE"
	PluginTypeAPI           PluginType = "API"
	PluginTypeSecurity      PluginType = "SECURITY"
	PluginTypeStorage       PluginType = "STORAGE"
	PluginTypeMessaging     PluginType = "MESSAGING"
	PluginTypeObservability PluginType = "OBSERVABILITY"
)

// PluginRegistry manages plugin lifecycle
type PluginRegistry interface {
	Register(plugin Plugin) error
	Unregister(name string) error
	Get(name string) (Plugin, error)
	List() []Plugin
	EnablePlugin(name string) error
	DisablePlugin(name string) error
	IsEnabled(name string) bool
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

// MarshalJSON custom JSON marshaling for Duration
func (p *RetryPolicy) MarshalJSON() ([]byte, error) {
	type Alias RetryPolicy
	return json.Marshal(&struct {
		InitialDelay string `json:"initial_delay"`
		MaxDelay     string `json:"max_delay"`
		*Alias
	}{
		InitialDelay: p.InitialDelay.String(),
		MaxDelay:     p.MaxDelay.String(),
		Alias:        (*Alias)(p),
	})
}

// NewExecutionToken creates a new execution token
func NewExecutionToken(runID WorkflowRunID, nodeID NodeID, attempt int, validity time.Duration) ExecutionToken {
	return ExecutionToken{
		Value:     uuid.New().String(),
		RunID:     runID,
		NodeID:    nodeID,
		Attempt:   attempt,
		ExpiresAt: time.Now().Add(validity),
	}
}

// NewErrorInfo creates an ErrorInfo from an error
func NewErrorInfo(err error) *ErrorInfo {
	if err == nil {
		return nil
	}
	return &ErrorInfo{
		Code:    "INTERNAL_ERROR",
		Message: err.Error(),
	}
}

// NewErrorInfoWithCode creates an ErrorInfo with a specific code
func NewErrorInfoWithCode(code, message string) *ErrorInfo {
	return &ErrorInfo{
		Code:    code,
		Message: message,
	}
}

// String returns a string representation of ErrorInfo
func (e *ErrorInfo) Error() string {
	return fmt.Sprintf("[%s] %s", e.Code, e.Message)
}
