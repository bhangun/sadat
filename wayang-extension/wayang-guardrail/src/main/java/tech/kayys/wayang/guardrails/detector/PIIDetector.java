package tech.kayys.wayang.guardrails.detector;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.regex.*;

@ApplicationScoped
public class PIIDetector {
    
    private static final Map<String, Pattern> PII_PATTERNS = Map.of(
        "SSN", Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
        "CREDIT_CARD", Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"),
        "EMAIL", Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
        "PHONE", Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b"),
        "IP_ADDRESS", Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b")
    );
    
    public Uni<DetectionResult> detect(String text) {
        List<Finding> findings = new ArrayList<>();
        
        for (Map.Entry<String, Pattern> entry : PII_PATTERNS.entrySet()) {
            Matcher matcher = entry.getValue().matcher(text);
            
            while (matcher.find()) {
                findings.add(new Finding(
                    entry.getKey(),
                    matcher.group(),
                    matcher.start(),
                    matcher.end(),
                    1.0
                ));
            }
        }
        
        if (findings.isEmpty()) {
            return Uni.createFrom().item(DetectionResult.safe("pii"));
        }
        
        boolean hasHighRiskPII = findings.stream()
            .anyMatch(f -> f.type().equals("SSN") || f.type().equals("CREDIT_CARD"));
        
        if (hasHighRiskPII) {
            return Uni.createFrom().item(
                DetectionResult.blocked("pii", "High-risk PII detected")
            );
        }
        
        return Uni.createFrom().item(
            DetectionResult.warning("pii", "PII detected", findings)
        );
    }
}