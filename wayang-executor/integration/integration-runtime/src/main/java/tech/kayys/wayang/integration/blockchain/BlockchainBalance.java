package tech.kayys.silat.executor.camel.blockchain;

import java.math.BigDecimal;
import java.time.Instant;

record BlockchainBalance(
    String address,
    String currency,
    BigDecimal balance,
    String rawBalance,
    String network,
    Instant queriedAt
) {}