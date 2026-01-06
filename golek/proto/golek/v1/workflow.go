


// golek Workflow Engine - gRPC API Definitions
// Package: tech.kayys.golek.v1

syntax = "proto3";

package golek.v1;

option go_package = "tech.kayys.golek/pkg/api/v1;golekv1";
option java_package = "tech.kayys.golek.api.v1";
option java_multiple_files = true;

import "google/protobuf/timestamp.proto";
import "google/protobuf/struct.proto";
import "google/protobuf/empty.proto";

// ============================================================================
// WORKFLOW SERVICE - Main workflow operations
// ============================================================================

service WorkflowService {
  // Workflow Run Operations
  rpc CreateRun(CreateRunRequest) returns (RunResponse);
  rpc StartRun(StartRunRequest) returns (RunResponse);
  rpc GetRun(GetRunRequest) returns (RunResponse);
  rpc SuspendRun(SuspendRunRequest) returns (RunResponse);
  rpc ResumeRun(ResumeRunRequest) returns (RunResponse);
  rpc CancelRun(CancelRunRequest) returns (google.protobuf.Empty);
  rpc SignalRun(SignalRequest) returns (google.protobuf.Empty);
  
  // Query Operations
  rpc QueryRuns(QueryRunsRequest) returns (QueryRunsResponse);
  rpc GetExecutionHistory(GetExecutionHistoryRequest) returns (ExecutionHistoryResponse);
  rpc GetActiveRunsCount(GetActiveRunsCountRequest) returns (CountResponse);
  
  // Streaming Operations
  rpc StreamRunStatus(StreamRunStatusRequest) returns (stream RunStatusUpdate);
}
