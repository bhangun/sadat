@Value
@Builder
public class Suggestion {
    SuggestionType type;
    String title;
    String description;
    Impact impact;
    Difficulty difficulty;
    String nodeId;
    Map<String, Object> parameters;
}