@Value
@Builder
public class Relation {
    String id;
    String type;
    String subjectId;
    String objectId;
    double confidence;
    Map<String, Object> properties;
}