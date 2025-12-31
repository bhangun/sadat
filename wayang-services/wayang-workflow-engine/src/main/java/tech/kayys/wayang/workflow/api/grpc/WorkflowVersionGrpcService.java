package tech.kayys.wayang.workflow.api.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.stream.Collectors;

import tech.kayys.wayang.workflow.version.service.WorkflowVersionManager;
import tech.kayys.wayang.workflow.v1.*;
import com.google.protobuf.Empty;
import tech.kayys.wayang.workflow.version.dto.VersionRequest;
import tech.kayys.wayang.workflow.version.dto.PublishOptions;
import tech.kayys.wayang.workflow.security.annotations.ControlPlaneSecured;

@GrpcService
@ControlPlaneSecured
public class WorkflowVersionGrpcService implements WorkflowVersionService {

        @Inject
        WorkflowVersionManager versionManager;

        @Override
        public Uni<WorkflowVersion> createVersion(CreateVersionRequest request) {
                VersionRequest domainRequest = new VersionRequest(
                                request.getWorkflowId(),
                                request.getVersion(),
                                request.getPreviousVersion(),
                                request.getCreatedBy());

                return versionManager.createVersion(domainRequest)
                                .map(this::toProto);
        }

        @Override
        public Uni<ListVersionsResponse> listVersions(ListVersionsRequest request) {
                return versionManager.getVersionHistory(request.getWorkflowId())
                                .map(list -> ListVersionsResponse.newBuilder()
                                                .addAllVersions(list.stream().map(this::toProtoList)
                                                                .collect(Collectors.toList()))
                                                .build());
        }

        @Override
        public Uni<WorkflowVersion> getVersion(GetVersionRequest request) {
                return versionManager.getVersion(request.getWorkflowId(), request.getVersion())
                                .map(this::toProto);
        }

        @Override
        public Uni<WorkflowVersion> publishVersion(PublishVersionRequest request) {
                PublishOptions options = new PublishOptions(
                                request.getCanaryDeployment(),
                                request.getCanaryPercentage(),
                                request.getAutoMigrate(),
                                request.getPublishedBy());

                return versionManager.publishVersion(request.getVersionId(), options)
                                .map(this::toProto);
        }

        @Override
        public Uni<WorkflowVersion> promoteCanary(PromoteCanaryRequest request) {
                return versionManager.promoteCanary(request.getVersionId())
                                .map(this::toProto);
        }

        @Override
        public Uni<Empty> rollbackCanary(RollbackCanaryRequest request) {
                return versionManager.rollbackCanary(request.getVersionId(), request.getReason())
                                .replaceWith(Empty.getDefaultInstance());
        }

        @Override
        public Uni<WorkflowVersion> deprecateVersion(DeprecateVersionRequest request) {
                return versionManager
                                .deprecateVersion(request.getVersionId(), request.getReason(),
                                                Instant.ofEpochMilli(request.getSunsetDate()))
                                .map(this::toProto);
        }

        @Override
        public Uni<MigrateVersionResponse> migrateVersion(MigrateVersionRequest request) {
                return versionManager
                                .migrateVersion(request.getWorkflowId(), request.getFromVersion(),
                                                request.getToVersion())
                                .map(result -> MigrateVersionResponse.newBuilder()
                                                .setSuccess(result.failedMigrations() == 0)
                                                .setMessage(String.format(
                                                                "Total: %d, Success: %d, Failed: %d. Errors: %s",
                                                                result.totalRuns(), result.successfulMigrations(),
                                                                result.failedMigrations(),
                                                                result.errors()))
                                                .setMigratedCount(result.successfulMigrations())
                                                .build());
        }

        @Override
        public Uni<CompareVersionsResponse> compareVersions(CompareVersionsRequest request) {
                return versionManager
                                .compareVersions(request.getWorkflowId(), request.getVersion1(), request.getVersion2())
                                .map(diff -> CompareVersionsResponse.newBuilder()
                                                .setDiff(diff.toString())
                                                .build());
        }

        // Mappers

        private WorkflowVersion toProto(tech.kayys.wayang.workflow.version.model.WorkflowVersion domain) {
                if (domain == null)
                        return null;
                WorkflowVersion.Builder builder = WorkflowVersion.newBuilder()
                                .setVersionId(domain.getVersionId())
                                .setWorkflowId(domain.getWorkflowId())
                                .setVersion(domain.getVersion())
                                .setPreviousVersion(
                                                domain.getPreviousVersion() != null ? domain.getPreviousVersion() : "")
                                .setStatus(domain.getStatus().name())
                                .setCanaryPercentage(domain.getCanaryPercentage())
                                .setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt().toEpochMilli() : 0)
                                .setPublishedAt(domain.getPublishedAt() != null ? domain.getPublishedAt().toEpochMilli()
                                                : 0);

                if (domain.getBreakingChanges() != null) {
                        builder.addAllBreakingChanges(domain.getBreakingChanges().stream()
                                        .map(java.lang.Object::toString)
                                        .collect(Collectors.toList()));
                }
                if (domain.getDeprecationWarnings() != null)
                        builder.addAllDeprecationWarnings(domain.getDeprecationWarnings());

                return builder.build();
        }

        private WorkflowVersionListItem toProtoList(tech.kayys.wayang.workflow.version.model.WorkflowVersion domain) {
                if (domain == null)
                        return null;
                return WorkflowVersionListItem.newBuilder()
                                .setVersionId(domain.getVersionId())
                                .setVersion(domain.getVersion())
                                .setStatus(domain.getStatus().name())
                                .setBreakingChangesCount(
                                                domain.getBreakingChanges() != null ? domain.getBreakingChanges().size()
                                                                : 0)
                                .setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt().toEpochMilli() : 0)
                                .build();
        }
}
