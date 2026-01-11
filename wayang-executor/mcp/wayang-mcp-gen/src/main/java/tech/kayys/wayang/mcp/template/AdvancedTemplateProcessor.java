package tech.kayys.wayang.mcp.template;

import tech.kayys.wayang.mcp.plugin.*;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class AdvancedTemplateProcessor implements TemplateProcessor {

    @Inject
    Engine quteEngine;

    @Inject
    CustomTemplateManager templateManager;

    private final Map<String, TemplateFunction> customFunctions = new ConcurrentHashMap<>();
    private final Map<String, Template> compiledTemplates = new ConcurrentHashMap<>();

    // Advanced template patterns
    private static final Pattern INCLUDE_PATTERN = Pattern.compile("\\{\\{include\\s+([^}]+)\\}\\}");
    private static final Pattern CONDITIONAL_PATTERN = Pattern
            .compile("\\{\\{#if\\s+([^}]+)\\}\\}([\\s\\S]*?)\\{\\{/if\\}\\}");
    private static final Pattern LOOP_PATTERN = Pattern
            .compile("\\{\\{#each\\s+([^}]+)\\}\\}([\\s\\S]*?)\\{\\{/each\\}\\}");

    @Override
    public String getTemplateType() {
        return "advanced";
    }

    @Override
    public void initialize() throws PluginException {
        // Add advanced template functions
        addCustomFunction("camelCase", args -> toCamelCase(args[0].toString()));
        addCustomFunction("pascalCase", args -> toPascalCase(args[0].toString()));
        addCustomFunction("kebabCase", args -> toKebabCase(args[0].toString()));
        addCustomFunction("snakeCase", args -> toSnakeCase(args[0].toString()));
        addCustomFunction("upperCase", args -> args[0].toString().toUpperCase());
        addCustomFunction("lowerCase", args -> args[0].toString().toLowerCase());
        addCustomFunction("capitalize", args -> capitalize(args[0].toString()));
        addCustomFunction("pluralize", args -> pluralize(args[0].toString()));
        addCustomFunction("singularize", args -> singularize(args[0].toString()));

        // Utility functions
        addCustomFunction("isEmpty", args -> args[0] == null || args[0].toString().trim().isEmpty());
        addCustomFunction("isNotEmpty", args -> args[0] != null && !args[0].toString().trim().isEmpty());
        addCustomFunction("contains", args -> args[0].toString().contains(args[1].toString()));
        addCustomFunction("startsWith", args -> args[0].toString().startsWith(args[1].toString()));
        addCustomFunction("endsWith", args -> args[0].toString().endsWith(args[1].toString()));
        addCustomFunction("replace", args -> args[0].toString().replace(args[1].toString(), args[2].toString()));
        addCustomFunction("substring", args -> {
            String str = args[0].toString();
            int start = Integer.parseInt(args[1].toString());
            int end = args.length > 2 ? Integer.parseInt(args[2].toString()) : str.length();
            return str.substring(start, Math.min(end, str.length()));
        });

        // Date/time functions
        addCustomFunction("now", args -> System.currentTimeMillis());
        addCustomFunction("formatDate", args -> {
            // Simplified date formatting
            return new java.util.Date(Long.parseLong(args[0].toString())).toString();
        });

        // Math functions
        addCustomFunction("add", args -> Integer.parseInt(args[0].toString()) + Integer.parseInt(args[1].toString()));
        addCustomFunction("subtract",
                args -> Integer.parseInt(args[0].toString()) - Integer.parseInt(args[1].toString()));
        addCustomFunction("multiply",
                args -> Integer.parseInt(args[0].toString()) * Integer.parseInt(args[1].toString()));
        addCustomFunction("divide",
                args -> Integer.parseInt(args[0].toString()) / Integer.parseInt(args[1].toString()));
        addCustomFunction("mod", args -> Integer.parseInt(args[0].toString()) % Integer.parseInt(args[1].toString()));

        // Collection functions
        addCustomFunction("size", args -> {
            if (args[0] instanceof java.util.Collection) {
                return ((java.util.Collection<?>) args[0]).size();
            }
            return args[0].toString().length();
        });
        addCustomFunction("join", args -> {
            if (args[0] instanceof java.util.Collection) {
                return String.join(args[1].toString(), ((java.util.Collection<?>) args[0]).stream()
                        .map(Object::toString).toArray(String[]::new));
            }
            return args[0].toString();
        });
    }

    @Override
    public boolean supports(String templateType) {
        return "advanced".equalsIgnoreCase(templateType) ||
                "custom".equalsIgnoreCase(templateType) ||
                "enhanced".equalsIgnoreCase(templateType);
    }

    @Override
    public String processTemplate(String templateContent, Map<String, Object> data,
            PluginExecutionContext context) throws PluginException {
        try {
            // Pre-process template for advanced features
            String processedContent = preprocessTemplate(templateContent, data, context);

            // Use compiled template if available
            Template template = getCompiledTemplate(processedContent);

            // Enrich data with custom functions and context
            Map<String, Object> enrichedData = new HashMap<>(data);
            enrichedData.putAll(customFunctions);
            enrichedData.put("context", context);

            return template.data(enrichedData).render();

        } catch (Exception e) {
            throw new PluginException("advanced-template-processor", "process",
                    "Failed to process advanced template", e);
        }
    }

    @Override
    public void addCustomFunction(String name, TemplateFunction function) {
        customFunctions.put(name, function);
    }

    private String preprocessTemplate(String content, Map<String, Object> data, PluginExecutionContext context) {
        // Process includes
        content = processIncludes(content, data, context);

        // Process advanced conditionals
        content = processConditionals(content, data);

        // Process loops
        content = processLoops(content, data);

        return content;
    }

    private String processIncludes(String content, Map<String, Object> data, PluginExecutionContext context) {
        Matcher matcher = INCLUDE_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String templateId = matcher.group(1).trim().replace("\"", "");

            try {
                var includeTemplate = templateManager.getTemplate(templateId);
                if (includeTemplate.isPresent()) {
                    String includedContent = processTemplate(includeTemplate.get().getContent(), data, context);
                    matcher.appendReplacement(result, Matcher.quoteReplacement(includedContent));
                } else {
                    matcher.appendReplacement(result, "<!-- Template not found: " + templateId + " -->");
                }
            } catch (Exception e) {
                matcher.appendReplacement(result, "<!-- Include error: " + e.getMessage() + " -->");
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String processConditionals(String content, Map<String, Object> data) {
        // Simplified conditional processing
        Matcher matcher = CONDITIONAL_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String condition = matcher.group(1).trim();
            String ifContent = matcher.group(2);

            boolean conditionMet = evaluateCondition(condition, data);
            String replacement = conditionMet ? ifContent : "";

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String processLoops(String content, Map<String, Object> data) {
        // Simplified loop processing
        Matcher matcher = LOOP_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String iterableExpression = matcher.group(1).trim();
            String loopContent = matcher.group(2);

            String replacement = processLoop(iterableExpression, loopContent, data);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private boolean evaluateCondition(String condition, Map<String, Object> data) {
        // Simplified condition evaluation
        if (condition.contains("!=")) {
            String[] parts = condition.split("!=", 2);
            String left = resolveValue(parts[0].trim(), data);
            String right = resolveValue(parts[1].trim(), data);
            return !left.equals(right);
        } else if (condition.contains("==")) {
            String[] parts = condition.split("==", 2);
            String left = resolveValue(parts[0].trim(), data);
            String right = resolveValue(parts[1].trim(), data);
            return left.equals(right);
        } else {
            // Simple truthy evaluation
            String value = resolveValue(condition, data);
            return value != null && !value.isEmpty() && !"false".equals(value) && !"0".equals(value);
        }
    }

    private String processLoop(String iterableExpression, String loopContent, Map<String, Object> data) {
        StringBuilder result = new StringBuilder();

        // Extract variable name and iterable
        String[] parts = iterableExpression.split(" in ", 2);
        if (parts.length != 2) {
            return "<!-- Invalid loop syntax: " + iterableExpression + " -->";
        }

        String itemVar = parts[0].trim();
        String iterableName = parts[1].trim();

        Object iterable = data.get(iterableName);
        if (iterable instanceof java.util.Collection) {
            java.util.Collection<?> collection = (java.util.Collection<?>) iterable;
            for (Object item : collection) {
                Map<String, Object> loopData = new HashMap<>(data);
                loopData.put(itemVar, item);

                String processedLoopContent = replaceVariables(loopContent, loopData);
                result.append(processedLoopContent);
            }
        }

        return result.toString();
    }

    private String resolveValue(String expression, Map<String, Object> data) {
        expression = expression.trim();

        // Remove quotes if present
        if (expression.startsWith("\"") && expression.endsWith("\"")) {
            return expression.substring(1, expression.length() - 1);
        }

        // Resolve from data
        Object value = data.get(expression);
        return value != null ? value.toString() : "";
    }

    private String replaceVariables(String content, Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            content = content.replace(placeholder, value);
        }
        return content;
    }

    private Template getCompiledTemplate(String content) {
        String hash = String.valueOf(content.hashCode());
        return compiledTemplates.computeIfAbsent(hash, k -> quteEngine.parse(content));
    }

    // Utility methods for string transformations
    private String toCamelCase(String input) {
        if (input == null || input.isEmpty())
            return input;

        String[] parts = input.split("[_\\-\\s]+");
        StringBuilder result = new StringBuilder(parts[0].toLowerCase());

        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(parts[i].substring(0, 1).toUpperCase())
                        .append(parts[i].substring(1).toLowerCase());
            }
        }

        return result.toString();
    }

    private String toPascalCase(String input) {
        String camelCase = toCamelCase(input);
        if (camelCase.isEmpty())
            return camelCase;
        return camelCase.substring(0, 1).toUpperCase() + camelCase.substring(1);
    }

    private String toKebabCase(String input) {
        if (input == null || input.isEmpty())
            return input;
        return input.replaceAll("([a-z])([A-Z])", "$1-$2")
                .replaceAll("[_\\s]+", "-")
                .toLowerCase();
    }

    private String toSnakeCase(String input) {
        if (input == null || input.isEmpty())
            return input;
        return input.replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("[\\-\\s]+", "_")
                .toLowerCase();
    }

    private String capitalize(String input) {
        if (input == null || input.isEmpty())
            return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    private String pluralize(String input) {
        if (input == null || input.isEmpty())
            return input;
        // Simplified pluralization
        if (input.endsWith("y")) {
            return input.substring(0, input.length() - 1) + "ies";
        } else if (input.endsWith("s") || input.endsWith("x") || input.endsWith("z") ||
                input.endsWith("ch") || input.endsWith("sh")) {
            return input + "es";
        } else {
            return input + "s";
        }
    }

    private String singularize(String input) {
        if (input == null || input.isEmpty())
            return input;
        // Simplified singularization
        if (input.endsWith("ies")) {
            return input.substring(0, input.length() - 3) + "y";
        } else if (input.endsWith("es")) {
            return input.substring(0, input.length() - 2);
        } else if (input.endsWith("s") && !input.endsWith("ss")) {
            return input.substring(0, input.length() - 1);
        }
        return input;
    }
}
