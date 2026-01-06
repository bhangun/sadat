package tech.kayys.wayang.project.service;

import java.util.List;

/**
 * Agent Memory interface
 */
public interface AgentMemory {
    void store(String key, String value);

    String retrieve(String key);

    List<String> search(String query, int limit);
}
