# API Reference

The Wayang Workflow Engine exposes both **gRPC** (primary) and **REST** (compatibility) APIs.

## gRPC Services
Default Port: `9090`

### 1. WorkflowRegistryService
Manage workflow definitions.
- `RegisterWorkflow`: Create/Update a workflow.
- `GetWorkflow`: Retrieve a definition.
- `ListWorkflows`: List all workflows.

### 2. WorkflowRunService
Manage execution instances.
- `RunWorkflow`: Trigger a new run.
- `GetRun`: Get run status.
- `PauseRun` / `ResumeRun` / `CancelRun`: Lifecycle management.

### 3. WorkflowSchedulerService
Manage scheduled executions.
- `ScheduleWorkflow`: Create a cron schedule.
- `ListSchedules`: View active schedules.

### 4. NodeRegistryService
Discovery of available node types.
- `ListNodeTypes`: Get available processing nodes.
- `GetNodeTypeInfo`: Get schema for a specific node.

### 5. ProvenanceService
Audit and lineage.
- `GetProvenanceReport`: Retrieve execution lineage and compliance data.

### 6. EventStoreService
Event-driven architecture support.
- `PublishEvent`: Ingest external events.
- `SubscribeToEvents`: Stream events.

## REST API
Default Port: `7001`
Base Path: `/api/v1`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/workflows` | Register workflow |
| GET | `/workflows` | List workflows |
| GET | `/workflows/{id}` | Get workflow |
| POST | `/workflows/{id}/runs` | Trigger run |
| GET | `/runs/{id}` | Get run status |
| GET | `/nodes` | List node types |

## Protobuf Definitions
The source of truth for the API is the `.proto` files located in `src/main/proto/`.
- `workflow_registry.proto`
- `workflow_run.proto`
- `node_registry.proto`
- ... (and others)
