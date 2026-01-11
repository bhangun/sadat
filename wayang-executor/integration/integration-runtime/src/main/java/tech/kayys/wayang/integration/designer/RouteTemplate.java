package tech.kayys.wayang.integration.designer;

import java.time.Instant;
import java.util.List;

record RouteTemplate(
    String templateId,
    String name,
    String description,
    String category,
    RouteDesign design,
    List<String> tags,
    int usageCount,
    double rating,
    Instant publishedAt
) {}