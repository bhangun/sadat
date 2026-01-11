package tech.kayys.silat.executor.camel.blockchain;

import java.time.Instant;

record IPFSUploadResult(
    String cid,
    String fileName,
    long size,
    String url,
    Instant uploadedAt
) {}