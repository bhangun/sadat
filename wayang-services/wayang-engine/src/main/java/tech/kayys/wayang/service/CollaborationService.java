package tech.kayys.wayang.service;

import java.util.Map;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.dto.AuditEvent;
import tech.kayys.wayang.service.AuditService;
import tech.kayys.wayang.schema.CollaborationEvent;
import tech.kayys.wayang.schema.EventType;
import tech.kayys.wayang.schema.NodeMovePayload;
import tech.kayys.wayang.schema.PointDTO;
import tech.kayys.wayang.schema.ConnectionPayload;

/**
 * CollaborationService - Handles real-time collaboration
 */
@ApplicationScoped
public class CollaborationService {

        private static final Logger LOG = Logger.getLogger(CollaborationService.class);

        @Inject
        WorkflowCommandService commandService;

        @Inject
        AuditService auditService;

        /**
         * User joined workflow
         */
        public Uni<CollaborationEvent> userJoined(String workflowId, String userId,
                        String tenantId) {

                LOG.infof("User joined: workflow=%s, user=%s", workflowId, userId);

                return Uni.createFrom().item(() -> {
                        // Audit
                        auditService.log(AuditEvent.builder()
                                        .type("USER_JOINED")
                                        .entityType("Workflow")
                                        .entityId(workflowId)
                                        .userId(userId)
                                        .tenantId(tenantId)
                                        .build());

                        // Build event
                        return CollaborationEvent.builder()
                                        .type(EventType.USER_JOINED)
                                        .userId(userId)
                                        .workflowId(workflowId)
                                        .payload(Map.of("userId", userId))
                                        .build();
                });
        }

        /**
         * User left workflow
         */
        public Uni<CollaborationEvent> userLeft(String workflowId, String userId,
                        String tenantId) {

                LOG.infof("User left: workflow=%s, user=%s", workflowId, userId);

                // Release all locks held by user
                return releaseLocks(workflowId, userId, tenantId)
                                .map(v -> {
                                        // Audit
                                        auditService.log(AuditEvent.builder()
                                                        .type("USER_LEFT")
                                                        .entityType("Workflow")
                                                        .entityId(workflowId)
                                                        .userId(userId)
                                                        .tenantId(tenantId)
                                                        .build());

                                        // Build event
                                        return CollaborationEvent.builder()
                                                        .type(EventType.USER_LEFT)
                                                        .userId(userId)
                                                        .workflowId(workflowId)
                                                        .payload(Map.of("userId", userId))
                                                        .build();
                                });
        }

        /**
         * Move node
         */
        public Uni<CollaborationEvent> moveNode(String workflowId, String nodeId,
                        PointDTO position, String userId,
                        String tenantId) {

                LOG.debugf("Moving node: workflow=%s, node=%s, pos=(%f,%f)",
                                workflowId, nodeId, position.getX(), position.getY());

                // Update position in database (async)
                return Uni.createFrom().item(() -> {
                        // Build event immediately for low latency
                        NodeMovePayload payload = new NodeMovePayload();
                        payload.setNodeId(nodeId);
                        payload.setPosition(position);

                        return CollaborationEvent.builder()
                                        .type(EventType.NODE_MOVED)
                                        .userId(userId)
                                        .workflowId(workflowId)
                                        .payload(payload)
                                        .build();
                });
        }

        /**
         * Lock node for collaboration
         */
        public Uni<CollaborationEvent> lockNode(String workflowId, String nodeId,
                        String userId, String tenantId) {

                return commandService.lockNode(workflowId, nodeId, userId, tenantId)
                                .map(lock -> CollaborationEvent.builder()
                                                .type(EventType.NODE_LOCKED)
                                                .userId(userId)
                                                .workflowId(workflowId)
                                                .payload(lock)
                                                .build());
        }

        /**
         * Unlock node
         */
        public Uni<CollaborationEvent> unlockNode(String workflowId, String nodeId,
                        String userId, String tenantId) {

                return commandService.unlockNode(workflowId, nodeId, userId, tenantId)
                                .map(success -> CollaborationEvent.builder()
                                                .type(EventType.NODE_UNLOCKED)
                                                .userId(userId)
                                                .workflowId(workflowId)
                                                .payload(Map.of("nodeId", nodeId))
                                                .build());
        }

        /**
         * Release all locks held by user
         */
        private Uni<Void> releaseLocks(String workflowId, String userId, String tenantId) {
                // Implementation: find all locked nodes and unlock them
                return Uni.createFrom().voidItem();
        }

        public Uni<CollaborationEvent> updateNode(String workflowId, String nodeId, Map<String, Object> changes,
                        String userId, String tenantId) {
                return Uni.createFrom().item(CollaborationEvent.builder()
                                .type(EventType.NODE_UPDATED)
                                .userId(userId)
                                .workflowId(workflowId)
                                .payload(Map.of("nodeId", nodeId, "changes", changes))
                                .build());
        }

        public Uni<CollaborationEvent> addConnection(String workflowId, ConnectionPayload connData, String userId,
                        String tenantId) {
                return Uni.createFrom().item(CollaborationEvent.builder()
                                .type(EventType.CONNECTION_ADDED)
                                .userId(userId)
                                .workflowId(workflowId)
                                .payload(connData)
                                .build());
        }

        public Uni<CollaborationEvent> deleteConnection(String workflowId, String connectionId, String userId,
                        String tenantId) {
                return Uni.createFrom().item(CollaborationEvent.builder()
                                .type(EventType.CONNECTION_DELETED)
                                .userId(userId)
                                .workflowId(workflowId)
                                .payload(Map.of("connectionId", connectionId))
                                .build());
        }
}
