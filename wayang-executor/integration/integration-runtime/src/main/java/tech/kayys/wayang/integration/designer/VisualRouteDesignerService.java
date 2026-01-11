package tech.kayys.wayang.integration.designer;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class VisualRouteDesignerService {

    private static final Logger LOG = LoggerFactory.getLogger(VisualRouteDesignerService.class);

    @Inject
    CamelContext camelContext;

    private final Map<String, RouteDesign> designStore = new ConcurrentHashMap<>();

    /**
     * Create new route design
     */
    public Uni<RouteDesign> createRoute(
            String name,
            String description,
            String category,
            String tenantId) {

        return Uni.createFrom().item(() -> {
            String routeId = UUID.randomUUID().toString();

            RouteDesign design = new RouteDesign(
                routeId,
                name,
                description,
                category,
                tenantId,
                new ArrayList<>(),
                new ArrayList<>(),
                new DesignMetadata(
                    Instant.now(),
                    Instant.now(),
                    "v1.0.0",
                    "DRAFT",
                    Map.of()
                )
            );

            designStore.put(routeId, design);
            LOG.info("Created route design: {} ({})", name, routeId);

            return design;
        });
    }

    /**
     * Get route design
     */
    public Uni<RouteDesign> getRoute(String routeId) {
        return Uni.createFrom().item(() -> {
            RouteDesign design = designStore.get(routeId);
            if (design == null) {
                throw new RuntimeException("Route design not found: " + routeId);
            }
            return design;
        });
    }

    /**
     * Update route design
     */
    public Uni<RouteDesign> updateRoute(String routeId, UpdateRouteRequest request) {
        return getRoute(routeId).map(design -> {
            // Update design properties
            RouteDesign updated = new RouteDesign(
                design.routeId(),
                request.name() != null ? request.name() : design.name(),
                request.description() != null ? request.description() : design.description(),
                design.category(),
                design.tenantId(),
                design.nodes(),
                design.connections(),
                new DesignMetadata(
                    design.metadata().createdAt(),
                    Instant.now(),
                    design.metadata().version(),
                    design.metadata().status(),
                    design.metadata().tags()
                )
            );

            designStore.put(routeId, updated);
            return updated;
        });
    }

    /**
     * Add node to route design
     */
    public Uni<DesignNode> addNode(
            String routeId,
            String nodeType,
            String label,
            Position position,
            Map<String, Object> configuration) {

        return getRoute(routeId).map(design -> {
            String nodeId = UUID.randomUUID().toString();

            DesignNode node = new DesignNode(
                nodeId,
                nodeType,
                label,
                position,
                configuration,
                new ArrayList<>(),
                new ArrayList<>(),
                Instant.now()
            );

            design.nodes().add(node);
            designStore.put(routeId, design);

            LOG.info("Added node {} to route {}", nodeId, routeId);
            return node;
        });
    }

    /**
     * Add connection between nodes
     */
    public Uni<DesignConnection> addConnection(
            String routeId,
            String sourceNodeId,
            String targetNodeId,
            String connectionType,
            String condition) {

        return getRoute(routeId).map(design -> {
            String connectionId = UUID.randomUUID().toString();

            DesignConnection connection = new DesignConnection(
                connectionId,
                sourceNodeId,
                targetNodeId,
                connectionType,
                condition,
                Instant.now()
            );

            design.connections().add(connection);

            // Update node ports
            design.nodes().stream()
                .filter(n -> n.nodeId().equals(sourceNodeId))
                .findFirst()
                .ifPresent(node -> node.outputPorts().add(connectionId));

            design.nodes().stream()
                .filter(n -> n.nodeId().equals(targetNodeId))
                .findFirst()
                .ifPresent(node -> node.inputPorts().add(connectionId));

            designStore.put(routeId, design);

            LOG.info("Added connection {} to route {}", connectionId, routeId);
            return connection;
        });
    }

    /**
     * Generate Camel route from visual design
     */
    public Uni<GeneratedRoute> generateCamelRoute(String routeId) {
        return getRoute(routeId).map(design -> {
            StringBuilder dsl = new StringBuilder();
            StringBuilder javaCode = new StringBuilder();

            // Generate Camel DSL
            dsl.append("from(\"").append(getStartNodeUri(design)).append("\")\n");
            dsl.append("  .routeId(\"").append(design.routeId()).append("\")\n");

            // Process nodes in execution order
            List<DesignNode> sortedNodes = topologicalSort(design);

            for (DesignNode node : sortedNodes) {
                String nodeConfig = generateNodeConfiguration(node);
                dsl.append(nodeConfig);
            }

            // Generate Java code
            javaCode.append("public class ").append(toCamelCase(design.name()))
                    .append("Route extends RouteBuilder {\n");
            javaCode.append("  @Override\n");
            javaCode.append("  public void configure() throws Exception {\n");
            javaCode.append("    ").append(dsl.toString().replace("\n", "\n    "));
            javaCode.append("  }\n");
            javaCode.append("}\n");

            return new GeneratedRoute(
                design.routeId(),
                dsl.toString(),
                javaCode.toString(),
                design.name(),
                Instant.now()
            );
        });
    }

    /**
     * Deploy generated route to Camel context
     */
    public Uni<DeploymentResult> deployRoute(String routeId) {
        return generateCamelRoute(routeId).flatMap(generatedRoute -> {
            return Uni.createFrom().completionStage(() ->
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        // Parse and add route
                        RouteBuilder builder = createRouteBuilder(generatedRoute);
                        camelContext.addRoutes(builder);

                        // Start route
                        camelContext.getRouteController().startRoute(routeId);

                        LOG.info("Successfully deployed route: {}", routeId);

                        return new DeploymentResult(
                            routeId,
                            true,
                            "Route deployed successfully",
                            Instant.now()
                        );

                    } catch (Exception e) {
                        LOG.error("Failed to deploy route: {}", routeId, e);
                        return new DeploymentResult(
                            routeId,
                            false,
                            "Deployment failed: " + e.getMessage(),
                            Instant.now()
                        );
                    }
                })
            );
        });
    }

    private RouteBuilder createRouteBuilder(GeneratedRoute generatedRoute) {
        return new org.apache.camel.builder.RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Parse DSL and create route
                // This is simplified - in production, use proper DSL parser
                eval(generatedRoute.camelDSL());
            }

            private void eval(String dsl) {
                // DSL evaluation logic
                LOG.debug("Evaluating DSL: {}", dsl);
            }
        };
    }

    private String getStartNodeUri(RouteDesign design) {
        return design.nodes().stream()
            .filter(node -> node.nodeType().equals("START"))
            .findFirst()
            .map(node -> (String) node.configuration().get("uri"))
            .orElse("direct:start");
    }

    private List<DesignNode> topologicalSort(RouteDesign design) {
        // Simplified topological sort
        return design.nodes();
    }

    private String generateNodeConfiguration(DesignNode node) {
        return switch (node.nodeType()) {
            case "TRANSFORM" -> "  .transform().simple(\"${body}\")\n";
            case "FILTER" -> "  .filter(simple(\"${body} != null\"))\n";
            case "SPLIT" -> "  .split(body())\n";
            case "LOG" -> "  .log(\"Processing: ${body}\")\n";
            case "TO" -> "  .to(\"" + node.configuration().get("uri") + "\")\n";
            default -> "  .process(exchange -> { /* " + node.nodeType() + " */ })\n";
        };
    }

    private String toCamelCase(String name) {
        return name.replaceAll("\\s+", "");
    }
}