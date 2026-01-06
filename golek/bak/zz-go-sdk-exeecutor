// Package executor provides the SDK for building golek workflow executors
// Executors are external services that execute workflow nodes
package executor

import (
	"context"
	"fmt"
	"sync"
	"time"

	"go.uber.org/zap"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"

	pb "tech.kayys.golek/pkg/api/v1"
	"tech.kayys.golek/pkg/core"
)

// ============================================================================
// EXECUTOR INTERFACE
// ============================================================================

// Executor defines the interface that all executors must implement
type Executor interface {
	// Execute processes a workflow node task
	Execute(ctx context.Context, task *Task) (*Result, error)

	// Metadata returns executor metadata
	Metadata() ExecutorMetadata

	// HealthCheck performs health check
	HealthCheck(ctx context.Context) error
}

// ExecutorMetadata contains executor information
type ExecutorMetadata struct {
	Type          string
	Version       string
	Capabilities  []string
	MaxConcurrent int
}

// Task represents a workflow node execution task
type Task struct {
	RunID   string
	NodeID  string
	Attempt int
	Token   string
	Context map[string]any
	Config  map[string]any
	Timeout time.Duration
}

// Result represents the execution result
type Result struct {
	Status core.NodeExecutionStatus
	Output map[string]any
	Error  *core.ErrorInfo
}

// ============================================================================
// EXECUTOR RUNTIME
// ============================================================================

// Runtime manages executor lifecycle and communication with the engine
type Runtime struct {
	executor Executor
	config   RuntimeConfig
	conn     *grpc.ClientConn
	client   pb.ExecutorServiceClient
	logger   *zap.Logger

	// State
	executorID   string
	isRegistered bool
	isRunning    bool

	// Concurrency control
	semaphore chan struct{}
	wg        sync.WaitGroup
	mu        sync.RWMutex

	// Metrics
	metrics *ExecutorMetrics
}

// RuntimeConfig contains runtime configuration
type RuntimeConfig struct {
	EngineEndpoint          string
	ExecutorID              string
	HeartbeatInterval       time.Duration
	ReconnectDelay          time.Duration
	GracefulShutdownTimeout time.Duration
}

// DefaultRuntimeConfig provides sensible defaults
func DefaultRuntimeConfig() RuntimeConfig {
	return RuntimeConfig{
		EngineEndpoint:          "localhost:9090",
		HeartbeatInterval:       10 * time.Second,
		ReconnectDelay:          5 * time.Second,
		GracefulShutdownTimeout: 30 * time.Second,
	}
}

// NewRuntime creates a new executor runtime
func NewRuntime(executor Executor, config RuntimeConfig, logger *zap.Logger) *Runtime {
	metadata := executor.Metadata()

	// Generate executor ID if not provided
	if config.ExecutorID == "" {
		config.ExecutorID = fmt.Sprintf("%s-%d", metadata.Type, time.Now().Unix())
	}

	return &Runtime{
		executor:   executor,
		config:     config,
		logger:     logger,
		executorID: config.ExecutorID,
		semaphore:  make(chan struct{}, metadata.MaxConcurrent),
		metrics:    NewExecutorMetrics(metadata.Type),
	}
}

// Start starts the executor runtime
func (r *Runtime) Start(ctx context.Context) error {
	r.logger.Info("Starting executor runtime",
		zap.String("executor_id", r.executorID),
		zap.String("type", r.executor.Metadata().Type),
	)

	// Connect to engine
	if err := r.connect(ctx); err != nil {
		return fmt.Errorf("failed to connect: %w", err)
	}

	// Register with engine
	if err := r.register(ctx); err != nil {
		return fmt.Errorf("failed to register: %w", err)
	}

	r.mu.Lock()
	r.isRunning = true
	r.mu.Unlock()

	// Start heartbeat
	r.wg.Add(1)
	go r.heartbeatLoop(ctx)

	// Start task receiver
	r.wg.Add(1)
	go r.receiveTasksLoop(ctx)

	r.logger.Info("Executor runtime started")

	return nil
}

// Stop stops the executor runtime
func (r *Runtime) Stop(ctx context.Context) error {
	r.logger.Info("Stopping executor runtime")

	r.mu.Lock()
	r.isRunning = false
	r.mu.Unlock()

	// Create shutdown context with timeout
	shutdownCtx, cancel := context.WithTimeout(context.Background(), r.config.GracefulShutdownTimeout)
	defer cancel()

	// Wait for active tasks to complete
	done := make(chan struct{})
	go func() {
		r.wg.Wait()
		close(done)
	}()

	select {
	case <-done:
		r.logger.Info("All tasks completed gracefully")
	case <-shutdownCtx.Done():
		r.logger.Warn("Shutdown timeout reached, some tasks may be interrupted")
	}

	// Unregister from engine
	if r.isRegistered {
		if err := r.unregister(ctx); err != nil {
			r.logger.Error("Failed to unregister", zap.Error(err))
		}
	}

	// Close connection
	if r.conn != nil {
		if err := r.conn.Close(); err != nil {
			r.logger.Error("Failed to close connection", zap.Error(err))
		}
	}

	r.logger.Info("Executor runtime stopped")

	return nil
}

// ============================================================================
// PRIVATE METHODS
// ============================================================================

// connect establishes connection to the engine
func (r *Runtime) connect(ctx context.Context) error {
	r.logger.Info("Connecting to engine", zap.String("endpoint", r.config.EngineEndpoint))

	conn, err := grpc.DialContext(ctx, r.config.EngineEndpoint,
		grpc.WithTransportCredentials(insecure.NewCredentials()),
		grpc.WithBlock(),
	)
	if err != nil {
		return fmt.Errorf("failed to connect: %w", err)
	}

	r.conn = conn
	r.client = pb.NewExecutorServiceClient(conn)

	r.logger.Info("Connected to engine")

	return nil
}

// register registers the executor with the engine
func (r *Runtime) register(ctx context.Context) error {
	metadata := r.executor.Metadata()

	req := &pb.RegisterExecutorRequest{
		ExecutorId:        r.executorID,
		ExecutorType:      metadata.Type,
		CommunicationType: pb.CommunicationType_COMMUNICATION_GRPC,
		Endpoint:          "", // Engine connects to us via streaming
		Metadata:          map[string]string{"version": metadata.Version},
		Capabilities:      metadata.Capabilities,
		MaxConcurrent:     int32(metadata.MaxConcurrent),
	}

	resp, err := r.client.RegisterExecutor(ctx, req)
	if err != nil {
		return fmt.Errorf("registration failed: %w", err)
	}

	r.isRegistered = true

	r.logger.Info("Executor registered",
		zap.String("status", resp.Status),
		zap.Time("registered_at", resp.RegisteredAt.AsTime()),
	)

	return nil
}

// unregister unregisters the executor from the engine
func (r *Runtime) unregister(ctx context.Context) error {
	req := &pb.UnregisterExecutorRequest{
		ExecutorId: r.executorID,
	}

	_, err := r.client.UnregisterExecutor(ctx, req)
	if err != nil {
		return err
	}

	r.isRegistered = false
	r.logger.Info("Executor unregistered")

	return nil
}

// heartbeatLoop sends periodic heartbeats
func (r *Runtime) heartbeatLoop(ctx context.Context) {
	defer r.wg.Done()

	ticker := time.NewTicker(r.config.HeartbeatInterval)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			r.mu.RLock()
			running := r.isRunning
			r.mu.RUnlock()

			if !running {
				return
			}

			if err := r.sendHeartbeat(ctx); err != nil {
				r.logger.Error("Heartbeat failed", zap.Error(err))
			}
		}
	}
}

// sendHeartbeat sends a heartbeat to the engine
func (r *Runtime) sendHeartbeat(ctx context.Context) error {
	req := &pb.HeartbeatRequest{
		ExecutorId: r.executorID,
		Metrics:    r.metrics.ToMap(),
	}

	_, err := r.client.Heartbeat(ctx, req)
	return err
}

// receiveTasksLoop receives tasks from the engine
func (r *Runtime) receiveTasksLoop(ctx context.Context) {
	defer r.wg.Done()

	for {
		r.mu.RLock()
		running := r.isRunning
		r.mu.RUnlock()

		if !running {
			return
		}

		if err := r.receiveTasksStream(ctx); err != nil {
			r.logger.Error("Task stream error", zap.Error(err))

			// Reconnect after delay
			select {
			case <-ctx.Done():
				return
			case <-time.After(r.config.ReconnectDelay):
				continue
			}
		}
	}
}

// receiveTasksStream opens a stream to receive tasks
func (r *Runtime) receiveTasksStream(ctx context.Context) error {
	metadata := r.executor.Metadata()

	req := &pb.StreamTasksRequest{
		ExecutorId:   r.executorID,
		ExecutorType: metadata.Type,
	}

	stream, err := r.client.StreamTasks(ctx, req)
	if err != nil {
		return err
	}

	r.logger.Info("Task stream opened")

	for {
		pbTask, err := stream.Recv()
		if err != nil {
			return err
		}

		r.logger.Debug("Received task",
			zap.String("task_id", pbTask.TaskId),
			zap.String("node_id", pbTask.NodeId),
		)

		// Execute task asynchronously
		r.wg.Add(1)
		go r.handleTask(ctx, pbTask)
	}
}

// handleTask processes a single task
func (r *Runtime) handleTask(ctx context.Context, pbTask *pb.ExecutionTask) {
	defer r.wg.Done()

	// Acquire semaphore for concurrency control
	r.semaphore <- struct{}{}
	defer func() { <-r.semaphore }()

	r.metrics.TaskStarted()
	startTime := time.Now()

	// Convert protobuf task to domain task
	task := &Task{
		RunID:   pbTask.RunId,
		NodeID:  pbTask.NodeId,
		Attempt: int(pbTask.Attempt),
		Token:   pbTask.ExecutionToken,
		Context: pbTask.Context.AsMap(),
		Config:  pbTask.Config.AsMap(),
	}

	if pbTask.TimeoutSeconds != nil {
		task.Timeout = time.Duration(*pbTask.TimeoutSeconds) * time.Second
	}

	// Create timeout context if specified
	taskCtx := ctx
	if task.Timeout > 0 {
		var cancel context.CancelFunc
		taskCtx, cancel = context.WithTimeout(ctx, task.Timeout)
		defer cancel()
	}

	// Execute task
	result, err := r.executor.Execute(taskCtx, task)
	if err != nil {
		r.logger.Error("Task execution failed",
			zap.String("task_id", pbTask.TaskId),
			zap.Error(err),
		)

		result = &Result{
			Status: core.NodeStatusFailed,
			Error:  core.NewErrorInfo(err),
		}
	}

	duration := time.Since(startTime)

	if result.Status == core.NodeStatusCompleted {
		r.metrics.TaskCompleted(duration)
	} else {
		r.metrics.TaskFailed(duration)
	}

	// Report result
	if err := r.reportResult(ctx, pbTask, result); err != nil {
		r.logger.Error("Failed to report result",
			zap.String("task_id", pbTask.TaskId),
			zap.Error(err),
		)
	}
}

// reportResult sends the task result back to the engine
func (r *Runtime) reportResult(ctx context.Context, pbTask *pb.ExecutionTask, result *Result) error {
	pbResult := &pb.TaskResult{
		TaskId:         pbTask.TaskId,
		RunId:          pbTask.RunId,
		NodeId:         pbTask.NodeId,
		Attempt:        pbTask.Attempt,
		ExecutionToken: pbTask.ExecutionToken,
	}

	// Set status
	switch result.Status {
	case core.NodeStatusCompleted:
		pbResult.Status = pb.TaskStatus_TASK_STATUS_COMPLETED
	case core.NodeStatusFailed:
		pbResult.Status = pb.TaskStatus_TASK_STATUS_FAILED
	default:
		pbResult.Status = pb.TaskStatus_TASK_STATUS_FAILED
	}

	// Set output
	if result.Output != nil {
		// Convert output to protobuf Struct
		// (simplified - would need proper conversion)
	}

	// Set error
	if result.Error != nil {
		pbResult.Error = &pb.ErrorInfo{
			Code:    result.Error.Code,
			Message: result.Error.Message,
		}
	}

	_, err := r.client.ReportResult(ctx, pbResult)
	return err
}

// ============================================================================
// EXECUTOR METRICS
// ============================================================================

// ExecutorMetrics tracks executor performance metrics
type ExecutorMetrics struct {
	executorType   string
	tasksStarted   int64
	tasksCompleted int64
	tasksFailed    int64
	totalDuration  time.Duration
	mu             sync.RWMutex
}

// NewExecutorMetrics creates new metrics tracker
func NewExecutorMetrics(executorType string) *ExecutorMetrics {
	return &ExecutorMetrics{
		executorType: executorType,
	}
}

// TaskStarted records task start
func (m *ExecutorMetrics) TaskStarted() {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.tasksStarted++
}

// TaskCompleted records task completion
func (m *ExecutorMetrics) TaskCompleted(duration time.Duration) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.tasksCompleted++
	m.totalDuration += duration
}

// TaskFailed records task failure
func (m *ExecutorMetrics) TaskFailed(duration time.Duration) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.tasksFailed++
	m.totalDuration += duration
}

// ToMap converts metrics to map for reporting
func (m *ExecutorMetrics) ToMap() map[string]string {
	m.mu.RLock()
	defer m.mu.RUnlock()

	avgDuration := time.Duration(0)
	if m.tasksCompleted > 0 {
		avgDuration = m.totalDuration / time.Duration(m.tasksCompleted)
	}

	return map[string]string{
		"tasks_started":   fmt.Sprintf("%d", m.tasksStarted),
		"tasks_completed": fmt.Sprintf("%d", m.tasksCompleted),
		"tasks_failed":    fmt.Sprintf("%d", m.tasksFailed),
		"avg_duration_ms": fmt.Sprintf("%d", avgDuration.Milliseconds()),
	}
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

// Success creates a successful result
func Success(output map[string]any) *Result {
	return &Result{
		Status: core.NodeStatusCompleted,
		Output: output,
	}
}

// Failure creates a failed result
func Failure(err error) *Result {
	return &Result{
		Status: core.NodeStatusFailed,
		Error:  core.NewErrorInfo(err),
	}
}

// FailureWithCode creates a failed result with custom error code
func FailureWithCode(code, message string) *Result {
	return &Result{
		Status: core.NodeStatusFailed,
		Error:  core.NewErrorInfoWithCode(code, message),
	}
}
