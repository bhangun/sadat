package tech.kayys.wayang.agent.model;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Database Query Tool
 * Executes safe database queries
 */
@ApplicationScoped
public class DatabaseQueryTool extends AbstractTool {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseQueryTool.class);

    public DatabaseQueryTool() {
        super("database_query", "Executes read-only database queries. " +
                "Can query tables and retrieve data.");
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of(
                                "type", "string",
                                "description", "SQL SELECT query (read-only)"),
                        "database", Map.of(
                                "type", "string",
                                "description", "Database name",
                                "default", "default")),
                "required", List.of("query"));
    }

    @Override
    public Uni<Boolean> validate(Map<String, Object> arguments) {
        return super.validate(arguments)
                .flatMap(valid -> {
                    if (!valid)
                        return Uni.createFrom().item(false);

                    String query = getParam(arguments, "query", String.class);

                    // Ensure it's a SELECT query
                    if (!query.trim().toUpperCase().startsWith("SELECT")) {
                        LOG.warn("Only SELECT queries are allowed");
                        return Uni.createFrom().item(false);
                    }

                    return Uni.createFrom().item(true);
                });
    }

    @Override
    public Uni<String> execute(Map<String, Object> arguments, AgentContext context) {
        String query = getParam(arguments, "query", String.class);
        String database = getParamOrDefault(arguments, "database", "default");

        LOG.debug("Executing query on {}: {}", database, query);

        // In production, execute actual database query
        // For now, return placeholder
        return Uni.createFrom().item(
                "Query executed successfully. Results: [placeholder]");
    }

    @Override
    public boolean requiresAuth() {
        return true; // Requires database credentials
    }
}
