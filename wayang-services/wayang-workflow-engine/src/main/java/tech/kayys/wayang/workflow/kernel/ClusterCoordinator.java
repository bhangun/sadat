package tech.kayys.wayang.workflow.kernel;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.kayys.wayang.workflow.kernel.ClusterCoordinator.ClusterEvent;
import tech.kayys.wayang.workflow.kernel.ClusterCoordinator.ClusterInfo;
import tech.kayys.wayang.workflow.kernel.ClusterCoordinator.ClusterNode;
import tech.kayys.wayang.workflow.kernel.ClusterCoordinator.ClusterStatus;
import tech.kayys.wayang.workflow.kernel.WorkflowRunId;
import tech.kayys.wayang.workflow.service.DistributedLockManager;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Coordinates workflow execution across a cluster of nodes
 */
@ApplicationScoped
public class ClusterCoordinator {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterCoordinator.class);

    @Inject
    ClusterDiscoveryService clusterDiscoveryService;

    @Inject
    MessageBroker messageBroker;

    @Inject
    HealthCheckService healthCheckService;

    @Inject
    ConfigurationService configService;

    @Inject
    ClusterDiscoveryService discoveryService;

    @Inject
    DistributedLockManager lockManager;

    private final Map<String, ClusterNode> nodes = new ConcurrentHashMap<>();
    private final Map<String, RunAssignment> runAssignments = new ConcurrentHashMap<>();
    private final Map<String, List<ClusterEventListener>> eventListeners = new ConcurrentHashMap<>();
    private final String currentNodeId;
    private volatile ClusterNode currentNode;
    private volatile ClusterStatus status = ClusterStatus.DISCONNECTED;

    public ClusterCoordinator() {
        this.currentNodeId = generateNodeId();
        this.currentNode = new ClusterNode(currentNodeId, "localhost", Instant.now());
        nodes.put(currentNodeId, currentNode);
    }

    public Uni<Void> initialize() {
        return Uni.createFrom().deferred(() -> {
            if (!configService.isClusterEnabled()) {
                LOG.info("Cluster mode is disabled");
                return Uni.createFrom().voidItem();
            }

            return joinCluster()
                    .onItem().invoke(() -> {
                        LOG.info("Cluster coordinator initialized in cluster mode");
                    })
                    .onFailure().recoverWithUni(th -> {
                        LOG.error("Failed to initialize cluster coordinator", th);
                        return Uni.createFrom().voidItem();
                    });
        });
    }

    public Uni<Void> joinCluster() {
        return discoveryService.register(currentNode)
                .flatMap(registered -> {
                    if (registered) {
                        status = ClusterStatus.JOINING;
                        LOG.info("Node {} joining cluster", currentNodeId);

                        return discoveryService.getClusterMembers()
                                .onItem().invoke(members -> {
                                    members.forEach(node -> nodes.put(node.getId(), node));
                                    status = ClusterStatus.CONNECTED;
                                    LOG.info("Node {} connected to cluster with {} members",
                                            currentNodeId, nodes.size());
                                })
                                .replaceWithVoid();
                    } else {
                        return Uni.createFrom().failure(
                                new IllegalStateException("Failed to join cluster"));
                    }
                });
    }

    public Uni<Void> leaveCluster() {
        status = ClusterStatus.LEAVING;
        LOG.info("Node {} leaving cluster", currentNodeId);

        return discoveryService.unregister(currentNodeId)
                .onItem().invoke(success -> {
                    if (success) {
                        status = ClusterStatus.DISCONNECTED;
                        nodes.clear();
                        runAssignments.clear();
                        LOG.info("Node {} left cluster", currentNodeId);
                    }
                })
                .replaceWithVoid();
    }

    public Uni<String> assignRun(WorkflowRunId runId, AssignmentStrategy strategy) {
        return selectNodeForRun(runId, strategy)
                .flatMap(selectedNodeId -> {
                    if (selectedNodeId == null) {
                        return Uni.createFrom().failure(
                                new IllegalStateException("No suitable node found for run assignment"));
                    }

                    RunAssignment assignment = new RunAssignment(
                            runId.value(),
                            selectedNodeId,
                            Instant.now(),
                            strategy);

                    runAssignments.put(runId.value(), assignment);

                    // Notify the assigned node
                    return notifyAssignment(selectedNodeId, assignment)
                            .replaceWith(selectedNodeId);
                });
    }

    public Uni<Void> replicateState(String targetNodeId, String stateKey, Object state) {
        String messageId = UUID.randomUUID().toString();
        ReplicationMessage message = new ReplicationMessage(
                messageId,
                currentNodeId,
                targetNodeId,
                stateKey,
                state,
                Instant.now());

        return messageBroker.send("replication", targetNodeId, message)
                .onItem().invoke(success -> {
                    if (success) {
                        LOG.debug("State replicated to node {}: {}", targetNodeId, stateKey);
                    }
                });
    }

    public Uni<ClusterInfo> getClusterInfo() {
        return healthCheckService.checkClusterHealth()
                .map(health -> {
                    List<ClusterNode> activeNodes = nodes.values().stream()
                            .filter(node -> node.getLastSeen()
                                    .isAfter(Instant.now().minus(Duration.ofMinutes(1))))
                            .toList();

                    Map<String, Long> runsPerNode = new HashMap<>();
                    runAssignments.values().forEach(assignment -> runsPerNode.merge(assignment.nodeId, 1L, Long::sum));

                    return new ClusterInfo(
                            nodes.size(),
                            activeNodes.size(),
                            currentNodeId,
                            status,
                            runsPerNode);
                });
    }

    public Multi<ClusterEvent> streamClusterEvents() {
        return Multi.createFrom().emitter(emitter -> {
            // Subscribe to cluster events
            messageBroker.subscribe("cluster-events", currentNodeId, message -> {
                if (message instanceof ClusterEvent event) {
                    emitter.emit(event);
                }
            });

            // Also emit local events
            emitter.emit(new ClusterEvent.NodeJoined(currentNodeId, Instant.now()));
        });
    }

    public Uni<Void> subscribeToRun(String runId, ClusterEventListener listener) {
        return Uni.createFrom().deferred(() -> {
            eventListeners.computeIfAbsent(runId, k -> new CopyOnWriteArrayList<>())
                    .add(listener);
            return Uni.createFrom().voidItem();
        });
    }

    public Uni<Void> unsubscribeFromRun(String runId, ClusterEventListener listener) {
        return Uni.createFrom().deferred(() -> {
            List<ClusterEventListener> listeners = eventListeners.get(runId);
            if (listeners != null) {
                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    eventListeners.remove(runId);
                }
            }
            return Uni.createFrom().voidItem();
        });
    }

    public Uni<Boolean> isLeader() {
        return discoveryService.getLeader()
                .map(leader -> leader != null && leader.getId().equals(currentNodeId));
    }

    public Uni<Void> electLeader() {
        return discoveryService.electLeader(currentNode)
                .onItem().invoke(elected -> {
                    if (elected) {
                        LOG.info("Node {} elected as cluster leader", currentNodeId);
                        currentNode.setLeader(true);
                        broadcastEvent(new ClusterEvent.LeaderElected(currentNodeId, Instant.now()));
                    }
                })
                .replaceWithVoid();
    }

    public Uni<List<String>> getAvailableNodes() {
        return Uni.createFrom().item(() -> nodes.values().stream()
                .filter(node -> node.getLastSeen()
                        .isAfter(Instant.now().minus(Duration.ofSeconds(30))))
                .map(ClusterNode::getId)
                .toList());
    }

    public boolean isEnabled() {
        return status != ClusterStatus.DISCONNECTED;
    }

    private Uni<String> selectNodeForRun(WorkflowRunId runId, AssignmentStrategy strategy) {
        return getAvailableNodes()
                .map(availableNodes -> {
                    if (availableNodes.isEmpty()) {
                        return null;
                    }

                    return switch (strategy) {
                        case RANDOM -> selectRandomNode(availableNodes);
                        case LOAD_BALANCED -> selectLoadBalancedNode(availableNodes);
                        case AFFINITY -> selectAffinityNode(runId, availableNodes);
                        case LEADER -> selectLeaderNode(availableNodes);
                    };
                });
    }

    private String selectRandomNode(List<String> availableNodes) {
        if (availableNodes.isEmpty())
            return null;
        int index = new Random().nextInt(availableNodes.size());
        return availableNodes.get(index);
    }

    private String selectLoadBalancedNode(List<String> availableNodes) {
        // Simple round-robin based on run count
        return availableNodes.stream()
                .min(Comparator.comparingLong(nodeId -> runAssignments.values().stream()
                        .filter(a -> a.nodeId.equals(nodeId))
                        .count()))
                .orElse(null);
    }

    private String selectAffinityNode(WorkflowRunId runId, List<String> availableNodes) {
        // Try to assign to the same node that handled previous runs of this workflow
        // For now, default to current node for affinity
        return availableNodes.contains(currentNodeId) ? currentNodeId : selectLoadBalancedNode(availableNodes);
    }

    private String selectLeaderNode(List<String> availableNodes) {
        return currentNode.isLeader() && availableNodes.contains(currentNodeId) ? currentNodeId : null;
    }

    private Uni<Boolean> notifyAssignment(String nodeId, RunAssignment assignment) {
        if (nodeId.equals(currentNodeId)) {
            // Self-assignment, handle locally
            return handleLocalAssignment(assignment);
        } else {
            // Send to remote node
            return messageBroker.send("run-assignment", nodeId, assignment)
                    .onFailure().recoverWithItem(false);
        }
    }

    private Uni<Boolean> handleLocalAssignment(RunAssignment assignment) {
        return Uni.createFrom().item(() -> {
            LOG.info("Run {} assigned locally", assignment.runId);
            // Trigger local execution
            notifyRunAssigned(assignment.runId, currentNodeId);
            return true;
        });
    }

    private void notifyRunAssigned(String runId, String nodeId) {
        List<ClusterEventListener> listeners = eventListeners.get(runId);
        if (listeners != null) {
            listeners.forEach(listener -> {
                try {
                    listener.onRunAssigned(runId, nodeId);
                } catch (Exception e) {
                    LOG.error("Error notifying listener for run {}", runId, e);
                }
            });
        }
    }

    private void broadcastEvent(ClusterEvent event) {
        nodes.keySet().forEach(nodeId -> {
            if (!nodeId.equals(currentNodeId)) {
                messageBroker.send("cluster-events", nodeId, event)
                        .subscribe().with(
                                success -> LOG.debug("Event broadcast to {}", nodeId),
                                failure -> LOG.warn("Failed to broadcast event to {}", nodeId, failure));
            }
        });
    }

    private String generateNodeId() {
        return "node-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Inner classes
    public enum ClusterStatus {
        DISCONNECTED, JOINING, CONNECTED, LEAVING, DEGRADED
    }

    public enum AssignmentStrategy {
        RANDOM, LOAD_BALANCED, AFFINITY, LEADER
    }

    public static class ClusterNode {
        private final String id;
        private final String host;
        private final Instant joinedAt;
        private volatile Instant lastSeen;
        private volatile boolean leader;
        private volatile Map<String, Object> metadata;

        public ClusterNode(String id, String host, Instant joinedAt) {
            this.id = id;
            this.host = host;
            this.joinedAt = joinedAt;
            this.lastSeen = joinedAt;
            this.metadata = new HashMap<>();
        }

        public String getId() {
            return id;
        }

        public String getHost() {
            return host;
        }

        public Instant getJoinedAt() {
            return joinedAt;
        }

        public Instant getLastSeen() {
            return lastSeen;
        }

        public boolean isLeader() {
            return leader;
        }

        public Map<String, Object> getMetadata() {
            return Map.copyOf(metadata);
        }

        public void setLastSeen(Instant lastSeen) {
            this.lastSeen = lastSeen;
        }

        public void setLeader(boolean leader) {
            this.leader = leader;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
        }

        public void updateLastSeen() {
            this.lastSeen = Instant.now();
        }
    }

    public static class RunAssignment {
        private final String runId;
        private final String nodeId;
        private final Instant assignedAt;
        private final AssignmentStrategy strategy;

        public RunAssignment(String runId, String nodeId, Instant assignedAt,
                AssignmentStrategy strategy) {
            this.runId = runId;
            this.nodeId = nodeId;
            this.assignedAt = assignedAt;
            this.strategy = strategy;
        }

        // Getters...
    }

    public static class ClusterInfo {
        private final int nodeCount;
        private final int activeNodeCount;
        private final String currentNodeId;
        private final ClusterStatus status;
        private final Map<String, Long> runsPerNode;

        public ClusterInfo(int nodeCount, int activeNodeCount, String currentNodeId,
                ClusterStatus status, Map<String, Long> runsPerNode) {
            this.nodeCount = nodeCount;
            this.activeNodeCount = activeNodeCount;
            this.currentNodeId = currentNodeId;
            this.status = status;
            this.runsPerNode = Map.copyOf(runsPerNode);
        }

        // Getters...
    }

    public static class ReplicationMessage {
        private final String messageId;
        private final String sourceNodeId;
        private final String targetNodeId;
        private final String stateKey;
        private final Object state;
        private final Instant sentAt;

        public ReplicationMessage(String messageId, String sourceNodeId, String targetNodeId,
                String stateKey, Object state, Instant sentAt) {
            this.messageId = messageId;
            this.sourceNodeId = sourceNodeId;
            this.targetNodeId = targetNodeId;
            this.stateKey = stateKey;
            this.state = state;
            this.sentAt = sentAt;
        }

        // Getters...
    }

    public interface ClusterEventListener {
        void onRunAssigned(String runId, String nodeId);

        void onNodeJoined(String nodeId);

        void onNodeLeft(String nodeId);

        void onLeaderElected(String nodeId);
    }

    public abstract static class ClusterEvent {
        private final String eventId;
        private final Instant timestamp;

        protected ClusterEvent() {
            this.eventId = UUID.randomUUID().toString();
            this.timestamp = Instant.now();
        }

        public String getEventId() {
            return eventId;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public static class NodeJoined extends ClusterEvent {
            private final String nodeId;
            private final Instant joinedAt;

            public NodeJoined(String nodeId, Instant joinedAt) {
                this.nodeId = nodeId;
                this.joinedAt = joinedAt;
            }

            public String getNodeId() {
                return nodeId;
            }

            public Instant getJoinedAt() {
                return joinedAt;
            }
        }

        public static class NodeLeft extends ClusterEvent {
            private final String nodeId;
            private final Instant leftAt;

            public NodeLeft(String nodeId, Instant leftAt) {
                this.nodeId = nodeId;
                this.leftAt = leftAt;
            }

            // Getters...
        }

        public static class LeaderElected extends ClusterEvent {
            private final String nodeId;
            private final Instant electedAt;

            public LeaderElected(String nodeId, Instant electedAt) {
                this.nodeId = nodeId;
                this.electedAt = electedAt;
            }

            // Getters...
        }

        public static class RunAssigned extends ClusterEvent {
            private final String runId;
            private final String nodeId;

            public RunAssigned(String runId, String nodeId) {
                this.runId = runId;
                this.nodeId = nodeId;
            }

            // Getters...
        }
    }
}
