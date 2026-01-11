package tech.kayys.silat.executor.camel.blockchain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

record FabricTransactionResult(
    String transactionId,
    String channelName,
    String chaincodeName,
    String functionName,
    List<String> args,
    String status,
    Map<String, Object> result,
    Instant timestamp
) {}