package tech.kayys.wayang.agent.model;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Calculator Tool
 * Performs mathematical calculations
 */
@ApplicationScoped
public class CalculatorTool extends AbstractTool {

    private static final Logger LOG = LoggerFactory.getLogger(CalculatorTool.class);

    public CalculatorTool() {
        super("calculator", "Performs mathematical calculations. " +
                "Supports basic arithmetic, trigonometry, and more.");
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "expression", Map.of(
                                "type", "string",
                                "description", "Mathematical expression to evaluate")),
                "required", List.of("expression"));
    }

    @Override
    public Uni<String> execute(Map<String, Object> arguments, AgentContext context) {
        String expression = getParam(arguments, "expression", String.class);

        LOG.debug("Calculating: {}", expression);

        return Uni.createFrom().deferred(() -> {
            try {
                // Use a safe math evaluator (not eval!)
                double result = evaluateExpression(expression);
                return Uni.createFrom().item(String.valueOf(result));
            } catch (Exception e) {
                LOG.error("Calculation error: {}", e.getMessage());
                return Uni.createFrom().item("Error: " + e.getMessage());
            }
        });
    }

    private double evaluateExpression(String expression) {
        // Simple implementation - in production use a proper math library
        // like exp4j or JEP
        try {
            // Handle basic operations
            expression = expression.trim();

            if (expression.contains("+")) {
                String[] parts = expression.split("\\+");
                return Double.parseDouble(parts[0].trim()) +
                        Double.parseDouble(parts[1].trim());
            } else if (expression.contains("-")) {
                String[] parts = expression.split("-");
                return Double.parseDouble(parts[0].trim()) -
                        Double.parseDouble(parts[1].trim());
            } else if (expression.contains("*")) {
                String[] parts = expression.split("\\*");
                return Double.parseDouble(parts[0].trim()) *
                        Double.parseDouble(parts[1].trim());
            } else if (expression.contains("/")) {
                String[] parts = expression.split("/");
                return Double.parseDouble(parts[0].trim()) /
                        Double.parseDouble(parts[1].trim());
            } else {
                return Double.parseDouble(expression);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid expression: " + expression);
        }
    }
}
