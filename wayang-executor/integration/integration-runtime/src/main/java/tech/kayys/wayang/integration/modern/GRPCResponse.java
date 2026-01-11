package tech.kayys.silat.executor.camel.modern;

import java.time.Instant;
import java.util.Map;

record GRPCResponse(
    String serviceName,
    String methodName,
    Object response,
    Map<String, Object> metadata,
    Instant timestamp
) {}