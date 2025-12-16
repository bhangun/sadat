package tech.kayys.wayang.guardrails.detector;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;

@ApplicationScoped
public class HallucinationDetector {

    public Uni<HallucinationResult> detect(String text, Map<String, Object> metadata) {
        return Uni.createFrom().item(() -> {
            List<String> unsupportedClaims = extractClaims(text);
            List<String> factualErrors = checkFactualConsistency(text, metadata);
            boolean contradictsSource = checkContradiction(text, metadata);

            boolean hallucinated = !unsupportedClaims.isEmpty() ||
                    !factualErrors.isEmpty() ||
                    contradictsSource;

            double confidence = calculateHallucinationConfidence(
                    unsupportedClaims.size(),
                    factualErrors.size(),
                    contradictsSource);

            return new HallucinationResult(
                    hallucinated,
                    confidence,
                    unsupportedClaims,
                    factualErrors,
                    contradictsSource);
        });
    }

    private List<String> extractClaims(String text) {
        List<String> claims = new ArrayList<>();
        List<String> claimIndicators = List.of(
                "according to", "research shows", "studies prove",
                "it is known that", "scientists say");

        String[] sentences = text.split("[.!?]+");
        for (String sentence : sentences) {
            String lowerSentence = sentence.toLowerCase().trim();

            if (claimIndicators.stream().anyMatch(lowerSentence::contains) &&
                    !hasCitations(sentence)) {
                claims.add(sentence.trim());
            }

            // Check for definitive statements without evidence
            if (lowerSentence.matches(".*\\b(is|are|was|were)\\s+(always|never|all|every)\\b.*") &&
                    !hasCitations(sentence)) {
                claims.add(sentence.trim());
            }
        }

        return claims;
    }

    private boolean hasCitations(String sentence) {
        return sentence.matches(".*\\[\\d+\\].*") ||
                sentence.matches(".*\\([A-Za-z]+,\\s*\\d{4}\\).*");
    }

    private List<String> checkFactualConsistency(String text, Map<String, Object> metadata) {
        List<String> errors = new ArrayList<>();

        // This would integrate with fact-checking APIs or knowledge bases
        // For now, implementing a simple placeholder
        if (metadata.containsKey("sourceText")) {
            String sourceText = (String) metadata.get("sourceText");
            errors.addAll(checkNumericalConsistency(text, sourceText));
        }

        return errors;
    }

    private List<String> checkNumericalConsistency(String generated, String source) {
        List<String> errors = new ArrayList<>();

        // Extract numbers from both texts
        List<String> genNumbers = extractNumbers(generated);
        List<String> srcNumbers = extractNumbers(source);

        // Check if generated text introduces new numbers not in source
        for (String num : genNumbers) {
            if (!srcNumbers.contains(num)) {
                errors.add("Introduced number: " + num);
            }
        }

        return errors;
    }

    private List<String> extractNumbers(String text) {
        return Arrays.stream(text.split("\\s+"))
                .filter(word -> word.matches(".*\\d+.*"))
                .toList();
    }

    private boolean checkContradiction(String text, Map<String, Object> metadata) {
        if (!metadata.containsKey("sourceText"))
            return false;

        String sourceText = (String) metadata.get("sourceText");

        // Simple contradiction detection based on negation
        Map<String, String> keyTerms = extractKeyTerms(sourceText);
        Map<String, String> genKeyTerms = extractKeyTerms(text);

        for (Map.Entry<String, String> entry : keyTerms.entrySet()) {
            String term = entry.getKey();
            String sentiment = entry.getValue();

            if (genKeyTerms.containsKey(term) &&
                    !genKeyTerms.get(term).equals(sentiment)) {
                return true;
            }
        }

        return false;
    }

    private Map<String, String> extractKeyTerms(String text) {
        Map<String, String> terms = new HashMap<>();
        List<String> positiveWords = List.of("good", "success", "effective", "positive");
        List<String> negativeWords = List.of("bad", "failure", "ineffective", "negative");

        String[] words = text.toLowerCase().split("\\s+");
        for (String word : words) {
            if (positiveWords.contains(word)) {
                terms.put(word, "positive");
            } else if (negativeWords.contains(word)) {
                terms.put(word, "negative");
            }
        }

        return terms;
    }

    private double calculateHallucinationConfidence(int unsupportedClaims,
            int factualErrors,
            boolean contradicts) {
        double score = 0.0;
        score += unsupportedClaims * 0.2;
        score += factualErrors * 0.3;
        score += contradicts ? 0.5 : 0.0;

        return Math.min(score, 1.0);
    }

    public static class HallucinationResult {
        private final boolean hallucinated;
        private final double confidence;
        private final List<String> unsupportedClaims;
        private final List<String> factualErrors;
        private final boolean contradictsSource;

        public HallucinationResult(boolean hallucinated, double confidence,
                List<String> unsupportedClaims,
                List<String> factualErrors,
                boolean contradictsSource) {
            this.hallucinated = hallucinated;
            this.confidence = confidence;
            this.unsupportedClaims = unsupportedClaims;
            this.factualErrors = factualErrors;
            this.contradictsSource = contradictsSource;
        }

        // Getters
        public boolean isHallucinated() {
            return hallucinated;
        }

        public double getConfidence() {
            return confidence;
        }

        public List<String> getUnsupportedClaims() {
            return unsupportedClaims;
        }

        public List<String> getFactualErrors() {
            return factualErrors;
        }

        public boolean isContradictsSource() {
            return contradictsSource;
        }
    }
}
