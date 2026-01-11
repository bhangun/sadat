package tech.kayys.silat.executor.camel.blockchain;

import java.time.Instant;

record IPFSDownloadResult(
    String cid,
    byte[] data,
    long size,
    Instant downloadedAt
) {}