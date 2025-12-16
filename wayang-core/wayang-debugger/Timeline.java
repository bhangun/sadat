
@Value
@Builder
public class Timeline {
    List<TimelineEvent> events;
    Instant startTime;
    Instant endTime;
    Duration duration;
}