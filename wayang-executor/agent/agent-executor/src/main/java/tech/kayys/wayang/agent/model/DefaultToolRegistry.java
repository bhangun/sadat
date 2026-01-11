package tech.kayys.wayang.agent.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultToolRegistry implements ToolRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultToolRegistry.class);

    // Map: tenantId -> toolName -> Tool
    private final Map<String, Map<String, Tool>> tools = new ConcurrentHashMap<>();

    @Inject
    CalculatorTool calculatorTool;

    @Inject
    WebSearchTool webSearchTool;

    @Inject
    CurrentTimeTool currentTimeTool;

    @jakarta.annotation.PostConstruct
    void init() {
        LOG.info("Initializing tool registry");

        // Register built-in tools for all tenants
        // In production, this would be more dynamic
        registerGlobalTool(calculatorTool);
        registerGlobalTool(webSearchTool);
        registerGlobalTool(currentTimeTool);

        LOG.info("Registered {} global tools", 3);
    }

    private void registerGlobalTool(Tool tool) {
        // Register for a special "global" tenant
        registerTool(tool, "_global");
    }

    @Override
    public void registerTool(Tool tool, String tenantId) {
        tools.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>())
                .put(tool.name(), tool);

        LOG.info("Registered tool: {} for tenant: {}", tool.name(), tenantId);
    }

    @Override
    public Uni<Tool> getTool(String name, String tenantId) {
        // Check tenant-specific tools first
        Tool tool = tools.getOrDefault(tenantId, Map.of()).get(name);

        // Fall back to global tools
        if (tool == null) {
            tool = tools.getOrDefault("_global", Map.of()).get(name);
        }

        if (tool == null) {
            LOG.warn("Tool not found: {} for tenant: {}", name, tenantId);
        }

        return Uni.createFrom().item(tool);
    }

    @Override
    public Uni<List<Tool>> getTools(List<String> names, String tenantId) {
        List<Uni<Tool>> toolUnis = names.stream()
                .map(name -> getTool(name, tenantId))
                .collect(Collectors.toList());

        return Uni.combine().all().unis(toolUnis).with(
                results -> results.stream()
                        .map(result -> (Tool) result)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }

    @Override
    public Uni<List<Tool>> getAllTools(String tenantId) {
        List<Tool> result = new ArrayList<>();

        // Add global tools
        result.addAll(tools.getOrDefault("_global", Map.of()).values());

        // Add tenant-specific tools
        result.addAll(tools.getOrDefault(tenantId, Map.of()).values());

        return Uni.createFrom().item(result);
    }

    @Override
    public void unregisterTool(String name, String tenantId) {
        Map<String, Tool> tenantTools = tools.get(tenantId);
        if (tenantTools != null) {
            tenantTools.remove(name);
            LOG.info("Unregistered tool: {} for tenant: {}", name, tenantId);
        }
    }

    @Override
    public boolean hasTool(String name, String tenantId) {
        return tools.getOrDefault(tenantId, Map.of()).containsKey(name) ||
                tools.getOrDefault("_global", Map.of()).containsKey(name);
    }
}
