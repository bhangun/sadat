package tech.kayys.silat.executor.camel.blockchain;

import java.math.BigDecimal;
import java.time.Instant;

record TransactionReceipt(
    String transactionHash,
    String fromAddress,
    String toAddress,
    BigDecimal amount,
    String currency,
    String status,
    Long blockNumber,
    String network,
    Instant timestamp
) {}