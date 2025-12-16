package tech.kayys.wayang.guardrails.detector;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;

@ApplicationScoped
public class ToxicityDetector {

    private static final Set<String> TOXIC_WORDS = Set.of(
            "hate", "stupid", "idiot", "kill", "worthless", "ugly",
            "disgusting", "pathetic", "moron", "scum", "trash");

    private static final Set<String> HATE_INDICATORS = Set.of(
            "race", "gender", "religion", "ethnicity", "sexual orientation");

    public Uni<ToxicityResult> detect(String text) {
        return Uni.createFrom().item(() -> {
            String lowerText = text.toLowerCase();

            List<String> toxicWordsFound = TOXIC_WORDS.stream()
                    .filter(lowerText::contains)
                    .toList();

            boolean hasHateSpeech = HATE_INDICATORS.stream()
                    .anyMatch(indicator -> lowerText.contains(indicator) &&
                            toxicWordsFound.stream().anyMatch(lowerText::contains));

            double toxicityScore = calculateToxicityScore(lowerText, toxicWordsFound);
            boolean isToxic = toxicityScore > 0.5 || hasHateSpeech;

            return new ToxicityResult(isToxic, toxicityScore, toxicWordsFound, hasHateSpeech);
        });
    }

    private double calculateToxicityScore(String text, List<String> toxicWordsFound) {
        if (toxicWordsFound.isEmpty())
            return 0.0;

        double wordScore = toxicWordsFound.size() * 0.2;

        // Check for intensity modifiers
        double intensityModifier = 1.0;
        if (text.contains("very ") || text.contains("extremely ")) {
            intensityModifier = 1.5;
        }

        // Check for all caps (shouting)
        long capsWords = Arrays.stream(text.split("\\s+"))
                .filter(word -> word.length() > 2 && word.equals(word.toUpperCase()))
                .count();
        double capsModifier = 1.0 + (capsWords * 0.1);

        return Math.min(wordScore * intensityModifier * capsModifier, 1.0);
    }

    public static class ToxicityResult {
        private final boolean toxic;
        private final double score;
        private final List<String> toxicWords;
        private final boolean hateSpeech;

        public ToxicityResult(boolean toxic, double score, List<String> toxicWords, boolean hateSpeech) {
            this.toxic = toxic;
            this.score = score;
            this.toxicWords = toxicWords;
            this.hateSpeech = hateSpeech;
        }

        // Getters
        public boolean isToxic() {
            return toxic;
        }

        public double getScore() {
            return score;
        }

        public List<String> getToxicWords() {
            return toxicWords;
        }

        public boolean isHateSpeech() {
            return hateSpeech;
        }
    }
}