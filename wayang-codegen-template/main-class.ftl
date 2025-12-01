package tech.kayys.generated;

import tech.kayys.wayang.runtime.minimal.RuntimeContext;
import tech.kayys.wayang.runtime.minimal.WorkflowExecutor;
import tech.kayys.wayang.core.workflow.WorkflowDefinition;
import tech.kayys.wayang.core.execution.ExecutionResult;
import tech.kayys.wayang.core.execution.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;

/**
 * Generated Standalone Agent: ${workflowName}
 * Generated at: ${generationTimestamp}
 * 
 * This agent contains only the components used in your workflow:
<#list usedNodeTypes as nodeType>
 * - ${nodeType} Node
</#list>
 * 
 * Required capabilities:
<#list requiredCapabilities as capability>
 * - ${capability}
</#list>
 */
public class StandaloneAgent {
    
    private static final Logger log = LoggerFactory.getLogger(StandaloneAgent.class);
    
    // Embedded workflow definition
    private static final String WORKFLOW_JSON = """
${workflowJson}
    """;
    
    public static void main(String[] args) {
        log.info("Starting Standalone Agent: ${workflowName}");
        log.info("Version: ${version}");
        
        StandaloneAgent agent = new StandaloneAgent();
        
        try {
            // Parse arguments
            AgentConfig config = parseArguments(args);
            
            // Initialize runtime
            agent.initialize(config);
            
            // Execute workflow
            ExecutionResult result = agent.execute();
            
            // Handle result
            if (result.getStatus() == Status.SUCCESS) {
                log.info("Workflow executed successfully");
                System.exit(0);
            } else {
                log.error("Workflow execution failed: {}", result.getError());
                System.exit(1);
            }
            
        } catch (Exception e) {
            log.error("Agent execution failed", e);
            System.exit(1);
        }
    }
    
    private final RuntimeContext context;
    private final WorkflowExecutor executor;
    private final WorkflowDefinition workflow;
    
    public StandaloneAgent() throws IOException {
        // Parse embedded workflow
        this.workflow = WorkflowDefinition.fromJson(WORKFLOW_JSON);
        
        // Create minimal runtime context
        this.context = new RuntimeContext();
        
        // Create executor with only required components
        this.executor = new WorkflowExecutor(context);
        
<#if requiredCapabilities?seq_contains("LLM_ACCESS")>
        // Initialize LLM support
        context.registerAdapter(new LLMAdapter(context));
</#if>
<#if requiredCapabilities?seq_contains("RAG_QUERY")>
        // Initialize RAG support
        context.registerAdapter(new RAGAdapter(context));
</#if>
<#if requiredCapabilities?seq_contains("TOOL_EXECUTION")>
        // Initialize Tool support
        context.registerAdapter(new ToolAdapter(context));
</#if>
<#if requiredCapabilities?seq_contains("MEMORY_ACCESS")>
        // Initialize Memory support
        context.registerAdapter(new MemoryAdapter(context));
</#if>
    }
    
    public void initialize(AgentConfig config) {
        log.info("Initializing agent with config: {}", config);
        context.setConfig(config);
        context.initialize();
    }
    
    public ExecutionResult execute() {
        log.info("Executing workflow: {}", workflow.getName());
        
        Instant startTime = Instant.now();
        
        try {
            ExecutionResult result = executor.execute(workflow);
            
            log.info("Execution completed in {} ms", 
                java.time.Duration.between(startTime, Instant.now()).toMillis());
            
            return result;
            
        } catch (Exception e) {
            log.error("Execution failed", e);
            return ExecutionResult.builder()
                .status(Status.FAILED)
                .error(e.getMessage())
                .build();
        }
    }
    
    private static AgentConfig parseArguments(String[] args) {
        AgentConfig config = new AgentConfig();
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--config":
                    config.setConfigFile(args[++i]);
                    break;
                case "--input":
                    config.setInputData(args[++i]);
                    break;
                case "--output":
                    config.setOutputFile(args[++i]);
                    break;
                case "--verbose":
                    config.setVerbose(true);
                    break;
<#if requiredCapabilities?seq_contains("LLM_ACCESS")>
                case "--llm-endpoint":
                    config.setLlmEndpoint(args[++i]);
                    break;
</#if>
<#if requiredCapabilities?seq_contains("RAG_QUERY")>
                case "--rag-index":
                    config.setRagIndex(args[++i]);
                    break;
</#if>
            }
        }
        
        return config;
    }
}