package tech.kayys.silat.executor.camel.ai;

import java.time.Instant;
import java.util.List;

record NERResult(
    List<Entity> entities,
    Instant extractedAt
) {}