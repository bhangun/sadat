package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Workflow Sharding & Partitioning
 */
interface WorkflowShardingService {

    /**
     * Distribute workflow runs across shards
     */
    Uni<ShardAssignment> assignShard(tech.kayys.silat.core.domain.WorkflowRunId runId);

    /**
     * Rebalance shards
     */
    Uni<Void> rebalanceShards();

    /**
     * Get shard statistics
     */
    Uni<List<ShardStats>> getShardStats();
}