package tech.kayys.silat.executor.camel.modern;

import java.time.Instant;
import java.util.List;
import java.util.Map;

record GraphQLResponse(
    Map<String, Object> data,
    List<Map<String, Object>> errors,
    boolean hasErrors,
    Instant timestamp
) {}