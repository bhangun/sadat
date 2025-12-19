package tech.kayys.wayang.workflow.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * PII detector service.
 */
@ApplicationScoped
class PIIDetector {

    private static final Logger LOG = Logger.getLogger(PIIDetector.class);

    // Patterns for PII detection
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b");

    public Uni<PIIResult> scan(Map<String, Object> data) {
        return Uni.createFrom().item(() -> {
            Set<String> piiTypes = new HashSet<>();
            String text = extractText(data);

            if (EMAIL_PATTERN.matcher(text).find()) {
                piiTypes.add("email");
            }
            if (PHONE_PATTERN.matcher(text).find()) {
                piiTypes.add("phone");
            }
            if (SSN_PATTERN.matcher(text).find()) {
                piiTypes.add("ssn");
            }
            if (CREDIT_CARD_PATTERN.matcher(text).find()) {
                piiTypes.add("credit_card");
            }

            return PIIResult.builder()
                    .hasPII(!piiTypes.isEmpty())
                    .types(piiTypes)
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
