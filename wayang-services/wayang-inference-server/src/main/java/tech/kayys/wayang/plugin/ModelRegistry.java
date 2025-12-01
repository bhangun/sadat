package tech.kayys.wayang.plugin;

import java.util.Map;

public class ModelRegistry {
    
    private static final Map<String, ModelSpec> POPULAR_MODELS = Map.ofEntries(
        Map.entry("llama2-7b-chat", new ModelSpec(
            "TheBloke/Llama-2-7B-Chat-GGUF",
            "llama-2-7b-chat.Q4_K_M.gguf",
            "Llama 2 7B Chat (4-bit quantized)"
        )),
        Map.entry("llama2-13b-chat", new ModelSpec(
            "TheBloke/Llama-2-13B-Chat-GGUF",
            "llama-2-13b-chat.Q4_K_M.gguf",
            "Llama 2 13B Chat (4-bit quantized)"
        )),
        Map.entry("mistral-7b-instruct", new ModelSpec(
            "TheBloke/Mistral-7B-Instruct-v0.2-GGUF",
            "mistral-7b-instruct-v0.2.Q5_K_M.gguf",
            "Mistral 7B Instruct v0.2 (5-bit quantized)"
        )),
        Map.entry("codellama-7b", new ModelSpec(
            "TheBloke/CodeLlama-7B-GGUF",
            "codellama-7b.Q4_K_M.gguf",
            "Code Llama 7B (4-bit quantized)"
        )),
        Map.entry("tinyllama-1.1b", new ModelSpec(
            "TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF",
            "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            "TinyLlama 1.1B Chat (4-bit quantized)"
        )),
        Map.entry("phi-2", new ModelSpec(
            "TheBloke/phi-2-GGUF",
            "phi-2.Q5_K_M.gguf",
            "Phi-2 (5-bit quantized)"
        ))
    );
    
    public static ModelSpec getModel(String alias) {
        return POPULAR_MODELS.get(alias);
    }
    
    public static Map<String, ModelSpec> getAllModels() {
        return POPULAR_MODELS;
    }
    
    public record ModelSpec(
        String repoId,
        String filename,
        String description
    ) {}
}
