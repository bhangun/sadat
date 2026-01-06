package tech.kayys.wayang.project.dto;

public record CreatePatternRequest(
        String patternName,
        String description,
        EIPPatternType patternType,
        EndpointConfig sourceConfig,
        EndpointConfig targetConfig,
        TransformationConfig transformation,
        ErrorHandlingConfig errorHandling) {
}
