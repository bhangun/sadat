package tech.kayys.wayang.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tech.kayys.wayang.engine.EnginePlugin;
import tech.kayys.wayang.service.Metrics;

public class PromptTemplatePlugin implements EnginePlugin {
    private static final Logger log = LoggerFactory.getLogger(PromptTemplatePlugin.class);

    private PluginContext context;
    private final Map<String, Template> templates = new ConcurrentHashMap<>();
    private final Map<String, TemplateUsage> templateUsage = new ConcurrentHashMap<>();
    private final AtomicLong totalRenders = new AtomicLong(0);
    private final AtomicLong failedRenders = new AtomicLong(0);

    // Enhanced pattern to handle nested variables and default values
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)(?::([^}]+))?\\}\\}");
    private static final Pattern TEMPLATE_SYNTAX_PATTERN = Pattern.compile("@template:(\\w+)(?:\\|([^@]+))?");

    @Override
    public String getId() {
        return "prompt-templates";
    }

    @Override
    public String getName() {
        return "Prompt Template Plugin";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Reusable prompt templates with variables, validation, and metrics";
    }

    @Override
    public String[] getDependencies() {
        return new String[0];
    }

@Override
public void initialize(PluginContext context) throws PluginException {
    this.context = context;
    
    try {
        // Load templates from configuration if available
        loadTemplatesFromConfig();
        
        // Register built-in templates
        registerBuiltInTemplates();
        
        // Register event listeners - they should accept PluginEvent
        context.addEventListener("template.registered", this::onTemplateRegistered);
        context.addEventListener("template.rendered", this::onTemplateRendered);
        
        log.info("PromptTemplatePlugin initialized with {} templates", templates.size());
        
    } catch (Exception e) {
        throw new PluginException("Failed to initialize PromptTemplatePlugin", e);
    }
}

    @Override
    public void start() {
        log.info("PromptTemplatePlugin started");
    }

    @Override
    public void stop() {
        // Export usage statistics before clearing
        exportUsageStatistics();

        templates.clear();
        templateUsage.clear();
        log.info("PromptTemplatePlugin stopped");
    }

    private void loadTemplatesFromConfig() {
        try {
            Map<String, Object> config = context.getConfig();
            if (config.containsKey("templates")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> templateConfigs = (List<Map<String, Object>>) config.get("templates");

                for (Map<String, Object> templateConfig : templateConfigs) {
                    try {
                        String name = (String) templateConfig.get("name");
                        String content = (String) templateConfig.get("content");
                        @SuppressWarnings("unchecked")
                        List<String> requiredVars = (List<String>) templateConfig.get("requiredVars");
                        String description = (String) templateConfig.getOrDefault("description", "");

                        // Use safe factory method for config-loaded templates
                        Template template = createTemplateSafe(name, content,
                                requiredVars != null ? requiredVars : List.of(), description);
                        registerTemplate(name, template);

                    } catch (Exception e) {
                        log.warn("Failed to load template from config: {}", templateConfig, e);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load templates from configuration", e);
        }
    }

    private void registerBuiltInTemplates() {
        try {
            registerTemplate("summarize", createTemplate(
                    "summarize",
                    "Summarize the following text in {{max_words:200}} words. Focus on key points and main ideas:\n\n{{text}}",
                    List.of("text"),
                    "Summarize text with configurable length"));

            registerTemplate("translate", createTemplate(
                    "translate",
                    "Translate the following text from {{source_lang:English}} to {{target_lang}}:\n\n{{text}}\n\nProvide only the translation without additional commentary.",
                    List.of("text", "target_lang"),
                    "Translate text between languages"));

            registerTemplate("code_review", createTemplate(
                    "code_review",
                    "Review this {{language}} code and provide constructive feedback:\n\n```{{language}}\n{{code}}\n```\n\nFocus on: {{focus_areas:code quality, best practices, potential bugs}}\nProvide specific suggestions for improvement.",
                    List.of("language", "code"),
                    "Code review with focus areas"));

            registerTemplate("qa", createTemplate(
                    "qa",
                    "Based on the following context, answer the question:\n\nContext: {{context}}\n\nQuestion: {{question}}\n\nAnswer:",
                    List.of("context", "question"),
                    "Question answering with context"));

            registerTemplate("classification", createTemplate(
                    "classification",
                    "Classify the following text into one of these categories: {{categories}}\n\nText: {{text}}\n\nClassification:",
                    List.of("categories", "text"),
                    "Text classification with custom categories"));
        } catch (PluginException e) {
            log.error("Failed to register built-in templates", e);
            throw new RuntimeException("Failed to initialize built-in templates", e);
        }
    }

    // Factory method to create templates with validation
    private Template createTemplate(String name, String content, List<String> requiredVars, String description)
            throws PluginException {
        validateTemplateContent(content, requiredVars);
        return new Template(name, content, requiredVars, description);
    }

    private Template createTemplateSafe(String name, String content, List<String> requiredVars, String description) {
        try {
            validateTemplateContent(content, requiredVars);
            return new Template(name, content, requiredVars, description);
        } catch (PluginException e) {
            log.warn("Invalid template configuration for {}: {}", name, e.getMessage());
            // Return a safe template that will fail gracefully during rendering
            return new Template(name, content, requiredVars, description + " [INVALID: " + e.getMessage() + "]");
        }
    }

    public void registerTemplate(String name, Template template) throws PluginException {
    if (name == null || name.trim().isEmpty()) {
        throw new PluginException("Template name cannot be null or empty");
    }

    if (template == null) {
        throw new PluginException("Template cannot be null");
    }

    // Validate template content
    try {
        validateTemplate(template);
    } catch (PluginException e) {
        throw e;
    } catch (Exception e) {
        throw new PluginException("Invalid template: " + name, e);
    }

    templates.put(name, template);
    templateUsage.put(name, new TemplateUsage(name));

    // Emit event - CORRECT WAY
    context.emitEvent("template.registered", Map.of(
        "templateName", name,
        "requiredVars", template.requiredVars(),
        "timestamp", System.currentTimeMillis()
    ));

    log.debug("Template registered: {}", name);
}
    private void validateTemplate(Template template) throws PluginException {
        validateTemplateContent(template.content(), template.requiredVars());
    }

    private void validateTemplateContent(String content, List<String> requiredVars) throws PluginException {
        if (content == null || content.trim().isEmpty()) {
            throw new PluginException("Template content cannot be null or empty");
        }

        // Extract all variables from content
        Set<String> contentVars = extractVariables(content);

        // Check if required variables exist in content
        for (String requiredVar : requiredVars) {
            if (!contentVars.contains(requiredVar)) {
                throw new PluginException(
                        "Required variable '" + requiredVar + "' not found in template content");
            }
        }

        // Validate no unknown required variables
        for (String requiredVar : requiredVars) {
            if (!contentVars.contains(requiredVar)) {
                throw new PluginException(
                        "Required variable '" + requiredVar + "' is not present in template content");
            }
        }
    }

    private Set<String> extractVariables(String content) {
        Set<String> variables = new HashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            variables.add(matcher.group(1)); // Variable name without default value
        }
        return variables;
    }

    public String renderTemplate(String templateName, Map<String, String> variables)
        throws PluginException {
    long startTime = System.currentTimeMillis();

    try {
        Template template = templates.get(templateName);
        if (template == null) {
            failedRenders.incrementAndGet();
            throw new TemplateNotFoundException("Template not found: " + templateName);
        }

        // Track usage
        TemplateUsage usage = templateUsage.get(templateName);
        if (usage != null) {
            usage.recordUsage();
        }

        // Validate required variables
        validateVariables(template, variables);

        // Render template with default values support
        String result = renderTemplateContent(template.content(), variables);

        totalRenders.incrementAndGet();

        // Emit event - CORRECT WAY
        context.emitEvent("template.rendered", Map.of(
            "templateName", templateName,
            "variables", variables.keySet(),
            "renderTime", System.currentTimeMillis() - startTime,
            "timestamp", System.currentTimeMillis()
        ));

        log.debug("Template rendered successfully: {}", templateName);
        return result;

    } catch (TemplateNotFoundException e) {
        failedRenders.incrementAndGet();
        throw e;
    } catch (Exception e) {
        failedRenders.incrementAndGet();
        throw new PluginException("Failed to render template: " + templateName, e);
    }
}

    private void validateVariables(Template template, Map<String, String> variables)
            throws PluginException {
        List<String> missingVars = new ArrayList<>();

        for (String requiredVar : template.requiredVars()) {
            if (!variables.containsKey(requiredVar) ||
                    variables.get(requiredVar) == null ||
                    variables.get(requiredVar).trim().isEmpty()) {
                missingVars.add(requiredVar);
            }
        }

        if (!missingVars.isEmpty()) {
            throw new PluginException("Missing required variables: " + missingVars);
        }
    }

    private String renderTemplateContent(String content, Map<String, String> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String defaultValue = matcher.group(2); // Could be null

            String value = variables.get(varName);
            if (value == null || value.trim().isEmpty()) {
                value = defaultValue != null ? defaultValue : "";
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    @Override
    public String preprocessPrompt(String prompt) {
        if (prompt == null) {
            return null;
        }

        // Support multiple template syntaxes
        Matcher matcher = TEMPLATE_SYNTAX_PATTERN.matcher(prompt);

        if (matcher.find()) {
            try {
                String templateName = matcher.group(1);
                String variablesPart = matcher.group(2);

                Map<String, String> variables = parseVariables(variablesPart);
                return renderTemplate(templateName, variables);

            } catch (Exception e) {
                log.warn("Failed to process template prompt: {}", prompt, e);
                // Fall back to original prompt on error
                return prompt;
            }
        }

        // Also support JSON template syntax:
        // @template:{"name":"summarize","vars":{"text":"..."}}
        if (prompt.startsWith("@template:{")) {
            try {
                return processJsonTemplateSyntax(prompt.substring(10));
            } catch (Exception e) {
                log.warn("Failed to process JSON template prompt: {}", prompt, e);
                return prompt;
            }
        }

        return prompt;
    }

    private Map<String, String> parseVariables(String variablesPart) {
        Map<String, String> variables = new HashMap<>();

        if (variablesPart != null && !variablesPart.trim().isEmpty()) {
            // Support both key=value and JSON formats
            if (variablesPart.trim().startsWith("{")) {
                // JSON format
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, String> jsonVars = mapper.readValue(variablesPart, Map.class);
                    variables.putAll(jsonVars);
                } catch (Exception e) {
                    log.warn("Failed to parse JSON variables: {}", variablesPart, e);
                }
            } else {
                // Key=value format
                String[] pairs = variablesPart.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        variables.put(kv[0].trim(), kv[1].trim());
                    }
                }
            }
        }

        return variables;
    }

    private String processJsonTemplateSyntax(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        String templateName = root.get("name").asText();
        JsonNode varsNode = root.get("vars");

        Map<String, String> variables = new HashMap<>();
        if (varsNode != null && varsNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = varsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                variables.put(field.getKey(), field.getValue().asText());
            }
        }

        return renderTemplate(templateName, variables);
    }

    // Plugin management methods
    public List<Template> listTemplates() {
        return new ArrayList<>(templates.values());
    }

    public Template getTemplate(String name) {
        return templates.get(name);
    }

    public boolean hasTemplate(String name) {
        return templates.containsKey(name);
    }

public void unregisterTemplate(String name) {
    Template template = templates.remove(name);
    templateUsage.remove(name);

    if (template != null) {
        // Emit event - CORRECT WAY
        context.emitEvent("template.unregistered", Map.of(
            "templateName", name,
            "timestamp", System.currentTimeMillis()
        ));
        log.debug("Template unregistered: {}", name);
    }
}

    // Statistics and monitoring
    public TemplateStatistics getStatistics() {
        return new TemplateStatistics(
                templates.size(),
                totalRenders.get(),
                failedRenders.get(),
                templateUsage.values().stream()
                        .collect(Collectors.toMap(
                                TemplateUsage::getTemplateName,
                                TemplateUsage::getUsageCount)));
    }

    private void exportUsageStatistics() {
        TemplateStatistics stats = getStatistics();
        log.info("Template usage statistics: {}", stats);

        // Could export to metrics system here
        if (context.getSharedData("metrics") instanceof Metrics metrics) {
            metrics.setGauge("templates.total", stats.totalTemplates());
            metrics.setGauge("templates.renders.total", stats.totalRenders());
            metrics.setGauge("templates.renders.failed", stats.failedRenders());
        }
    }

    // Event handlers
    private void onTemplateRegistered(PluginEvent event) {
        log.debug("Template registered event: {}", event.data());
    }

    private void onTemplateRendered(PluginEvent event) {
        log.debug("Template rendered event: {}", event.data());
    }

    // Supporting classes - Template as a simple record without validation
    public record Template(
            String name,
            String content,
            List<String> requiredVars,
            String description) {
        // No validation in constructor - validation is done separately
    }

    private static class TemplateUsage {
        private final String templateName;
        private final AtomicLong usageCount = new AtomicLong(0);
        private final AtomicLong lastUsed = new AtomicLong(0);

        public TemplateUsage(String templateName) {
            this.templateName = templateName;
        }

        public void recordUsage() {
            usageCount.incrementAndGet();
            lastUsed.set(System.currentTimeMillis());
        }

        public String getTemplateName() {
            return templateName;
        }

        public long getUsageCount() {
            return usageCount.get();
        }

        public long getLastUsed() {
            return lastUsed.get();
        }
    }

    public record TemplateStatistics(
            int totalTemplates,
            long totalRenders,
            long failedRenders,
            Map<String, Long> templateUsage) {
    }

    // Custom exceptions
    public static class TemplateNotFoundException extends PluginException {
        public TemplateNotFoundException(String message) {
            super(message);
        }
    }
}