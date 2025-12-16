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
    


      public Uni<PIIResult> detect(String text) {
       
  
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
        
   /*      return Uni.createFrom().item(
            DetectionResult.warning("pii", "PII detected", findings)
        );
 */
         return Uni.createFrom().item(() -> {
            List<String> detectedEntities = PII_PATTERNS.stream()
                .flatMap(pattern -> pattern.matcher(text).results()
                    .map(match -> match.group()))
                .distinct()
                .toList();
            
            boolean hasPII = !detectedEntities.isEmpty();
            double confidence = calculateConfidence(detectedEntities, text);
            
            return new PIIResult(hasPII, detectedEntities, confidence);
        });
    }

    private double calculateConfidence(List<String> entities, String text) {
        if (entities.isEmpty()) return 0.0;
        
        // Simple confidence calculation based on number and type of entities
        double baseConfidence = Math.min(entities.size() * 0.3, 1.0);
        
        // Increase confidence if multiple different types of PII are found
        long uniqueTypes = entities.stream()
            .map(this::classifyPIIType)
            .distinct()
            .count();
        
        return Math.min(baseConfidence + (uniqueTypes - 1) * 0.2, 1.0);
    }
    
    private String classifyPIIType(String entity) {
        if (entity.contains("@")) return "EMAIL";
        if (entity.matches(".*\\d{3}-\\d{2}-\\d{4}.*")) return "SSN";
        if (entity.replaceAll("[^\\d]", "").length() >= 16) return "CREDIT_CARD";
        if (entity.matches(".*\\+?\\d[\\d\\s-]{8,}.*")) return "PHONE";
        return "ADDRESS";
    }
    
    public static class PIIResult {
        private final boolean detected;
        private final List<String> entities;
        private final double confidence;
        
        public PIIResult(boolean detected, List<String> entities, double confidence) {
            this.detected = detected;
            this.entities = entities;
            this.confidence = confidence;
        }
        
        // Getters
        public boolean isDetected() { return detected; }
        public List<String> getEntities() { return entities; }
        public double getConfidence() { return confidence; }
    }
}