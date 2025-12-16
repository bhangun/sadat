
/**
 * Resource requirements and constraints for model execution
 */
public record ResourceRequirements(
    MemorySize minMemory,
    MemorySize recommendedMemory,
    MemorySize minVRAM,
    Optional<Integer> minCores,
    Optional<DiskSpace> diskSpace
) {}