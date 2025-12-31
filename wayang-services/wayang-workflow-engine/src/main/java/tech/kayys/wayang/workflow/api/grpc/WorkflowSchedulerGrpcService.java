package tech.kayys.wayang.workflow.api.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.scheduler.service.WorkflowScheduler;
import tech.kayys.wayang.workflow.v1.*;
import com.google.protobuf.Empty;
import java.util.stream.Collectors;
import tech.kayys.wayang.workflow.scheduler.dto.ScheduleRequest;
import tech.kayys.wayang.workflow.scheduler.dto.ScheduleUpdateRequest;
import tech.kayys.wayang.workflow.security.annotations.ControlPlaneSecured;

@GrpcService
@ControlPlaneSecured
public class WorkflowSchedulerGrpcService implements WorkflowSchedulerService {

        @Inject
        WorkflowScheduler scheduler;

        @Override
        public Uni<WorkflowSchedule> createSchedule(CreateScheduleRequest request) {
                tech.kayys.wayang.workflow.scheduler.dto.ScheduleType type = tech.kayys.wayang.workflow.scheduler.dto.ScheduleType.CRON;

                java.util.Map<String, Object> inputs = new java.util.HashMap<>(request.getInputsMap());

                ScheduleRequest domainRequest = new ScheduleRequest(
                                request.getWorkflowId(),
                                null,
                                null,
                                type,
                                request.getCronExpression(),
                                null,
                                null,
                                null,
                                request.getTimezone(),
                                0,
                                0,
                                inputs,
                                null,
                                "grpc");

                return scheduler.createSchedule(domainRequest)
                                .map(this::toProto);
        }

        @Override
        public Uni<ListSchedulesResponse> listSchedules(ListSchedulesRequest request) {
                return scheduler.listSchedules(request.getTenantId(), request.getWorkflowId(), null)
                                .map(list -> ListSchedulesResponse.newBuilder()
                                                .addAllSchedules(list.stream().map(this::toProto)
                                                                .collect(Collectors.toList()))
                                                .build());
        }

        @Override
        public Uni<WorkflowSchedule> getSchedule(GetScheduleRequest request) {
                return scheduler.getSchedule(request.getScheduleId())
                                .map(this::toProto);
        }

        @Override
        public Uni<WorkflowSchedule> updateSchedule(UpdateScheduleRequest request) {
                java.util.Map<String, Object> inputs = new java.util.HashMap<>(request.getInputsMap());

                ScheduleUpdateRequest domainRequest = new ScheduleUpdateRequest(
                                request.getCronExpression(),
                                null,
                                inputs,
                                request.getEnabled());

                return scheduler.updateSchedule(request.getScheduleId(), domainRequest)
                                .map(this::toProto);
        }

        @Override
        public Uni<Empty> deleteSchedule(GetScheduleRequest request) {
                return scheduler.deleteSchedule(request.getScheduleId())
                                .map(v -> Empty.getDefaultInstance());
        }

        @Override
        public Uni<GetExecutionHistoryResponse> getExecutionHistory(GetExecutionHistoryRequest request) {
                return scheduler.getExecutionHistory(request.getScheduleId(), request.getLimit())
                                .map(list -> GetExecutionHistoryResponse.newBuilder()
                                                .addAllHistory(list.stream().map(this::toProtoHistory)
                                                                .collect(Collectors.toList()))
                                                .build());
        }

        // Mappers

        private WorkflowSchedule toProto(tech.kayys.wayang.workflow.scheduler.model.WorkflowSchedule domain) {
                if (domain == null)
                        return null;
                return WorkflowSchedule.newBuilder()
                                .setScheduleId(domain.getScheduleId())
                                .setWorkflowId(domain.getWorkflowId())
                                .setStatus(domain.isEnabled() ? "ENABLED" : "DISABLED")
                                .setNextExecutionAt(
                                                domain.getNextExecutionAt() != null
                                                                ? domain.getNextExecutionAt().toEpochMilli()
                                                                : 0)
                                .setExecutionCount(domain.getExecutionCount())
                                .setCronExpression(domain.getCronExpression() != null ? domain.getCronExpression() : "")
                                .setTimezone(domain.getTimezone() != null ? domain.getTimezone() : "")
                                .build();
        }

        private ExecutionHistoryItem toProtoHistory(tech.kayys.wayang.workflow.scheduler.dto.ScheduleExecution domain) {
                if (domain == null)
                        return null;
                return ExecutionHistoryItem.newBuilder()
                                .setScheduleId(domain.getScheduleId())
                                .setRunId(domain.getRunId())
                                .setScheduledAt(domain.getScheduledFor() != null
                                                ? domain.getScheduledFor().toEpochMilli()
                                                : 0)
                                .setExecutedAt(domain.getStartedAt() != null ? domain.getStartedAt().toEpochMilli()
                                                : 0)
                                .setStatus(domain.getStatus().name())
                                .build();
        }
}
