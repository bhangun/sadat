package tech.kayys.wayang.workflow.kernel;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for discovering and managing cluster nodes
 */
@ApplicationScoped
public class ClusterDiscoveryService {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterDiscoveryService.class);

    private final Map<String, ClusterNode> clusterMembers = new ConcurrentHashMap<>();
    private final Map<String, Instant> memberLastSeen = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> memberMetadata = new ConcurrentHashMap<>();
    private ClusterNode currentLeader;
    private final String clusterId;
    private final ScheduledExecutorService heartbeatExecutor;
    private volatile boolean isRunning = false;

    public ClusterDiscoveryService() {
        this.clusterId = generateClusterId();
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cluster-discovery-heartbeat");
            t.setDaemon(true);
            return t;
        });
    }

    public Uni<Boolean> register(ClusterNode node) {
        return Uni.createFrom().deferred(() -> {
            if (node == null || node.getId() == null) {
                return Uni.createFrom().item(false);
            }

            clusterMembers.put(node.getId(), node);
            memberLastSeen.put(node.getId(), Instant.now());
            memberMetadata.put(node.getId(), new HashMap<>(node.getMetadata()));

            LOG.info("Node {} registered to cluster {}", node.getId(), clusterId);
            broadcastNodeJoined(node);

            // Start heartbeat if not already running
            if (!isRunning) {
                startHeartbeat();
            }

            return Uni.createFrom().item(true);
        });
    }

    public Uni<Boolean> unregister(String nodeId) {
        return Uni.createFrom().deferred(() -> {
            ClusterNode node = clusterMembers.remove(nodeId);
            if (node != null) {
                memberLastSeen.remove(nodeId);
                memberMetadata.remove(nodeId);

                // If this was the leader, trigger new election
                if (currentLeader != null && currentLeader.getId().equals(nodeId)) {
                    currentLeader = null;
                    triggerLeaderElection();
                }

                broadcastNodeLeft(nodeId);
                LOG.info("Node {} unregistered from cluster {}", nodeId, clusterId);
                return Uni.createFrom().item(true);
            }
            return Uni.createFrom().item(false);
        });
    }

    public Uni<List<ClusterNode>> getClusterMembers() {
        return Uni.createFrom().deferred(() -> {
            cleanupStaleMembers();

            List<ClusterNode> members = new ArrayList<>();
            Instant now = Instant.now();
            Duration staleThreshold = Duration.ofSeconds(30);

            for (Map.Entry<String, ClusterNode> entry : clusterMembers.entrySet()) {
                Instant lastSeen = memberLastSeen.get(entry.getKey());
                if (lastSeen != null && lastSeen.plus(staleThreshold).isAfter(now)) {
                    members.add(entry.getValue());
                }
            }

            return Uni.createFrom().item(List.copyOf(members));
        });
    }

    public Uni<ClusterNode> getLeader() {
        return Uni.createFrom().deferred(() -> {
            if (currentLeader == null) {
                triggerLeaderElection();
            }

            // Verify leader is still active
            if (currentLeader != null) {
                Instant lastSeen = memberLastSeen.get(currentLeader.getId());
                if (lastSeen == null || lastSeen.plus(Duration.ofSeconds(30)).isBefore(Instant.now())) {
                    currentLeader = null;
                    triggerLeaderElection();
                }
            }

            return Uni.createFrom().item(currentLeader);
        });
    }

    public Uni<Boolean> electLeader(ClusterNode candidate) {
        return Uni.createFrom().deferred(() -> {
            if (candidate == null || !clusterMembers.containsKey(candidate.getId())) {
                return Uni.createFrom().item(false);
            }

            // Simple leader election: oldest registered node becomes leader
            if (currentLeader == null) {
                Optional<ClusterNode> oldestNode = clusterMembers.values().stream()
                        .min(Comparator.comparing(ClusterNode::getJoinedAt));

                if (oldestNode.isPresent()) {
                    currentLeader = oldestNode.get();
                    currentLeader.setLeader(true);
                    broadcastLeaderElected(currentLeader);
                    LOG.info("Node {} elected as leader of cluster {}", currentLeader.getId(), clusterId);
                    return Uni.createFrom().item(currentLeader.getId().equals(candidate.getId()));
                }
            }

            return Uni.createFrom().item(currentLeader != null &&
                    currentLeader.getId().equals(candidate.getId()));
        });
    }

    public Uni<Void> updateNodeStatus(String nodeId, Map<String, Object> status) {
        return Uni.createFrom().deferred(() -> {
            ClusterNode node = clusterMembers.get(nodeId);
            if (node != null) {
                memberLastSeen.put(nodeId, Instant.now());

                Map<String, Object> metadata = memberMetadata.computeIfAbsent(nodeId, k -> new HashMap<>());
                metadata.putAll(status);
                metadata.put("lastUpdate", Instant.now().toString());

                node.setMetadata(metadata);
                node.updateLastSeen();

                // Broadcast status update
                broadcastNodeStatus(nodeId, status);
            }
            return Uni.createFrom().voidItem();
        });
    }

    public Uni<Map<String, Object>> getClusterHealth() {
        return getClusterMembers()
                .map(members -> {
                    Map<String, Object> health = new HashMap<>();

                    Instant now = Instant.now();
                    Duration healthyThreshold = Duration.ofSeconds(10);
                    Duration warningThreshold = Duration.ofSeconds(30);

                    int total = members.size();
                    int healthy = 0;
                    int warning = 0;
                    int critical = 0;

                    for (ClusterNode member : members) {
                        Instant lastSeen = memberLastSeen.get(member.getId());
                        if (lastSeen == null) {
                            critical++;
                        } else if (lastSeen.plus(healthyThreshold).isAfter(now)) {
                            healthy++;
                        } else if (lastSeen.plus(warningThreshold).isAfter(now)) {
                            warning++;
                        } else {
                            critical++;
                        }
                    }

                    health.put("clusterId", clusterId);
                    health.put("totalNodes", total);
                    health.put("healthyNodes", healthy);
                    health.put("warningNodes", warning);
                    health.put("criticalNodes", critical);
                    health.put("leaderId", currentLeader != null ? currentLeader.getId() : null);
                    health.put("timestamp", now.toString());

                    return health;
                });
    }

    public Uni<Void> shutdown() {
        return Uni.createFrom().deferred(() -> {
            isRunning = false;
            heartbeatExecutor.shutdown();
            clusterMembers.clear();
            memberLastSeen.clear();
            memberMetadata.clear();
            currentLeader = null;

            LOG.info("Cluster discovery service shutdown");
            return Uni.createFrom().voidItem();
        });
    }

    private void startHeartbeat() {
        if (isRunning) {
            return;
        }

        isRunning = true;
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                sendHeartbeat();
                cleanupStaleMembers();
            } catch (Exception e) {
                LOG.error("Error in cluster discovery heartbeat", e);
            }
        }, 5, 5, TimeUnit.SECONDS);

        LOG.debug("Cluster discovery heartbeat started");
    }

    private void sendHeartbeat() {
        Instant now = Instant.now();
        Map<String, Object> heartbeat = Map.of(
                "timestamp", now.toString(),
                "clusterId", clusterId);

        // Update own status
        memberLastSeen.keySet().forEach(nodeId -> updateNodeStatus(nodeId, heartbeat).subscribe().with(
                success -> LOG.trace("Heartbeat sent for node {}", nodeId),
                failure -> LOG.warn("Failed to send heartbeat for node {}", nodeId, failure)));
    }

    private void cleanupStaleMembers() {
        Instant now = Instant.now();
        Duration staleThreshold = Duration.ofMinutes(5);

        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, Instant> entry : memberLastSeen.entrySet()) {
            if (entry.getValue().plus(staleThreshold).isBefore(now)) {
                toRemove.add(entry.getKey());
            }
        }

        toRemove.forEach(nodeId -> {
            clusterMembers.remove(nodeId);
            memberLastSeen.remove(nodeId);
            memberMetadata.remove(nodeId);

            if (currentLeader != null && currentLeader.getId().equals(nodeId)) {
                currentLeader = null;
                triggerLeaderElection();
            }

            LOG.warn("Removed stale node {} from cluster", nodeId);
        });
    }

    private void triggerLeaderElection() {
        if (clusterMembers.isEmpty()) {
            return;
        }

        // Simple election: select node with earliest join time
        Optional<ClusterNode> newLeader = clusterMembers.values().stream()
                .min(Comparator.comparing(ClusterNode::getJoinedAt));

        if (newLeader.isPresent()) {
            currentLeader = newLeader.get();
            currentLeader.setLeader(true);
            broadcastLeaderElected(currentLeader);
            LOG.info("New leader elected: {}", currentLeader.getId());
        }
    }

    private void broadcastNodeJoined(ClusterNode node) {
        // In a real implementation, this would use a message broker
        // For now, just log
        LOG.debug("Broadcasting node joined: {}", node.getId());
    }

    private void broadcastNodeLeft(String nodeId) {
        LOG.debug("Broadcasting node left: {}", nodeId);
    }

    private void broadcastLeaderElected(ClusterNode leader) {
        LOG.debug("Broadcasting leader elected: {}", leader.getId());
    }

    private void broadcastNodeStatus(String nodeId, Map<String, Object> status) {
        LOG.trace("Broadcasting node status: {} - {}", nodeId, status);
    }

    private String generateClusterId() {
        return "cluster-" + UUID.randomUUID().toString().substring(0, 8);
    }
}