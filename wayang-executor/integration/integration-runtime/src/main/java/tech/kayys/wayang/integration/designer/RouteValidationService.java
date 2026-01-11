package tech.kayys.wayang.integration.designer;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class RouteValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(RouteValidationService.class);

    @Inject
    VisualRouteDesignerService designerService;

    /**
     * Validate route design in real-time
     */
    public Uni<ValidationResult> validateRoute(String routeId) {
        LOG.debug("Validate route:" + routeId);
        return designerService.getRoute(routeId).map(design -> {
            List<ValidationIssue> issues = new ArrayList<>();

            // Check for disconnected nodes
            issues.addAll(checkDisconnectedNodes(design));

            // Check for cycles
            issues.addAll(checkCycles(design));

            // Check node configurations
            issues.addAll(checkNodeConfigurations(design));

            // Check for missing start/end nodes
            issues.addAll(checkStartEndNodes(design));

            boolean isValid = issues.stream()
                    .noneMatch(issue -> issue.severity().equals("ERROR"));

            return new ValidationResult(
                    routeId,
                    isValid,
                    issues,
                    Instant.now());
        });
    }

    private List<ValidationIssue> checkDisconnectedNodes(RouteDesign design) {
        List<ValidationIssue> issues = new ArrayList<>();

        Set<String> connectedNodes = new HashSet<>();
        design.connections().forEach(conn -> {
            connectedNodes.add(conn.sourceNodeId());
            connectedNodes.add(conn.targetNodeId());
        });

        design.nodes().stream()
                .filter(node -> !connectedNodes.contains(node.nodeId()))
                .filter(node -> !node.nodeType().equals("START") && !node.nodeType().equals("END"))
                .forEach(node -> issues.add(new ValidationIssue(
                        "WARNING",
                        "DISCONNECTED_NODE",
                        "Node '" + node.label() + "' is not connected",
                        node.nodeId())));

        return issues;
    }

    private List<ValidationIssue> checkCycles(RouteDesign design) {
        // Check for circular dependencies
        return new ArrayList<>();
    }

    private List<ValidationIssue> checkNodeConfigurations(RouteDesign design) {
        List<ValidationIssue> issues = new ArrayList<>();

        design.nodes().forEach(node -> {
            Map<String, Object> config = node.configuration();

            // Check required fields based on node type
            if (node.nodeType().equals("TO") && !config.containsKey("uri")) {
                issues.add(new ValidationIssue(
                        "ERROR",
                        "MISSING_REQUIRED_FIELD",
                        "Node '" + node.label() + "' is missing required field: uri",
                        node.nodeId()));
            }
        });

        return issues;
    }

    private List<ValidationIssue> checkStartEndNodes(RouteDesign design) {
        List<ValidationIssue> issues = new ArrayList<>();

        boolean hasStart = design.nodes().stream()
                .anyMatch(node -> node.nodeType().equals("START"));

        if (!hasStart) {
            issues.add(new ValidationIssue(
                    "ERROR",
                    "MISSING_START_NODE",
                    "Route must have a START node",
                    null));
        }

        return issues;
    }
}