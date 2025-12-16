package io.kayys.wayang.designer.service;

import io.kayys.wayang.designer.domain.*;

package io.kayys.wayang.designer.domain;


// DTOs for diff/merge results continued in next artifact...

package io.kayys.wayang.designer.graphql;

import io.kayys.wayang.designer.domain.*;
import io.kayys.wayang.designer.service.*;
import io.kayys.wayang.common.context.TenantContext;


/**
* GraphQL Error Handler
*/
package io.kayys.wayang.designer.graphql;

package io.kayys.wayang.designer.websocket;


package io.kayys.wayang.designer.websocket;

import io.kayys.wayang.common.context.TenantContext;
import io.kayys.wayang.designer.service.LockService;







<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>io.kayys.wayang</groupId>
  <artifactId>wayang-designer-backend</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>jar</packaging>

  <name>Wayang Designer Backend</name>
  <description>Visual AI Agent Workflow Designer Backend Service</description>

  <properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

    <!-- Quarkus -->
    <quarkus.platform.version>3.16.3</quarkus.platform.version>
    <quarkus.platform.group-id>io.quarkus.platform</quarkus.platform.group-id>
    <quarkus.platform.artifact-id>quarkus-bom</quarkus.platform.artifact-id>

    <!-- Dependencies -->
    <hypersistence-utils.version>3.8.2</hypersistence-utils.version>
    <mapstruct.version>1.6.3</mapstruct.version>
    <lombok.version>1.18.34</lombok.version>

    <!-- Plugin versions -->
    <compiler-plugin.version>3.13.0</compiler-plugin.version>
    <surefire-plugin.version>3.5.2</surefire-plugin.version>
    <failsafe-plugin.version>3.5.2</failsafe-plugin.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <!-- Quarkus BOM -->
      <dependency>
        <groupId>${quarkus.platform.group-id}</groupId>
        <artifactId>${quarkus.platform.artifact-id}</artifactId>
        <version>${quarkus.platform.version}</version>
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
      <artifactId>quarkus-resteasy-reactive</artifactId>
    </dependency>
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-resteasy-reactive-jackson</artifactId>
    </dependency>

    <!-- Reactive Hibernate & Panache -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-hibernate-reactive-panache</artifactId>
    </dependency>
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-reactive-pg-client</artifactId>
    </dependency>

    <!-- JSON Support (Hypersistence for JSONB) -->
    <dependency>
      <groupId>io.hypersistence</groupId>
      <artifactId>hypersistence-utils-hibernate-63</artifactId>
      <version>${hypersistence-utils.version}</version>
    </dependency>

    <!-- Redis -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-redis-client</artifactId>
    </dependency>
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-cache</artifactId>
    </dependency>

    <!-- Security -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-oidc</artifactId>
    </dependency>
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-smallrye-jwt</artifactId>
    </dependency>
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-security</artifactId>
    </dependency>

    <!-- REST Client -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-rest-client-reactive</artifactId>
    </dependency>
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-rest-client-reactive-jackson</artifactId>
    </dependency>

    <!-- Fault Tolerance -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-smallrye-fault-tolerance</artifactId>
    </dependency>

    <!-- OpenAPI/Swagger -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-smallrye-openapi</artifactId>
    </dependency>

    <!-- Observability - Health -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-smallrye-health</artifactId>
    </dependency>

    <!-- Observability - Metrics -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-micrometer-registry-prometheus</artifactId>
    </dependency>

    <!-- Observability - Tracing -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-opentelemetry</artifactId>
    </dependency>

    <!-- Validation -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-hibernate-validator</artifactId>
    </dependency>

    <!-- Kubernetes -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-kubernetes</artifactId>
    </dependency>
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-container-image-jib</artifactId>
    </dependency>

    <!-- Utilities -->
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <version>${lombok.version}</version>
      <scope>provided</scope>
    </dependency>

    <!-- Testing -->
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-junit5</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-junit5-mockito</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>io.rest-assured</groupId>
      <artifactId>rest-assured</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-test-security</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>postgresql</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.assertj</groupId>
      <artifactId>assertj-core</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <!-- Quarkus Plugin -->
      <plugin>
        <groupId>${quarkus.platform.group-id}</groupId>
        <artifactId>quarkus-maven-plugin</artifactId>
        <version>${quarkus.platform.version}</version>
        <extensions>true</extensions>
        <executions>
          <execution>
            <goals>
              <goal>build</goal>
              <goal>generate-code</goal>
              <goal>generate-code-tests</goal>
            </goals>
          </execution>
        </executions>
      </plugin>

      <!-- Compiler Plugin -->
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>${compiler-plugin.version}</version>
        <configuration>
          <parameters>true</parameters>
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

      <!-- Surefire Plugin (Unit Tests) -->
      <plugin>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>${surefire-plugin.version}</version>
        <configuration>
          <systemPropertyVariables>
            <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
            <maven.home>${maven.home}</maven.home>
          </systemPropertyVariables>
        </configuration>
      </plugin>

      <!-- Failsafe Plugin (Integration Tests) -->
      <plugin>
        <artifactId>maven-failsafe-plugin</artifactId>
        <version>${failsafe-plugin.version}</version>
        <executions>
          <execution>
            <goals>
              <goal>integration-test</goal>
              <goal>verify</goal>
            </goals>
            <configuration>
              <systemPropertyVariables>
                <native.image.path>${project.build.directory}/${project.build.finalName}-runner</native.image.path>
                <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
                <maven.home>${maven.home}</maven.home>
              </systemPropertyVariables>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>

  <profiles>
    <!-- Native Image Profile -->
    <profile>
      <id>native</id>
      <activation>
        <property>
          <name>native</name>
        </property>
      </activation>
      <properties>
        <skipITs>false</skipITs>
        <quarkus.package.type>native</quarkus.package.type>
      </properties>
    </profile>
  </profiles>
</project>
package io.kayys.wayang.common.security;

import io.kayys.wayang.common.context.TenantContext;


# application.yml - Wayang Designer Backend Configuration


package io.kayys.wayang.designer.exception;


package io.kayys.wayang.designer.client;

import io.kayys.wayang.designer.domain.*;


package io.kayys.wayang.designer.api;

import io.kayys.wayang.designer.domain.*;
import io.kayys.wayang.designer.service.*;
import io.kayys.wayang.common.api.ResponseWrapper;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
* WorkspaceResource - REST API for workspace management
*/
@Path("/api/v1/workspaces")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Workspaces", description = "Workspace management operations")
@RolesAllowed({"admin", "designer"})
public class WorkspaceResource {

private static final Logger LOG = Logger.getLogger(WorkspaceResource.class);

@Inject
WorkspaceService workspaceService;

@GET
@Operation(summary = "List all workspaces", description = "Get all workspaces for current tenant")
@APIResponse(responseCode = "200", description = "Success",
content = @Content(schema = @Schema(implementation = WorkspaceListResponse.class)))
public Uni
<Response> listWorkspaces() {
return workspaceService.listWorkspaces()
.map(workspaces -> Response.ok(new WorkspaceListResponse(workspaces)).build());
}

@POST
@Operation(summary = "Create workspace", description = "Create a new workspace")
@APIResponse(responseCode = "201", description = "Created",
content = @Content(schema = @Schema(implementation = WorkspaceResponse.class)))
@APIResponse(responseCode = "400", description = "Invalid request")
@ResponseStatus(201)
public Uni
<Response> createWorkspace(@Valid CreateWorkspaceDTO request) {
WorkspaceService.CreateWorkspaceRequest serviceRequest =
new WorkspaceService.CreateWorkspaceRequest();
serviceRequest.name = request.name;
serviceRequest.description = request.description;
serviceRequest.metadata = request.metadata;

return workspaceService.createWorkspace(serviceRequest)
.map(workspace -> Response.created(
UriBuilder.fromResource(WorkspaceResource.class)
.path(workspace.id.toString())
.build())
.entity(new WorkspaceResponse(workspace))
.build());
}

@GET
@Path("/{workspaceId}")
@Operation(summary = "Get workspace", description = "Get workspace by ID")
@APIResponse(responseCode = "200", description = "Success")
@APIResponse(responseCode = "404", description = "Workspace not found")
public Uni
<Response> getWorkspace(@PathParam("workspaceId") UUID workspaceId) {
return workspaceService.getWorkspace(workspaceId)
.map(workspace -> Response.ok(new WorkspaceResponse(workspace)).build());
}

@PUT
@Path("/{workspaceId}")
@Operation(summary = "Update workspace", description = "Update workspace details")
@APIResponse(responseCode = "200", description = "Success")
@APIResponse(responseCode = "404", description = "Workspace not found")
public Uni
<Response> updateWorkspace(
@PathParam("workspaceId") UUID workspaceId,
@Valid UpdateWorkspaceDTO request) {

WorkspaceService.UpdateWorkspaceRequest serviceRequest =
new WorkspaceService.UpdateWorkspaceRequest();
serviceRequest.name = request.name;
serviceRequest.description = request.description;
serviceRequest.metadata = request.metadata;

return workspaceService.updateWorkspace(workspaceId, serviceRequest)
.map(workspace -> Response.ok(new WorkspaceResponse(workspace)).build());
}

@DELETE
@Path("/{workspaceId}")
@Operation(summary = "Delete workspace", description = "Delete workspace (soft delete)")
@APIResponse(responseCode = "204", description = "Deleted")
@APIResponse(responseCode = "404", description = "Workspace not found")
public Uni
<Response> deleteWorkspace(@PathParam("workspaceId") UUID workspaceId) {
return workspaceService.deleteWorkspace(workspaceId)
.replaceWith(Response.noContent().build());
}

// DTOs
public static class CreateWorkspaceDTO {
@NotBlank(message = "Workspace name is required")
public String name;
public String description;
public java.util.Map
<String , Object> metadata;
}

public static class UpdateWorkspaceDTO {
public String name;
public String description;
public java.util.Map
<String , Object> metadata;
}

public record WorkspaceResponse(
UUID id,
String name,
String description,
String tenantId,
String ownerId,
java.time.Instant createdAt,
java.time.Instant updatedAt,
Workspace.WorkspaceStatus status,
java.util.Map
<String , Object> metadata
) {
public WorkspaceResponse(Workspace workspace) {
this(
workspace.id,
workspace.name,
workspace.description,
workspace.tenantId,
workspace.ownerId,
workspace.createdAt,
workspace.updatedAt,
workspace.status,
workspace.metadata
);
}
}

public record WorkspaceListResponse(List
<WorkspaceResponse> workspaces) {
public WorkspaceListResponse(List
<Workspace> workspaces) {
this(workspaces.stream().map(WorkspaceResponse::new).toList());
}
}
}


package io.kayys.wayang.designer.service;

import io.kayys.wayang.designer.domain.*;
import io.kayys.wayang.designer.persistence.*;
import io.kayys.wayang.designer.client.*;
import io.kayys.wayang.designer.exception.*;
import io.kayys.wayang.common.context.TenantContext;
import io.kayys.wayang.common.audit.AuditEvent;
import io.kayys.wayang.common.audit.AuditService;


package io.kayys.wayang.designer.persistence;

import io.kayys.wayang.designer.domain.*;


package io.kayys.wayang.designer.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import java.time.Instant;
import java.util.*;

/**
* Workspace - Top-level organizational container for workflows.
* Supports multi-tenancy isolation.
*/
@Entity
@Table(name = "workspaces", indexes = {
@Index(name = "idx_workspace_tenant", columnList = "tenant_id"),
@Index(name = "idx_workspace_owner", columnList = "owner_id")
})
public class Workspace extends PanacheEntityBase {

@Id
@GeneratedValue
public UUID id;

@Column(nullable = false, length = 128)
public String name;

@Column(length = 2000)
public String description;

@Column(name = "tenant_id", nullable = false)
public String tenantId;

@Column(name = "owner_id", nullable = false)
public String ownerId;

@Column(name = "created_at", nullable = false)
public Instant createdAt;

@Column(name = "updated_at")
public Instant updatedAt;

@Type(JsonBinaryType.class)
@Column(columnDefinition = "jsonb")
public Map
<String , Object> metadata;

@Column(nullable = false)
@Enumerated(EnumType.STRING)
public WorkspaceStatus status = WorkspaceStatus.ACTIVE;

@PrePersist
void prePersist() {
createdAt = Instant.now();
updatedAt = createdAt;
if (metadata == null) metadata = new HashMap
<>();
}

@PreUpdate
void preUpdate() {
updatedAt = Instant.now();
}

public enum WorkspaceStatus {
ACTIVE, ARCHIVED, DELETED
}

// Multi-tenant query helpers
public static Uni
<List
<Workspace>> findByTenant(String tenantId) {
return find("tenantId = ?1 and status = ?2", tenantId, WorkspaceStatus.ACTIVE)
.list();
}
}

/**
* Workflow - Complete workflow definition with logic and UI layers.
*/
@Entity
@Table(name = "workflows", indexes = {
@Index(name = "idx_workflow_workspace", columnList = "workspace_id"),
@Index(name = "idx_workflow_tenant", columnList = "tenant_id"),
@Index(name = "idx_workflow_version", columnList = "version")
})
public class Workflow extends PanacheEntityBase {

@Id
@GeneratedValue
public UUID id;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "workspace_id", nullable = false)
public Workspace workspace;

@Column(nullable = false, length = 128)
public String name;

@Column(length = 2000)
public String description;

@Column(name = "tenant_id", nullable = false)
public String tenantId;

@Column(nullable = false, length = 32)
public String version;

@Column(name = "created_by", nullable = false)
public String createdBy;

@Column(name = "created_at", nullable = false)
public Instant createdAt;

@Column(name = "updated_at")
public Instant updatedAt;

@Column(name = "published_at")
public Instant publishedAt;

@Column(nullable = false)
@Enumerated(EnumType.STRING)
public WorkflowStatus status = WorkflowStatus.DRAFT;

/**
* Logic layer - nodes, connections, rules (JSONB)
*/
@Type(JsonBinaryType.class)
@Column(name = "logic_definition", columnDefinition = "jsonb", nullable = false)
public LogicDefinition logic;

/**
* UI layer - canvas state, node positions (JSONB)
*/
@Type(JsonBinaryType.class)
@Column(name = "ui_definition", columnDefinition = "jsonb")
public UIDefinition ui;

/**
* Runtime configuration (JSONB)
*/
@Type(JsonBinaryType.class)
@Column(name = "runtime_config", columnDefinition = "jsonb")
public RuntimeConfig runtime;

/**
* Validation results cache
*/
@Type(JsonBinaryType.class)
@Column(name = "validation_result", columnDefinition = "jsonb")
public ValidationResult validationResult;

@Type(JsonBinaryType.class)
@Column(columnDefinition = "jsonb")
public Map
<String , Object> metadata;

@Version
public Long entityVersion; // Optimistic locking

@PrePersist
void prePersist() {
createdAt = Instant.now();
updatedAt = createdAt;
if (metadata == null) metadata = new HashMap
<>();
}

@PreUpdate
void preUpdate() {
updatedAt = Instant.now();
}

public enum WorkflowStatus {
DRAFT, // Being edited
VALIDATING, // Validation in progress
VALID, // Passed validation
INVALID, // Failed validation
PUBLISHED, // Immutable published version
ARCHIVED, // Historical version
DELETED // Soft deleted
}

// Domain logic
public boolean canPublish() {
return status == WorkflowStatus.VALID && validationResult != null
&& validationResult.isValid();
}

public boolean isPublished() {
return status == WorkflowStatus.PUBLISHED;
}

// Multi-tenant queries
public static Uni
<List
<Workflow>> findByWorkspace(UUID workspaceId, String tenantId) {
return find("workspace.id = ?1 and tenantId = ?2 and status != ?3",
workspaceId, tenantId, WorkflowStatus.DELETED)
.list();
}

public static Uni
<Workflow> findLatestVersion(UUID workspaceId, String name, String tenantId) {
return find("workspace.id = ?1 and name = ?2 and tenantId = ?3 " +
"and status != ?4 order by createdAt desc",
workspaceId, name, tenantId, WorkflowStatus.DELETED)
.firstResult();
}
}


/**
* NodeDefinition - Individual node in workflow
*/
public class NodeDefinition {
public String id; // Unique within workflow
public String type; // Node type (plugin ID)
public String name;
public String description;
public Map
<String , Object> properties = new HashMap
<>();
public List
<PortDescriptor> inputs = new ArrayList
<>();
public List
<OutputChannel> outputs = new ArrayList
<>();
public Map
<String , Object> errorHandling;
public ResourceProfile resourceProfile;
public Map
<String , Object> metadata = new HashMap
<>();
}

/**
* ConnectionDefinition - Edge between nodes
*/
public class ConnectionDefinition {
public String id;
public String from; // Source node ID
public String to; // Target node ID
public String fromPort; // Source port name
public String toPort; // Target port name
public String condition; // CEL expression (optional)
public ConnectionType type = ConnectionType.DATA;
public Map
<String , Object> metadata = new HashMap
<>();

public enum ConnectionType {
DATA, // Normal data flow
ERROR, // Error handling flow
CONTROL // Control flow only
}
}

/**
* UIDefinition - Visual layout information
*/
public class UIDefinition {
public CanvasState canvas = new CanvasState();
public List
<NodeUI> nodes = new ArrayList
<>();
public List
<ConnectionUI> connections = new ArrayList
<>();

public static class CanvasState {
public double zoom = 1.0;
public Point offset = new Point(0, 0);
public String background = "grid";
public boolean snapToGrid = true;
}

public static class NodeUI {
public String ref; // Node ID
public Point position;
public Size size;
public String icon;
public String color;
public String shape = "rectangle";
public boolean collapsed = false;
public int zIndex = 0;
}

public static class ConnectionUI {
public String ref; // Connection ID
public String color;
public String pathStyle = "bezier";
}

public static class Point {
public double x;
public double y;

public Point(double x, double y) {
this.x = x;
this.y = y;
}
}

public static class Size {
public double width;
public double height;
}
}

/**
* RuntimeConfig - Runtime execution configuration
*/
public class RuntimeConfig {
public ExecutionMode mode = ExecutionMode.SYNC;
public RetryPolicy retryPolicy;
public Map
<String , Object> sla;
public Map
<String , Object> telemetry;
public List
<Trigger> triggers = new ArrayList
<>();

public enum ExecutionMode {
SYNC, ASYNC, STREAM
}

public static class RetryPolicy {
public int maxAttempts = 3;
public long initialDelayMs = 200;
public long maxDelayMs = 30000;
public BackoffStrategy backoff = BackoffStrategy.EXPONENTIAL;
public boolean jitter = true;

public enum BackoffStrategy {
FIXED, EXPONENTIAL, LINEAR
}
}

public static class Trigger {
public TriggerType type;
public String expression; // Cron or CEL
public String path; // Webhook path

public enum TriggerType {
CRON, WEBHOOK, MQ, MANUAL, SCHEDULE
}
}
}

/**
* ValidationResult - Workflow validation outcome
*/
public class ValidationResult {
public boolean valid = true;
public Instant validatedAt;
public String validatorVersion;
public List
<ValidationError> errors = new ArrayList
<>();
public List
<ValidationWarning> warnings = new ArrayList
<>();
public Map
<String , Object> metadata = new HashMap
<>();

public boolean isValid() {
return valid && (errors == null || errors.isEmpty());
}

public static class ValidationError {
public String code;
public String message;
public String nodeId;
public String path;
public ErrorSeverity severity = ErrorSeverity.ERROR;

public enum ErrorSeverity {
ERROR, WARNING, INFO
}
}

public static class ValidationWarning {
public String message;
public String nodeId;
public String suggestion;
}
}

/**
* PortDescriptor - Node input/output port definition
*/
public class PortDescriptor {
public String name;
public String displayName;
public String description;
public DataType data;

public static class DataType {
public String type; // json, string, number, etc.
public String format; // text, html, base64, etc.
public Object schema; // JSON Schema
public Multiplicity multiplicity = Multiplicity.SINGLE;
public boolean required = true;
public Object defaultValue;
public boolean sensitive = false;

public enum Multiplicity {
SINGLE, LIST, MAP, STREAM
}
}
}

/**
* OutputChannel - Node output with routing
*/
public class OutputChannel {
public String name;
public String displayName;
public ChannelType type;
public String condition; // CEL expression
public Object schema;

public enum ChannelType {
SUCCESS, ERROR, CONDITIONAL, AGENT_DECISION, STREAM, EVENT
}
}

/**
* ResourceProfile - Resource requirements
*/
public class ResourceProfile {
public String cpu = "100m";
public String memory = "128Mi";
public int gpu = 0;
public String ephemeralStorage;
public long timeoutMs = 30000;
}

/**
* WorkflowLock - Concurrent editing lock
*/
@Entity
@Table(name = "workflow_locks", indexes = {
@Index(name = "idx_lock_workflow", columnList = "workflow_id"),
@Index(name = "idx_lock_expires", columnList = "expires_at")
})
public class WorkflowLock extends PanacheEntityBase {

@Id
@GeneratedValue
public UUID id;

@Column(name = "workflow_id", nullable = false)
public UUID workflowId;

@Column(name = "user_id", nullable = false)
public String userId;

@Column(name = "session_id", nullable = false)
public String sessionId;

@Column(name = "acquired_at", nullable = false)
public Instant acquiredAt;

@Column(name = "expires_at", nullable = false)
public Instant expiresAt;

@Column(name = "heartbeat_at")
public Instant heartbeatAt;

@Column(nullable = false)
@Enumerated(EnumType.STRING)
public LockType type;

public enum LockType {
EXCLUSIVE, // Full write lock
SHARED // Read lock
}

public boolean isExpired() {
return Instant.now().isAfter(expiresAt);
}

public static Uni
<WorkflowLock> findActiveLock(UUID workflowId) {
return find("workflowId = ?1 and expiresAt > ?2 order by acquiredAt desc",
workflowId, Instant.now())
.firstResult();
}
}

/**
* WorkflowDraft - Auto-saved draft snapshots
*/
@Entity
@Table(name = "workflow_drafts", indexes = {
@Index(name = "idx_draft_workflow", columnList = "workflow_id"),
@Index(name = "idx_draft_user", columnList = "user_id")
})
public class WorkflowDraft extends PanacheEntityBase {

@Id
@GeneratedValue
public UUID id;

@Column(name = "workflow_id", nullable = false)
public UUID workflowId;

@Column(name = "user_id", nullable = false)
public String userId;

@Type(JsonBinaryType.class)
@Column(name = "draft_content", columnDefinition = "jsonb", nullable = false)
public WorkflowSnapshot content;

@Column(name = "saved_at", nullable = false)
public Instant savedAt;

@Column(name = "auto_saved")
public boolean autoSaved = true;

@PrePersist
void prePersist() {
savedAt = Instant.now();
}

public static class WorkflowSnapshot {
public LogicDefinition logic;
public UIDefinition ui;
public Map
<String , Object> metadata;
}
}