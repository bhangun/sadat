package tech.kayys.wayang.workflow.api.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.service.WorkflowEventStore;
import tech.kayys.wayang.workflow.v1.*;
import java.util.stream.Collectors;

@GrpcService
public class EventStoreGrpcService implements EventStoreService {

    @Inject
    WorkflowEventStore eventStore;

    @Override
    public Uni<GetEventsResponse> getRunEvents(GetRunEventsRequest request) {
        return eventStore.getEvents(request.getRunId())
                .map(list -> GetEventsResponse.newBuilder()
                        .addAllEvents(list.stream().map(this::toProto).collect(Collectors.toList()))
                        .build());
    }

    @Override
    public Uni<GetEventsResponse> getEventsByType(GetEventsByTypeRequest request) {
        // Assuming WorkflowEventStore has filtering or we filter manually
        // WorkflowEventStore usually has getEvents(runId)
        // I will filter manually for now if no specific method exists
        return eventStore.getEvents(request.getRunId())
                .map(list -> list.stream()
                        .filter(e -> e.type().name().equals(request.getEventType()))
                        .map(this::toProto)
                        .collect(Collectors.toList()))
                .map(filtered -> GetEventsResponse.newBuilder().addAllEvents(filtered).build());
    }

    // Mapper

    private WorkflowEvent toProto(tech.kayys.wayang.workflow.api.model.WorkflowEvent domain) {
        if (domain == null)
            return null;
        return WorkflowEvent.newBuilder()
                .setId(domain.id())
                .setType(domain.type().name())
                .setTimestamp(domain.timestamp().toEpochMilli())
                // .setPayloadJson(...) // Need json serialization or just toString for now
                .setPayloadJson(domain.data() != null ? domain.data().toString() : "")
                .build();
    }
}
