
/**
 * Runner metadata for selection and diagnostics
 */
public record RunnerMetadata(
    String name,
    String version,
    List<ModelFormat> supportedFormats,
    List<DeviceType> supportedDevices,
    ExecutionMode executionMode,
    Map<String, Object> capabilities
) {}