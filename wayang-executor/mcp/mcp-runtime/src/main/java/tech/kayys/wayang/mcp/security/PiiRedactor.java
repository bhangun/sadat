package tech.kayys.wayang.mcp.security;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * PII redactor
 */
@ApplicationScoped
public class PiiRedactor {

    public Map<String, Object> redact(
            Map<String, Object> data,
            Set<String> piiPatterns) {

        if (piiPatterns == null || piiPatterns.isEmpty()) {
            return data;
        }

        Map<String, Object> redacted = new HashMap<>();

        data.forEach((key, value) -> {
            if (value instanceof String) {
                String str = (String) value;
                for (String pattern : piiPatterns) {
                    str = str.replaceAll(pattern, "[REDACTED]");
                }
                redacted.put(key, str);
            } else {
                redacted.put(key, value);
            }
        });

        return redacted;
    }
}