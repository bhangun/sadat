package tech.kayys.wayang.hitl.dto;

import java.util.List;

record PagedTaskResponse(
    List<TaskDto> tasks,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}