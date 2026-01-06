package tech.kayys.wayang.workflow.api.grpc;

import io.quarkus.grpc.GrpcService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.workflow.service.WorkflowEventStore;
import tech.kayys.wayang.workflow.v1.*;
import java.util.stream.Collectors;
import tech.kayys.wayang.workflow.security.annotations.ControlPlaneSecured;

@GrpcService
@ControlPlaneSecured
public class EventStoreGrpcService implements EventStoreService {

    private static final Logger LOG = Logger.getLogger(EventStoreGrpcService.class);

    @Inject
    WorkflowEventStore eventStore;

    @Inject
    SecurityIdentity securityIdentity;

    private String getTenantId() {
        if (securityIdentity.isAnonymous()) {
            // Safety check
        }
        return securityIdentity.getPrincipal().getName();
    }

    @Override
    public Uni<GetEventsResponse> getRunEvents(GetRunEventsRequest request) {
        LOG.infof("Getting events for run %s", request.getRunId());
        return eventStore.getEvents(request.getRunId())
                .map(list -> GetEventsResponse.newBuilder()
                        .addAllEvents(list.stream().map(this::toProto).collect(Collectors.toList()))
                        .build());
    }

    @Override
    public Uni<GetEventsResponse> getEventsByType(GetEventsByTypeRequest request) {
        return eventStore.getEvents(request.getRunId())
                .map(list -> list.stream()
                        .filter(e -> e.type().name().equals(request.getEventType()))
                        .map(this::toProto)
                        .collect(Collectors.toList()))
                .map(filtered -> GetEventsResponse.newBuilder().addAllEvents(filtered).build());
    }

    private WorkflowEvent toProto(tech.kayys.wayang.workflow.api.model.WorkflowEvent domain) {
        if (domain == null)
            return null;
        return WorkflowEvent.newBuilder()
                .setId(domain.id())
                .setType(domain.type().name())
                .setTimestamp(domain.timestamp().toEpochMilli())
                .setPayloadJson(domain.data() != null ? domain.data().toString() : "")
                .build();
    }
}
