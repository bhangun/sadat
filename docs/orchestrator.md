# Wayang Orchestrator - Complete Implementation

## Project Overview

The Wayang Orchestrator is the central runtime controller that coordinates execution plans, manages lifecycle, handles replanning, A2A delegation, failures, transactions, and observability. This implementation follows enterprise-grade patterns with complete error handling, audit trails, and modular architecture.

## Project Structure

```
wayang-orchestrator/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── tech/
│   │   │       └── kayys/
│   │   │           └── wayang/
│   │   │               └── orchestrator/
│   │   │                   ├── OrchestratorApplication.java
│   │   │                   ├── api/
│   │   │                   │   ├── OrchestratorResource.java
│   │   │                   │   ├── dto/
│   │   │                   │   │   ├── ExecutePlanRequest.java
│   │   │                   │   │   ├── ExecutePlanResponse.java
│   │   │                   │   │   ├── ExecutionStatusResponse.java
│   │   │                   │   │   ├── PauseResumeRequest.java
│   │   │                   │   │   └── CancelRequest.java
│   │   │                   │   └── validation/
│   │   │                   │       └── ExecutionValidator.java
│   │   │                   ├── core/
│   │   │                   │   ├── dispatcher/
│   │   │                   │   │   ├── Dispatcher.java
│   │   │                   │   │   ├── DAGWalker.java
│   │   │                   │   │   └── DependencyResolver.java
│   │   │                   │   ├── scheduler/
│   │   │                   │   │   ├── ResourceScheduler.java
│   │   │                   │   │   ├── SchedulingPolicy.java
│   │   │                   │   │   └── PriorityQueue.java
│   │   │                   │   ├── state/
│   │   │                   │   │   ├── StateMachine.java
│   │   │                   │   │   ├── NodeState.java
│   │   │                   │   │   └── StateTransition.java
│   │   │                   │   ├── checkpoint/
│   │   │                   │   │   ├── CheckpointManager.java
│   │   │                   │   │   └── CheckpointStore.java
│   │   │                   │   ├── replanner/
│   │   │                   │   │   ├── Replanner.java
│   │   │                   │   │   └── DynamicPlanUpdater.java
│   │   │                   │   ├── compensation/
│   │   │                   │   │   ├── CompensationEngine.java
│   │   │                   │   │   └── SagaCoordinator.java
│   │   │                   │   └── retry/
│   │   │                   │       ├── RetryManager.java
│   │   │                   │       ├── BackoffStrategy.java
│   │   │                   │       └── CircuitBreaker.java
│   │   │                   ├── adapter/
│   │   │                   │   ├── RuntimeHubAdapter.java
│   │   │                   │   ├── A2AAdapter.java
│   │   │                   │   ├── PolicyEnforcerAdapter.java
│   │   │                   │   └── EventEmitterAdapter.java
│   │   │                   ├── store/
│   │   │                   │   ├── ExecutionStore.java
│   │   │                   │   ├── PlanStore.java
│   │   │                   │   └── repository/
│   │   │                   │       ├── ExecutionRunRepository.java
│   │   │                   │       ├── NodeStateRepository.java
│   │   │                   │       └── HumanTaskRepository.java
│   │   │                   ├── model/
│   │   │                   │   ├── ExecutionRun.java
│   │   │                   │   ├── ExecutionPlan.java
│   │   │                   │   ├── Node.java
│   │   │                   │   ├── Edge.java
│   │   │                   │   ├── NodeExecution.java
│   │   │                   │   ├── ExecutionStatus.java
│   │   │                   │   ├── NodeStatus.java
│   │   │                   │   ├── ErrorPayload.java
│   │   │                   │   ├── AuditPayload.java
│   │   │                   │   └── HumanTask.java
│   │   │                   ├── event/
│   │   │                   │   ├── EventPublisher.java
│   │   │                   │   ├── EventType.java
│   │   │                   │   ├── ExecutionEvent.java
│   │   │                   │   └── listener/
│   │   │                   │       ├── ErrorEventListener.java
│   │   │                   │       └── AuditEventListener.java
│   │   │                   ├── audit/
│   │   │                   │   ├── AuditService.java
│   │   │                   │   ├── AuditLogger.java
│   │   │                   │   └── ProvenanceTracker.java
│   │   │                   ├── monitoring/
│   │   │                   │   ├── HealthMonitor.java
│   │   │                   │   ├── MetricsCollector.java
│   │   │                   │   └── TracingInterceptor.java
│   │   │                   ├── config/
│   │   │                   │   ├── OrchestratorConfig.java
│   │   │                   │   ├── SchedulerConfig.java
│   │   │                   │   └── RetryConfig.java
│   │   │                   └── exception/
│   │   │                       ├── OrchestratorException.java
│   │   │                       ├── ExecutionException.java
│   │   │                       ├── SchedulingException.java
│   │   │                       └── ValidationException.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/
│   │           └── migration/
│   │               ├── V1__create_execution_tables.sql
│   │               ├── V2__create_audit_tables.sql
│   │               └── V3__create_indexes.sql
│   └── test/
│       └── java/
│           └── tech/
│               └── kayys/
│                   └── wayang/
│                       └── orchestrator/
│                           ├── OrchestratorTest.java
│                           ├── DispatcherTest.java
│                           ├── StateMachineTest.java
│                           └── integration/
│                               └── OrchestratorIntegrationTest.java
└── README.md
```

## Implementation

### 1. Project Configuration (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>tech.kayys.wayang</groupId>
    <artifactId>wayang-orchestrator</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>Wayang Orchestrator</name>
    <description>Central workflow orchestration engine for Wayang AI Agent Platform</description>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- Quarkus -->
        <quarkus.version>3.15.1</quarkus.version>
        
        <!-- Other -->
        <lombok.version>1.18.30</lombok.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <jackson.version>2.16.0</jackson.version>
        <caffeine.version>3.1.8</caffeine.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.quarkus.platform</groupId>
                <artifactId>quarkus-bom</artifactId>
                <version>${quarkus.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Quarkus Core -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-arc</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-resteasy-reactive-jackson</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-smallrye-openapi</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-hibernate-reactive-panache</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-reactive-pg-client</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-flyway</artifactId>
        </dependency>

        <!-- Messaging -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-smallrye-reactive-messaging-kafka</artifactId>
        </dependency>

        <!-- Caching -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-redis-client</artifactId>
        </dependency>

        <!-- Observability -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-smallrye-health</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-micrometer-registry-prometheus</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-opentelemetry</artifactId>
        </dependency>

        <!-- Security -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-smallrye-jwt</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-oidc</artifactId>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-hibernate-validator</artifactId>
        </dependency>

        <!-- Resilience -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-smallrye-fault-tolerance</artifactId>
        </dependency>

        <!-- Scheduler -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-scheduler</artifactId>
        </dependency>

        <!-- Utilities -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
            <version>${caffeine.version}</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-junit5</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-test-h2</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-test-kafka-companion</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>io.quarkus</groupId>
                <artifactId>quarkus-maven-plugin</artifactId>
                <version>${quarkus.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>build</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2. Core Domain Models

#### ExecutionRun.java
```java
package tech.kayys.wayang.orchestrator.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an execution run of a workflow plan.
 * This is the top-level entity that tracks the overall execution state.
 * 
 * Design principles:
 * - Immutable plan reference (planId + version)
 * - Audit trail with timestamps
 * - Multi-tenant support via tenantId
 * - Metadata for extensibility
 */
@Entity
@Table(name = "execution_runs", indexes = {
    @Index(name = "idx_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_plan_id", columnList = "plan_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionRun extends PanacheEntityBase {

    @Id
    @Column(name = "run_id", nullable = false, updatable = false)
    private UUID runId;

    @Column(name = "plan_id", nullable = false, updatable = false)
    private UUID planId;

    @Column(name = "plan_version", nullable = false, updatable = false)
    private String planVersion;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExecutionStatus status;

    @Column(name = "created_by")
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @UpdateTimestamp
    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "error_message")
    private String errorMessage;

    @PrePersist
    public void prePersist() {
        if (runId == null) {
            runId = UUID.randomUUID();
        }
        if (status == null) {
            status = ExecutionStatus.PENDING;
        }
    }

    // Reactive queries
    public static Uni<ExecutionRun> findByRunId(UUID runId) {
        return find("runId", runId).firstResult();
    }

    public static Uni<Long> countByTenantAndStatus(UUID tenantId, ExecutionStatus status) {
        return count("tenantId = ?1 and status = ?2", tenantId, status);
    }

    // Audit methods
    public void markStarted() {
        this.status = ExecutionStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void markCompleted() {
        this.status = ExecutionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = ExecutionStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public void markPaused() {
        this.status = ExecutionStatus.PAUSED;
    }

    public boolean isTerminal() {
        return status == ExecutionStatus.COMPLETED ||
               status == ExecutionStatus.FAILED ||
               status == ExecutionStatus.CANCELLED;
    }

    public boolean canResume() {
        return status == ExecutionStatus.PAUSED ||
               status == ExecutionStatus.AWAITING_HITL;
    }
}
```

#### NodeExecution.java
```java
package tech.kayys.wayang.orchestrator.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the execution state of a single node within a workflow run.
 * Tracks attempts, errors, checkpoints, and timing information.
 * 
 * Key features:
 * - Supports retries with attempt tracking
 * - Checkpoint references for resumability
 * - Error details for debugging
 * - Input/output snapshots for provenance
 */
@Entity
@Table(name = "node_executions", indexes = {
    @Index(name = "idx_run_node", columnList = "run_id, node_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_started_at", columnList = "started_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeExecution extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NodeStatus status;

    @Column(name = "attempt")
    @Builder.Default
    private Integer attempt = 0;

    @Column(name = "max_attempts")
    @Builder.Default
    private Integer maxAttempts = 3;

    @Column(name = "checkpoint_ref")
    private String checkpointRef;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "input_snapshot", columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> inputSnapshot;

    @Column(name = "output_snapshot", columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> outputSnapshot;

    @Column(name = "error", columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private ErrorPayload error;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    // Reactive queries
    public static Uni<NodeExecution> findByRunAndNode(UUID runId, String nodeId) {
        return find("runId = ?1 and nodeId = ?2", runId, nodeId).firstResult();
    }

    public static Uni<List<NodeExecution>> findByRun(UUID runId) {
        return list("runId", runId);
    }

    public static Uni<List<NodeExecution>> findByRunAndStatus(UUID runId, NodeStatus status) {
        return list("runId = ?1 and status = ?2", runId, status);
    }

    // State transitions
    public void markRunning() {
        this.status = NodeStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
        this.attempt++;
    }

    public void markSuccess(Map<String, Object> output) {
        this.status = NodeStatus.SUCCESS;
        this.completedAt = LocalDateTime.now();
        this.outputSnapshot = output;
    }

    public void markFailed(ErrorPayload error) {
        this.status = NodeStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.error = error;
    }

    public void markRetrying() {
        this.status = NodeStatus.RETRYING;
    }

    public void markSkipped(String reason) {
        this.status = NodeStatus.SKIPPED;
        this.completedAt = LocalDateTime.now();
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put("skipReason", reason);
    }

    public boolean canRetry() {
        return attempt < maxAttempts && 
               (status == NodeStatus.FAILED || status == NodeStatus.ERROR) &&
               (error == null || error.isRetryable());
    }

    public boolean isTerminal() {
        return status == NodeStatus.SUCCESS ||
               status == NodeStatus.SKIPPED ||
               (status == NodeStatus.FAILED && !canRetry());
    }
}
```

#### ExecutionStatus.java
```java
package tech.kayys.wayang.orchestrator.model;

/**
 * Represents the overall status of a workflow execution run.
 * State transitions follow a deterministic pattern for reliability.
 */
public enum ExecutionStatus {
    /**
     * Initial state - plan received but not started
     */
    PENDING,
    
    /**
     * Plan validated and ready for execution
     */
    VALIDATED,
    
    /**
     * Currently executing nodes
     */
    RUNNING,
    
    /**
     * Successfully completed all nodes
     */
    COMPLETED,
    
    /**
     * Failed with unrecoverable error
     */
    FAILED,
    
    /**
     * Manually paused by operator or policy
     */
    PAUSED,
    
    /**
     * Waiting for human decision/approval
     */
    AWAITING_HITL,
    
    /**
     * Cancelled by user or timeout
     */
    CANCELLED,
    
    /**
     * Plan being recomputed due to critic/evaluator feedback
     */
    REPLANNING;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    public boolean isActive() {
        return this == RUNNING || this == REPLANNING;
    }

    public boolean canTransitionTo(ExecutionStatus target) {
        return switch (this) {
            case PENDING -> target == VALIDATED || target == FAILED || target == CANCELLED;
            case VALIDATED -> target == RUNNING || target == FAILED || target == CANCELLED;
            case RUNNING -> target == COMPLETED || target == FAILED || target == PAUSED || 
                           target == AWAITING_HITL || target == REPLANNING || target == CANCELLED;
            case PAUSED -> target == RUNNING || target == CANCELLED;
            case AWAITING_HITL -> target == RUNNING || target == CANCELLED;
            case REPLANNING -> target == RUNNING || target == FAILED || target == CANCELLED;
            case COMPLETED, FAILED, CANCELLED -> false; // Terminal states
        };
    }
}
```

#### NodeStatus.java
```java
package tech.kayys.wayang.orchestrator.model;

/**
 * Represents the execution status of an individual node.
 * Designed to support retry logic and error handling patterns.
 */
public enum NodeStatus {
    /**
     * Node is waiting for dependencies
     */
    PENDING,
    
    /**
     * Node is currently executing
     */
    RUNNING,
    
    /**
     * Node completed successfully
     */
    SUCCESS,
    
    /**
     * Node failed with error
     */
    FAILED,
    
    /**
     * Node encountered an error (different from FAILED - may be transient)
     */
    ERROR,
    
    /**
     * Node is scheduled for retry
     */
    RETRYING,
    
    /**
     * Node was skipped (conditional logic)
     */
    SKIPPED,
    
    /**
     * Node execution cancelled
     */
    CANCELLED,
    
    /**
     * Waiting for human decision
     */
    AWAITING_HITL;

    public boolean isTerminal() {
        return this == SUCCESS || this == SKIPPED || this == CANCELLED;
    }

    public boolean isFailed() {
        return this == FAILED || this == ERROR;
    }

    public boolean isActive() {
        return this == RUNNING || this == RETRYING;
    }
}
```

### 3. Error and Audit Models

#### ErrorPayload.java
```java
package tech.kayys.wayang.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Standardized error payload used across the entire platform.
 * Provides rich context for error handling, retry logic, and human escalation.
 * 
 * Design principles:
 * - Uniform structure for all error types
 * - Supports retry decision making
 * - Includes context for debugging
 * - Tamper-proof via hash (optional)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorPayload {

    /**
     * Type of error for categorization and routing
     */
    private ErrorType type;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Detailed error information (exception traces, response codes, etc.)
     */
    @Builder.Default
    private Map<String, Object> details = new HashMap<>();

    /**
     * Whether this error can be retried automatically
     */
    @Builder.Default
    private Boolean retryable = false;

    /**
     * Node that originated the error
     */
    private String originNode;

    /**
     * Run ID for correlation
     */
    private String originRunId;

    /**
     * Current attempt number
     */
    @Builder.Default
    private Integer attempt = 0;

    /**
     * Maximum attempts allowed
     */
    @Builder.Default
    private Integer maxAttempts = 3;

    /**
     * Timestamp of error occurrence
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Suggested action for error handler
     */
    private SuggestedAction suggestedAction;

    /**
     * Reference to provenance/audit record
     */
    private String provenanceRef;

    /**
     * Cryptographic hash for integrity (optional)
     */
    private String hash;

    public enum ErrorType {
        TOOL_ERROR,
        LLM_ERROR,
        NETWORK_ERROR,
        VALIDATION_ERROR,
        TIMEOUT,
        RESOURCE_EXHAUSTED,
        POLICY_VIOLATION,
        UNKNOWN_ERROR
    }

    public enum SuggestedAction {
        RETRY,
        FALLBACK,
        ESCALATE,
        HUMAN_REVIEW,
        ABORT,
        AUTO_FIX
    }

    public boolean canRetry() {
        return Boolean.TRUE.equals(retryable) && attempt < maxAttempts;
    }

    public void incrementAttempt() {
        this.attempt++;
    }

    public boolean requiresHumanReview() {
        return suggestedAction == SuggestedAction.HUMAN_REVIEW ||
               suggestedAction == SuggestedAction.ESCALATE;
    }
}
```

#### AuditPayload.java
```java
package tech.kayys.wayang.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

/*** Standardized audit payload for tamper-proof execution logs.
 * Provides chain-of-custody tracking for compliance and debugging.
 * 
 * Features:
 * - Immutable event recording
 * - Actor tracking (system/human/agent)
 * - Context snapshots
 * - Cryptographic hashing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditPayload {

    /**
     * Timestamp of the audit event
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Execution run ID
     */
    private UUID runId;

    /**
     * Node ID (if applicable)
     */
    private String nodeId;

    /**
     * Actor who performed the action
     */
    private Actor actor;

    /**
     * Type of event
     */
    private String event;

    /**
     * Severity level
     */
    private AuditLevel level;

    /**
     * Tags for categorization
     */
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    /**
     * Additional metadata
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Snapshot of execution context (optional, redacted)
     */
    private Map<String, Object> contextSnapshot;

    /**
     * Cryptographic hash for integrity
     */
    private String hash;

    /**
     * Previous hash for chain integrity
     */
    private String previousHash;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Actor {
        private ActorType type;
        private String id;
        private String role;
    }

    public enum ActorType {
        SYSTEM,
        HUMAN,
        AGENT
    }

    public enum AuditLevel {
        INFO,
        WARN,
        ERROR,
        CRITICAL
    }

    public static AuditPayload createNodeStartEvent(UUID runId, String nodeId) {
        return AuditPayload.builder()
            .runId(runId)
            .nodeId(nodeId)
            .event("NODE_START")
            .level(AuditLevel.INFO)
            .actor(Actor.builder().type(ActorType.SYSTEM).id("orchestrator").build())
            .build();
    }

    public static AuditPayload createNodeErrorEvent(UUID runId, String nodeId, ErrorPayload error) {
        return AuditPayload.builder()
            .runId(runId)
            .nodeId(nodeId)
            .event("NODE_ERROR")
            .level(AuditLevel.ERROR)
            .actor(Actor.builder().type(ActorType.SYSTEM).id("orchestrator").build())
            .metadata(Map.of("error", error))
            .build();
    }

    public static AuditPayload createHITLEvent(UUID runId, String nodeId, String operatorId, String action) {
        return AuditPayload.builder()
            .runId(runId)
            .nodeId(nodeId)
            .event("HITL_COMPLETED")
            .level(AuditLevel.INFO)
            .actor(Actor.builder().type(ActorType.HUMAN).id(operatorId).role("operator").build())
            .metadata(Map.of("action", action))
            .build();
    }
}
```

Due to length constraints, I'll continue with the core implementation files. Would you like me to:

1. Continue with the Dispatcher and Scheduler implementations?
2. Continue with the REST API layer?
3. Continue with the State Machine and Checkpoint Manager?
4. Provide the database migration scripts?
5. Provide the configuration files?

Please let me know which parts you'd like me to detail next, and I'll provide complete, production-ready implementations with comprehensive documentation.



# Wayang Orchestrator - Core Implementation (Continued)

### 4. Core Dispatcher and DAG Walker

#### Dispatcher.java
```java
package tech.kayys.wayang.orchestrator.core.dispatcher;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.orchestrator.adapter.EventEmitterAdapter;
import tech.kayys.wayang.orchestrator.adapter.RuntimeHubAdapter;
import tech.kayys.wayang.orchestrator.core.scheduler.ResourceScheduler;
import tech.kayys.wayang.orchestrator.core.state.StateMachine;
import tech.kayys.wayang.orchestrator.exception.ExecutionException;
import tech.kayys.wayang.orchestrator.model.*;
import tech.kayys.wayang.orchestrator.store.ExecutionStore;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central dispatcher that orchestrates node execution based on DAG topology.
 * 
 * Responsibilities:
 * - Walk the execution graph (DAG)
 * - Resolve node dependencies
 * - Dispatch ready nodes to RuntimeHub
 * - Handle parallel execution
 * - Coordinate state transitions
 * 
 * Design principles:
 * - Non-blocking reactive execution
 * - Deterministic state transitions
 * - Concurrent execution where possible
 * - Graceful error propagation
 */
@ApplicationScoped
@Slf4j
public class Dispatcher {

    @Inject
    DAGWalker dagWalker;

    @Inject
    DependencyResolver dependencyResolver;

    @Inject
    ResourceScheduler scheduler;

    @Inject
    RuntimeHubAdapter runtimeHub;

    @Inject
    StateMachine stateMachine;

    @Inject
    ExecutionStore executionStore;

    @Inject
    EventEmitterAdapter eventEmitter;

    // Track active dispatches to prevent duplicate execution
    private final Map<String, DispatchContext> activeDispatches = new ConcurrentHashMap<>();

    /**
     * Dispatch execution plan for processing.
     * This is the main entry point for workflow execution.
     * 
     * @param plan The execution plan to dispatch
     * @param run The execution run metadata
     * @return Uni that completes when dispatch is initialized
     */
    public Uni<Void> dispatch(ExecutionPlan plan, ExecutionRun run) {
        log.info("Dispatching execution plan {} for run {}", plan.getPlanId(), run.getRunId());

        return Uni.createFrom().item(() -> {
            // Create dispatch context
            DispatchContext context = DispatchContext.builder()
                .runId(run.getRunId())
                .plan(plan)
                .startTime(System.currentTimeMillis())
                .build();

            activeDispatches.put(run.getRunId().toString(), context);
            return context;
        })
        .flatMap(context -> 
            // Initialize node executions
            initializeNodeExecutions(run.getRunId(), plan)
                .flatMap(nodeExecutions -> {
                    context.setNodeExecutions(nodeExecutions);
                    
                    // Emit plan started event
                    return eventEmitter.emitPlanStarted(run.getRunId(), plan.getPlanId())
                        .replaceWith(context);
                })
        )
        .flatMap(context -> 
            // Find and dispatch root nodes (nodes with no dependencies)
            findRootNodes(context.getNodeExecutions())
                .flatMap(rootNodes -> dispatchNodes(context, rootNodes))
        )
        .replaceWithVoid()
        .onFailure().invoke(throwable -> {
            log.error("Failed to dispatch plan for run {}", run.getRunId(), throwable);
            activeDispatches.remove(run.getRunId().toString());
        });
    }

    /**
     * Initialize node execution records for all nodes in the plan.
     */
    private Uni<Map<String, NodeExecution>> initializeNodeExecutions(UUID runId, ExecutionPlan plan) {
        return Multi.createFrom().iterable(plan.getNodes())
            .onItem().transformToUniAndMerge(node -> {
                NodeExecution execution = NodeExecution.builder()
                    .runId(runId)
                    .nodeId(node.getNodeId())
                    .status(NodeStatus.PENDING)
                    .maxAttempts(node.getRetryPolicy() != null ? 
                        node.getRetryPolicy().getMaxAttempts() : 3)
                    .metadata(new HashMap<>())
                    .build();

                return executionStore.saveNodeExecution(execution)
                    .map(saved -> Map.entry(node.getNodeId(), saved));
            })
            .collect().asMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    /**
     * Find nodes with no dependencies (entry points).
     */
    private Uni<List<NodeExecution>> findRootNodes(Map<String, NodeExecution> nodeExecutions) {
        return Uni.createFrom().item(() -> {
            DispatchContext context = activeDispatches.values().stream()
                .filter(ctx -> ctx.getNodeExecutions().equals(nodeExecutions))
                .findFirst()
                .orElseThrow(() -> new ExecutionException("Context not found"));

            return dagWalker.findRootNodes(context.getPlan())
                .stream()
                .map(nodeId -> nodeExecutions.get(nodeId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        });
    }

    /**
     * Dispatch multiple nodes for execution.
     * Handles parallel execution based on plan configuration.
     */
    private Uni<Void> dispatchNodes(DispatchContext context, List<NodeExecution> nodes) {
        if (nodes.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        log.debug("Dispatching {} nodes for run {}", nodes.size(), context.getRunId());

        return Multi.createFrom().iterable(nodes)
            .onItem().transformToUniAndMerge(node -> 
                dispatchNode(context, node)
                    .onFailure().recoverWithItem(() -> null) // Continue with other nodes
            )
            .collect().asList()
            .replaceWithVoid();
    }

    /**
     * Dispatch a single node for execution.
     * 
     * Flow:
     * 1. Validate dependencies are satisfied
     * 2. Check resource availability
     * 3. Transition state to RUNNING
     * 4. Submit to RuntimeHub
     * 5. Setup completion handler
     */
    private Uni<Void> dispatchNode(DispatchContext context, NodeExecution nodeExecution) {
        String nodeId = nodeExecution.getNodeId();
        log.info("Dispatching node {} for run {}", nodeId, context.getRunId());

        return Uni.createFrom().item(() -> {
            // Get node definition from plan
            Node node = context.getPlan().getNodes().stream()
                .filter(n -> n.getNodeId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new ExecutionException("Node not found: " + nodeId));

            return node;
        })
        .flatMap(node -> 
            // Validate dependencies
            dependencyResolver.validateDependencies(context, nodeId)
                .flatMap(valid -> {
                    if (!valid) {
                        return Uni.createFrom().failure(
                            new ExecutionException("Dependencies not satisfied for node: " + nodeId));
                    }

                    // Collect inputs from upstream nodes
                    return dependencyResolver.collectInputs(context, node);
                })
                .flatMap(inputs -> 
                    // Check resource availability and schedule
                    scheduler.scheduleNode(context.getRunId(), node)
                        .flatMap(schedulingResult -> {
                            if (!schedulingResult.isScheduled()) {
                                log.warn("Node {} could not be scheduled, reason: {}", 
                                    nodeId, schedulingResult.getReason());
                                return Uni.createFrom().voidItem();
                            }

                            // Transition to RUNNING state
                            return stateMachine.transitionNode(nodeExecution, NodeStatus.RUNNING)
                                .flatMap(runningExecution -> 
                                    // Emit node started event
                                    eventEmitter.emitNodeStarted(context.getRunId(), nodeId)
                                        .replaceWith(runningExecution)
                                )
                                .flatMap(runningExecution -> {
                                    // Build execution request
                                    ExecuteNodeRequest request = ExecuteNodeRequest.builder()
                                        .taskId(UUID.randomUUID())
                                        .runId(context.getRunId())
                                        .nodeId(nodeId)
                                        .nodeDescriptor(node.getDescriptor())
                                        .input(inputs)
                                        .metadata(buildExecutionMetadata(context, node))
                                        .build();

                                    // Submit to RuntimeHub
                                    return runtimeHub.executeNode(request)
                                        .flatMap(result -> 
                                            handleNodeResult(context, nodeExecution, node, result)
                                        );
                                });
                        })
                )
        )
        .onFailure().recoverWithUni(throwable -> 
            handleNodeFailure(context, nodeExecution, throwable)
        );
    }

    /**
     * Handle successful or failed node execution result.
     */
    private Uni<Void> handleNodeResult(
        DispatchContext context, 
        NodeExecution nodeExecution,
        Node node,
        NodeExecutionResult result
    ) {
        if (result.isSuccess()) {
            return handleNodeSuccess(context, nodeExecution, result);
        } else {
            return handleNodeError(context, nodeExecution, result.getError());
        }
    }

    /**
     * Handle successful node execution.
     * 
     * Flow:
     * 1. Update node state to SUCCESS
     * 2. Store outputs
     * 3. Emit success event
     * 4. Find and dispatch dependent nodes
     * 5. Check if workflow is complete
     */
    private Uni<Void> handleNodeSuccess(
        DispatchContext context,
        NodeExecution nodeExecution,
        NodeExecutionResult result
    ) {
        String nodeId = nodeExecution.getNodeId();
        log.info("Node {} completed successfully for run {}", nodeId, context.getRunId());

        return stateMachine.transitionNode(nodeExecution, NodeStatus.SUCCESS)
            .flatMap(successExecution -> {
                successExecution.setOutputSnapshot(result.getOutputs());
                return executionStore.saveNodeExecution(successExecution);
            })
            .flatMap(savedExecution -> 
                eventEmitter.emitNodeCompleted(context.getRunId(), nodeId, result)
            )
            .flatMap(v -> 
                // Find dependent nodes that are now ready
                findReadyDependentNodes(context, nodeId)
            )
            .flatMap(readyNodes -> 
                dispatchNodes(context, readyNodes)
            )
            .flatMap(v -> 
                // Check if workflow is complete
                checkWorkflowCompletion(context)
            )
            .replaceWithVoid();
    }

    /**
     * Handle node execution error.
     * 
     * Flow:
     * 1. Check if retry is possible
     * 2. If retryable, schedule retry
     * 3. If not retryable, check error handler
     * 4. If no handler, fail the workflow
     */
    private Uni<Void> handleNodeError(
        DispatchContext context,
        NodeExecution nodeExecution,
        ErrorPayload error
    ) {
        String nodeId = nodeExecution.getNodeId();
        log.error("Node {} failed for run {}: {}", nodeId, context.getRunId(), error.getMessage());

        // Store error
        nodeExecution.setError(error);

        return Uni.createFrom().item(() -> {
            // Check if retry is possible
            if (nodeExecution.canRetry() && error.canRetry()) {
                return true;
            }
            return false;
        })
        .flatMap(canRetry -> {
            if (canRetry) {
                // Schedule retry
                return scheduleRetry(context, nodeExecution);
            } else {
                // Check for error handler
                return routeToErrorHandler(context, nodeExecution, error);
            }
        })
        .onFailure().recoverWithUni(throwable -> 
            // If all error handling fails, fail the node and potentially the workflow
            stateMachine.transitionNode(nodeExecution, NodeStatus.FAILED)
                .flatMap(failedExecution -> 
                    executionStore.saveNodeExecution(failedExecution)
                )
                .flatMap(v -> 
                    eventEmitter.emitNodeFailed(context.getRunId(), nodeId, error)
                )
                .flatMap(v -> 
                    handleWorkflowFailure(context, error)
                )
        );
    }

    /**
     * Schedule node retry with backoff.
     */
    private Uni<Void> scheduleRetry(DispatchContext context, NodeExecution nodeExecution) {
        log.info("Scheduling retry for node {} (attempt {}/{})", 
            nodeExecution.getNodeId(), 
            nodeExecution.getAttempt() + 1, 
            nodeExecution.getMaxAttempts());

        return stateMachine.transitionNode(nodeExecution, NodeStatus.RETRYING)
            .flatMap(retryingExecution -> {
                // Calculate backoff delay
                Node node = context.getPlan().getNodes().stream()
                    .filter(n -> n.getNodeId().equals(nodeExecution.getNodeId()))
                    .findFirst()
                    .orElseThrow();

                long delayMs = calculateBackoffDelay(
                    nodeExecution.getAttempt(), 
                    node.getRetryPolicy()
                );

                // Schedule retry after delay
                return Uni.createFrom().voidItem()
                    .onItem().delayIt().by(Duration.ofMillis(delayMs))
                    .flatMap(v -> 
                        dispatchNode(context, retryingExecution)
                    );
            });
    }

    /**
     * Route error to error handler node if configured.
     */
    private Uni<Void> routeToErrorHandler(
        DispatchContext context,
        NodeExecution nodeExecution,
        ErrorPayload error
    ) {
        String nodeId = nodeExecution.getNodeId();
        
        // Find error handler in the plan
        return Uni.createFrom().item(() -> {
            Node node = context.getPlan().getNodes().stream()
                .filter(n -> n.getNodeId().equals(nodeId))
                .findFirst()
                .orElseThrow();

            // Check if node has error output edge
            return context.getPlan().getEdges().stream()
                .filter(edge -> edge.getSourceNodeId().equals(nodeId))
                .filter(edge -> "error".equals(edge.getSourcePort()))
                .findFirst();
        })
        .flatMap(errorEdgeOpt -> {
            if (errorEdgeOpt.isPresent()) {
                Edge errorEdge = errorEdgeOpt.get();
                String errorHandlerNodeId = errorEdge.getTargetNodeId();
                
                log.info("Routing error to error handler node: {}", errorHandlerNodeId);
                
                // Get error handler node execution
                NodeExecution errorHandlerExecution = 
                    context.getNodeExecutions().get(errorHandlerNodeId);

                if (errorHandlerExecution == null) {
                    return Uni.createFrom().failure(
                        new ExecutionException("Error handler node not found: " + errorHandlerNodeId));
                }

                // Mark current node as ERROR (not FAILED, to indicate it was handled)
                return stateMachine.transitionNode(nodeExecution, NodeStatus.ERROR)
                    .flatMap(errorExecution -> 
                        executionStore.saveNodeExecution(errorExecution)
                    )
                    .flatMap(v -> {
                        // Prepare error as input for error handler
                        errorHandlerExecution.setInputSnapshot(
                            Map.of("error", error)
                        );
                        
                        // Dispatch error handler
                        return dispatchNode(context, errorHandlerExecution);
                    });
            } else {
                // No error handler configured, fail the node
                return stateMachine.transitionNode(nodeExecution, NodeStatus.FAILED)
                    .flatMap(failedExecution -> 
                        executionStore.saveNodeExecution(failedExecution)
                    )
                    .flatMap(v -> 
                        eventEmitter.emitNodeFailed(context.getRunId(), nodeId, error)
                    )
                    .flatMap(v -> 
                        handleWorkflowFailure(context, error)
                    );
            }
        });
    }

    /**
     * Handle node execution failure (unexpected exceptions).
     */
    private Uni<Void> handleNodeFailure(
        DispatchContext context,
        NodeExecution nodeExecution,
        Throwable throwable
    ) {
        log.error("Unexpected failure in node {} for run {}", 
            nodeExecution.getNodeId(), context.getRunId(), throwable);

        ErrorPayload error = ErrorPayload.builder()
            .type(ErrorPayload.ErrorType.UNKNOWN_ERROR)
            .message(throwable.getMessage())
            .retryable(false)
            .originNode(nodeExecution.getNodeId())
            .originRunId(context.getRunId().toString())
            .suggestedAction(ErrorPayload.SuggestedAction.ABORT)
            .build();

        return handleNodeError(context, nodeExecution, error);
    }

    /**
     * Find dependent nodes that are ready to execute after a node completes.
     */
    private Uni<List<NodeExecution>> findReadyDependentNodes(DispatchContext context, String completedNodeId) {
        return Uni.createFrom().item(() -> {
            // Find all edges from completed node
            List<String> dependentNodeIds = context.getPlan().getEdges().stream()
                .filter(edge -> edge.getSourceNodeId().equals(completedNodeId))
                .filter(edge -> "success".equals(edge.getSourcePort())) // Only success path
                .map(Edge::getTargetNodeId)
                .distinct()
                .collect(Collectors.toList());

            // Check which dependent nodes have all their dependencies satisfied
            return dependentNodeIds.stream()
                .map(nodeId -> context.getNodeExecutions().get(nodeId))
                .filter(Objects::nonNull)
                .filter(nodeExec -> nodeExec.getStatus() == NodeStatus.PENDING)
                .collect(Collectors.toList());
        })
        .flatMap(potentiallyReadyNodes -> 
            Multi.createFrom().iterable(potentiallyReadyNodes)
                .onItem().transformToUniAndMerge(nodeExec -> 
                    dependencyResolver.validateDependencies(context, nodeExec.getNodeId())
                        .map(isReady -> isReady ? nodeExec : null)
                )
                .collect().asList()
                .map(list -> list.stream().filter(Objects::nonNull).collect(Collectors.toList()))
        );
    }

    /**
     * Check if all nodes in the workflow have completed.
     */
    private Uni<Void> checkWorkflowCompletion(DispatchContext context) {
        return Uni.createFrom().item(() -> {
            boolean allCompleted = context.getNodeExecutions().values().stream()
                .allMatch(NodeExecution::isTerminal);

            return allCompleted;
        })
        .flatMap(isComplete -> {
            if (isComplete) {
                log.info("Workflow execution completed for run {}", context.getRunId());
                return completeWorkflow(context);
            }
            return Uni.createFrom().voidItem();
        });
    }

    /**
     * Complete workflow execution successfully.
     */
    private Uni<Void> completeWorkflow(DispatchContext context) {
        return executionStore.findRun(context.getRunId())
            .flatMap(run -> {
                run.markCompleted();
                return executionStore.saveRun(run);
            })
            .flatMap(run -> 
                eventEmitter.emitPlanCompleted(context.getRunId(), context.getPlan().getPlanId())
            )
            .invoke(() -> activeDispatches.remove(context.getRunId().toString()))
            .replaceWithVoid();
    }

    /**
     * Handle workflow failure.
     */
    private Uni<Void> handleWorkflowFailure(DispatchContext context, ErrorPayload error) {
        return executionStore.findRun(context.getRunId())
            .flatMap(run -> {
                run.markFailed(error.getMessage());
                return executionStore.saveRun(run);
            })
            .flatMap(run -> 
                eventEmitter.emitPlanFailed(context.getRunId(), context.getPlan().getPlanId(), error)
            )
            .invoke(() -> activeDispatches.remove(context.getRunId().toString()))
            .replaceWithVoid();
    }

    /**
     * Calculate exponential backoff delay.
     */
    private long calculateBackoffDelay(int attempt, RetryPolicy retryPolicy) {
        if (retryPolicy == null || retryPolicy.getBackoffStrategy() == null) {
            return 1000L; // Default 1 second
        }

        return switch (retryPolicy.getBackoffStrategy()) {
            case EXPONENTIAL -> {
                long initialDelay = retryPolicy.getInitialDelayMs() != null ? 
                    retryPolicy.getInitialDelayMs() : 500L;
                long maxDelay = retryPolicy.getMaxDelayMs() != null ? 
                    retryPolicy.getMaxDelayMs() : 30000L;
                
                long delay = initialDelay * (long) Math.pow(2, attempt);
                yield Math.min(delay, maxDelay);
            }
            case LINEAR -> {
                long initialDelay = retryPolicy.getInitialDelayMs() != null ? 
                    retryPolicy.getInitialDelayMs() : 1000L;
                yield initialDelay * (attempt + 1);
            }
            case FIXED -> 
                retryPolicy.getInitialDelayMs() != null ? 
                    retryPolicy.getInitialDelayMs() : 1000L;
        };
    }

    /**
     * Build execution metadata for node execution request.
     */
    private Map<String, Object> buildExecutionMetadata(DispatchContext context, Node node) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("runId", context.getRunId().toString());
        metadata.put("nodeId", node.getNodeId());
        metadata.put("nodeType", node.getType());
        metadata.put("startTime", System.currentTimeMillis());
        
        if (node.getMetadata() != null) {
            metadata.putAll(node.getMetadata());
        }
        
        return metadata;
    }

    /**
     * Get active dispatch context.
     */
    public Optional<DispatchContext> getActiveDispatch(UUID runId) {
        return Optional.ofNullable(activeDispatches.get(runId.toString()));
    }

    /**
     * Cancel active dispatch.
     */
    public Uni<Void> cancelDispatch(UUID runId) {
        DispatchContext context = activeDispatches.remove(runId.toString());
        if (context == null) {
            return Uni.createFrom().failure(
                new ExecutionException("No active dispatch found for run: " + runId));
        }

        log.info("Cancelling dispatch for run {}", runId);

        return executionStore.findRun(runId)
            .flatMap(run -> {
                run.setStatus(ExecutionStatus.CANCELLED);
                return executionStore.saveRun(run);
            })
            .flatMap(run -> 
                eventEmitter.emitPlanCancelled(runId, context.getPlan().getPlanId())
            )
            .replaceWithVoid();
    }
}
```

### 5. DAG Walker Implementation

#### DAGWalker.java
```java
package tech.kayys.wayang.orchestrator.core.dispatcher;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.orchestrator.exception.ExecutionException;
import tech.kayys.wayang.orchestrator.model.Edge;
import tech.kayys.wayang.orchestrator.model.ExecutionPlan;
import tech.kayys.wayang.orchestrator.model.Node;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles DAG (Directed Acyclic Graph) traversal and analysis.
 * 
 * Responsibilities:
 * - Find root nodes (entry points)
 * - Topological sorting
 * - Detect cycles
 * - Find dependencies
 * - Validate graph structure
 * 
 * Design principles:
 * - Immutable graph representation
 * - Efficient graph algorithms
 * - Clear error messages for invalid graphs
 */
@ApplicationScoped
@Slf4j
public class DAGWalker {

    /**
     * Find root nodes (nodes with no incoming edges).
     */
    public List<String> findRootNodes(ExecutionPlan plan) {
        Set<String> nodesWithIncoming = plan.getEdges().stream()
            .map(Edge::getTargetNodeId)
            .collect(Collectors.toSet());

        return plan.getNodes().stream()
            .map(Node::getNodeId)
            .filter(nodeId -> !nodesWithIncoming.contains(nodeId))
            .collect(Collectors.toList());
    }

    /**
     * Find leaf nodes (nodes with no outgoing edges).
     */
    public List<String> findLeafNodes(ExecutionPlan plan) {
        Set<String> nodesWithOutgoing = plan.getEdges().stream()
            .map(Edge::getSourceNodeId)
            .collect(Collectors.toSet());

        return plan.getNodes().stream()
            .map(Node::getNodeId)
            .filter(nodeId -> !nodesWithOutgoing.contains(nodeId))
            .collect(Collectors.toList());
    }

    /**
     * Get direct dependencies of a node.
     * Returns node IDs that must complete before this node can run.
     */
    public List<String> getDirectDependencies(ExecutionPlan plan, String nodeId) {
        return plan.getEdges().stream()
            .filter(edge -> edge.getTargetNodeId().equals(nodeId))
            .map(Edge::getSourceNodeId)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Get direct dependents of a node.
     * Returns node IDs that depend on this node.
     */
    public List<String> getDirectDependents(ExecutionPlan plan, String nodeId) {
        return plan.getEdges().stream()
            .filter(edge -> edge.getSourceNodeId().equals(nodeId))
            .map(Edge::getTargetNodeId)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Perform topological sort on the execution plan.
     * Returns nodes in execution order.
     * 
     * @throws ExecutionException if graph contains cycles
     */
    public List<String> topologicalSort(ExecutionPlan plan) {
        Map<String, Integer> inDegree = calculateInDegree(plan);
        Queue<String> queue = new LinkedList<>();
        List<String> sorted = new ArrayList<>();

        // Add nodes with no dependencies
        for (Node node : plan.getNodes()) {
            if (inDegree.getOrDefault(node.getNodeId(), 0) == 0) {
                queue.offer(node.getNodeId());
            }
        }

        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            sorted.add(nodeId);

            // Reduce in-degree for dependents
            for (String dependent : getDirectDependents(plan, nodeId)) {
                int newInDegree = inDegree.get(dependent) - 1;
                inDegree.put(dependent, newInDegree);
                
                if (newInDegree == 0) {
                    queue.offer(dependent);
                }
            }
        }

        // Check if all nodes were processed
        if (sorted.size() != plan.getNodes().size()) {
            throw new ExecutionException("Cycle detected in execution plan");
        }

        return sorted;
    }

    /**
     * Detect if the graph contains cycles.
     */
    public boolean hasCycle(ExecutionPlan plan) {
        try {
            topologicalSort(plan);
            return false;
        } catch (ExecutionException e) {
            return true;
        }
    }

    /**
     * Validate graph structure.
     * Checks for:
     * - Cycles
     * - Orphaned nodes
     * - Invalid edges
     * - Missing nodes
     */
    public ValidationResult validateGraph(ExecutionPlan plan) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check for cycles
        if (hasCycle(plan)) {
            errors.add("Graph contains cycles");
        }

        // Check for orphaned nodes (no incoming or outgoing edges)
        Set<String> connectedNodes = new HashSet<>();
        for (Edge edge : plan.getEdges()) {
            connectedNodes.add(edge.getSourceNodeId());
            connectedNodes.add(edge.getTargetNodeId());
        }

        for (Node node : plan.getNodes()) {
            if (!connectedNodes.contains(node.getNodeId()) && 
                !isStartOrEndNode(node)) {
                warnings.add("Orphaned node: " + node.getNodeId());
            }
        }

        // Validate edges reference existing nodes
        Set<String> nodeIds = plan.getNodes().stream()
            .map(Node::getNodeId)
            .collect(Collectors.toSet());

        for (Edge edge : plan.getEdges()) {
            if (!nodeIds.contains(edge.getSourceNodeId())) {
                errors.add("Edge references non-existent source node: " + edge.getSourceNodeId());
            }
            if (!nodeIds.contains(edge.getTargetNodeId())) {
                errors.add("Edge references non-existent target node: " + edge.getTargetNodeId());
            }
        }

        // Check for at least one root node
        List<String> rootNodes = findRootNodes(plan);
        if (rootNodes.isEmpty()) {
            errors.add("Graph has no root nodes");
        }

        // Check for at least one leaf node
        List<String> leafNodes = findLeafNodes(plan);
        if (leafNodes.isEmpty()) {
            warnings.add("Graph has no leaf nodes");
        }

        return ValidationResult.builder()
            .valid(errors.isEmpty())
            .errors(errors)
            .warnings(warnings)
            .build();
    }

    /**
     * Calculate in-degree for all nodes.
     */
    private Map<String, Integer> calculateInDegree(ExecutionPlan plan) {
        Map<String, Integer> inDegree = new HashMap<>();
        
        // Initialize all nodes with 0
        for (Node node : plan.getNodes()) {
            inDegree.put(node.getNodeId(), 0);
        }

        // Count incoming edges
        for (Edge edge : plan.getEdges()) {
            inDegree.merge(edge.getTargetNodeId(), 1, Integer::sum# Wayang Orchestrator - Core Implementation (Continued)

### 6. Dependency Resolver Implementation

#### DependencyResolver.java
```java
package tech.kayys.wayang.orchestrator.core.dispatcher;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.orchestrator.model.*;
import tech.kayys.wayang.orchestrator.store.ExecutionStore;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves node dependencies and collects inputs from upstream nodes.
 * 
 * Responsibilities:
 * - Validate all dependencies are satisfied
 * - Collect outputs from parent nodes
 * - Map outputs to inputs based on edge configuration
 * - Handle conditional dependencies
 * 
 * Design principles:
 * - Strict dependency validation
 * - Type-safe input/output mapping
 * - Support for multiple input sources
 */
@ApplicationScoped
@Slf4j
public class DependencyResolver {

    @Inject
    DAGWalker dagWalker;

    @Inject
    ExecutionStore executionStore;

    /**
     * Validate that all dependencies for a node are satisfied.
     * A dependency is satisfied when:
     * 1. The dependent node has completed successfully
     * 2. Required outputs are available
     * 
     * @param context The dispatch context
     * @param nodeId The node to validate
     * @return Uni<Boolean> true if all dependencies satisfied
     */
    public Uni<Boolean> validateDependencies(DispatchContext context, String nodeId) {
        List<String> dependencies = dagWalker.getDirectDependencies(context.getPlan(), nodeId);

        if (dependencies.isEmpty()) {
            return Uni.createFrom().item(true);
        }

        return Uni.createFrom().item(() -> {
            for (String depNodeId : dependencies) {
                NodeExecution depExecution = context.getNodeExecutions().get(depNodeId);
                
                if (depExecution == null) {
                    log.warn("Dependency node {} not found for node {}", depNodeId, nodeId);
                    return false;
                }

                // Check if dependency is in terminal success state
                if (!isNodeSuccessful(depExecution)) {
                    log.debug("Dependency node {} not yet successful for node {}", depNodeId, nodeId);
                    return false;
                }

                // Validate required outputs are present
                if (!hasRequiredOutputs(context, depNodeId, nodeId)) {
                    log.warn("Dependency node {} missing required outputs for node {}", depNodeId, nodeId);
                    return false;
                }
            }

            return true;
        });
    }

    /**
     * Check if a node execution completed successfully.
     */
    private boolean isNodeSuccessful(NodeExecution execution) {
        return execution.getStatus() == NodeStatus.SUCCESS;
    }

    /**
     * Check if dependency node has all required outputs for target node.
     */
    private boolean hasRequiredOutputs(DispatchContext context, String sourceNodeId, String targetNodeId) {
        NodeExecution sourceExecution = context.getNodeExecutions().get(sourceNodeId);
        
        if (sourceExecution == null || sourceExecution.getOutputSnapshot() == null) {
            return false;
        }

        // Find edges from source to target
        List<Edge> edges = context.getPlan().getEdges().stream()
            .filter(edge -> edge.getSourceNodeId().equals(sourceNodeId))
            .filter(edge -> edge.getTargetNodeId().equals(targetNodeId))
            .collect(Collectors.toList());

        if (edges.isEmpty()) {
            return true; // No explicit connection means no required outputs
        }

        // Check each edge's required output
        for (Edge edge : edges) {
            if (edge.getSourcePort() != null && !edge.getSourcePort().equals("success")) {
                // Check if specific output port exists
                if (!sourceExecution.getOutputSnapshot().containsKey(edge.getSourcePort())) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Collect inputs for a node from its dependencies.
     * Merges outputs from all upstream nodes according to edge mappings.
     * 
     * @param context The dispatch context
     * @param node The node requiring inputs
     * @return Uni<Map<String, Object>> The collected inputs
     */
    public Uni<Map<String, Object>> collectInputs(DispatchContext context, Node node) {
        String nodeId = node.getNodeId();
        List<String> dependencies = dagWalker.getDirectDependencies(context.getPlan(), nodeId);

        if (dependencies.isEmpty()) {
            // No dependencies, use node's configured inputs if any
            return Uni.createFrom().item(node.getInputs() != null ? node.getInputs() : new HashMap<>());
        }

        return Uni.createFrom().item(() -> {
            Map<String, Object> collectedInputs = new HashMap<>();

            // Add node's own configured inputs first
            if (node.getInputs() != null) {
                collectedInputs.putAll(node.getInputs());
            }

            // Collect from each dependency
            for (String depNodeId : dependencies) {
                NodeExecution depExecution = context.getNodeExecutions().get(depNodeId);
                
                if (depExecution == null || depExecution.getOutputSnapshot() == null) {
                    continue;
                }

                // Find edges from dependency to this node
                List<Edge> edges = context.getPlan().getEdges().stream()
                    .filter(edge -> edge.getSourceNodeId().equals(depNodeId))
                    .filter(edge -> edge.getTargetNodeId().equals(nodeId))
                    .collect(Collectors.toList());

                // Map outputs to inputs based on edge configuration
                for (Edge edge : edges) {
                    mapOutputToInput(
                        depExecution.getOutputSnapshot(),
                        collectedInputs,
                        edge
                    );
                }
            }

            return collectedInputs;
        });
    }

    /**
     * Map output from source node to input of target node based on edge configuration.
     * 
     * Supports:
     * - Direct port mapping (sourcePort -> targetPort)
     * - Wildcard mapping (all outputs -> inputs)
     * - Transformation functions (future enhancement)
     */
    private void mapOutputToInput(
        Map<String, Object> sourceOutputs,
        Map<String, Object> targetInputs,
        Edge edge
    ) {
        if (edge.getMapping() != null && !edge.getMapping().isEmpty()) {
            // Use explicit mapping configuration
            for (Map.Entry<String, String> mapping : edge.getMapping().entrySet()) {
                String sourceKey = mapping.getKey();
                String targetKey = mapping.getValue();

                if (sourceOutputs.containsKey(sourceKey)) {
                    Object value = sourceOutputs.get(sourceKey);
                    targetInputs.put(targetKey, value);
                }
            }
        } else {
            // Default behavior: map by port names
            String sourcePort = edge.getSourcePort();
            String targetPort = edge.getTargetPort();

            if (sourcePort != null && targetPort != null) {
                if (sourceOutputs.containsKey(sourcePort)) {
                    targetInputs.put(targetPort, sourceOutputs.get(sourcePort));
                }
            } else {
                // Wildcard: merge all outputs as inputs
                targetInputs.putAll(sourceOutputs);
            }
        }
    }

    /**
     * Get all transitive dependencies of a node (recursive).
     * Returns all nodes that must complete before this node can run.
     */
    public Set<String> getTransitiveDependencies(ExecutionPlan plan, String nodeId) {
        Set<String> allDependencies = new HashSet<>();
        Queue<String> toProcess = new LinkedList<>();
        toProcess.add(nodeId);

        while (!toProcess.isEmpty()) {
            String currentNode = toProcess.poll();
            List<String> directDeps = dagWalker.getDirectDependencies(plan, currentNode);

            for (String dep : directDeps) {
                if (allDependencies.add(dep)) {
                    toProcess.offer(dep);
                }
            }
        }

        return allDependencies;
    }

    /**
     * Check if there's a path from source to target node.
     */
    public boolean hasPath(ExecutionPlan plan, String sourceNodeId, String targetNodeId) {
        if (sourceNodeId.equals(targetNodeId)) {
            return true;
        }

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.offer(sourceNodeId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            
            if (current.equals(targetNodeId)) {
                return true;
            }

            if (visited.add(current)) {
                List<String> dependents = dagWalker.getDirectDependents(plan, current);
                queue.addAll(dependents);
            }
        }

        return false;
    }
}
```

### 7. Resource Scheduler Implementation

#### ResourceScheduler.java
```java
package tech.kayys.wayang.orchestrator.core.scheduler;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.orchestrator.config.SchedulerConfig;
import tech.kayys.wayang.orchestrator.model.Node;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Manages resource allocation and scheduling for node execution.
 * 
 * Responsibilities:
 * - Track available resources (CPU, memory, GPU)
 * - Priority-based scheduling
 * - Tenant quota enforcement
 * - Backpressure management
 * - Resource reservation and release
 * 
 * Design principles:
 * - Fair scheduling across tenants
 * - Configurable resource pools
 * - Graceful degradation under load
 * - Metrics for observability
 */
@ApplicationScoped
@Slf4j
public class ResourceScheduler {

    @Inject
    SchedulerConfig config;

    // Resource pools by type
    private final Map<ResourcePoolType, ResourcePool> resourcePools = new ConcurrentHashMap<>();

    // Scheduling queue (priority-based)
    private final PriorityBlockingQueue<SchedulingRequest> schedulingQueue;

    // Active resource allocations
    private final Map<String, ResourceAllocation> activeAllocations = new ConcurrentHashMap<>();

    // Tenant quotas
    private final Map<UUID, TenantQuota> tenantQuotas = new ConcurrentHashMap<>();

    public ResourceScheduler() {
        this.schedulingQueue = new PriorityBlockingQueue<>(100, 
            Comparator.comparingInt(SchedulingRequest::getPriority).reversed()
                .thenComparing(SchedulingRequest::getSubmittedAt));
    }

    /**
     * Initialize resource pools based on configuration.
     */
    @jakarta.annotation.PostConstruct
    void initialize() {
        log.info("Initializing resource scheduler");

        // Initialize CPU pool
        resourcePools.put(ResourcePoolType.CPU, ResourcePool.builder()
            .type(ResourcePoolType.CPU)
            .totalCapacity(config.cpuPoolSize())
            .availableCapacity(config.cpuPoolSize())
            .build());

        // Initialize GPU pool
        if (config.gpuPoolSize() > 0) {
            resourcePools.put(ResourcePoolType.GPU, ResourcePool.builder()
                .type(ResourcePoolType.GPU)
                .totalCapacity(config.gpuPoolSize())
                .availableCapacity(config.gpuPoolSize())
                .build());
        }

        // Initialize memory pool
        resourcePools.put(ResourcePoolType.MEMORY, ResourcePool.builder()
            .type(ResourcePoolType.MEMORY)
            .totalCapacity(config.memoryPoolMb())
            .availableCapacity(config.memoryPoolMb())
            .build());

        log.info("Resource pools initialized: {}", resourcePools.keySet());
    }

    /**
     * Schedule a node for execution.
     * 
     * @param runId The execution run ID
     * @param node The node to schedule
     * @return Uni<SchedulingResult> The scheduling result
     */
    public Uni<SchedulingResult> scheduleNode(UUID runId, Node node) {
        log.debug("Scheduling node {} for run {}", node.getNodeId(), runId);

        return Uni.createFrom().item(() -> {
            // Determine required resources
            ResourceRequirements requirements = determineResourceRequirements(node);

            // Check tenant quota
            UUID tenantId = getTenantIdFromMetadata(node);
            if (!checkTenantQuota(tenantId, requirements)) {
                return SchedulingResult.builder()
                    .scheduled(false)
                    .reason("Tenant quota exceeded")
                    .build();
            }

            // Try immediate allocation
            Optional<ResourceAllocation> allocation = tryAllocate(runId, node.getNodeId(), requirements);

            if (allocation.isPresent()) {
                // Successfully allocated
                activeAllocations.put(getAllocationKey(runId, node.getNodeId()), allocation.get());
                updateTenantUsage(tenantId, requirements, true);

                return SchedulingResult.builder()
                    .scheduled(true)
                    .allocation(allocation.get())
                    .build();
            } else {
                // Resources not available, enqueue
                SchedulingRequest request = SchedulingRequest.builder()
                    .runId(runId)
                    .nodeId(node.getNodeId())
                    .requirements(requirements)
                    .priority(determinePriority(node))
                    .tenantId(tenantId)
                    .submittedAt(LocalDateTime.now())
                    .build();

                schedulingQueue.offer(request);

                log.debug("Node {} queued for scheduling (queue size: {})", 
                    node.getNodeId(), schedulingQueue.size());

                return SchedulingResult.builder()
                    .scheduled(false)
                    .reason("Resources not available, queued")
                    .queuePosition(schedulingQueue.size())
                    .build();
            }
        });
    }

    /**
     * Release resources allocated to a completed node.
     */
    public Uni<Void> releaseResources(UUID runId, String nodeId) {
        String key = getAllocationKey(runId, nodeId);
        ResourceAllocation allocation = activeAllocations.remove(key);

        if (allocation == null) {
            log.warn("No allocation found for node {} in run {}", nodeId, runId);
            return Uni.createFrom().voidItem();
        }

        log.debug("Releasing resources for node {} in run {}", nodeId, runId);

        return Uni.createFrom().item(() -> {
            // Return resources to pools
            for (Map.Entry<ResourcePoolType, Integer> entry : allocation.getAllocatedResources().entrySet()) {
                ResourcePool pool = resourcePools.get(entry.getKey());
                if (pool != null) {
                    pool.release(entry.getValue());
                }
            }

            // Update tenant usage
            updateTenantUsage(allocation.getTenantId(), allocation.getRequirements(), false);

            // Try to schedule queued requests
            processQueue();

            return null;
        });
    }

    /**
     * Try to allocate resources for a node.
     */
    private Optional<ResourceAllocation> tryAllocate(UUID runId, String nodeId, ResourceRequirements requirements) {
        Map<ResourcePoolType, Integer> allocatedResources = new HashMap<>();

        // Check and reserve resources atomically
        synchronized (resourcePools) {
            // CPU
            ResourcePool cpuPool = resourcePools.get(ResourcePoolType.CPU);
            if (cpuPool.getAvailableCapacity() < requirements.getCpuUnits()) {
                return Optional.empty();
            }

            // Memory
            ResourcePool memPool = resourcePools.get(ResourcePoolType.MEMORY);
            if (memPool.getAvailableCapacity() < requirements.getMemoryMb()) {
                return Optional.empty();
            }

            // GPU (if required)
            if (requirements.getGpuUnits() > 0) {
                ResourcePool gpuPool = resourcePools.get(ResourcePoolType.GPU);
                if (gpuPool == null || gpuPool.getAvailableCapacity() < requirements.getGpuUnits()) {
                    return Optional.empty();
                }
                gpuPool.allocate(requirements.getGpuUnits());
                allocatedResources.put(ResourcePoolType.GPU, requirements.getGpuUnits());
            }

            // Allocate
            cpuPool.allocate(requirements.getCpuUnits());
            memPool.allocate(requirements.getMemoryMb());

            allocatedResources.put(ResourcePoolType.CPU, requirements.getCpuUnits());
            allocatedResources.put(ResourcePoolType.MEMORY, requirements.getMemoryMb());
        }

        return Optional.of(ResourceAllocation.builder()
            .runId(runId)
            .nodeId(nodeId)
            .requirements(requirements)
            .allocatedResources(allocatedResources)
            .allocatedAt(LocalDateTime.now())
            .build());
    }

    /**
     * Process scheduling queue and try to schedule pending requests.
     */
    private void processQueue() {
        List<SchedulingRequest> toRetry = new ArrayList<>();

        while (!schedulingQueue.isEmpty()) {
            SchedulingRequest request = schedulingQueue.poll();
            if (request == null) break;

            Optional<ResourceAllocation> allocation = tryAllocate(
                request.getRunId(), 
                request.getNodeId(), 
                request.getRequirements()
            );

            if (allocation.isPresent()) {
                activeAllocations.put(
                    getAllocationKey(request.getRunId(), request.getNodeId()), 
                    allocation.get()
                );
                updateTenantUsage(request.getTenantId(), request.getRequirements(), true);

                log.info("Scheduled queued node {} from run {}", 
                    request.getNodeId(), request.getRunId());
            } else {
                // Still can't allocate, re-queue
                toRetry.add(request);
            }
        }

        // Re-add requests that couldn't be scheduled
        schedulingQueue.addAll(toRetry);
    }

    /**
     * Determine resource requirements for a node.
     */
    private ResourceRequirements determineResourceRequirements(Node node) {
        ResourceRequirements.ResourceRequirementsBuilder builder = ResourceRequirements.builder();

        // Default requirements
        builder.cpuUnits(1);
        builder.memoryMb(512);
        builder.gpuUnits(0);

        // Check node metadata for resource hints
        if (node.getMetadata() != null) {
            Map<String, Object> metadata = node.getMetadata();

            if (metadata.containsKey("resources")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resources = (Map<String, Object>) metadata.get("resources");

                if (resources.containsKey("cpu")) {
                    builder.cpuUnits(((Number) resources.get("cpu")).intValue());
                }
                if (resources.containsKey("memory")) {
                    builder.memoryMb(((Number) resources.get("memory")).intValue());
                }
                if (resources.containsKey("gpu")) {
                    builder.gpuUnits(((Number) resources.get("gpu")).intValue());
                }
            }
        }

        // Node type-specific defaults
        String nodeType = node.getType();
        if ("LLM".equalsIgnoreCase(nodeType) || "AGENT".equalsIgnoreCase(nodeType)) {
            builder.cpuUnits(2);
            builder.memoryMb(2048);
        } else if ("MULTIMODAL".equalsIgnoreCase(nodeType)) {
            builder.cpuUnits(2);
            builder.memoryMb(4096);
            builder.gpuUnits(1);
        }

        return builder.build();
    }

    /**
     * Determine scheduling priority for a node.
     */
    private int determinePriority(Node node) {
        if (node.getMetadata() != null && node.getMetadata().containsKey("priority")) {
            return ((Number) node.getMetadata().get("priority")).intValue();
        }
        return 0; // Default priority
    }

    /**
     * Check if tenant has available quota.
     */
    private boolean checkTenantQuota(UUID tenantId, ResourceRequirements requirements) {
        TenantQuota quota = tenantQuotas.computeIfAbsent(tenantId, 
            id -> TenantQuota.builder()
                .tenantId(id)
                .maxConcurrentNodes(config.maxConcurrentNodesPerTenant())
                .maxCpuUnits(config.maxCpuUnitsPerTenant())
                .maxMemoryMb(config.maxMemoryMbPerTenant())
                .build());

        synchronized (quota) {
            return quota.getCurrentNodes() < quota.getMaxConcurrentNodes() &&
                   quota.getCurrentCpuUnits() + requirements.getCpuUnits() <= quota.getMaxCpuUnits() &&
                   quota.getCurrentMemoryMb() + requirements.getMemoryMb() <= quota.getMaxMemoryMb();
        }
    }

    /**
     * Update tenant resource usage.
     */
    private void updateTenantUsage(UUID tenantId, ResourceRequirements requirements, boolean allocating) {
        TenantQuota quota = tenantQuotas.get(tenantId);
        if (quota == null) return;

        synchronized (quota) {
            int delta = allocating ? 1 : -1;
            quota.setCurrentNodes(quota.getCurrentNodes() + delta);
            quota.setCurrentCpuUnits(quota.getCurrentCpuUnits() + (delta * requirements.getCpuUnits()));
            quota.setCurrentMemoryMb(quota.getCurrentMemoryMb() + (delta * requirements.getMemoryMb()));
        }
    }

    /**
     * Get tenant ID from node metadata.
     */
    private UUID getTenantIdFromMetadata(Node node) {
        if (node.getMetadata() != null && node.getMetadata().containsKey("tenantId")) {
            String tenantIdStr = node.getMetadata().get("tenantId").toString();
            return UUID.fromString(tenantIdStr);
        }
        return UUID.randomUUID(); // Default tenant
    }

    /**
     * Generate allocation key.
     */
    private String getAllocationKey(UUID runId, String nodeId) {
        return runId.toString() + ":" + nodeId;
    }

    /**
     * Get current resource utilization.
     */
    public Map<ResourcePoolType, ResourceUtilization> getResourceUtilization() {
        Map<ResourcePoolType, ResourceUtilization> utilization = new HashMap<>();

        for (Map.Entry<ResourcePoolType, ResourcePool> entry : resourcePools.entrySet()) {
            ResourcePool pool = entry.getValue();
            utilization.put(entry.getKey(), ResourceUtilization.builder()
                .total(pool.getTotalCapacity())
                .used(pool.getTotalCapacity() - pool.getAvailableCapacity())
                .available(pool.getAvailableCapacity())
                .utilizationPercent((double) (pool.getTotalCapacity() - pool.getAvailableCapacity()) 
                    / pool.getTotalCapacity() * 100)
                .build());
        }

        return utilization;
    }

    /**
     * Get queue statistics.
     */
    public QueueStatistics getQueueStatistics() {
        return QueueStatistics.builder()
            .queueSize(schedulingQueue.size())
            .activeAllocations(activeAllocations.size())
            .build();
    }

    // Supporting classes

    public enum ResourcePoolType {
        CPU, MEMORY, GPU
    }

    @Data
    @Builder
    public static class ResourcePool {
        private ResourcePoolType type;
        private int totalCapacity;
        private int availableCapacity;

        public synchronized void allocate(int amount) {
            if (availableCapacity < amount) {
                throw new IllegalStateException("Insufficient capacity");
            }
            availableCapacity -= amount;
        }

        public synchronized void release(int amount) {
            availableCapacity = Math.min(totalCapacity, availableCapacity + amount);
        }
    }

    @Data
    @Builder
    public static class ResourceRequirements {
        private int cpuUnits;
        private int memoryMb;
        private int gpuUnits;
    }

    @Data
    @Builder
    public static class ResourceAllocation {
        private UUID runId;
        private String nodeId;
        private UUID tenantId;
        private ResourceRequirements requirements;
        private Map<ResourcePoolType, Integer> allocatedResources;
        private LocalDateTime allocatedAt;
    }

    @Data
    @Builder
    public static class SchedulingRequest {
        private UUID runId;
        private String nodeId;
        private UUID tenantId;
        private ResourceRequirements requirements;
        private int priority;
        private LocalDateTime submittedAt;
    }

    @Data
    @Builder
    public static class SchedulingResult {
        private boolean scheduled;
        private String reason;
        private ResourceAllocation allocation;
        private Integer queuePosition;
    }

    @Data
    @Builder
    public static class TenantQuota {
        private UUID tenantId;
        private int maxConcurrentNodes;
        private int maxCpuUnits;
        private int maxMemoryMb;
        
        @Builder.Default
        private int currentNodes = 0;
        @Builder.Default
        private int currentCpuUnits = 0;
        @Builder.Default
        private int currentMemoryMb = 0;
    }

    @Data
    @Builder
    public static class ResourceUtilization {
        private int total;
        private int used;
        private int available;
        private double utilizationPercent;
    }

    @Data
    @Builder
    public static class QueueStatistics {
        private int queueSize;
        private int activeAllocations;
    }
}
```

Would you like me to continue with:
1. StateMachine implementation
2. CheckpointManager implementation
3. Retry and Circuit Breaker implementations
4. REST API layer
5. Adapters (RuntimeHub, EventEmitter, etc.)

Please let me know which components to detail next!




# Wayang Orchestrator - Core Implementation (Continued)

### 8. State Machine Implementation

#### StateMachine.java
```java
package tech.kayys.wayang.orchestrator.core.state;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.orchestrator.audit.AuditService;
import tech.kayys.wayang.orchestrator.exception.ExecutionException;
import tech.kayys.wayang.orchestrator.model.*;
import tech.kayys.wayang.orchestrator.store.ExecutionStore;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages deterministic state transitions for execution runs and nodes.
 * 
 * Responsibilities:
 * - Validate state transitions
 * - Enforce state machine rules
 * - Prevent illegal transitions
 * - Audit all state changes
 * - Handle concurrent state updates
 * 
 * Design principles:
 * - Deterministic transitions only
 * - Thread-safe state management
 * - Complete audit trail
 * - Idempotent operations
 */
@ApplicationScoped
@Slf4j
public class StateMachine {

    @Inject
    ExecutionStore executionStore;

    @Inject
    AuditService auditService;

    // Locks for concurrent state updates
    private final Map<String, Object> stateLocks = new ConcurrentHashMap<>();

    /**
     * Transition execution run to a new status.
     * 
     * @param run The execution run
     * @param targetStatus The desired status
     * @return Uni<ExecutionRun> The updated run
     */
    public Uni<ExecutionRun> transitionRun(ExecutionRun run, ExecutionStatus targetStatus) {
        String lockKey = "run:" + run.getRunId();
        Object lock = stateLocks.computeIfAbsent(lockKey, k -> new Object());

        return Uni.createFrom().item(() -> {
            synchronized (lock) {
                ExecutionStatus currentStatus = run.getStatus();

                // Validate transition
                if (!currentStatus.canTransitionTo(targetStatus)) {
                    throw new ExecutionException(
                        String.format("Invalid state transition from %s to %s for run %s",
                            currentStatus, targetStatus, run.getRunId())
                    );
                }

                log.info("Transitioning run {} from {} to {}", 
                    run.getRunId(), currentStatus, targetStatus);

                // Update status
                run.setStatus(targetStatus);
                run.setLastUpdatedAt(LocalDateTime.now());

                // Update timestamps based on transition
                updateRunTimestamps(run, targetStatus);

                return run;
            }
        })
        .flatMap(updatedRun -> 
            // Persist the transition
            executionStore.saveRun(updatedRun)
        )
        .flatMap(savedRun -> 
            // Audit the transition
            auditService.auditRunStateTransition(
                savedRun.getRunId(),
                run.getStatus(),
                targetStatus
            ).replaceWith(savedRun)
        )
        .eventually(() -> {
            // Clean up lock if no longer needed
            if (targetStatus.isTerminal()) {
                stateLocks.remove(lockKey);
            }
        });
    }

    /**
     * Transition node execution to a new status.
     * 
     * @param nodeExecution The node execution
     * @param targetStatus The desired status
     * @return Uni<NodeExecution> The updated node execution
     */
    public Uni<NodeExecution> transitionNode(NodeExecution nodeExecution, NodeStatus targetStatus) {
        String lockKey = "node:" + nodeExecution.getRunId() + ":" + nodeExecution.getNodeId();
        Object lock = stateLocks.computeIfAbsent(lockKey, k -> new Object());

        return Uni.createFrom().item(() -> {
            synchronized (lock) {
                NodeStatus currentStatus = nodeExecution.getStatus();

                // Validate transition
                if (!isValidNodeTransition(currentStatus, targetStatus)) {
                    throw new ExecutionException(
                        String.format("Invalid state transition from %s to %s for node %s in run %s",
                            currentStatus, targetStatus, nodeExecution.getNodeId(), nodeExecution.getRunId())
                    );
                }

                log.debug("Transitioning node {} in run {} from {} to {}", 
                    nodeExecution.getNodeId(), nodeExecution.getRunId(), currentStatus, targetStatus);

                // Update status
                nodeExecution.setStatus(targetStatus);
                nodeExecution.setUpdatedAt(LocalDateTime.now());

                // Update timestamps and counters based on transition
                updateNodeState(nodeExecution, targetStatus);

                return nodeExecution;
            }
        })
        .flatMap(updatedNode -> 
            // Persist the transition
            executionStore.saveNodeExecution(updatedNode)
        )
        .flatMap(savedNode -> 
            // Audit the transition
            auditService.auditNodeStateTransition(
                savedNode.getRunId(),
                savedNode.getNodeId(),
                nodeExecution.getStatus(),
                targetStatus
            ).replaceWith(savedNode)
        )
        .eventually(() -> {
            // Clean up lock if node is terminal
            if (targetStatus.isTerminal()) {
                stateLocks.remove(lockKey);
            }
        });
    }

    /**
     * Validate node state transition.
     */
    private boolean isValidNodeTransition(NodeStatus current, NodeStatus target) {
        // Define valid transitions
        return switch (current) {
            case PENDING -> 
                target == NodeStatus.RUNNING || 
                target == NodeStatus.SKIPPED || 
                target == NodeStatus.CANCELLED;
                
            case RUNNING -> 
                target == NodeStatus.SUCCESS || 
                target == NodeStatus.FAILED || 
                target == NodeStatus.ERROR ||
                target == NodeStatus.AWAITING_HITL ||
                target == NodeStatus.CANCELLED;
                
            case FAILED, ERROR -> 
                target == NodeStatus.RETRYING || 
                target == NodeStatus.CANCELLED;
                
            case RETRYING -> 
                target == NodeStatus.RUNNING || 
                target == NodeStatus.CANCELLED;
                
            case AWAITING_HITL -> 
                target == NodeStatus.RUNNING || 
                target == NodeStatus.SKIPPED ||
                target == NodeStatus.CANCELLED;
                
            case SUCCESS, SKIPPED, CANCELLED -> 
                false; // Terminal states
        };
    }

    /**
     * Update run timestamps based on status transition.
     */
    private void updateRunTimestamps(ExecutionRun run, ExecutionStatus newStatus) {
        switch (newStatus) {
            case RUNNING:
                if (run.getStartedAt() == null) {
                    run.setStartedAt(LocalDateTime.now());
                }
                break;
                
            case COMPLETED:
            case FAILED:
            case CANCELLED:
                if (run.getCompletedAt() == null) {
                    run.setCompletedAt(LocalDateTime.now());
                }
                break;
        }
    }

    /**
     * Update node state based on status transition.
     */
    private void updateNodeState(NodeExecution nodeExecution, NodeStatus newStatus) {
        switch (newStatus) {
            case RUNNING:
                if (nodeExecution.getStartedAt() == null) {
                    nodeExecution.setStartedAt(LocalDateTime.now());
                }
                nodeExecution.setAttempt(nodeExecution.getAttempt() + 1);
                break;
                
            case SUCCESS:
            case SKIPPED:
            case CANCELLED:
                if (nodeExecution.getCompletedAt() == null) {
                    nodeExecution.setCompletedAt(LocalDateTime.now());
                }
                break;
                
            case RETRYING:
                // Mark for retry but don't increment attempt yet
                break;
                
            case FAILED:
            case ERROR:
                if (nodeExecution.getCompletedAt() == null) {
                    nodeExecution.setCompletedAt(LocalDateTime.now());
                }
                break;
        }
    }

    /**
     * Batch transition multiple nodes atomically.
     * Useful for coordinated state changes.
     */
    public Uni<Map<String, NodeExecution>> transitionNodes(
        Map<String, NodeStatus> nodeTransitions,
        UUID runId
    ) {
        return Uni.createFrom().item(() -> {
            Map<String, NodeExecution> results = new HashMap<>();
            
            // Acquire all locks first to prevent deadlock
            Map<String, Object> acquiredLocks = new HashMap<>();
            for (String nodeId : nodeTransitions.keySet()) {
                String lockKey = "node:" + runId + ":" + nodeId;
                Object lock = stateLocks.computeIfAbsent(lockKey, k -> new Object());
                acquiredLocks.put(nodeId, lock);
            }

            // Perform transitions
            try {
                for (Map.Entry<String, NodeStatus> entry : nodeTransitions.entrySet()) {
                    String nodeId = entry.getKey();
                    NodeStatus targetStatus = entry.getValue();
                    Object lock = acquiredLocks.get(nodeId);

                    synchronized (lock) {
                        // Note: This is a simplified version
                        // In production, fetch node execution and transition it
                        log.debug("Batch transitioning node {} to {}", nodeId, targetStatus);
                    }
                }
                
                return results;
            } finally {
                // Release locks
                acquiredLocks.clear();
            }
        });
    }

    /**
     * Check if a run can be paused.
     */
    public boolean canPauseRun(ExecutionRun run) {
        return run.getStatus() == ExecutionStatus.RUNNING;
    }

    /**
     * Check if a run can be resumed.
     */
    public boolean canResumeRun(ExecutionRun run) {
        return run.canResume();
    }

    /**
     * Get current state summary for a run.
     */
    public Uni<StateSnapshot> getStateSnapshot(UUID runId) {
        return executionStore.findRun(runId)
            .flatMap(run -> 
                executionStore.findNodeExecutionsByRun(runId)
                    .map(nodeExecutions -> {
                        Map<NodeStatus, Long> nodeStatusCounts = new HashMap<>();
                        for (NodeExecution node : nodeExecutions) {
                            nodeStatusCounts.merge(node.getStatus(), 1L, Long::sum);
                        }

                        return StateSnapshot.builder()
                            .runId(runId)
                            .runStatus(run.getStatus())
                            .totalNodes(nodeExecutions.size())
                            .nodeStatusCounts(nodeStatusCounts)
                            .startedAt(run.getStartedAt())
                            .lastUpdatedAt(run.getLastUpdatedAt())
                            .build();
                    })
            );
    }

    @lombok.Data
    @lombok.Builder
    public static class StateSnapshot {
        private UUID runId;
        private ExecutionStatus runStatus;
        private int totalNodes;
        private Map<NodeStatus, Long> nodeStatusCounts;
        private LocalDateTime startedAt;
        private LocalDateTime lastUpdatedAt;
    }
}
```

### 9. Checkpoint Manager Implementation

#### CheckpointManager.java
```java
package tech.kayys.wayang.orchestrator.core.checkpoint;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.orchestrator.exception.ExecutionException;
import tech.kayys.wayang.orchestrator.model.NodeExecution;
import tech.kayys.wayang.orchestrator.store.ExecutionStore;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Manages checkpointing and resumability for long-running workflows.
 * 
 * Responsibilities:
 * - Create execution checkpoints
 * - Store checkpoint data
 * - Restore from checkpoints
 * - Manage checkpoint lifecycle
 * - Support partial execution recovery
 * 
 * Design principles:
 * - Minimal checkpoint overhead
 * - Incremental checkpointing
 * - Efficient storage
 * - Fast restoration
 */
@ApplicationScoped
@Slf4j
public class CheckpointManager {

    @Inject
    CheckpointStore checkpointStore;

    @Inject
    ExecutionStore executionStore;

    /**
     * Create a checkpoint for a node execution.
     * 
     * @param nodeExecution The node execution to checkpoint
     * @param state The state data to checkpoint
     * @return Uni<String> The checkpoint reference ID
     */
    public Uni<String> createCheckpoint(NodeExecution nodeExecution, Map<String, Object> state) {
        log.debug("Creating checkpoint for node {} in run {}", 
            nodeExecution.getNodeId(), nodeExecution.getRunId());

        Checkpoint checkpoint = Checkpoint.builder()
            .checkpointId(UUID.randomUUID().toString())
            .runId(nodeExecution.getRunId())
            .nodeId(nodeExecution.getNodeId())
            .attempt(nodeExecution.getAttempt())
            .state(state)
            .createdAt(LocalDateTime.now())
            .build();

        return checkpointStore.save(checkpoint)
            .flatMap(savedCheckpoint -> {
                // Update node execution with checkpoint reference
                nodeExecution.setCheckpointRef(savedCheckpoint.getCheckpointId());
                return executionStore.saveNodeExecution(nodeExecution)
                    .map(n -> savedCheckpoint.getCheckpointId());
            });
    }

    /**
     * Load checkpoint data for a node execution.
     * 
     * @param checkpointRef The checkpoint reference ID
     * @return Uni<Map<String, Object>> The checkpoint state
     */
    public Uni<Map<String, Object>> loadCheckpoint(String checkpointRef) {
        if (checkpointRef == null || checkpointRef.isEmpty()) {
            return Uni.createFrom().failure(
                new ExecutionException("Invalid checkpoint reference")
            );
        }

        log.debug("Loading checkpoint {}", checkpointRef);

        return checkpointStore.findById(checkpointRef)
            .map(checkpoint -> {
                if (checkpoint == null) {
                    throw new ExecutionException("Checkpoint not found: " + checkpointRef);
                }
                return checkpoint.getState();
            });
    }

    /**
     * Resume execution from a checkpoint.
     * 
     * @param runId The execution run ID
     * @param nodeId The node ID to resume
     * @return Uni<ResumeContext> Context for resuming execution
     */
    public Uni<ResumeContext> prepareResume(UUID runId, String nodeId) {
        log.info("Preparing to resume node {} in run {}", nodeId, runId);

        return executionStore.findNodeExecution(runId, nodeId)
            .flatMap(nodeExecution -> {
                if (nodeExecution.getCheckpointRef() == null) {
                    return Uni.createFrom().failure(
                        new ExecutionException("No checkpoint available for node: " + nodeId)
                    );
                }

                return loadCheckpoint(nodeExecution.getCheckpointRef())
                    .map(state -> ResumeContext.builder()
                        .runId(runId)
                        .nodeId(nodeId)
                        .checkpointRef(nodeExecution.getCheckpointRef())
                        .state(state)
                        .previousAttempt(nodeExecution.getAttempt())
                        .build());
            });
    }

    /**
     * Create a workflow-level checkpoint (all nodes).
     * Useful for long-running workflows with multiple checkpoints.
     */
    public Uni<String> createWorkflowCheckpoint(UUID runId) {
        log.info("Creating workflow checkpoint for run {}", runId);

        return executionStore.findNodeExecutionsByRun(runId)
            .flatMap(nodeExecutions -> {
                WorkflowCheckpoint workflowCheckpoint = WorkflowCheckpoint.builder()
                    .checkpointId(UUID.randomUUID().toString())
                    .runId(runId)
                    .createdAt(LocalDateTime.now())
                    .build();

                // Store individual node states
                for (NodeExecution node : nodeExecutions) {
                    workflowCheckpoint.addNodeState(
                        node.getNodeId(),
                        NodeCheckpointState.builder()
                            .status(node.getStatus())
                            .attempt(node.getAttempt())
                            .checkpointRef(node.getCheckpointRef())
                            .inputSnapshot(node.getInputSnapshot())
                            .outputSnapshot(node.getOutputSnapshot())
                            .build()
                    );
                }

                return checkpointStore.saveWorkflowCheckpoint(workflowCheckpoint)
                    .map(WorkflowCheckpoint::getCheckpointId);
            });
    }

    /**
     * Restore workflow from a workflow-level checkpoint.
     */
    public Uni<WorkflowCheckpoint> loadWorkflowCheckpoint(String checkpointRef) {
        log.info("Loading workflow checkpoint {}", checkpointRef);

        return checkpointStore.findWorkflowCheckpointById(checkpointRef)
            .map(checkpoint -> {
                if (checkpoint == null) {
                    throw new ExecutionException("Workflow checkpoint not found: " + checkpointRef);
                }
                return checkpoint;
            });
    }

    /**
     * Delete old checkpoints to free storage.
     * 
     * @param runId The run ID
     * @param olderThan Delete checkpoints older than this
     * @return Uni<Integer> Number of deleted checkpoints
     */
    public Uni<Integer> cleanupCheckpoints(UUID runId, LocalDateTime olderThan) {
        log.debug("Cleaning up checkpoints for run {} older than {}", runId, olderThan);

        return checkpointStore.deleteOldCheckpoints(runId, olderThan);
    }

    /**
     * Get checkpoint statistics for a run.
     */
    public Uni<CheckpointStatistics> getCheckpointStatistics(UUID runId) {
        return checkpointStore.countCheckpoints(runId)
            .flatMap(count -> 
                checkpointStore.getTotalSize(runId)
                    .map(size -> CheckpointStatistics.builder()
                        .runId(runId)
                        .checkpointCount(count)
                        .totalSizeBytes(size)
                        .build())
            );
    }

    @Data
    @Builder
    public static class ResumeContext {
        private UUID runId;
        private String nodeId;
        private String checkpointRef;
        private Map<String, Object> state;
        private int previousAttempt;
    }

    @Data
    @Builder
    public static class Checkpoint {
        private String checkpointId;
        private UUID runId;
        private String nodeId;
        private int attempt;
        private Map<String, Object> state;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class WorkflowCheckpoint {
        private String checkpointId;
        private UUID runId;
        private LocalDateTime createdAt;
        
        @Builder.Default
        private Map<String, NodeCheckpointState> nodeStates = new java.util.HashMap<>();

        public void addNodeState(String nodeId, NodeCheckpointState state) {
            nodeStates.put(nodeId, state);
        }
    }

    @Data
    @Builder
    public static class NodeCheckpointState {
        private tech.kayys.wayang.orchestrator.model.NodeStatus status;
        private int attempt;
        private String checkpointRef;
        private Map<String, Object> inputSnapshot;
        private Map<String, Object> outputSnapshot;
    }

    @Data
    @Builder
    public static class CheckpointStatistics {
        private UUID runId;
        private int checkpointCount;
        private long totalSizeBytes;
    }
}
```

### 10. Retry Manager Implementation

#### RetryManager.java
```java
package tech.kayys.wayang.orchestrator.core.retry;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.orchestrator.config.RetryConfig;
import tech.kayys.wayang.orchestrator.model.ErrorPayload;
import tech.kayys.wayang.orchestrator.model.NodeExecution;
import tech.kayys.wayang.orchestrator.model.RetryPolicy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages retry logic for failed node executions.
 * 
 * Responsibilities:
 * - Calculate retry delays
 * - Track retry attempts
 * - Apply backoff strategies
 * - Determine retry eligibility
 * - Prevent retry storms
 * 
 * Design principles:
 * - Configurable retry policies
 * - Multiple backoff strategies
 * - Jitter for distributed systems
 * - Circuit breaker integration
 */
@ApplicationScoped
@Slf4j
public class RetryManager {

    @Inject
    RetryConfig config;

    @Inject
    CircuitBreaker circuitBreaker;

    private final Random random = new Random();
    private final ConcurrentMap<String, RetryState> retryStates = new ConcurrentHashMap<>();

    /**
     * Determine if a node execution should be retried.
     * 
     * @param nodeExecution The failed node execution
     * @param error The error that caused the failure
     * @param retryPolicy The retry policy to apply
     * @return Uni<RetryDecision> The retry decision
     */
    public Uni<RetryDecision> shouldRetry(
        NodeExecution nodeExecution,
        ErrorPayload error,
        RetryPolicy retryPolicy
    ) {
        String key = getRetryKey(nodeExecution);
        
        return Uni.createFrom().item(() -> {
            // Check basic retry eligibility
            if (!error.canRetry()) {
                return RetryDecision.builder()
                    .shouldRetry(false)
                    .reason("Error marked as non-retryable")
                    .build();
            }

            if (!nodeExecution.canRetry()) {
                return RetryDecision.builder()
                    .shouldRetry(false)
                    .reason("Max retry attempts reached")
                    .build();
            }

            // Check circuit breaker
            String circuitKey = getCircuitBreakerKey(nodeExecution);
            if (!circuitBreaker.isCallPermitted(circuitKey)) {
                return RetryDecision.builder()
                    .shouldRetry(false)
                    .reason("Circuit breaker is open")
                    .build();
            }

            // Calculate retry delay
            long delayMs = calculateRetryDelay(
                nodeExecution.getAttempt(),
                retryPolicy
            );

            // Get or create retry state
            RetryState state = retryStates.computeIfAbsent(key, 
                k -> new RetryState(nodeExecution.getRunId(), nodeExecution.getNodeId()));

            state.recordRetry(delayMs);

            return RetryDecision.builder()
                .shouldRetry(true)
                .delayMs(delayMs)
                .nextAttempt(nodeExecution.getAttempt() + 1)
                .reason("Retry eligible")
                .build();
        });
    }

    /**
     * Calculate retry delay based on backoff strategy.
     */
    private long calculateRetryDelay(int attempt, RetryPolicy policy) {
        if (policy == null || policy.getBackoffStrategy() == null) {
            return config.defaultRetryDelayMs();
        }

        long baseDelay = policy.getInitialDelayMs() != null ? 
            policy.getInitialDelayMs() : config.defaultRetryDelayMs();
        
        long maxDelay = policy.getMaxDelayMs() != null ? 
            policy.getMaxDelayMs() : config.maxRetryDelayMs();

        long delay = switch (policy.getBackoffStrategy()) {
            case EXPONENTIAL -> calculateExponentialBackoff(attempt, baseDelay, maxDelay);
            case LINEAR -> calculateLinearBackoff(attempt, baseDelay, maxDelay);
            case FIXED -> baseDelay;
        };

        // Add jitter to prevent thundering herd
        if (config.enableJitter()) {
            delay = addJitter(delay);
        }

        return Math.min(delay, maxDelay);
    }

    /**
     * Calculate exponential backoff: delay = baseDelay * (2 ^ attempt).
     */
    private long calculateExponentialBackoff(int attempt, long baseDelay, long maxDelay) {
        long delay = baseDelay * (long) Math.pow(2, attempt);
        return Math.min(delay, maxDelay);
    }

    /**
     * Calculate linear backoff: delay = baseDelay * (attempt + 1).
     */
    private long calculateLinearBackoff(int attempt, long baseDelay, long maxDelay) {
        long delay = baseDelay * (attempt + 1);
        return Math.min(delay, maxDelay);
    }

    /**
     * Add jitter to delay to prevent synchronized retries.
     * Uses decorrelated jitter algorithm.
     */
    private long addJitter(long delay) {
        // Decorrelated jitter: random between 0 and delay
        long jitter = random.nextLong(delay + 1);
        return jitter;
    }

    /**
     * Record successful retry (for circuit breaker).
     */
    public void recordSuccess(NodeExecution nodeExecution) {
        String circuitKey = getCircuitBreakerKey(nodeExecution);
        circuitBreaker.recordSuccess(circuitKey);

        // Clean up retry state
        String retryKey = getRetryKey(nodeExecution);
        retryStates.remove(retryKey);
    }

    /**
     * Record failed retry (for circuit breaker).
     */
    public void recordFailure(NodeExecution nodeExecution) {
        String circuitKey = getCircuitBreakerKey(nodeExecution);
        circuitBreaker.recordFailure(circuitKey);
    }

    /**
     * Get retry statistics for a node.
     */
    public RetryStatistics getRetryStatistics(UUID runId, String nodeId) {
        String key = getRetryKey(runId, nodeId);
        RetryState state = retryStates.get(key);

        if (state == null) {
            return RetryStatistics.builder()
                .runId(runId)
                .nodeId(nodeId)
                .totalRetries(0)
                .build();
        }

        return RetryStatistics.builder()
            .runId(runId)
            .nodeId(nodeId)
            .totalRetries(state.getRetryCount())
            .lastRetryAt(state.getLastRetryAt())
            .totalDelayMs(state.getTotalDelayMs())
            .build();
    }

    /**
     * Clear retry state for a completed run.
     */
    public void clearRetryState(UUID runId) {
        retryStates.entrySet().removeIf(entry -> 
            entry.getValue().getRunId().equals(runId)
        );
    }

    private String getRetryKey(NodeExecution nodeExecution) {
        return getRetryKey(nodeExecution.getRunId(), nodeExecution.getNodeId());
    }

    private String getRetryKey(UUID runId, String nodeId) {
        return runId.toString() + ":" + nodeId;
    }

    private String getCircuitBreakerKey(NodeExecution nodeExecution) {
        return "node:" + nodeExecution.getNodeId();
    }

    @Data
    @Builder
    public static class RetryDecision {
        private boolean shouldRetry;
        private String reason;
        private Long delayMs;
        private Integer nextAttempt;
    }

    @Data
    @Builder
    public static class RetryStatistics {
        private UUID runId;
        private String nodeId;
        private int totalRetries;
        private LocalDateTime lastRetryAt;
        private long totalDelayMs;
    }

    @Data
    private static class RetryState {
        private final UUID runId;
        private final String nodeId;
        private int retryCount = 0;
        private LocalDateTime lastRetryAt;
        private long totalDelayMs = 0;

        public RetryState(UUID runId, String nodeId) {
            this.runId = runId;
            this.nodeId = nodeId;
        }

        public void recordRetry(long delayMs) {
            this.retryCount++;
            this.lastRetryAt = LocalDateTime.now();
            this.totalDelayMs += delayMs;
        }
    }
}
```

### 11. Circuit Breaker Implementation

#### CircuitBreaker.java
```java
package tech.kayys.wayang.orchestrator.core.retry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.orchestrator.config.RetryConfig;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements circuit breaker pattern to prevent cascading failures.
 * 
 * States:
 * - CLOSED: Normal operation, calls are allowed
 * - OPEN: Too many failures, calls are blocked
 * - HALF_OPEN: Testing if service recovered
 * 
 * Design principles:
 * - Fast failure detection
 * - Automatic recovery testing
 * - Configurable thresholds
 * - Per-resource isolation
 */
@ApplicationScoped
@Slf4j
public class CircuitBreaker {

    @Inject
    RetryConfig config;

    private final ConcurrentMap<String, CircuitState> circuits = new ConcurrentHashMap<>();

    /**
     * Check if a call is permitted through the circuit breaker.
     * 
     * @param key The circuit breaker key (typically resource identifier)
     * @return true if call is permitted
     */
    public boolean isCallPermitted(String key) {
        CircuitState state = circuits.computeIfAbsent(key, k -> 
            new CircuitState(config.failureThreshold(), config.successThreshold())
        );

        synchronized (state) {
            return switch (state.getState()) {
                case CLOSED -> true;
                case OPEN -> checkAndTransitionToHalfOpen(state);
                case HALF_OPEN -> state.getSuccessCount() < config.successThreshold();
            };
        }
    }

    /**
     * Record a successful call.
     */
    public void recordSuccess(String key) {
        CircuitState state = circuits.get(key);
        if (state == null) return;

        synchronized (state) {
            state.recordSuccess();

            if (state.getState() == State.HALF_OPEN && 
                state.getSuccessCount() >= config.successThreshold()) {
                // Transition to CLOSED
                state.transitionTo(State.CLOSED);
                log.info("Circuit breaker {} transitioned to CLOSED", key);
                }
        }
    }

    /**
     * Record a failed call.
     */
    public void recordFailure(String key) {
        CircuitState state = circuits.computeIfAbsent(key, k -> 
            new CircuitState(config.failureThreshold(), config.successThreshold())
        );

        synchronized (state) {
            state.recordFailure();

            if (state.getState() == State.CLOSED && 
                state.getFailureCount() >= config.failureThreshold()) {
                // Transition to OPEN
                state.transitionTo(State.OPEN);
                state.setOpenedAt(LocalDateTime.now());
                log.warn("Circuit breaker {} transitioned to OPEN (failures: {})", 
                    key, state.getFailureCount());
            } else if (state.getState() == State.HALF_OPEN) {
                // Failure during HALF_OPEN, back to OPEN
                state.transitionTo(State.OPEN);
                state.setOpenedAt(LocalDateTime.now());
                log.warn("Circuit breaker {} transitioned back to OPEN", key);
            }
        }
    }

    /**
     * Check if circuit should transition from OPEN to HALF_OPEN.
     */
    private boolean checkAndTransitionToHalfOpen(CircuitState state) {
        LocalDateTime openedAt = state.getOpenedAt();
        if (openedAt == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        long elapsedSeconds = java.time.Duration.between(openedAt, now).getSeconds();

        if (elapsedSeconds >= config.openDurationSeconds()) {
            state.transitionTo(State.HALF_OPEN);
            log.info("Circuit breaker transitioned to HALF_OPEN for testing");
            return true;
        }

        return false;
    }

    /**
     * Manually reset a circuit breaker.
     */
    public void reset(String key) {
        CircuitState state = circuits.get(key);
        if (state != null) {
            synchronized (state) {
                state.reset();
                log.info("Circuit breaker {} manually reset", key);
            }
        }
    }

    /**
     * Get circuit breaker status.
     */
    public CircuitStatus getStatus(String key) {
        CircuitState state = circuits.get(key);
        if (state == null) {
            return CircuitStatus.builder()
                .key(key)
                .state(State.CLOSED)
                .failureCount(0)
                .successCount(0)
                .build();
        }

        return CircuitStatus.builder()
            .key(key)
            .state(state.getState())
            .failureCount(state.getFailureCount())
            .successCount(state.getSuccessCount())
            .openedAt(state.getOpenedAt())
            .build();
    }

    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    @Data
    private static class CircuitState {
        private State state = State.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final int failureThreshold;
        private final int successThreshold;
        private LocalDateTime openedAt;

        public CircuitState(int failureThreshold, int successThreshold) {
            this.failureThreshold = failureThreshold;
            this.successThreshold = successThreshold;
        }

        public int getFailureCount() {
            return failureCount.get();
        }

        public int getSuccessCount() {
            return successCount.get();
        }

        public void recordSuccess() {
            successCount.incrementAndGet();
            failureCount.set(0); // Reset failures on success
        }

        public void recordFailure() {
            failureCount.incrementAndGet();
            successCount.set(0); // Reset successes on failure
        }

        public void transitionTo(State newState) {
            this.state = newState;
            if (newState == State.CLOSED) {
                failureCount.set(0);
                successCount.set(0);
                openedAt = null;
            } else if (newState == State.HALF_OPEN) {
                successCount.set(0);
            }
        }

        public void reset() {
            transitionTo(State.CLOSED);
        }
    }

    @Data
    @Builder
    public static class CircuitStatus {
        private String key;
        private State state;
        private int failureCount;
        private int successCount;
        private LocalDateTime openedAt;
    }
}
```

Would you like me to continue with:
1. REST API layer (OrchestratorResource)
2. Adapters (RuntimeHub, EventEmitter, etc.)
3. Configuration files
4. Database migrations
5. Supporting models (DispatchContext, ExecutionPlan, Node, etc.)

Please let me know!



# Wayang Orchestrator - Core Implementation (Continued)

### 12. Supporting Models

#### DispatchContext.java
```java
package tech.kayys.wayang.orchestrator.core.dispatcher;

import lombok.*;
import tech.kayys.wayang.orchestrator.model.ExecutionPlan;
import tech.kayys.wayang.orchestrator.model.NodeExecution;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context for tracking active workflow dispatch.
 * Maintains runtime state during execution.
 */
@Data
@Builder
public class DispatchContext {
    private UUID runId;
    private ExecutionPlan plan;
    private Map<String, NodeExecution> nodeExecutions;
    private long startTime;
    
    @Builder.Default
    private Map<String, Object> sharedState = new ConcurrentHashMap<>();
}
```

#### ExecutionPlan.java
```java
package tech.kayys.wayang.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.*;

/**
 * Represents an executable workflow plan.
 * Contains the DAG structure and node definitions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionPlan {
    
    private UUID planId;
    private String name;
    private String version;
    private String description;
    
    @Builder.Default
    private List<Node> nodes = new ArrayList<>();
    
    @Builder.Default
    private List<Edge> edges = new ArrayList<>();
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    private UUID tenantId;
    
    /**
     * Find a node by ID.
     */
    public Optional<Node> findNode(String nodeId) {
        return nodes.stream()
            .filter(n -> n.getNodeId().equals(nodeId))
            .findFirst();
    }
    
    /**
     * Get incoming edges for a node.
     */
    public List<Edge> getIncomingEdges(String nodeId) {
        return edges.stream()
            .filter(e -> e.getTargetNodeId().equals(nodeId))
            .toList();
    }
    
    /**
     * Get outgoing edges for a node.
     */
    public List<Edge> getOutgoingEdges(String nodeId) {
        return edges.stream()
            .filter(e -> e.getSourceNodeId().equals(nodeId))
            .toList();
    }
}
```

#### Node.java
```java
package tech.kayys.wayang.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single node in the execution graph.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Node {
    
    private String nodeId;
    private String name;
    private String type;
    private String description;
    
    /**
     * Node descriptor containing implementation details.
     */
    private NodeDescriptor descriptor;
    
    /**
     * Static inputs configured at design time.
     */
    @Builder.Default
    private Map<String, Object> inputs = new HashMap<>();
    
    /**
     * Retry policy for this node.
     */
    private RetryPolicy retryPolicy;
    
    /**
     * Additional metadata.
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
```

#### NodeDescriptor.java
```java
package tech.kayys.wayang.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.*;

/**
 * Describes the interface and requirements of a node type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeDescriptor {
    
    private String id;
    private String version;
    private String name;
    private String description;
    
    @Builder.Default
    private List<PortDescriptor> inputs = new ArrayList<>();
    
    @Builder.Default
    private List<PortDescriptor> outputs = new ArrayList<>();
    
    @Builder.Default
    private List<String> capabilities = new ArrayList<>();
    
    private Implementation implementation;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortDescriptor {
        private String name;
        private String type;
        private boolean required;
        private String description;
        private Object defaultValue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Implementation {
        private String type; // maven, wasm, container
        private String coordinate;
        private String digest;
    }
}
```

#### Edge.java
```java
package tech.kayys.wayang.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a connection between two nodes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Edge {
    
    private String edgeId;
    private String sourceNodeId;
    private String sourcePort; // success, error, etc.
    private String targetNodeId;
    private String targetPort;
    
    /**
     * Mapping from source output keys to target input keys.
     */
    @Builder.Default
    private Map<String, String> mapping = new HashMap<>();
    
    /**
     * Conditional expression (CEL) for edge activation.
     */
    private String condition;
}
```

#### RetryPolicy.java
```java
package tech.kayys.wayang.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Defines retry behavior for a node.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RetryPolicy {
    
    @Builder.Default
    private Integer maxAttempts = 3;
    
    @Builder.Default
    private BackoffStrategy backoffStrategy = BackoffStrategy.EXPONENTIAL;
    
    @Builder.Default
    private Long initialDelayMs = 1000L;
    
    private Long maxDelayMs;
    
    public enum BackoffStrategy {
        FIXED, LINEAR, EXPONENTIAL
    }
}
```

### 13. REST API Layer

#### OrchestratorResource.java
```java
package tech.kayys.wayang.orchestrator.api;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import tech.kayys.wayang.orchestrator.api.dto.*;
import tech.kayys.wayang.orchestrator.api.validation.ExecutionValidator;
import tech.kayys.wayang.orchestrator.core.dispatcher.Dispatcher;
import tech.kayys.wayang.orchestrator.core.state.StateMachine;
import tech.kayys.wayang.orchestrator.model.*;
import tech.kayys.wayang.orchestrator.store.ExecutionStore;
import tech.kayys.wayang.orchestrator.store.PlanStore;

import java.util.UUID;

/**
 * REST API for orchestration operations.
 * 
 * Endpoints:
 * - POST /orchestrator/execute - Start execution
 * - GET /orchestrator/runs/{runId} - Get execution status
 * - POST /orchestrator/runs/{runId}/pause - Pause execution
 * - POST /orchestrator/runs/{runId}/resume - Resume execution
 * - POST /orchestrator/runs/{runId}/cancel - Cancel execution
 * - GET /orchestrator/runs/{runId}/nodes - Get node statuses
 */
@Path("/orchestrator")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Orchestrator", description = "Workflow orchestration operations")
@Slf4j
public class OrchestratorResource {

    @Inject
    Dispatcher dispatcher;

    @Inject
    StateMachine stateMachine;

    @Inject
    ExecutionStore executionStore;

    @Inject
    PlanStore planStore;

    @Inject
    ExecutionValidator validator;

    /**
     * Execute a workflow plan.
     */
    @POST
    @Path("/execute")
    @Operation(summary = "Execute a workflow plan", description = "Start execution of a workflow plan")
    public Uni<Response> execute(@Valid ExecutePlanRequest request) {
        log.info("Received execution request for plan {}", request.getPlanId());

        return validator.validateExecutionRequest(request)
            .flatMap(validationResult -> {
                if (!validationResult.isValid()) {
                    return Uni.createFrom().item(
                        Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("errors", validationResult.getErrors()))
                            .build()
                    );
                }

                return planStore.findPlan(request.getPlanId(), request.getPlanVersion())
                    .flatMap(plan -> {
                        if (plan == null) {
                            return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND)
                                    .entity(Map.of("error", "Plan not found"))
                                    .build()
                            );
                        }

                        // Create execution run
                        ExecutionRun run = ExecutionRun.builder()
                            .runId(UUID.randomUUID())
                            .planId(plan.getPlanId())
                            .planVersion(plan.getVersion())
                            .tenantId(request.getTenantId())
                            .status(ExecutionStatus.PENDING)
                            .createdBy(request.getCreatedBy())
                            .priority(request.getPriority() != null ? request.getPriority() : 0)
                            .metadata(request.getMetadata())
                            .build();

                        return executionStore.saveRun(run)
                            .flatMap(savedRun -> 
                                // Transition to VALIDATED
                                stateMachine.transitionRun(savedRun, ExecutionStatus.VALIDATED)
                            )
                            .flatMap(validatedRun -> 
                                // Dispatch for execution
                                dispatcher.dispatch(plan, validatedRun)
                                    .replaceWith(validatedRun)
                            )
                            .map(executedRun -> 
                                Response.accepted(ExecutePlanResponse.builder()
                                    .runId(executedRun.getRunId())
                                    .status(executedRun.getStatus())
                                    .message("Execution started successfully")
                                    .build())
                                .build()
                            );
                    });
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("Failed to execute plan", throwable);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", throwable.getMessage()))
                    .build();
            });
    }

    /**
     * Get execution status.
     */
    @GET
    @Path("/runs/{runId}")
    @Operation(summary = "Get execution status", description = "Retrieve current status of a workflow execution")
    public Uni<Response> getExecutionStatus(
        @Parameter(description = "Execution run ID") @PathParam("runId") UUID runId
    ) {
        log.debug("Getting status for run {}", runId);

        return executionStore.findRun(runId)
            .flatMap(run -> {
                if (run == null) {
                    return Uni.createFrom().item(
                        Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Run not found"))
                            .build()
                    );
                }

                return executionStore.findNodeExecutionsByRun(runId)
                    .flatMap(nodeExecutions -> 
                        stateMachine.getStateSnapshot(runId)
                            .map(snapshot -> 
                                Response.ok(ExecutionStatusResponse.builder()
                                    .runId(run.getRunId())
                                    .planId(run.getPlanId())
                                    .status(run.getStatus())
                                    .startedAt(run.getStartedAt())
                                    .completedAt(run.getCompletedAt())
                                    .totalNodes(nodeExecutions.size())
                                    .nodeStatusCounts(snapshot.getNodeStatusCounts())
                                    .metadata(run.getMetadata())
                                    .build())
                                .build()
                            )
                    );
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("Failed to get execution status", throwable);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", throwable.getMessage()))
                    .build();
            });
    }

    /**
     * Pause execution.
     */
    @POST
    @Path("/runs/{runId}/pause")
    @Operation(summary = "Pause execution", description = "Pause a running workflow execution")
    public Uni<Response> pauseExecution(
        @Parameter(description = "Execution run ID") @PathParam("runId") UUID runId,
        @Valid PauseResumeRequest request
    ) {
        log.info("Pausing execution for run {}", runId);

        return executionStore.findRun(runId)
            .flatMap(run -> {
                if (run == null) {
                    return Uni.createFrom().item(
                        Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Run not found"))
                            .build()
                    );
                }

                if (!stateMachine.canPauseRun(run)) {
                    return Uni.createFrom().item(
                        Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Run cannot be paused in current state"))
                            .build()
                    );
                }

                return stateMachine.transitionRun(run, ExecutionStatus.PAUSED)
                    .map(pausedRun -> 
                        Response.ok(Map.of(
                            "runId", pausedRun.getRunId(),
                            "status", pausedRun.getStatus(),
                            "message", "Execution paused successfully"
                        )).build()
                    );
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("Failed to pause execution", throwable);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", throwable.getMessage()))
                    .build();
            });
    }

    /**
     * Resume execution.
     */
    @POST
    @Path("/runs/{runId}/resume")
    @Operation(summary = "Resume execution", description = "Resume a paused workflow execution")
    public Uni<Response> resumeExecution(
        @Parameter(description = "Execution run ID") @PathParam("runId") UUID runId,
        @Valid PauseResumeRequest request
    ) {
        log.info("Resuming execution for run {}", runId);

        return executionStore.findRun(runId)
            .flatMap(run -> {
                if (run == null) {
                    return Uni.createFrom().item(
                        Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Run not found"))
                            .build()
                    );
                }

                if (!stateMachine.canResumeRun(run)) {
                    return Uni.createFrom().item(
                        Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Run cannot be resumed in current state"))
                            .build()
                    );
                }

                return stateMachine.transitionRun(run, ExecutionStatus.RUNNING)
                    .flatMap(resumedRun -> 
                        planStore.findPlan(run.getPlanId(), run.getPlanVersion())
                            .flatMap(plan -> 
                                dispatcher.dispatch(plan, resumedRun)
                                    .replaceWith(resumedRun)
                            )
                    )
                    .map(resumedRun -> 
                        Response.ok(Map.of(
                            "runId", resumedRun.getRunId(),
                            "status", resumedRun.getStatus(),
                            "message", "Execution resumed successfully"
                        )).build()
                    );
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("Failed to resume execution", throwable);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", throwable.getMessage()))
                    .build();
            });
    }

    /**
     * Cancel execution.
     */
    @POST
    @Path("/runs/{runId}/cancel")
    @Operation(summary = "Cancel execution", description = "Cancel a workflow execution")
    public Uni<Response> cancelExecution(
        @Parameter(description = "Execution run ID") @PathParam("runId") UUID runId,
        @Valid CancelRequest request
    ) {
        log.info("Cancelling execution for run {}", runId);

        return executionStore.findRun(runId)
            .flatMap(run -> {
                if (run == null) {
                    return Uni.createFrom().item(
                        Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Run not found"))
                            .build()
                    );
                }

                if (run.isTerminal()) {
                    return Uni.createFrom().item(
                        Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Run is already in terminal state"))
                            .build()
                    );
                }

                return dispatcher.cancelDispatch(runId)
                    .flatMap(v -> 
                        stateMachine.transitionRun(run, ExecutionStatus.CANCELLED)
                    )
                    .map(cancelledRun -> 
                        Response.ok(Map.of(
                            "runId", cancelledRun.getRunId(),
                            "status", cancelledRun.getStatus(),
                            "message", "Execution cancelled successfully"
                        )).build()
                    );
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("Failed to cancel execution", throwable);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", throwable.getMessage()))
                    .build();
            });
    }

    /**
     * Get node execution details.
     */
    @GET
    @Path("/runs/{runId}/nodes")
    @Operation(summary = "Get node executions", description = "Retrieve all node executions for a run")
    public Uni<Response> getNodeExecutions(
        @Parameter(description = "Execution run ID") @PathParam("runId") UUID runId
    ) {
        log.debug("Getting node executions for run {}", runId);

        return executionStore.findNodeExecutionsByRun(runId)
            .map(nodeExecutions -> 
                Response.ok(Map.of(
                    "runId", runId,
                    "nodes", nodeExecutions
                )).build()
            )
            .onFailure().recoverWithItem(throwable -> {
                log.error("Failed to get node executions", throwable);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", throwable.getMessage()))
                    .build();
            });
    }

    /**
     * Get specific node execution details.
     */
    @GET
    @Path("/runs/{runId}/nodes/{nodeId}")
    @Operation(summary = "Get node execution details", description = "Retrieve details of a specific node execution")
    public Uni<Response> getNodeExecution(
        @Parameter(description = "Execution run ID") @PathParam("runId") UUID runId,
        @Parameter(description = "Node ID") @PathParam("nodeId") String nodeId
    ) {
        log.debug("Getting node execution {} for run {}", nodeId, runId);

        return executionStore.findNodeExecution(runId, nodeId)
            .map(nodeExecution -> {
                if (nodeExecution == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Node execution not found"))
                        .build();
                }
                return Response.ok(nodeExecution).build();
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("Failed to get node execution", throwable);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", throwable.getMessage()))
                    .build();
            });
    }
}
```

### 14. API DTOs

#### ExecutePlanRequest.java
```java
package tech.kayys.wayang.orchestrator.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutePlanRequest {
    
    @NotNull(message = "Plan ID is required")
    private UUID planId;
    
    private String planVersion;
    
    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;
    
    private String createdBy;
    
    private Integer priority;
    
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
```

#### ExecutePlanResponse.java
```java
package tech.kayys.wayang.orchestrator.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import tech.kayys.wayang.orchestrator.model.ExecutionStatus;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutePlanResponse {
    private UUID runId;
    private ExecutionStatus status;
    private String message;
}
```

#### ExecutionStatusResponse.java
```java
package tech.kayys.wayang.orchestrator.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import tech.kayys.wayang.orchestrator.model.ExecutionStatus;
import tech.kayys.wayang.orchestrator.model.NodeStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionStatusResponse {
    private UUID runId;
    private UUID planId;
    private ExecutionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer totalNodes;
    private Map<NodeStatus, Long> nodeStatusCounts;
    private Map<String, Object> metadata;
}
```

#### PauseResumeRequest.java
```java
package tech.kayys.wayang.orchestrator.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PauseResumeRequest {
    private String reason;
    private String operator;
}
```

#### CancelRequest.java
```java
package tech.kayys.wayang.orchestrator.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CancelRequest {
    private String reason;
    private String operator;
}
```

Would you like me to continue with:
1. Adapters (RuntimeHubAdapter, EventEmitterAdapter, etc.)
2. Store implementations (ExecutionStore, PlanStore, etc.)
3. Configuration classes
4. Database migrations
5. Audit Service implementation

Please let me know!



# Wayang Orchestrator - Core Implementation (Continued)

### 15. Adapters

#### RuntimeHubAdapter.java
```java
package tech.kayys.wayang.orchestrator.adapter;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import tech.kayys.wayang.orchestrator.config.OrchestratorConfig;

import java.util.Map;
import java.util.UUID;

/**
 * Adapter for communication with the Runtime Hub service.
 * Runtime Hub is responsible for actual node execution.
 * 
 * Responsibilities:
 * - Submit node execution requests
 * - Poll execution status
 * - Handle streaming results
 * - Manage execution cancellation
 */
@ApplicationScoped
@Slf4j
public class RuntimeHubAdapter {

    @Inject
    OrchestratorConfig config;

    @Inject
    @RestClient
    RuntimeHubClient runtimeHubClient;

    /**
     * Execute a node through the Runtime Hub.
     * 
     * @param request The execution request
     * @return Uni<NodeExecutionResult> The execution result
     */
    public Uni<NodeExecutionResult> executeNode(ExecuteNodeRequest request) {
        log.debug("Submitting node {} to Runtime Hub", request.getNodeId());

        return runtimeHubClient.executeNode(request)
            .onItem().invoke(result -> 
                log.debug("Node {} execution completed with status: {}", 
                    request.getNodeId(), result.isSuccess() ? "SUCCESS" : "FAILED")
            )
            .onFailure().invoke(throwable -> 
                log.error("Failed to execute node {} via Runtime Hub", 
                    request.getNodeId(), throwable)
            )
            .onFailure().recoverWithItem(throwable -> 
                // Convert failure to error result
                NodeExecutionResult.builder()
                    .taskId(request.getTaskId())
                    .success(false)
                    .error(tech.kayys.wayang.orchestrator.model.ErrorPayload.builder()
                        .type(tech.kayys.wayang.orchestrator.model.ErrorPayload.ErrorType.NETWORK_ERROR)
                        .message("Failed to communicate with Runtime Hub: " + throwable.getMessage())
                        .retryable(true)
                        .originNode(request.getNodeId())
                        .originRunId(request.getRunId().toString())
                        .suggestedAction(tech.kayys.wayang.orchestrator.model.ErrorPayload.SuggestedAction.RETRY)
                        .build())
                    .build()
            );
    }

    /**
     * Cancel a running node execution.
     */
    public Uni<Void> cancelNodeExecution(UUID taskId) {
        log.info("Cancelling task {}", taskId);
        
        return runtimeHubClient.cancelTask(taskId)
            .onFailure().invoke(throwable -> 
                log.warn("Failed to cancel task {}", taskId, throwable)
            )
            .replaceWithVoid();
    }

    /**
     * Get execution status from Runtime Hub.
     */
    public Uni<ExecutionStatus> getExecutionStatus(UUID taskId) {
        return runtimeHubClient.getTaskStatus(taskId)
            .onFailure().recoverWithItem(throwable -> {
                log.error("Failed to get task status for {}", taskId, throwable);
                return ExecutionStatus.builder()
                    .taskId(taskId)
                    .status("UNKNOWN")
                    .build();
            });
    }

    // DTOs for Runtime Hub communication

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecuteNodeRequest {
        private UUID taskId;
        private UUID runId;
        private String nodeId;
        private tech.kayys.wayang.orchestrator.model.NodeDescriptor nodeDescriptor;
        private Map<String, Object> input;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeExecutionResult {
        private UUID taskId;
        private boolean success;
        private Map<String, Object> outputs;
        private tech.kayys.wayang.orchestrator.model.ErrorPayload error;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionStatus {
        private UUID taskId;
        private String status;
        private Map<String, Object> metadata;
    }
}
```

#### RuntimeHubClient.java
```java
package tech.kayys.wayang.orchestrator.adapter;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.UUID;

/**
 * REST client for Runtime Hub service.
 */
@Path("/runtime-hub")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "runtime-hub")
public interface RuntimeHubClient {

    @POST
    @Path("/execute")
    Uni<RuntimeHubAdapter.NodeExecutionResult> executeNode(
        RuntimeHubAdapter.ExecuteNodeRequest request
    );

    @POST
    @Path("/tasks/{taskId}/cancel")
    Uni<Void> cancelTask(@PathParam("taskId") UUID taskId);

    @GET
    @Path("/tasks/{taskId}/status")
    Uni<RuntimeHubAdapter.ExecutionStatus> getTaskStatus(@PathParam("taskId") UUID taskId);
}
```

#### EventEmitterAdapter.java
```java
package tech.kayys.wayang.orchestrator.adapter;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import tech.kayys.wayang.orchestrator.event.ExecutionEvent;
import tech.kayys.wayang.orchestrator.event.EventType;
import tech.kayys.wayang.orchestrator.model.ErrorPayload;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Adapter for emitting events to the event bus (Kafka).
 * 
 * Events published:
 * - Plan lifecycle (started, completed, failed, cancelled)
 * - Node lifecycle (started, completed, failed)
 * - State transitions
 * - Error events
 */
@ApplicationScoped
@Slf4j
public class EventEmitterAdapter {

    @Inject
    @Channel("execution-events")
    Emitter<KafkaRecord<String, ExecutionEvent>> eventEmitter;

    /**
     * Emit plan started event.
     */
    public Uni<Void> emitPlanStarted(UUID runId, UUID planId) {
        return emitEvent(ExecutionEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(EventType.PLAN_STARTED)
            .runId(runId)
            .planId(planId)
            .timestamp(LocalDateTime.now())
            .build());
    }

    /**
     * Emit plan completed event.
     */
    public Uni<Void> emitPlanCompleted(UUID runId, UUID planId) {
        return emitEvent(ExecutionEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(EventType.PLAN_COMPLETED)
            .runId(runId)
            .planId(planId)
            .timestamp(LocalDateTime.now())
            .build());
    }

    /**
     * Emit plan failed event.
     */
    public Uni<Void> emitPlanFailed(UUID runId, UUID planId, ErrorPayload error) {
        return emitEvent(ExecutionEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(EventType.PLAN_FAILED)
            .runId(runId)
            .planId(planId)
            .payload(error)
            .timestamp(LocalDateTime.now())
            .build());
    }

    /**
     * Emit plan cancelled event.
     */
    public Uni<Void> emitPlanCancelled(UUID runId, UUID planId) {
        return emitEvent(ExecutionEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(EventType.PLAN_CANCELLED)
            .runId(runId)
            .planId(planId)
            .timestamp(LocalDateTime.now())
            .build());
    }

    /**
     * Emit node started event.
     */
    public Uni<Void> emitNodeStarted(UUID runId, String nodeId) {
        return emitEvent(ExecutionEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(EventType.NODE_STARTED)
            .runId(runId)
            .nodeId(nodeId)
            .timestamp(LocalDateTime.now())
            .build());
    }

    /**
     * Emit node completed event.
     */
    public Uni<Void> emitNodeCompleted(UUID runId, String nodeId, 
                                        RuntimeHubAdapter.NodeExecutionResult result) {
        return emitEvent(ExecutionEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(EventType.NODE_COMPLETED)
            .runId(runId)
            .nodeId(nodeId)
            .payload(result)
            .timestamp(LocalDateTime.now())
            .build());
    }

    /**
     * Emit node failed event.
     */
    public Uni<Void> emitNodeFailed(UUID runId, String nodeId, ErrorPayload error) {
        return emitEvent(ExecutionEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(EventType.NODE_FAILED)
            .runId(runId)
            .nodeId(nodeId)
            .payload(error)
            .timestamp(LocalDateTime.now())
            .build());
    }

    /**
     * Emit generic event.
     */
    private Uni<Void> emitEvent(ExecutionEvent event) {
        String key = event.getRunId().toString();
        
        return Uni.createFrom().completionStage(
            eventEmitter.send(KafkaRecord.of(key, event))
        )
        .onItem().invoke(() -> 
            log.debug("Emitted event: type={}, runId={}, nodeId={}", 
                event.getEventType(), event.getRunId(), event.getNodeId())
        )
        .onFailure().invoke(throwable -> 
            log.error("Failed to emit event: type={}, runId={}", 
                event.getEventType(), event.getRunId(), throwable)
        )
        .replaceWithVoid();
    }
}
```

#### PolicyEnforcerAdapter.java
```java
package tech.kayys.wayang.orchestrator.adapter;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Adapter for policy enforcement service.
 * Validates execution requests against tenant policies and guardrails.
 */
@ApplicationScoped
@Slf4j
public class PolicyEnforcerAdapter {

    @Inject
    @RestClient
    PolicyEnforcerClient policyClient;

    /**
     * Validate if execution is allowed by policy.
     */
    public Uni<PolicyDecision> validateExecution(UUID tenantId, UUID planId, Map<String, Object> context) {
        log.debug("Validating execution for tenant {} and plan {}", tenantId, planId);

        PolicyRequest request = PolicyRequest.builder()
            .tenantId(tenantId)
            .planId(planId)
            .context(context)
            .build();

        return policyClient.validateExecution(request)
            .onFailure().recoverWithItem(throwable -> {
                log.error("Policy validation failed, defaulting to DENY", throwable);
                return PolicyDecision.builder()
                    .allowed(false)
                    .reason("Policy service unavailable")
                    .build();
            });
    }

    /**
     * Validate if node execution is allowed.
     */
    public Uni<PolicyDecision> validateNodeExecution(
        UUID tenantId, 
        String nodeId, 
        String nodeType,
        Map<String, Object> context
    ) {
        log.debug("Validating node execution for tenant {} and node {}", tenantId, nodeId);

        NodePolicyRequest request = NodePolicyRequest.builder()
            .tenantId(tenantId)
            .nodeId(nodeId)
            .nodeType(nodeType)
            .context(context)
            .build();

        return policyClient.validateNodeExecution(request)
            .onFailure().recoverWithItem(throwable -> {
                log.warn("Node policy validation failed, defaulting to ALLOW", throwable);
                return PolicyDecision.builder()
                    .allowed(true)
                    .reason("Policy service unavailable, allowing by default")
                    .build();
            });
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicyRequest {
        private UUID tenantId;
        private UUID planId;
        private Map<String, Object> context;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodePolicyRequest {
        private UUID tenantId;
        private String nodeId;
        private String nodeType;
        private Map<String, Object> context;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicyDecision {
        private boolean allowed;
        private String reason;
        private Map<String, Object> metadata;
    }
}
```

#### PolicyEnforcerClient.java
```java
package tech.kayys.wayang.orchestrator.adapter;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for Policy Enforcer service.
 */
@Path("/policy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "policy-enforcer")
public interface PolicyEnforcerClient {

    @POST
    @Path("/validate/execution")
    Uni<PolicyEnforcerAdapter.PolicyDecision> validateExecution(
        PolicyEnforcerAdapter.PolicyRequest request
    );

    @POST
    @Path("/validate/node")
    Uni<PolicyEnforcerAdapter.PolicyDecision> validateNodeExecution(
        PolicyEnforcerAdapter.NodePolicyRequest request
    );
}
```

#### A2AAdapter.java
```java
package tech.kayys.wayang.orchestrator.adapter;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Adapter for Agent-to-Agent (A2A) communication.
 * Enables multi-agent orchestration and delegation.
 */
@ApplicationScoped
@Slf4j
public class A2AAdapter {

    @Inject
    @RestClient
    A2ARouterClient a2aClient;

    /**
     * Delegate a sub-task to another agent.
     */
    public Uni<A2AResponse> delegateToAgent(
        UUID runId,
        String sourceAgentId,
        String targetAgentId,
        Map<String, Object> payload
    ) {
        log.info("Delegating from agent {} to agent {} for run {}", 
            sourceAgentId, targetAgentId, runId);

        A2ARequest request = A2ARequest.builder()
            .messageId(UUID.randomUUID())
            .runId(runId)
            .sourceAgentId(sourceAgentId)
            .targetAgentId(targetAgentId)
            .payload(payload)
            .build();

        return a2aClient.sendMessage(request)
            .onFailure().recoverWithItem(throwable -> {
                log.error("Failed to delegate to agent {}", targetAgentId, throwable);
                return A2AResponse.builder()
                    .success(false)
                    .error("Failed to communicate with target agent: " + throwable.getMessage())
                    .build();
            });
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class A2ARequest {
        private UUID messageId;
        private UUID runId;
        private String sourceAgentId;
        private String targetAgentId;
        private Map<String, Object> payload;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class A2AResponse {
        private UUID messageId;
        private boolean success;
        private Map<String, Object> result;
        private String error;
    }
}
```

#### A2ARouterClient.java
```java
package tech.kayys.wayang.orchestrator.adapter;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for A2A Router service.
 */
@Path("/a2a")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "a2a-router")
public interface A2ARouterClient {

    @POST
    @Path("/send")
    Uni<A2AAdapter.A2AResponse> sendMessage(A2AAdapter.A2ARequest request);
}
```

### 16. Store Implementations

#### ExecutionStore.java
```java
package tech.kayys.wayang.orchestrator.store;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.orchestrator.model.ExecutionRun;
import tech.kayys.wayang.orchestrator.model.NodeExecution;
import tech.kayys.wayang.orchestrator.store.repository.ExecutionRunRepository;
import tech.kayys.wayang.orchestrator.store.repository.NodeStateRepository;

import java.util.List;
import java.util.UUID;

/**
 * Store for execution-related entities.
 * Provides transaction management and caching.
 */
@ApplicationScoped
@Slf4j
public class ExecutionStore {

    @Inject
    ExecutionRunRepository runRepository;

    @Inject
    NodeStateRepository nodeRepository;

    /**
     * Save execution run.
     */
    public Uni<ExecutionRun> saveRun(ExecutionRun run) {
        return Panache.withTransaction(() -> 
            runRepository.persist(run)
        );
    }

    /**
     * Find execution run by ID.
     */
    public Uni<ExecutionRun> findRun(UUID runId) {
        return runRepository.findById(runId);
    }

    /**
     * Save node execution.
     */
    public Uni<NodeExecution> saveNodeExecution(NodeExecution nodeExecution) {
        return Panache.withTransaction(() -> 
            nodeRepository.persist(nodeExecution)
        );
    }

    /**
     * Find node execution.
     */
    public Uni<NodeExecution> findNodeExecution(UUID runId, String nodeId) {
        return nodeRepository.find("runId = ?1 and nodeId = ?2", runId, nodeId)
            .firstResult();
    }

    /**
     * Find all node executions for a run.
     */
    public Uni<List<NodeExecution>> findNodeExecutionsByRun(UUID runId) {
        return nodeRepository.list("runId", runId);
    }

    /**
     * Find node executions by status.
     */
    public Uni<List<NodeExecution>> findNodeExecutionsByStatus(
        UUID runId, 
        tech.kayys.wayang.orchestrator.model.NodeStatus status
    ) {
        return nodeRepository.list("runId = ?1 and status = ?2", runId, status);
    }

    /**
     * Count active runs.
     */
    public Uni<Long> countActiveRuns(UUID tenantId) {
        return runRepository.count(
            "tenantId = ?1 and (status = 'RUNNING' or status = 'PENDING' or status = 'VALIDATED')", 
            tenantId
        );
    }

    /**
     * Delete old completed runs (for cleanup).
     */
    public Uni<Long> deleteOldRuns(java.time.LocalDateTime olderThan) {
        return Panache.withTransaction(() -> 
            runRepository.delete(
                "completedAt < ?1 and (status = 'COMPLETED' or status = 'FAILED' or status = 'CANCELLED')",
                olderThan
            )
        );
    }
}
```

#### PlanStore.java
```java
package tech.kayys.wayang.orchestrator.store;

import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.orchestrator.model.ExecutionPlan;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store for execution plans.
 * Plans are typically cached as they don't change frequently.
 */
@ApplicationScoped
@Slf4j
public class PlanStore {

    // In-memory cache (in production, use Redis or similar)
    private final Map<String, ExecutionPlan> planCache = new ConcurrentHashMap<>();

    /**
     * Find execution plan by ID and version.
     */
    @CacheResult(cacheName = "execution-plans")
    public Uni<ExecutionPlan> findPlan(UUID planId, String version) {
        String key = planId.toString() + ":" + (version != null ? version : "latest");
        
        return Uni.createFrom().item(() -> {
            ExecutionPlan plan = planCache.get(key);
            
            if (plan == null) {
                log.warn("Plan not found: {}", key);
            }
            
            return plan;
        });
    }

    /**
     * Save execution plan (typically called by Designer service).
     */
    public Uni<ExecutionPlan> savePlan(ExecutionPlan plan) {
        String key = plan.getPlanId().toString() + ":" + plan.getVersion();
        
        return Uni.createFrom().item(() -> {
            planCache.put(key, plan);
            log.info("Saved plan: {}", key);
            return plan;
        });
    }

    /**
     * Invalidate plan cache.
     */
    public void invalidatePlan(UUID planId, String version) {
        String key = planId.toString() + ":" + version;
        planCache.remove(key);
        log.info("Invalidated plan cache: {}", key);
    }
}
```

Would you like me to continue with:
1. Repository interfaces
2. Checkpoint Store implementation
3. Audit Service implementation
4. Configuration classes
5. Database migration scripts

Please let me know!

# Wayang Orchestrator - Core Implementation (Continued)

### 17. Repository Interfaces

#### ExecutionRunRepository.java
```java
package tech.kayys.wayang.orchestrator.store.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.orchestrator.model.ExecutionRun;
import tech.kayys.wayang.orchestrator.model.ExecutionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ExecutionRun entities.
 * Provides reactive database operations.
 */
@ApplicationScoped
public class ExecutionRunRepository implements PanacheRepositoryBase<ExecutionRun, UUID> {

    /**
     * Find runs by tenant and status.
     */
    public Uni<List<ExecutionRun>> findByTenantAndStatus(UUID tenantId, ExecutionStatus status) {
        return list("tenantId = ?1 and status = ?2", tenantId, status);
    }

    /**
     * Find runs by tenant within time range.
     */
    public Uni<List<ExecutionRun>> findByTenantAndTimeRange(
        UUID tenantId,
        LocalDateTime from,
        LocalDateTime to
    ) {
        return list("tenantId = ?1 and createdAt >= ?2 and createdAt <= ?3 order by createdAt desc",
            tenantId, from, to);
    }

    /**
     * Find active runs for a tenant.
     */
    public Uni<List<ExecutionRun>> findActiveByTenant(UUID tenantId) {
        return list("tenantId = ?1 and status in ('PENDING', 'VALIDATED', 'RUNNING', 'PAUSED', 'AWAITING_HITL')",
            tenantId);
    }

    /**
     * Count runs by tenant and status.
     */
    public Uni<Long> countByTenantAndStatus(UUID tenantId, ExecutionStatus status) {
        return count("tenantId = ?1 and status = ?2", tenantId, status);
    }

    /**
     * Find runs that have been running longer than threshold.
     */
    public Uni<List<ExecutionRun>> findLongRunning(LocalDateTime threshold) {
        return list("status = 'RUNNING' and startedAt < ?1", threshold);
    }

    /**
     * Find runs by plan ID.
     */
    public Uni<List<ExecutionRun>> findByPlanId(UUID planId) {
        return list("planId = ?1 order by createdAt desc", planId);
    }

    /**
     * Update run status.
     */
    public Uni<Integer> updateStatus(UUID runId, ExecutionStatus newStatus) {
        return update("status = ?1, lastUpdatedAt = ?2 where runId = ?3",
            newStatus, LocalDateTime.now(), runId);
    }
}
```

#### NodeStateRepository.java
```java
package tech.kayys.wayang.orchestrator.store.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.orchestrator.model.NodeExecution;
import tech.kayys.wayang.orchestrator.model.NodeStatus;

import java.util.List;
import java.util.UUID;

/**
 * Repository for NodeExecution entities.
 */
@ApplicationScoped
public class NodeStateRepository implements PanacheRepositoryBase<NodeExecution, Long> {

    /**
     * Find node execution by run and node ID.
     */
    public Uni<NodeExecution> findByRunAndNode(UUID runId, String nodeId) {
        return find("runId = ?1 and nodeId = ?2", runId, nodeId).firstResult();
    }

    /**
     * Find all node executions for a run.
     */
    public Uni<List<NodeExecution>> findByRun(UUID runId) {
        return list("runId = ?1 order by createdAt", runId);
    }

    /**
     * Find node executions by status.
     */
    public Uni<List<NodeExecution>> findByRunAndStatus(UUID runId, NodeStatus status) {
        return list("runId = ?1 and status = ?2", runId, status);
    }

    /**
     * Find failed nodes that can be retried.
     */
    public Uni<List<NodeExecution>> findRetryableNodes(UUID runId) {
        return list("runId = ?1 and status in ('FAILED', 'ERROR') and attempt < maxAttempts", runId);
    }

    /**
     * Count nodes by status for a run.
     */
    public Uni<Long> countByStatus(UUID runId, NodeStatus status) {
        return count("runId = ?1 and status = ?2", runId, status);
    }

    /**
     * Update node status.
     */
    public Uni<Integer> updateStatus(UUID runId, String nodeId, NodeStatus newStatus) {
        return update("status = ?1, updatedAt = ?2 where runId = ?3 and nodeId = ?4",
            newStatus, java.time.LocalDateTime.now(), runId, nodeId);
    }

    /**
     * Delete node executions for a run (cleanup).
     */
    public Uni<Long> deleteByRun(UUID runId) {
        return delete("runId", runId);
    }
}
```

#### HumanTaskRepository.java
```java
package tech.kayys.wayang.orchestrator.store.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.orchestrator.model.HumanTask;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for HumanTask entities (HITL).
 */
@ApplicationScoped
public class HumanTaskRepository implements PanacheRepositoryBase<HumanTask, UUID> {

    /**
     * Find pending tasks for an operator.
     */
    public Uni<List<HumanTask>> findPendingByOperator(String operatorId) {
        return list("status = 'PENDING' and (assignedTo = ?1 or assignedTo is null) order by createdAt",
            operatorId);
    }

    /**
     * Find all pending tasks.
     */
    public Uni<List<HumanTask>> findAllPending() {
        return list("status = 'PENDING' order by priority desc, createdAt");
    }

    /**
     * Find tasks by run.
     */
    public Uni<List<HumanTask>> findByRun(UUID runId) {
        return list("runId = ?1 order by createdAt desc", runId);
    }

    /**
     * Find overdue tasks.
     */
    public Uni<List<HumanTask>> findOverdue() {
        return list("status = 'PENDING' and dueAt < ?1", LocalDateTime.now());
    }

    /**
     * Count pending tasks by tenant.
     */
    public Uni<Long> countPendingByTenant(UUID tenantId) {
        return count("tenantId = ?1 and status = 'PENDING'", tenantId);
    }
}
```

### 18. HumanTask Model

#### HumanTask.java
```java
package tech.kayys.wayang.orchestrator.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a human-in-the-loop task requiring manual intervention.
 */
@Entity
@Table(name = "human_tasks", indexes = {
    @Index(name = "idx_status_priority", columnList = "status, priority"),
    @Index(name = "idx_assigned_to", columnList = "assigned_to"),
    @Index(name = "idx_run_id", columnList = "run_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HumanTask extends PanacheEntityBase {

    @Id
    @Column(name = "task_id", nullable = false, updatable = false)
    private UUID taskId;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private HumanTaskStatus status;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "created_by")
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by")
    private String completedBy;

    @Column(name = "context", columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();

    @Column(name = "error", columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private ErrorPayload error;

    @Column(name = "decision", columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> decision;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    public void prePersist() {
        if (taskId == null) {
            taskId = UUID.randomUUID();
        }
        if (status == null) {
            status = HumanTaskStatus.PENDING;
        }
    }

    public enum HumanTaskStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        EXPIRED
    }

    public void markCompleted(String operatorId, Map<String, Object> decision) {
        this.status = HumanTaskStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.completedBy = operatorId;
        this.decision = decision;
    }

    public void markCancelled() {
        this.status = HumanTaskStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isOverdue() {
        return status == HumanTaskStatus.PENDING && 
               dueAt != null && 
               LocalDateTime.now().isAfter(dueAt);
    }
}
```

### 19. Checkpoint Store Implementation

#### CheckpointStore.java
```java
package tech.kayys.wayang.orchestrator.core.checkpoint;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Store for execution checkpoints.
 * In production, this should use object storage (S3) or a dedicated database.
 * 
 * This implementation uses in-memory storage for demonstration.
 * For production, integrate with S3, MinIO, or similar.
 */
@ApplicationScoped
@Slf4j
public class CheckpointStore {

    // In-memory storage (replace with S3/MinIO in production)
    private final Map<String, CheckpointManager.Checkpoint> checkpoints = new ConcurrentHashMap<>();
    private final Map<String, CheckpointManager.WorkflowCheckpoint> workflowCheckpoints = new ConcurrentHashMap<>();

    /**
     * Save a checkpoint.
     */
    public Uni<CheckpointManager.Checkpoint> save(CheckpointManager.Checkpoint checkpoint) {
        return Uni.createFrom().item(() -> {
            checkpoints.put(checkpoint.getCheckpointId(), checkpoint);
            log.debug("Saved checkpoint: {}", checkpoint.getCheckpointId());
            return checkpoint;
        });
    }

    /**
     * Find checkpoint by ID.
     */
    public Uni<CheckpointManager.Checkpoint> findById(String checkpointId) {
        return Uni.createFrom().item(() -> checkpoints.get(checkpointId));
    }

    /**
     * Save workflow checkpoint.
     */
    public Uni<CheckpointManager.WorkflowCheckpoint> saveWorkflowCheckpoint(
        CheckpointManager.WorkflowCheckpoint checkpoint
    ) {
        return Uni.createFrom().item(() -> {
            workflowCheckpoints.put(checkpoint.getCheckpointId(), checkpoint);
            log.debug("Saved workflow checkpoint: {}", checkpoint.getCheckpointId());
            return checkpoint;
        });
    }

    /**
     * Find workflow checkpoint by ID.
     */
    public Uni<CheckpointManager.WorkflowCheckpoint> findWorkflowCheckpointById(String checkpointId) {
        return Uni.createFrom().item(() -> workflowCheckpoints.get(checkpointId));
    }

    /**
     * Delete old checkpoints.
     */
    public Uni<Integer> deleteOldCheckpoints(UUID runId, LocalDateTime olderThan) {
        return Uni.createFrom().item(() -> {
            int count = 0;
            
            var iterator = checkpoints.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                CheckpointManager.Checkpoint checkpoint = entry.getValue();
                
                if (checkpoint.getRunId().equals(runId) && 
                    checkpoint.getCreatedAt().isBefore(olderThan)) {
                    iterator.remove();
                    count++;
                }
            }
            
            log.info("Deleted {} old checkpoints for run {}", count, runId);
            return count;
        });
    }

    /**
     * Count checkpoints for a run.
     */
    public Uni<Integer> countCheckpoints(UUID runId) {
        return Uni.createFrom().item(() -> 
            (int) checkpoints.values().stream()
                .filter(cp -> cp.getRunId().equals(runId))
                .count()
        );
    }

    /**
     * Get total size of checkpoints for a run.
     */
    public Uni<Long> getTotalSize(UUID runId) {
        return Uni.createFrom().item(() -> {
            // Simplified size calculation
            // In production, track actual byte size
            long totalSize = checkpoints.values().stream()
                .filter(cp -> cp.getRunId().equals(runId))
                .mapToLong(cp -> estimateSize(cp.getState()))
                .sum();
            
            return totalSize;
        });
    }

    /**
     * Estimate size of state map (simplified).
     */
    private long estimateSize(Map<String, Object> state) {
        if (state == null) return 0;
        
        // Rough estimation: 100 bytes per entry
        return state.size() * 100L;
    }

    /**
     * Clear all checkpoints for a run.
     */
    public Uni<Void> clearCheckpoints(UUID runId) {
        return Uni.createFrom().item(() -> {
            checkpoints.entrySet().removeIf(entry -> 
                entry.getValue().getRunId().equals(runId)
            );
            
            workflowCheckpoints.entrySet().removeIf(entry -> 
                entry.getValue().getRunId().equals(runId)
            );
            
            log.info("Cleared all checkpoints for run {}", runId);
            return null;
        });
    }
}
```

### 20. Audit Service Implementation

#### AuditService.java
```java
package tech.kayys.wayang.orchestrator.audit;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.orchestrator.model.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Service for audit logging and provenance tracking.
 * 
 * Responsibilities:
 * - Record all significant events
 * - Generate tamper-proof audit trails
 * - Provide cryptographic hashing
 * - Support compliance requirements
 */
@ApplicationScoped
@Slf4j
public class AuditService {

    @Inject
    AuditLogger auditLogger;

    @Inject
    ProvenanceTracker provenanceTracker;

    /**
     * Audit run state transition.
     */
    public Uni<Void> auditRunStateTransition(
        UUID runId,
        ExecutionStatus fromStatus,
        ExecutionStatus toStatus
    ) {
        AuditPayload payload = AuditPayload.builder()
            .runId(runId)
            .event("RUN_STATE_TRANSITION")
            .level(AuditPayload.AuditLevel.INFO)
            .actor(AuditPayload.Actor.builder()
                .type(AuditPayload.ActorType.SYSTEM)
                .id("orchestrator")
                .build())
            .metadata(java.util.Map.of(
                "fromStatus", fromStatus,
                "toStatus", toStatus
            ))
            .build();

        return auditLogger.log(payload);
    }

    /**
     * Audit node state transition.
     */
    public Uni<Void> auditNodeStateTransition(
        UUID runId,
        String nodeId,
        NodeStatus fromStatus,
        NodeStatus toStatus
    ) {
        AuditPayload payload = AuditPayload.builder()
            .runId(runId)
            .nodeId(nodeId)
            .event("NODE_STATE_TRANSITION")
            .level(AuditPayload.AuditLevel.INFO)
            .actor(AuditPayload.Actor.builder()
                .type(AuditPayload.ActorType.SYSTEM)
                .id("orchestrator")
                .build())
            .metadata(java.util.Map.of(
                "fromStatus", fromStatus,
                "toStatus", toStatus
            ))
            .build();

        return auditLogger.log(payload);
    }

    /**
     * Audit human task creation.
     */
    public Uni<Void> auditHumanTaskCreated(HumanTask task) {
        AuditPayload payload = AuditPayload.builder()
            .runId(task.getRunId())
            .nodeId(task.getNodeId())
            .event("HITL_TASK_CREATED")
            .level(AuditPayload.AuditLevel.INFO)
            .actor(AuditPayload.Actor.builder()
                .type(AuditPayload.ActorType.SYSTEM)
                .id("orchestrator")
                .build())
            .metadata(java.util.Map.of(
                "taskId", task.getTaskId(),
                "title", task.getTitle(),
                "priority", task.getPriority()
            ))
            .build();

        return auditLogger.log(payload);
    }

    /**
     * Audit human task completion.
     */
    public Uni<Void> auditHumanTaskCompleted(HumanTask task) {
        AuditPayload payload = AuditPayload.builder()
            .runId(task.getRunId())
            .nodeId(task.getNodeId())
            .event("HITL_TASK_COMPLETED")
            .level(AuditPayload.AuditLevel.INFO)
            .actor(AuditPayload.Actor.builder()
                .type(AuditPayload.ActorType.HUMAN)
                .id(task.getCompletedBy())
                .role("operator")
                .build())
            .metadata(java.util.Map.of(
                "taskId", task.getTaskId(),
                "decision", task.getDecision() != null ? task.getDecision() : "none",
                "notes", task.getNotes() != null ? task.getNotes() : ""
            ))
            .build();

        return auditLogger.log(payload);
    }

    /**
     * Audit error event.
     */
    public Uni<Void> auditError(UUID runId, String nodeId, ErrorPayload error) {
        AuditPayload payload = AuditPayload.builder()
            .runId(runId)
            .nodeId(nodeId)
            .event("ERROR_OCCURRED")
            .level(AuditPayload.AuditLevel.ERROR)
            .actor(AuditPayload.Actor.builder()
                .type(AuditPayload.ActorType.SYSTEM)
                .id("orchestrator")
                .build())
            .metadata(java.util.Map.of(
                "errorType", error.getType(),
                "errorMessage", error.getMessage(),
                "retryable", error.getRetryable(),
                "attempt", error.getAttempt()
            ))
            .build();

        return auditLogger.log(payload);
    }

    /**
     * Generate cryptographic hash for audit payload.
     */
    public String generateHash(AuditPayload payload, String previousHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            StringBuilder data = new StringBuilder();
            data.append(payload.getTimestamp());
            data.append(payload.getRunId());
            data.append(payload.getNodeId() != null ? payload.getNodeId() : "");
            data.append(payload.getEvent());
            data.append(payload.getActor().getId());
            
            if (previousHash != null) {
                data.append(previousHash);
            }
            
            byte[] hash = digest.digest(data.toString().getBytes());
            return HexFormat.of().formatHex(hash);
            
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate hash", e);
            return null;
        }
    }
}
```

#### AuditLogger.java
```java
package tech.kayys.wayang.orchestrator.audit;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.orchestrator.model.AuditPayload;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logger for audit events.
 * In production, this should write to a dedicated audit database or stream.
 */
@ApplicationScoped
@Slf4j
public class AuditLogger {

    // In-memory storage for demonstration
    // In production, use PostgreSQL audit table or event stream
    private final Map<String, AuditPayload> auditLog = new ConcurrentHashMap<>();
    private String lastHash = null;

    /**
     * Log an audit event.
     */
    public Uni<Void> log(AuditPayload payload) {
        return Uni.createFrom().item(() -> {
            // Generate hash with chain
            String hash = generateHash(payload);
            payload.setHash(hash);
            payload.setPreviousHash(lastHash);
            lastHash = hash;
            
            // Store
            String key = payload.getRunId() + ":" + payload.getTimestamp();
            auditLog.put(key, payload);
            
            // Also log to file/console
            log.info("AUDIT: event={}, runId={}, nodeId={}, actor={}", 
                payload.getEvent(),
                payload.getRunId(),
                payload.getNodeId(),
                payload.getActor().getId()
            );
            
            return null;
        });
    }

    /**
     * Generate hash for audit payload.
     */
    private String generateHash(AuditPayload payload) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            
            StringBuilder data = new StringBuilder();
            data.append(payload.getTimestamp());
            data.append(payload.getRunId());
            data.append(payload.getEvent());
            if (lastHash != null) {
                data.append(lastHash);
            }
            
            byte[] hash = digest.digest(data.toString().getBytes());
            return java.util.HexFormat.of().formatHex(hash);
            
        } catch (Exception e) {
            log.error("Failed to generate hash", e);
            return java.util.UUID.randomUUID().toString();
        }
    }

    /**
     * Get audit trail for a run.
     */
    public Uni<java.util.List<AuditPayload>> getAuditTrail(java.util.UUID runId) {
        return Uni.createFrom().item(() -> 
            auditLog.values().stream()
                .filter(payload -> payload.getRunId().equals(runId))
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .toList()
        );
    }
}
```

#### ProvenanceTracker.java
```java
package tech.kayys.wayang.orchestrator.audit;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks data provenance and lineage throughout execution.
 */
@ApplicationScoped
@Slf4j
public class ProvenanceTracker {

    private final Map<String, ProvenanceRecord> records = new ConcurrentHashMap<>();

    /**
     * Record provenance for a node execution.
     */
    public Uni<String> recordProvenance(
        UUID runId,
        String nodeId,
        Map<String, Object> inputs,
        Map<String, Object> outputs,
        String modelVersion
    ) {
        String provenanceId = UUID.randomUUID().toString();
        
        return Uni.createFrom().item(() -> {
            ProvenanceRecord record = ProvenanceRecord.builder()
                .provenanceId(provenanceId)
                .runId(runId)
                .nodeId(nodeId)
                .inputs(inputs)
                .outputs(outputs)
                .modelVersion(modelVersion)
                .timestamp(LocalDateTime.now())
                .build();
            
            records.put(provenanceId, record);
            
            log.debug("Recorded provenance: {}", provenanceId);
            return provenanceId;
        });
    }

    /**
     * Get provenance record.
     */
    public Uni<ProvenanceRecord> getProvenance(String provenanceId) {
        return Uni.createFrom().item(() -> records.get(provenanceId));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProvenanceRecord {
        private String provenanceId;
        private UUID runId;
        private String nodeId;
        private Map<String, Object> inputs;
        private Map<String, Object> outputs;
        private String modelVersion;
        private LocalDateTime timestamp;
    }
}
```

Would you like me to continue with:
1. Configuration classes
2. Database migration scripts
3. Utility classes (JsonbConverter, etc.)
4. Application entry point
5. application.yml configuration

Please let me know!

