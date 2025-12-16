
/**
 * Enumeration of supported model formats
 */
public enum ModelFormat {
    GGUF("gguf", "llama.cpp"),
    ONNX("onnx", "ONNX Runtime"),
    TENSORRT("trt", "TensorRT"),
    TORCHSCRIPT("pt", "TorchScript"),
    TENSORFLOW_SAVED_MODEL("pb", "TensorFlow");
    
    private final String extension;
    private final String runtime;
    
    ModelFormat(String extension, String runtime) {
        this.extension = extension;
        this.runtime = runtime;
    }
}
