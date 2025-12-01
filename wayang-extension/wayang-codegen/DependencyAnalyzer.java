
/**
 * Analyzes workflow schema and determines required dependencies
 */
@ApplicationScoped
public class DependencyAnalyzer {
    
    /**
     * Analyze workflow schema and extract required components
     */
    public DependencyGraph analyze(WorkflowSchema schema) {
        DependencyGraph graph = new DependencyGraph();
        
        // Always required
        graph.addCoreDependency("io.wayang:wayang-api");
        graph.addCoreDependency("io.wayang:wayang-runtime-core");
        
        // Analyze nodes
        for (NodeDefinition node : schema.getNodes()) {
            analyzeNode(node, graph);
        }
        
        // Analyze connections and data flow
        for (EdgeDefinition edge : schema.getEdges()) {
            analyzeEdge(edge, graph);
        }
        
        return graph;
    }
    
    private void analyzeNode(NodeDefinition node, DependencyGraph graph) {
        switch (node.getType()) {
            case "rag" -> {
                graph.addNodeDependency("io.wayang:wayang-node-rag");
                graph.addServiceDependency("io.wayang:wayang-service-embedding");
                graph.addServiceDependency("io.wayang:wayang-service-vector");
                
                // Analyze vector store provider
                String vectorProvider = node.getProperty("vectorStore", "pgvector");
                if ("pgvector".equals(vectorProvider)) {
                    graph.addRuntimeDependency("org.postgresql:postgresql");
                    graph.addRuntimeDependency("com.pgvector:pgvector-java");
                } else if ("milvus".equals(vectorProvider)) {
                    graph.addRuntimeDependency("io.milvus:milvus-sdk-java");
                }
                
                // Analyze embedding provider
                String embeddingProvider = node.getProperty("embeddingProvider", "local");
                if ("local".equals(embeddingProvider)) {
                    graph.addRuntimeDependency("ai.djl:api");
                    graph.addRuntimeDependency("ai.djl.huggingface:tokenizers");
                }
            }
            
            case "agent" -> {
                graph.addNodeDependency("io.wayang:wayang-node-agent");
                graph.addServiceDependency("io.wayang:wayang-service-llm");
                
                // Analyze LLM provider
                String llmProvider = node.getProperty("llmProvider", "ollama");
                if ("ollama".equals(llmProvider)) {
                    graph.addRuntimeDependency("io.github.ollama4j:ollama4j");
                } else if ("openai".equals(llmProvider)) {
                    graph.addRuntimeDependency("com.theokanning.openai-gpt3-java:service");
                }
            }
            
            case "tool" -> {
                graph.addNodeDependency("io.wayang:wayang-node-tool");
                graph.addServiceDependency("io.wayang:wayang-service-tool");
            }
            
            case "guardrails" -> {
                graph.addNodeDependency("io.wayang:wayang-node-guardrails");
            }
            
            case "evaluator" -> {
                graph.addNodeDependency("io.wayang:wayang-node-evaluator");
                graph.addServiceDependency("io.wayang:wayang-service-llm");
            }
            
            case "memory" -> {
                graph.addNodeDependency("io.wayang:wayang-node-memory");
                graph.addServiceDependency("io.wayang:wayang-service-memory");
            }
            
            // Add more node types...
        }
    }
}