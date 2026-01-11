package tech.kayys.silat.executor.camel.blockchain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

record FabricQueryResult(
    String channelName,
    String chaincodeName,
    String functionName,
    List<String> args,
    Map<String, Object> result,
    Instant timestamp
) {}