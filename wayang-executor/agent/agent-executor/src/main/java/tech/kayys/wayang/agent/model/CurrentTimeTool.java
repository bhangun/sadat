package tech.kayys.wayang.agent.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Current Time Tool
 * Returns current date and time
 */
@ApplicationScoped
public class CurrentTimeTool extends AbstractTool {

    public CurrentTimeTool() {
        super("current_time", "Returns the current date and time. " +
                "Useful when you need to know what time it is now.");
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "timezone", Map.of(
                                "type", "string",
                                "description", "Timezone (e.g., 'UTC', 'America/New_York')",
                                "default", "UTC"),
                        "format", Map.of(
                                "type", "string",
                                "description", "Output format: 'iso' or 'human'",
                                "default", "iso")),
                "required", List.of());
    }

    @Override
    public Uni<String> execute(Map<String, Object> arguments, AgentContext context) {
        String timezone = getParamOrDefault(arguments, "timezone", "UTC");
        String format = getParamOrDefault(arguments, "format", "iso");

        Instant now = Instant.now();

        if ("human".equals(format)) {
            return Uni.createFrom().item(
                    "Current time: " + now.toString() + " (" + timezone + ")");
        } else {
            return Uni.createFrom().item(now.toString());
        }
    }
}
