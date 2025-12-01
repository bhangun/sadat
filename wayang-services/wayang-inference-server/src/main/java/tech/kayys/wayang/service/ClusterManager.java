package tech.kayys.wayang.service;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ClusterManager {
    private static final Logger log = Logger.getLogger(ClusterManager.class);
    
    private final String nodeId;
    private final Map<String, ClusterNode> nodes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor;
    private NodeRole currentRole = NodeRole.WORKER;
    
    public ClusterManager(String nodeId) {
        this.nodeId = nodeId;
        this.heartbeatExecutor = Executors.newScheduledThreadPool(1);
        
        // Register this node
        nodes.put(nodeId, new ClusterNode(
            nodeId,
            System.currentTimeMillis(),
            NodeRole.WORKER,
            getNodeCapacity()
        ));
        
        startHeartbeat();
    }
    
    private void startHeartbeat() {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                sendHeartbeat();
                checkNodeHealth();
            } catch (Exception e) {
                log.error("Heartbeat failed", e);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
    
    private void sendHeartbeat() {
        ClusterNode thisNode = nodes.get(nodeId);
        if (thisNode != null) {
            thisNode.lastHeartbeat = System.currentTimeMillis();
            
            // Broadcast to other nodes (simplified)
            log.debugf("Heartbeat sent from node: %s", nodeId);
        }
    }
    
    private void checkNodeHealth() {
        long now = System.currentTimeMillis();
        long timeout = 30000; // 30 seconds
        
        nodes.entrySet().removeIf(entry -> {
            if (!entry.getKey().equals(nodeId)) {
                long timeSinceHeartbeat = now - entry.getValue().lastHeartbeat;
                if (timeSinceHeartbeat > timeout) {
                    log.warnf("Node %s is unhealthy (no heartbeat for %dms)", 
                        entry.getKey(), timeSinceHeartbeat);
                    return true;
                }
            }
            return false;
        });
        
        // Leader election if needed
        electLeader();
    }
    
    private void electLeader() {
        if (nodes.isEmpty()) {
            return;
        }
        
        // Simple leader election: node with lowest ID becomes leader
        String leaderId = nodes.keySet().stream()
            .sorted()
            .findFirst()
            .orElse(nodeId);
        
        if (leaderId.equals(nodeId) && currentRole != NodeRole.LEADER) {
            currentRole = NodeRole.LEADER;
            log.infof("Node %s elected as leader", nodeId);
        }
    }
    
    public ClusterNode selectNodeForRequest(String requestId) {
        // Simple round-robin or least-loaded selection
        return nodes.values().stream()
            .filter(n -> n.role == NodeRole.WORKER || n.role == NodeRole.LEADER)
            .min(Comparator.comparingInt(n -> n.capacity.currentLoad))
            .orElse(null);
    }
    
    public void registerNode(ClusterNode node) {
        nodes.put(node.nodeId, node);
        log.infof("Node registered: %s (%s)", node.nodeId, node.role);
    }
    
    public void shutdown() {
        heartbeatExecutor.shutdown();
        try {
            heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            heartbeatExecutor.shutdownNow();
        }
    }
    
    private NodeCapacity getNodeCapacity() {
        return new NodeCapacity(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().maxMemory(),
            0  // current load
        );
    }
    
    public enum NodeRole {
        LEADER,
        WORKER,
        STANDBY
    }
    
    public static class ClusterNode {
        private final String nodeId;
        private long lastHeartbeat;
        private final NodeRole role;
        private final NodeCapacity capacity;
        
        public ClusterNode(String nodeId, long lastHeartbeat, NodeRole role, NodeCapacity capacity) {
            this.nodeId = nodeId;
            this.lastHeartbeat = lastHeartbeat;
            this.role = role;
            this.capacity = capacity;
        }
        
        public String getNodeId() { return nodeId; }
        public long getLastHeartbeat() { return lastHeartbeat; }
        public NodeRole getRole() { return role; }
        public NodeCapacity getCapacity() { return capacity; }
    }
    
    private static class NodeCapacity {
        private final int cpuCores;
        private final long memoryBytes;
        private int currentLoad;
        
        NodeCapacity(int cpuCores, long memoryBytes, int currentLoad) {
            this.cpuCores = cpuCores;
            this.memoryBytes = memoryBytes;
            this.currentLoad = currentLoad;
        }
    }
}
