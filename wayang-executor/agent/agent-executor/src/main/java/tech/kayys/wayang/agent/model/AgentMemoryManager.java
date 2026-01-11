package tech.kayys.wayang.agent.model;

import java.util.List;

import io.smallrye.mutiny.Uni;

/**
 * ============================================================================
 * AGENT MEMORY MANAGEMENT SYSTEM
 * ============================================================================
 * 
 * Provides multi-strategy memory management for agents:
 * 
 * Memory Types:
 * 1. Buffer Memory: Simple FIFO buffer with window size
 * 2. Summary Memory: Maintains conversation summaries
 * 3. Vector Memory: Semantic search over conversation history
 * 4. Entity Memory: Tracks entities mentioned in conversation
 * 
 * Storage:
 * - Short-term: In-memory cache for active sessions
 * - Long-term: Database persistence for conversation history
 * - Vector: Vector database for semantic retrieval
 * 
 * Architecture:
 * ┌────────────────────────────────────────────────────────┐
 * │           AgentMemoryManager                           │
 * ├────────────────────────────────────────────────────────┤
 * │  ┌──────────┐  ┌──────────┐  ┌─────────┐  ┌────────┐ │
 * │  │  Buffer  │  │ Summary  │  │ Vector  │  │ Entity │ │
 * │  │  Memory  │  │  Memory  │  │ Memory  │  │ Memory │ │
 * │  └──────────┘  └──────────┘  └─────────┘  └────────┘ │
 * │       │             │              │           │       │
 * │  ┌────▼─────────────▼──────────────▼───────────▼───┐ │
 * │  │          Memory Storage Backend                 │ │
 * │  │  (Cache + Database + Vector Store)              │ │
 * │  └──────────────────────────────────────────────────┘ │
 * └────────────────────────────────────────────────────────┘
 */

// ==================== MEMORY MANAGER INTERFACE ====================

/**
 * Main interface for agent memory operations
 */
public interface AgentMemoryManager {

        /**
         * Load memory for a session
         */
        Uni<List<Message>> loadMemory(
                        String sessionId,
                        String tenantId,
                        String memoryType,
                        Integer windowSize);

        /**
         * Save messages to memory
         */
        Uni<Void> saveMessages(
                        String sessionId,
                        String tenantId,
                        List<Message> messages);

        /**
         * Clear memory for a session
         */
        Uni<Void> clearMemory(String sessionId, String tenantId);

        /**
         * Search memory semantically (for vector memory)
         */
        Uni<List<Message>> searchMemory(
                        String sessionId,
                        String tenantId,
                        String query,
                        int limit);

        /**
         * Get memory statistics
         */
        Uni<MemoryStats> getStats(String sessionId, String tenantId);
}
