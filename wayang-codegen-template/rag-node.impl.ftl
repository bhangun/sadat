package tech.kayys.generated.nodes;

import tech.kayys.wayang.core.node.AbstractNode;
import tech.kayys.wayang.core.node.NodeContext;
import tech.kayys.wayang.core.execution.ExecutionResult;
import tech.kayys.wayang.core.execution.Status;
import tech.kayys.wayang.runtime.rag.RAGAdapter;
import tech.kayys.wayang.runtime.rag.SearchQuery;
import tech.kayys.wayang.runtime.rag.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Generated RAG Node Implementation
 * 
 * Configuration:
<#list instances as instance>
 * - Node ID: ${instance.nodeId}
 *   Index: ${instance.config.index}
 *   Top K: ${instance.config.topK!5}
</#list>
 */
public class RAGNodeImplementation extends AbstractNode {
    
    private static final Logger log = LoggerFactory.getLogger(RAGNodeImplementation.class);
    
    private RAGAdapter ragAdapter;
    
    @Override
    protected void initializeResources() {
        this.ragAdapter = context.getAdapter(RAGAdapter.class);
        log.info("RAG node initialized with adapter: {}", ragAdapter);
    }
    
    @Override
    protected ExecutionResult doExecute(NodeContext context) {
        String nodeId = context.getNodeId();
        log.info("Executing RAG node: {}", nodeId);
        
        try {
            // Get input query
            String query = context.getInput("query", String.class);
            if (query == null || query.isEmpty()) {
                throw new IllegalArgumentException("Query input is required");
            }
            
            // Get node-specific configuration
            Map<String, Object> nodeConfig = getNodeConfig(nodeId);
            String index = (String) nodeConfig.get("index");
            int topK = (Integer) nodeConfig.getOrDefault("topK", 5);
            
            log.debug("Searching index '{}' with query: {}", index, query);
            
            // Build search query
            SearchQuery searchQuery = SearchQuery.builder()
                .query(query)
                .index(index)
                .topK(topK)
                .filters(context.getVariables())
                .build();
            
            // Execute search
            SearchResult result = ragAdapter.search(searchQuery);
            
            log.info("Found {} results", result.getResults().size());
            
            // Prepare outputs
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("results", result.getResults());
            outputs.put("context", result.getFormattedContext());
            outputs.put("count", result.getResults().size());
            
            return ExecutionResult.builder()
                .status(Status.SUCCESS)
                .outputs(outputs)
                .build();
                
        } catch (Exception e) {
            log.error("RAG node execution failed", e);
            return ExecutionResult.builder()
                .status(Status.FAILED)
                .error(e.getMessage())
                .build();
        }
    }
    
    private Map<String, Object> getNodeConfig(String nodeId) {
<#list instances as instance>
        if ("${instance.nodeId}".equals(nodeId)) {
            Map<String, Object> config = new HashMap<>();
            config.put("index", "${instance.config.index}");
            config.put("topK", ${instance.config.topK!5});
            return config;
        }
</#list>
        throw new IllegalArgumentException("Unknown node ID: " + nodeId);
    }
}