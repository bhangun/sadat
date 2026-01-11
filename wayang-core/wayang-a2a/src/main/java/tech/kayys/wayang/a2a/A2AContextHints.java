@Value
@Builder
public class A2AContextHints {
    Priority priority;
    Duration deadline;
    Map<String, Object> personaOverrides;
    List<String> requiredCapabilities;
}