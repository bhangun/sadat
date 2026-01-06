package tech.kayys.wayang.node.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import tech.kayys.wayang.node.dto.ConfigField;
import tech.kayys.wayang.node.dto.FieldType;
import tech.kayys.wayang.node.dto.NodeDefinition;
import tech.kayys.wayang.node.dto.NodePort;
import tech.kayys.wayang.node.dto.UIDescriptor;
import tech.kayys.wayang.node.dto.UIStyle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * SILAT NODE COMPONENT SYSTEM
 * ============================================================================
 * 
 * Comprehensive built-in node library with:
 * - Atomic nodes (memory, tools, models)
 * - Composite nodes (agents, orchestrators)
 * - EIP pattern nodes
 * - Plugin system for custom nodes
 * - UI descriptor for visual rendering
 * - Dynamic schema loading
 */

// ==================== NODE DEFINITION REGISTRY ====================

/**
 * Central registry for all node types
 */
@ApplicationScoped
public class NodeDefinitionRegistry {

        private static final Logger LOG = LoggerFactory.getLogger(NodeDefinitionRegistry.class);

        private final Map<String, NodeDefinition> registry = new LinkedHashMap<>();

        @Inject
        NodeDefinitionLoader loader;

        @jakarta.annotation.PostConstruct
        void initialize() {
                LOG.info("Initializing node definition registry");

                // Load built-in nodes
                loadBuiltInNodes();

                // Load custom/plugin nodes
                loadPluginNodes();

                LOG.info("Registered {} node definitions", registry.size());
        }

        private void loadBuiltInNodes() {
                // Atomic nodes
                registerAtomicNodes();

                // Composite nodes
                registerCompositeNodes();

                // EIP nodes
                registerEIPNodes();

                // Control flow nodes
                registerControlFlowNodes();

                // Integration nodes
                registerIntegrationNodes();
        }

        private void loadPluginNodes() {
                loader.loadFromPlugins()
                                .forEach(def -> register(def));
        }

        public void register(NodeDefinition definition) {
                registry.put(definition.type, definition);
                LOG.debug("Registered node: {} ({})", definition.label, definition.type);
        }

        public NodeDefinition get(String type) {
                return registry.get(type);
        }

        public List<NodeDefinition> getAll() {
                return new ArrayList<>(registry.values());
        }

        public List<NodeDefinition> getByCategory(String category) {
                return registry.values().stream()
                                .filter(def -> category.equals(def.category))
                                .toList();
        }

        // ==================== ATOMIC NODES ====================

        private void registerAtomicNodes() {
                // Memory nodes
                register(NodeDefinition.builder()
                                .type("memory.pgvector")
                                .label("PostgreSQL Vector Store")
                                .category("Memory")
                                .subCategory("Vector Database")
                                .description("Store and retrieve embeddings using pgvector")
                                .icon("database")
                                .color("#336791")
                                .isAtomic(true)
                                .addConfigField(ConfigField.builder()
                                                .name("connectionString")
                                                .label("Connection String")
                                                .type(FieldType.TEXT)
                                                .required(true)
                                                .description("PostgreSQL connection string")
                                                .defaultValue("postgresql://localhost:5432/vectordb")
                                                .build())
                                .addConfigField(ConfigField.builder()
                                                .name("tableName")
                                                .label("Table Name")
                                                .type(FieldType.TEXT)
                                                .required(true)
                                                .defaultValue("embeddings")
                                                .build())
                                .addConfigField(ConfigField.builder()
                                                .name("dimensions")
                                                .label("Vector Dimensions")
                                                .type(FieldType.NUMBER)
                                                .required(true)
                                                .defaultValue(1536)
                                                .validation(Map.of("min", 1, "max", 4096))
                                                .build())
                                .addPort(NodePort.input("query", "Query Vector", "vector"))
                                .addPort(NodePort.output("results", "Search Results", "array"))
                                .uiDescriptor(UIDescriptor.builder()
                                                .width(250)
                                                .height(120)
                                                .icon("database")
                                                .color("#336791")
                                                .style(UIStyle.ROUNDED)
                                                .build())
                                .build());

                register(NodeDefinition.builder()
                                .type("memory.qdrant")
                                .label("Qdrant Vector Store")
                                .category("Memory")
                                .subCategory("Vector Database")
                                .description("High-performance vector search with Qdrant")
                                .icon("search")
                                .color("#DC382C")
                                .isAtomic(true)
                                .addConfigField(ConfigField.builder()
                                                .name("url")
                                                .label("Qdrant URL")
                                                .type(FieldType.TEXT)
                                                .required(true)
                                                .defaultValue("http://localhost:6333")
                                                .build())
                                .addConfigField(ConfigField.builder()
                                                .name("collectionName")
                                                .label("Collection Name")
                                                .type(FieldType.TEXT)
                                                .required(true)
                                                .build())
                                .addConfigField(ConfigField.builder()
                                                .name("apiKey")
                                                .label("API Key")
                                                .type(FieldType.PASSWORD)
                                                .required(false)
                                                .build())
                                .addPort(NodePort.input("vectors", "Vectors", "vector[]"))
                                .addPort(NodePort.output("matches", "Matches", "array"))
                                .build());

                register(NodeDefinition.builder()
                                .type("memory.redis")
                                .label("Redis Memory Store")
                                .category("Memory")
                                .subCategory("Cache")
                                .description("Fast in-memory data storage")
                                .icon("memory")
                                .color("#DC382D")
                                .isAtomic(true)
                                .addConfigField(ConfigField.text("host", "Redis Host", true, "localhost"))
                                .addConfigField(ConfigField.number("port", "Port", true, 6379))
                                .addConfigField(ConfigField.password("password", "Password", false))
                                .addConfigField(ConfigField.number("ttl", "TTL (seconds)", false, 3600))
                                .addPort(NodePort.input("key", "Key", "string"))
                                .addPort(NodePort.input("value", "Value", "any"))
                                .addPort(NodePort.output("result", "Result", "any"))
                                .build());

                // LLM/Model nodes
                register(NodeDefinition.builder()
                                .type("llm.openai")
                                .label("OpenAI GPT")
                                .category("LLM")
                                .subCategory("Chat Model")
                                .description("OpenAI GPT models (GPT-4, GPT-3.5)")
                                .icon("brain")
                                .color("#10A37F")
                                .isAtomic(true)
                                .addConfigField(ConfigField.select("model", "Model", true,
                                                List.of("gpt-4", "gpt-4-turbo", "gpt-3.5-turbo"), "gpt-4"))
                                .addConfigField(ConfigField.password("apiKey", "API Key", true))
                                .addConfigField(ConfigField.slider("temperature", "Temperature", false, 0.7, 0.0, 2.0,
                                                0.1))
                                .addConfigField(ConfigField.number("maxTokens", "Max Tokens", false, 1000))
                                .addPort(NodePort.input("prompt", "Prompt", "string"))
                                .addPort(NodePort.input("messages", "Messages", "array"))
                                .addPort(NodePort.output("response", "Response", "string"))
                                .addPort(NodePort.output("usage", "Usage", "object"))
                                .build());

                register(NodeDefinition.builder()
                                .type("llm.anthropic")
                                .label("Anthropic Claude")
                                .category("LLM")
                                .subCategory("Chat Model")
                                .description("Claude 3 models (Opus, Sonnet, Haiku)")
                                .icon("brain")
                                .color("#CC785C")
                                .isAtomic(true)
                                .addConfigField(ConfigField.select("model", "Model", true,
                                                List.of("claude-3-opus-20240229", "claude-3-sonnet-20240229",
                                                                "claude-3-haiku-20240307"),
                                                "claude-3-sonnet-20240229"))
                                .addConfigField(ConfigField.password("apiKey", "API Key", true))
                                .addConfigField(ConfigField.slider("temperature", "Temperature", false, 1.0, 0.0, 1.0,
                                                0.1))
                                .addConfigField(ConfigField.number("maxTokens", "Max Tokens", true, 1024))
                                .addPort(NodePort.input("prompt", "Prompt", "string"))
                                .addPort(NodePort.output("response", "Response", "string"))
                                .build());

                // Tool nodes
                register(NodeDefinition.builder()
                                .type("tool.http")
                                .label("HTTP API Call")
                                .category("Tools")
                                .subCategory("Network")
                                .description("Make HTTP requests to external APIs")
                                .icon("globe")
                                .color("#5B8DEE")
                                .isAtomic(true)
                                .addConfigField(ConfigField.select("method", "HTTP Method", true,
                                                List.of("GET", "POST", "PUT", "DELETE", "PATCH"), "GET"))
                                .addConfigField(ConfigField.text("url", "URL", true))
                                .addConfigField(ConfigField.code("headers", "Headers (JSON)", false,
                                                "application/json"))
                                .addConfigField(ConfigField.code("body", "Request Body", false, "application/json"))
                                .addConfigField(ConfigField.number("timeout", "Timeout (ms)", false, 30000))
                                .addPort(NodePort.input("params", "Parameters", "object"))
                                .addPort(NodePort.output("response", "Response", "object"))
                                .addPort(NodePort.output("status", "Status Code", "number"))
                                .build());

                register(NodeDefinition.builder()
                                .type("tool.database")
                                .label("Database Query")
                                .category("Tools")
                                .subCategory("Data")
                                .description("Execute SQL queries")
                                .icon("database")
                                .color("#F29111")
                                .isAtomic(true)
                                .addConfigField(ConfigField.text("connectionString", "Connection String", true))
                                .addConfigField(ConfigField.code("query", "SQL Query", true, "sql"))
                                .addConfigField(ConfigField.toggle("prepared", "Use Prepared Statement", false, true))
                                .addPort(NodePort.input("parameters", "Parameters", "array"))
                                .addPort(NodePort.output("rows", "Result Rows", "array"))
                                .addPort(NodePort.output("count", "Row Count", "number"))
                                .build());

                register(NodeDefinition.builder()
                                .type("tool.python")
                                .label("Python Script")
                                .category("Tools")
                                .subCategory("Scripting")
                                .description("Execute Python code")
                                .icon("code")
                                .color("#3776AB")
                                .isAtomic(true)
                                .addConfigField(ConfigField.code("script", "Python Script", true, "python"))
                                .addConfigField(ConfigField.text("requirements", "Dependencies", false))
                                .addConfigField(ConfigField.number("timeout", "Timeout (seconds)", false, 60))
                                .addPort(NodePort.input("input", "Input Data", "any"))
                                .addPort(NodePort.output("output", "Output", "any"))
                                .addPort(NodePort.output("stdout", "Standard Output", "string"))
                                .addPort(NodePort.output("stderr", "Error Output", "string"))
                                .build());
        }

        // ==================== COMPOSITE NODES ====================

        private void registerCompositeNodes() {
                // AI Agent (composite)
                register(NodeDefinition.builder()
                                .type("agent.conversational")
                                .label("Conversational Agent")
                                .category("AI Agents")
                                .subCategory("Composite")
                                .description("Complete AI agent with memory, tools, and LLM")
                                .icon("robot")
                                .color("#6366F1")
                                .isComposite(true)
                                .addComponent("llm", "llm.openai", Map.of("model", "gpt-4"))
                                .addComponent("memory", "memory.redis", Map.of())
                                .addComponent("tools", "tool.http", Map.of())
                                .addConfigField(ConfigField.text("systemPrompt", "System Prompt", true))
                                .addConfigField(ConfigField.select("llmProvider", "LLM Provider", true,
                                                List.of("openai", "anthropic", "azure"), "openai"))
                                .addConfigField(ConfigField.multiSelect("tools", "Available Tools", false,
                                                List.of("http", "database", "python", "search")))
                                .addConfigField(ConfigField.toggle("enableMemory", "Enable Memory", false, true))
                                .addConfigField(ConfigField.number("memoryWindow", "Memory Window", false, 10))
                                .addPort(NodePort.input("message", "User Message", "string"))
                                .addPort(NodePort.input("context", "Context", "object"))
                                .addPort(NodePort.output("response", "Agent Response", "string"))
                                .addPort(NodePort.output("actions", "Actions Taken", "array"))
                                .uiDescriptor(UIDescriptor.builder()
                                                .width(300)
                                                .height(180)
                                                .icon("robot")
                                                .color("#6366F1")
                                                .style(UIStyle.ROUNDED)
                                                .badge("AI")
                                                .build())
                                .build());

                // Multi-Agent Orchestrator
                register(NodeDefinition.builder()
                                .type("agent.orchestrator")
                                .label("Agent Orchestrator")
                                .category("AI Agents")
                                .subCategory("Composite")
                                .description("Orchestrate multiple agents (planner, executor, evaluator)")
                                .icon("network")
                                .color("#8B5CF6")
                                .isComposite(true)
                                .addComponent("planner", "agent.conversational", Map.of(
                                                "systemPrompt",
                                                "You are a planning agent. Break down tasks into steps."))
                                .addComponent("executor", "agent.conversational", Map.of(
                                                "systemPrompt",
                                                "You are an execution agent. Perform the planned steps."))
                                .addComponent("evaluator", "agent.conversational", Map.of(
                                                "systemPrompt", "You are an evaluation agent. Assess results quality."))
                                .addConfigField(ConfigField.select("strategy", "Orchestration Strategy", true,
                                                List.of("sequential", "parallel", "dynamic"), "sequential"))
                                .addConfigField(ConfigField.number("maxIterations", "Max Iterations", false, 5))
                                .addConfigField(ConfigField.toggle("enableFeedback", "Enable Feedback Loop", false,
                                                true))
                                .addPort(NodePort.input("task", "Task Description", "string"))
                                .addPort(NodePort.output("result", "Final Result", "object"))
                                .addPort(NodePort.output("steps", "Execution Steps", "array"))
                                .addPort(NodePort.output("evaluation", "Quality Score", "number"))
                                .uiDescriptor(UIDescriptor.builder()
                                                .width(350)
                                                .height(200)
                                                .icon("network")
                                                .color("#8B5CF6")
                                                .style(UIStyle.GRADIENT)
                                                .badge("Multi-Agent")
                                                .showComponents(true)
                                                .build())
                                .build());

                // RAG (Retrieval Augmented Generation) Agent
                register(NodeDefinition.builder()
                                .type("agent.rag")
                                .label("RAG Agent")
                                .category("AI Agents")
                                .subCategory("Composite")
                                .description("Agent with retrieval-augmented generation")
                                .icon("book-open")
                                .color("#10B981")
                                .isComposite(true)
                                .addComponent("vectorStore", "memory.pgvector", Map.of())
                                .addComponent("embeddings", "llm.openai", Map.of("model", "text-embedding-ada-002"))
                                .addComponent("llm", "llm.openai", Map.of("model", "gpt-4"))
                                .addConfigField(ConfigField.number("topK", "Top K Results", false, 5))
                                .addConfigField(
                                                ConfigField.slider("similarityThreshold", "Similarity Threshold", false,
                                                                0.7, 0.0, 1.0, 0.05))
                                .addConfigField(ConfigField.text("systemPrompt", "System Prompt", false))
                                .addPort(NodePort.input("query", "Query", "string"))
                                .addPort(NodePort.output("answer", "Answer", "string"))
                                .addPort(NodePort.output("sources", "Sources", "array"))
                                .build());
        }

        // ==================== EIP NODES ====================

        private void registerEIPNodes() {
                // Message Router
                register(NodeDefinition.builder()
                                .type("eip.router.content")
                                .label("Content-Based Router")
                                .category("EIP")
                                .subCategory("Routing")
                                .description("Route messages based on content")
                                .icon("split")
                                .color("#F59E0B")
                                .isAtomic(true)
                                .addConfigField(ConfigField.code("routes", "Routing Rules (JSON)", true, "json"))
                                .addConfigField(ConfigField.text("defaultRoute", "Default Route", false))
                                .addPort(NodePort.input("message", "Message", "object"))
                                .addPort(NodePort.output("route1", "Route 1", "object"))
                                .addPort(NodePort.output("route2", "Route 2", "object"))
                                .addPort(NodePort.output("default", "Default", "object"))
                                .build());

                // Message Transformer
                register(NodeDefinition.builder()
                                .type("eip.transformer")
                                .label("Message Transformer")
                                .category("EIP")
                                .subCategory("Transformation")
                                .description("Transform message format")
                                .icon("transform")
                                .color("#EC4899")
                                .isAtomic(true)
                                .addConfigField(ConfigField.select("transformType", "Transform Type", true,
                                                List.of("jq", "jsonata", "javascript", "xslt"), "jq"))
                                .addConfigField(ConfigField.code("expression", "Transform Expression", true, "json"))
                                .addConfigField(ConfigField.number("timeout", "Timeout (ms)", false, 5000))
                                .addPort(NodePort.input("input", "Input Message", "any"))
                                .addPort(NodePort.output("output", "Transformed Message", "any"))
                                .build());

                // Splitter
                register(NodeDefinition.builder()
                                .type("eip.splitter")
                                .label("Splitter")
                                .category("EIP")
                                .subCategory("Routing")
                                .description("Split message into multiple parts")
                                .icon("scissors")
                                .color("#14B8A6")
                                .isAtomic(true)
                                .addConfigField(ConfigField.select("splitBy", "Split By", true,
                                                List.of("array", "delimiter", "size", "expression"), "array"))
                                .addConfigField(ConfigField.text("expression", "Split Expression", false))
                                .addPort(NodePort.input("message", "Input Message", "any"))
                                .addPort(NodePort.output("parts", "Message Parts", "array"))
                                .build());

                // Aggregator
                register(NodeDefinition.builder()
                                .type("eip.aggregator")
                                .label("Aggregator")
                                .category("EIP")
                                .subCategory("Routing")
                                .description("Aggregate multiple messages")
                                .icon("layers")
                                .color("#06B6D4")
                                .isAtomic(true)
                                .addConfigField(ConfigField.select("strategy", "Aggregation Strategy", true,
                                                List.of("collect", "merge", "reduce"), "collect"))
                                .addConfigField(ConfigField.number("batchSize", "Batch Size", false, 10))
                                .addConfigField(ConfigField.number("timeout", "Timeout (ms)", false, 5000))
                                .addPort(NodePort.input("messages", "Messages", "array"))
                                .addPort(NodePort.output("aggregated", "Aggregated Result", "object"))
                                .build());

                // Content Enricher
                register(NodeDefinition.builder()
                                .type("eip.enricher")
                                .label("Content Enricher")
                                .category("EIP")
                                .subCategory("Transformation")
                                .description("Enrich message with additional data")
                                .icon("plus-circle")
                                .color("#8B5CF6")
                                .isAtomic(true)
                                .addConfigField(ConfigField.select("source", "Enrichment Source", true,
                                                List.of("api", "database", "cache", "static"), "api"))
                                .addConfigField(ConfigField.text("endpoint", "Endpoint/Query", true))
                                .addConfigField(ConfigField.text("mergeKey", "Merge Key", false))
                                .addPort(NodePort.input("message", "Original Message", "object"))
                                .addPort(NodePort.output("enriched", "Enriched Message", "object"))
                                .build());
        }

        // ==================== CONTROL FLOW NODES ====================

        private void registerControlFlowNodes() {
                register(NodeDefinition.builder()
                                .type("control.start")
                                .label("Start")
                                .category("Control Flow")
                                .description("Workflow entry point")
                                .icon("play-circle")
                                .color("#10B981")
                                .isAtomic(true)
                                .addPort(NodePort.output("trigger", "Start", "trigger"))
                                .uiDescriptor(UIDescriptor.builder()
                                                .width(150)
                                                .height(60)
                                                .icon("play-circle")
                                                .color("#10B981")
                                                .style(UIStyle.PILL)
                                                .build())
                                .build());

                register(NodeDefinition.builder()
                                .type("control.end")
                                .label("End")
                                .category("Control Flow")
                                .description("Workflow termination")
                                .icon("stop-circle")
                                .color("#EF4444")
                                .isAtomic(true)
                                .addPort(NodePort.input("complete", "Complete", "trigger"))
                                .uiDescriptor(UIDescriptor.builder()
                                                .width(150)
                                                .height(60)
                                                .icon("stop-circle")
                                                .color("#EF4444")
                                                .style(UIStyle.PILL)
                                                .build())
                                .build());

                register(NodeDefinition.builder()
                                .type("control.decision")
                                .label("Decision")
                                .category("Control Flow")
                                .description("Conditional branching")
                                .icon("git-branch")
                                .color("#F59E0B")
                                .isAtomic(true)
                                .addConfigField(ConfigField.code("condition", "Condition Expression", true, "json"))
                                .addConfigField(ConfigField.number("timeout", "Timeout (ms)", false, 5000))
                                .addPort(NodePort.input("input", "Input", "any"))
                                .addPort(NodePort.output("true", "True Branch", "any"))
                                .addPort(NodePort.output("false", "False Branch", "any"))
                                .uiDescriptor(UIDescriptor.builder()
                                                .width(200)
                                                .height(100)
                                                .icon("git-branch")
                                                .color("#F59E0B")
                                                .style(UIStyle.DIAMOND)
                                                .build())
                                .build());

                register(NodeDefinition.builder()
                                .type("control.parallel")
                                .label("Parallel Gateway")
                                .category("Control Flow")
                                .description("Execute branches in parallel")
                                .icon("layers")
                                .color("#6366F1")
                                .isAtomic(true)
                                .addPort(NodePort.input("input", "Input", "any"))
                                .addPort(NodePort.output("branch1", "Branch 1", "any"))
                                .addPort(NodePort.output("branch2", "Branch 2", "any"))
                                .addPort(NodePort.output("branch3", "Branch 3", "any"))
                                .build());
        }

        // ==================== INTEGRATION NODES ====================

        private void registerIntegrationNodes() {
                register(NodeDefinition.builder()
                                .type("integration.webhook")
                                .label("Webhook")
                                .category("Integration")
                                .subCategory("Triggers")
                                .description("Receive HTTP webhook events")
                                .icon("webhook")
                                .color("#3B82F6")
                                .isAtomic(true)
                                .addConfigField(ConfigField.text("path", "Webhook Path", true, "/webhook"))
                                .addConfigField(ConfigField.select("method", "HTTP Method", false,
                                                List.of("POST", "GET", "PUT"), "POST"))
                                .addConfigField(ConfigField.password("secret", "Webhook Secret", false))
                                .addPort(NodePort.output("payload", "Payload", "object"))
                                .addPort(NodePort.output("headers", "Headers", "object"))
                                .build());

                register(NodeDefinition.builder()
                                .type("integration.schedule")
                                .label("Scheduled Trigger")
                                .category("Integration")
                                .subCategory("Triggers")
                                .description("Run on schedule (cron)")
                                .icon("clock")
                                .color("#8B5CF6")
                                .isAtomic(true)
                                .addConfigField(ConfigField.text("cron", "Cron Expression", true, "0 0 * * *"))
                                .addConfigField(ConfigField.text("timezone", "Timezone", false, "UTC"))
                                .addPort(NodePort.output("trigger", "Trigger", "timestamp"))
                                .build());

                register(NodeDefinition.builder()
                                .type("integration.kafka")
                                .label("Kafka Consumer")
                                .category("Integration")
                                .subCategory("Messaging")
                                .description("Consume messages from Kafka")
                                .icon("message-square")
                                .color("#231F20")
                                .isAtomic(true)
                                .addConfigField(ConfigField.text("brokers", "Bootstrap Servers", true,
                                                "localhost:9092"))
                                .addConfigField(ConfigField.text("topic", "Topic", true))
                                .addConfigField(ConfigField.text("groupId", "Consumer Group", true))
                                .addPort(NodePort.output("message", "Message", "object"))
                                .addPort(NodePort.output("key", "Key", "string"))
                                .addPort(NodePort.output("metadata", "Metadata", "object"))
                                .build());
        }
}
