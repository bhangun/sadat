package tech.kayys.wayang.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

import javax.tools.Tool;

public class Workflow {

    private String id;
    private String name;
    private String description;
    private List<Node> nodes;
    private List<Edge> edges;
    private List<Variable> variables;
    private List<Trigger> triggers;
    private AgentDefinition.Metadata metadata;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public void setVariables(List<Variable> variables) {
        this.variables = variables;
    }

    public List<Trigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<Trigger> triggers) {
        this.triggers = triggers;
    }

    public AgentDefinition.Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(AgentDefinition.Metadata metadata) {
        this.metadata = metadata;
    }

    public static class Node {
        private String id;
        private NodeType type;
        private String name;
        private String description;
        private Position position;
        private NodeConfig config;
        private List<NodeInput> inputs;
        private List<NodeOutput> outputs;
        private ErrorHandling errorHandling;
        private AgentDefinition.Metadata metadata;

        public enum NodeType {
            @JsonProperty("start")
            START,
            @JsonProperty("end")
            END,
            @JsonProperty("llm")
            LLM,
            @JsonProperty("tool")
            TOOL,
            @JsonProperty("decision")
            DECISION,
            @JsonProperty("loop")
            LOOP,
            @JsonProperty("parallel")
            PARALLEL,
            @JsonProperty("merge")
            MERGE,
            @JsonProperty("human_input")
            HUMAN_INPUT,
            @JsonProperty("code_execution")
            CODE_EXECUTION,
            @JsonProperty("webhook")
            WEBHOOK,
            @JsonProperty("schedule")
            SCHEDULE,
            @JsonProperty("condition")
            CONDITION,
            @JsonProperty("switch")
            SWITCH,
            @JsonProperty("delay")
            DELAY,
            @JsonProperty("transform")
            TRANSFORM,
            @JsonProperty("validator")
            VALIDATOR,
            @JsonProperty("router")
            ROUTER
        }

        public static class Position {
            private Double x;
            private Double y;

            public Double getX() {
                return x;
            }

            public void setX(Double x) {
                this.x = x;
            }

            public Double getY() {
                return y;
            }

            public void setY(Double y) {
                this.y = y;
            }
        }

        public static class NodeConfig {
            private LLMConfig llmConfig;
            private Tool tool;
            private String prompt;
            private PromptTemplate promptTemplate;
            private Condition condition;
            private LoopConfig loopConfig;
            private ParallelConfig parallelConfig;
            private TransformConfig transformConfig;
            private ValidationConfig validationConfig;
            private HumanInputConfig humanInputConfig;

            public static class PromptTemplate {
                private String template;
                private List<String> variables;

                public String getTemplate() {
                    return template;
                }

                public void setTemplate(String template) {
                    this.template = template;
                }

                public List<String> getVariables() {
                    return variables;
                }

                public void setVariables(List<String> variables) {
                    this.variables = variables;
                }
            }

            public static class Condition {
                private String expression;
                private String operator;
                private Object value;
                private String logic;

                public String getExpression() {
                    return expression;
                }

                public void setExpression(String expression) {
                    this.expression = expression;
                }

                public String getOperator() {
                    return operator;
                }

                public void setOperator(String operator) {
                    this.operator = operator;
                }

                public Object getValue() {
                    return value;
                }

                public void setValue(Object value) {
                    this.value = value;
                }

                public String getLogic() {
                    return logic;
                }

                public void setLogic(String logic) {
                    this.logic = logic;
                }
            }

            public static class LoopConfig {
                private Integer maxIterations;
                private String breakCondition;
                private String iterateOver;

                public Integer getMaxIterations() {
                    return maxIterations;
                }

                public void setMaxIterations(Integer maxIterations) {
                    this.maxIterations = maxIterations;
                }

                public String getBreakCondition() {
                    return breakCondition;
                }

                public void setBreakCondition(String breakCondition) {
                    this.breakCondition = breakCondition;
                }

                public String getIterateOver() {
                    return iterateOver;
                }

                public void setIterateOver(String iterateOver) {
                    this.iterateOver = iterateOver;
                }
            }

            public static class ParallelConfig {
                private Integer maxConcurrency;
                private Boolean waitForAll;
                private String aggregationStrategy;

                public Integer getMaxConcurrency() {
                    return maxConcurrency;
                }

                public void setMaxConcurrency(Integer maxConcurrency) {
                    this.maxConcurrency = maxConcurrency;
                }

                public Boolean getWaitForAll() {
                    return waitForAll;
                }

                public void setWaitForAll(Boolean waitForAll) {
                    this.waitForAll = waitForAll;
                }

                public String getAggregationStrategy() {
                    return aggregationStrategy;
                }

                public void setAggregationStrategy(String aggregationStrategy) {
                    this.aggregationStrategy = aggregationStrategy;
                }
            }

            public static class TransformConfig {
                private String type;
                private String script;
                private Map<String, Object> mapping;

                public String getType() {
                    return type;
                }

                public void setType(String type) {
                    this.type = type;
                }

                public String getScript() {
                    return script;
                }

                public void setScript(String script) {
                    this.script = script;
                }

                public Map<String, Object> getMapping() {
                    return mapping;
                }

                public void setMapping(Map<String, Object> mapping) {
                    this.mapping = mapping;
                }
            }

            public static class ValidationConfig {
                private Map<String, Object> schema;
                private String onFailure;
                private Object fallbackValue;

                public Map<String, Object> getSchema() {
                    return schema;
                }

                public void setSchema(Map<String, Object> schema) {
                    this.schema = schema;
                }

                public String getOnFailure() {
                    return onFailure;
                }

                public void setOnFailure(String onFailure) {
                    this.onFailure = onFailure;
                }

                public Object getFallbackValue() {
                    return fallbackValue;
                }

                public void setFallbackValue(Object fallbackValue) {
                    this.fallbackValue = fallbackValue;
                }
            }

            public static class HumanInputConfig {
                private Map<String, Object> formSchema;
                private Integer timeout;
                private List<String> notificationChannels;

                public Map<String, Object> getFormSchema() {
                    return formSchema;
                }

                public void setFormSchema(Map<String, Object> formSchema) {
                    this.formSchema = formSchema;
                }

                public Integer getTimeout() {
                    return timeout;
                }

                public void setTimeout(Integer timeout) {
                    this.timeout = timeout;
                }

                public List<String> getNotificationChannels() {
                    return notificationChannels;
                }

                public void setNotificationChannels(List<String> notificationChannels) {
                    this.notificationChannels = notificationChannels;
                }
            }

            // Getters and Setters
            public LLMConfig getLlmConfig() {
                return llmConfig;
            }

            public void setLlmConfig(LLMConfig llmConfig) {
                this.llmConfig = llmConfig;
            }

            public Tool getTool() {
                return tool;
            }

            public void setTool(Tool tool) {
                this.tool = tool;
            }

            public String getPrompt() {
                return prompt;
            }

            public void setPrompt(String prompt) {
                this.prompt = prompt;
            }

            public PromptTemplate getPromptTemplate() {
                return promptTemplate;
            }

            public void setPromptTemplate(PromptTemplate promptTemplate) {
                this.promptTemplate = promptTemplate;
            }

            public Condition getCondition() {
                return condition;
            }

            public void setCondition(Condition condition) {
                this.condition = condition;
            }

            public LoopConfig getLoopConfig() {
                return loopConfig;
            }

            public void setLoopConfig(LoopConfig loopConfig) {
                this.loopConfig = loopConfig;
            }

            public ParallelConfig getParallelConfig() {
                return parallelConfig;
            }

            public void setParallelConfig(ParallelConfig parallelConfig) {
                this.parallelConfig = parallelConfig;
            }

            public TransformConfig getTransformConfig() {
                return transformConfig;
            }

            public void setTransformConfig(TransformConfig transformConfig) {
                this.transformConfig = transformConfig;
            }

            public ValidationConfig getValidationConfig() {
                return validationConfig;
            }

            public void setValidationConfig(ValidationConfig validationConfig) {
                this.validationConfig = validationConfig;
            }

            public HumanInputConfig getHumanInputConfig() {
                return humanInputConfig;
            }

            public void setHumanInputConfig(HumanInputConfig humanInputConfig) {
                this.humanInputConfig = humanInputConfig;
            }
        }

        public static class NodeInput {
            private String name;
            private String type;
            private String source;
            private Boolean required;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getSource() {
                return source;
            }

            public void setSource(String source) {
                this.source = source;
            }

            public Boolean getRequired() {
                return required;
            }

            public void setRequired(Boolean required) {
                this.required = required;
            }
        }

        public static class NodeOutput {
            private String name;
            private String type;
            private String mapping;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getMapping() {
                return mapping;
            }

            public void setMapping(String mapping) {
                this.mapping = mapping;
            }
        }

        public static class ErrorHandling {
            private String strategy;
            private Integer maxRetries;
            private Integer retryDelay;
            private String fallbackNode;
            private String onError;

            public String getStrategy() {
                return strategy;
            }

            public void setStrategy(String strategy) {
                this.strategy = strategy;
            }

            public Integer getMaxRetries() {
                return maxRetries;
            }

            public void setMaxRetries(Integer maxRetries) {
                this.maxRetries = maxRetries;
            }

            public Integer getRetryDelay() {
                return retryDelay;
            }

            public void setRetryDelay(Integer retryDelay) {
                this.retryDelay = retryDelay;
            }

            public String getFallbackNode() {
                return fallbackNode;
            }

            public void setFallbackNode(String fallbackNode) {
                this.fallbackNode = fallbackNode;
            }

            public String getOnError() {
                return onError;
            }

            public void setOnError(String onError) {
                this.onError = onError;
            }
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public NodeType getType() {
            return type;
        }

        public void setType(NodeType type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Position getPosition() {
            return position;
        }

        public void setPosition(Position position) {
            this.position = position;
        }

        public NodeConfig getConfig() {
            return config;
        }

        public void setConfig(NodeConfig config) {
            this.config = config;
        }

        public List<NodeInput> getInputs() {
            return inputs;
        }

        public void setInputs(List<NodeInput> inputs) {
            this.inputs = inputs;
        }

        public List<NodeOutput> getOutputs() {
            return outputs;
        }

        public void setOutputs(List<NodeOutput> outputs) {
            this.outputs = outputs;
        }

        public ErrorHandling getErrorHandling() {
            return errorHandling;
        }

        public void setErrorHandling(ErrorHandling errorHandling) {
            this.errorHandling = errorHandling;
        }

        public AgentDefinition.Metadata getMetadata() {
            return metadata;
        }

        public void setMetadata(AgentDefinition.Metadata metadata) {
            this.metadata = metadata;
        }
    }

    public static class Edge {
        private String id;
        private String source;
        private String target;
        private String sourceHandle;
        private String targetHandle;
        private EdgeType type;
        private EdgeCondition condition;
        private String label;
        private Boolean animated;
        private AgentDefinition.Metadata metadata;

        public enum EdgeType {
            @JsonProperty("default")
            DEFAULT,
            @JsonProperty("conditional")
            CONDITIONAL,
            @JsonProperty("fallback")
            FALLBACK,
            @JsonProperty("loop")
            LOOP
        }

        public static class EdgeCondition {
            private String expression;
            private Integer priority;

            public String getExpression() {
                return expression;
            }

            public void setExpression(String expression) {
                this.expression = expression;
            }

            public Integer getPriority() {
                return priority;
            }

            public void setPriority(Integer priority) {
                this.priority = priority;
            }
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getSourceHandle() {
            return sourceHandle;
        }

        public void setSourceHandle(String sourceHandle) {
            this.sourceHandle = sourceHandle;
        }

        public String getTargetHandle() {
            return targetHandle;
        }

        public void setTargetHandle(String targetHandle) {
            this.targetHandle = targetHandle;
        }

        public EdgeType getType() {
            return type;
        }

        public void setType(EdgeType type) {
            this.type = type;
        }

        public EdgeCondition getCondition() {
            return condition;
        }

        public void setCondition(EdgeCondition condition) {
            this.condition = condition;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Boolean getAnimated() {
            return animated;
        }

        public void setAnimated(Boolean animated) {
            this.animated = animated;
        }

        public AgentDefinition.Metadata getMetadata() {
            return metadata;
        }

        public void setMetadata(AgentDefinition.Metadata metadata) {
            this.metadata = metadata;
        }
    }

    public static class Trigger {
        private TriggerType type;
        private Map<String, Object> config;

        public enum TriggerType {
            @JsonProperty("manual")
            MANUAL,
            @JsonProperty("schedule")
            SCHEDULE,
            @JsonProperty("webhook")
            WEBHOOK,
            @JsonProperty("event")
            EVENT,
            @JsonProperty("message")
            MESSAGE
        }

        // Getters and Setters
        public TriggerType getType() {
            return type;
        }

        public void setType(TriggerType type) {
            this.type = type;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }
    }
}