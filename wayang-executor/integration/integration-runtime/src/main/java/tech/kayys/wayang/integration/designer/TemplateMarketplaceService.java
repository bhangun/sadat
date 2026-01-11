package tech.kayys.wayang.integration.designer;

import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class TemplateMarketplaceService {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateMarketplaceService.class);

    @Inject
    VisualRouteDesignerService designerService;

    private final List<RouteTemplate> templates = initializeTemplates();

    /**
     * Search templates
     */
    @CacheResult(cacheName = "template-search")
    public Uni<List<RouteTemplate>> searchTemplates(String category, String searchTerm) {
        return Uni.createFrom().item(() ->
            templates.stream()
                .filter(t -> category == null || t.category().equals(category))
                .filter(t -> searchTerm == null ||
                           t.name().toLowerCase().contains(searchTerm.toLowerCase()) ||
                           t.description().toLowerCase().contains(searchTerm.toLowerCase()))
                .collect(Collectors.toList())
        );
    }

    /**
     * Clone template to tenant
     */
    public Uni<RouteDesign> cloneTemplate(String templateId, String tenantId) {
        RouteTemplate template = templates.stream()
            .filter(t -> t.templateId().equals(templateId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Template not found"));

        return designerService.createRoute(
            template.name() + " (Copy)",
            template.description(),
            template.category(),
            tenantId
        ).map(design -> {
            // Copy nodes and connections from template
            template.design().nodes().forEach(node -> {
                try {
                    designerService.addNode(
                        design.routeId(),
                        node.nodeType(),
                        node.label(),
                        node.position(),
                        node.configuration()
                    ).await().indefinitely();
                } catch (Exception e) {
                    LOG.error("Error copying node", e);
                }
            });

            return design;
        });
    }

    private List<RouteTemplate> initializeTemplates() {
        List<RouteTemplate> templates = new ArrayList<>();

        // REST API to Database template
        templates.add(createRESTToDatabaseTemplate());

        // File Processing template
        templates.add(createFileProcessingTemplate());

        // Event-Driven template
        templates.add(createEventDrivenTemplate());

        return templates;
    }

    private RouteTemplate createRESTToDatabaseTemplate() {
        RouteDesign design = new RouteDesign(
            "template-rest-db",
            "REST API to Database",
            "Receive REST request and save to database",
            "Integration",
            "system",
            List.of(
                new DesignNode("start", "START", "HTTP Endpoint",
                    new Position(100, 100), Map.of("uri", "rest:post:orders"),
                    new ArrayList<>(), new ArrayList<>(), Instant.now()),
                new DesignNode("transform", "TRANSFORM", "Transform",
                    new Position(300, 100), Map.of(),
                    new ArrayList<>(), new ArrayList<>(), Instant.now()),
                new DesignNode("save", "TO", "Save to DB",
                    new Position(500, 100), Map.of("uri", "jdbc:dataSource"),
                    new ArrayList<>(), new ArrayList<>(), Instant.now())
            ),
            new ArrayList<>(),
            new DesignMetadata(Instant.now(), Instant.now(), "v1.0.0", "PUBLISHED", Map.of())
        );

        return new RouteTemplate(
            "template-rest-db",
            "REST API to Database",
            "Receive REST request and save to database",
            "Integration",
            design,
            List.of("rest", "database", "integration"),
            100,
            4.5,
            Instant.now()
        );
    }

    private RouteTemplate createFileProcessingTemplate() {
        // Simplified implementation
        return null;
    }

    private RouteTemplate createEventDrivenTemplate() {
        // Simplified implementation
        return null;
    }
}