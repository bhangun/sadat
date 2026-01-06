package tech.kayys.wayang.guardrails.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.guardrails.dto.GuardrailAction;
import tech.kayys.wayang.guardrails.dto.GuardrailCheckResult;
import tech.kayys.wayang.guardrails.dto.GuardrailSeverity;
import tech.kayys.wayang.guardrails.dto.PIIMatch;
import tech.kayys.wayang.guardrails.dto.PIIPolicy;

/**
 * PII detection and redaction service
 */
@ApplicationScoped
public class PIIDetectionService {

    private static final Logger LOG = LoggerFactory.getLogger(PIIDetectionService.class);

    // PII patterns
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern
            .compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern
            .compile("\\b\\d{4}[-.\\s]?\\d{4}[-.\\s]?\\d{4}[-.\\s]?\\d{4}\\b");
    private static final Pattern IP_ADDRESS_PATTERN = Pattern
            .compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");

    public Uni<GuardrailCheckResult> check(
            String content,
            PIIPolicy policy) {

        return Uni.createFrom().deferred(() -> {
            List<PIIMatch> matches = detectPII(content);

            if (!matches.isEmpty()) {
                return Uni.createFrom().item(GuardrailCheckResult.violation(
                        "pii_detection",
                        GuardrailSeverity.HIGH,
                        policy.action(),
                        String.format("Detected %d PII instances", matches.size()),
                        Map.of("matches", matches.size(), "types",
                                matches.stream().map(PIIMatch::type).toList())));
            }

            return Uni.createFrom().item(GuardrailCheckResult.passed("pii_detection"));
        });
    }

    public Uni<GuardrailCheckResult> checkAndRedact(
            String content,
            PIIPolicy policy) {
        LOG.debug("Checking PII for content: {}", content);
        return check(content, policy)
                .map(result -> {
                    if (!result.passed() && policy.action() == GuardrailAction.REDACT) {
                        String redacted = redact(content);
                        return GuardrailCheckResult.passedWithModification(
                                "pii_detection",
                                "PII redacted",
                                Map.of("original_length", content.length(),
                                        "redacted_length", redacted.length()));
                    }
                    return result;
                });
    }

    public String redact(String content) {
        String redacted = content;

        redacted = SSN_PATTERN.matcher(redacted).replaceAll("XXX-XX-XXXX");
        redacted = EMAIL_PATTERN.matcher(redacted).replaceAll("[EMAIL REDACTED]");
        redacted = PHONE_PATTERN.matcher(redacted).replaceAll("XXX-XXX-XXXX");
        redacted = CREDIT_CARD_PATTERN.matcher(redacted).replaceAll("XXXX-XXXX-XXXX-XXXX");
        redacted = IP_ADDRESS_PATTERN.matcher(redacted).replaceAll("[IP REDACTED]");

        return redacted;
    }

    private List<PIIMatch> detectPII(String content) {
        List<PIIMatch> matches = new ArrayList<>();

        if (SSN_PATTERN.matcher(content).find()) {
            matches.add(new PIIMatch("ssn", "Social Security Number"));
        }
        if (EMAIL_PATTERN.matcher(content).find()) {
            matches.add(new PIIMatch("email", "Email Address"));
        }
        if (PHONE_PATTERN.matcher(content).find()) {
            matches.add(new PIIMatch("phone", "Phone Number"));
        }
        if (CREDIT_CARD_PATTERN.matcher(content).find()) {
            matches.add(new PIIMatch("credit_card", "Credit Card"));
        }

        return matches;
    }
}
