package tech.kayys.silat.executor.camel.blockchain;

import java.time.Instant;
import java.util.List;

record SmartContractResult(
    String contractAddress,
    String functionName,
    List<Object> parameters,
    Object result,
    String transactionHash,
    String network,
    Instant executedAt
) {}