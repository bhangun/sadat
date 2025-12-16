
/**
 * Device type enumeration with capability flags
 */
public enum DeviceType {
    CPU(false, false),
    CUDA(true, false),
    ROCM(true, false),
    METAL(true, false),
    TPU(false, true),
    OPENVINO(true, false);
    
    private final boolean supportsGpu;
    private final boolean supportsTpu;
}