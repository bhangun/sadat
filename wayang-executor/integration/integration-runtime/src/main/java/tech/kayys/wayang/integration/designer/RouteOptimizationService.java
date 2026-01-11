package tech.kayys.wayang.integration.designer;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class RouteOptimizationService {

    private static final Logger LOG = LoggerFactory.getLogger(RouteOptimizationService.class);

    @Inject
    VisualRouteDesignerService designerService;

    /**
     * Analyze route and provide AI-powered optimization suggestions
     */
    public Uni<OptimizationSuggestions> analyzeAndOptimize(String routeId) {
        LOG.debug("Analyze and optimize: " + routeId);
        return designerService.getRoute(routeId).map(design -> {
            List<OptimizationSuggestion> suggestions = new ArrayList<>();

            // Analyze performance bottlenecks
            suggestions.addAll(detectBottlenecks(design));

            // Suggest parallelization opportunities
            suggestions.addAll(suggestParallelization(design));

            // Recommend caching
            suggestions.addAll(suggestCaching(design));

            // Suggest error handling improvements
            suggestions.addAll(suggestErrorHandling(design));

            // Calculate potential performance improvement
            double estimatedImprovement = calculateEstimatedImprovement(suggestions);

            return new OptimizationSuggestions(
                    routeId,
                    suggestions,
                    estimatedImprovement,
                    Instant.now());
        });
    }

    private List<OptimizationSuggestion> detectBottlenecks(RouteDesign design) {
        List<OptimizationSuggestion> suggestions = new ArrayList<>();

        design.nodes().forEach(node -> {
            if (node.nodeType().equals("TO") &&
                    node.configuration().containsKey("uri")) {

                String uri = (String) node.configuration().get("uri");

                if (uri.startsWith("http") && !uri.contains("timeout")) {
                    suggestions.add(new OptimizationSuggestion(
                            "PERFORMANCE",
                            "ADD_TIMEOUT",
                            "Add timeout to HTTP endpoint to prevent blocking",
                            node.nodeId(),
                            Map.of("suggestedTimeout", "30000"),
                            8));
                }
            }
        });

        return suggestions;
    }

    private List<OptimizationSuggestion> suggestParallelization(RouteDesign design) {
        List<OptimizationSuggestion> suggestions = new ArrayList<>();

        // Detect sequential processing that could be parallelized
        long sequentialNodes = design.nodes().stream()
                .filter(node -> node.nodeType().equals("TO"))
                .count();

        if (sequentialNodes > 3) {
            suggestions.add(new OptimizationSuggestion(
                    "PERFORMANCE",
                    "PARALLELIZE",
                    "Consider using parallel processing for independent operations",
                    null,
                    Map.of("nodeCount", sequentialNodes),
                    9));
        }

        return suggestions;
    }

    private List<OptimizationSuggestion> suggestCaching(RouteDesign design) {
        List<OptimizationSuggestion> suggestions = new ArrayList<>();

        // Detect repeated external calls
        design.nodes().stream()
                .filter(node -> node.nodeType().equals("TO"))
                .forEach(node -> {
                    suggestions.add(new OptimizationSuggestion(
                            "PERFORMANCE",
                            "ADD_CACHING",
                            "Cache results from external endpoint",
                            node.nodeId(),
                            Map.of("cacheTTL", "300"),
                            7));
                });

        return suggestions;
    }

    private List<OptimizationSuggestion> suggestErrorHandling(RouteDesign design) {
        List<OptimizationSuggestion> suggestions = new ArrayList<>();

        boolean hasErrorHandler = design.nodes().stream()
                .anyMatch(node -> node.nodeType().equals("ERROR_HANDLER"));

        if (!hasErrorHandler) {
            suggestions.add(new OptimizationSuggestion(
                    "RELIABILITY",
                    "ADD_ERROR_HANDLER",
                    "Add error handler to improve reliability",
                    null,
                    Map.of("strategy", "deadLetterChannel"),
                    10));
        }

        return suggestions;
    }

    private double calculateEstimatedImprovement(List<OptimizationSuggestion> suggestions) {
        return suggestions.stream()
                .mapToInt(OptimizationSuggestion::priority)
                .average()
                .orElse(0) * 10; // Simplified calculation
    }
}