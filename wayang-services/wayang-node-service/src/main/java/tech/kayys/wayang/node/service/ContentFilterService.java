package tech.kayys.wayang.workflow.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.kafka.clients.admin.DeleteAclsResult.FilterResult;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Content filter service.
 */
@ApplicationScoped
class ContentFilterService {

    private static final Logger LOG = Logger.getLogger(ContentFilterService.class);

    // Patterns for harmful content detection
    private static final List<Pattern> HARMFUL_PATTERNS = List.of(
            Pattern.compile("\\b(kill|harm|hurt)\\b.*\\b(yourself|myself|themselves)\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(bomb|explosive|weapon)\\b.*\\b(make|build|create)\\b",
                    Pattern.CASE_INSENSITIVE)
    // Add more patterns
    );

    public Uni<FilterResult> filter(Map<String, Object> data) {
        return Uni.createFrom().item(() -> {
            List<String> detectedCategories = new ArrayList<>();

            String text = extractText(data);

            for (Pattern pattern : HARMFUL_PATTERNS) {
                if (pattern.matcher(text).find()) {
                    detectedCategories.add("potentially_harmful");
                    break;
                }
            }

            return FilterResult.builder()
                    .harmful(!detectedCategories.isEmpty())
                    .categories(detectedCategories)
                    .build();
        });
    }

    private String extractText(Map<String, Object> data) {
        StringBuilder text = new StringBuilder();
        for (Object value : data.values()) {
            if (value instanceof String str) {
                text.append(str).append(" ");
            }
        }
        return text.toString();
    }
}
