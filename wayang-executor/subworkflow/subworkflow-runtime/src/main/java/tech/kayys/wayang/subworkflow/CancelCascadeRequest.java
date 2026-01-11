package tech.kayys.silat.api.subworkflow;

import jakarta.validation.constraints.NotNull;

record CancelCascadeRequest(
    @NotNull String reason
) {}