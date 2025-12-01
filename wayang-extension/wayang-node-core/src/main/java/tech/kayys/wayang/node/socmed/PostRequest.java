/**
 * Post request
 */
record PostRequest(
    String content,
    List<String> mediaIds,
    Map<String, String> metadata,
    ScheduleOptions scheduleOptions
) {}