
// Timeline Renderer
@ApplicationScoped
public class ExecutionTimelineRenderer {
    public Timeline renderTimeline(List<TraceEntry> traces) {
        List<TimelineEvent> events = traces.stream()
            .map(this::toTimelineEvent)
            .collect(Collectors.toList());
        
        return Timeline.builder()
            .events(events)
            .startTime(events.get(0).getTimestamp())
            .endTime(events.get(events.size() - 1).getTimestamp())
            .duration(Duration.between(
                events.get(0).getTimestamp(),
                events.get(events.size() - 1).getTimestamp()
            ))
            .build();
    }
}