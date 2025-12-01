package tech.kayys.wayang.model;

import java.util.List;

public record ModelListResponse(
    String object,
    List<ModelResponse> data
) {}
