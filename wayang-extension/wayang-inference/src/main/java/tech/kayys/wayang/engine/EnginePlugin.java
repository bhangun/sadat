package tech.kayys.wayang.engine;

import java.util.List;

import tech.kayys.wayang.model.ChatMessage;
import tech.kayys.wayang.model.GenerationResult;
import tech.kayys.wayang.plugin.Plugin;
public interface EnginePlugin extends Plugin {
    
    /**
     * Pre-process prompt before generation
     */
    default String preprocessPrompt(String prompt) {
        return prompt;
    }
    
    /**
     * Post-process generated text
     */
    default String postprocessOutput(String output) {
        return output;
    }
    
    /**
     * Intercept chat messages before processing
     */
    default List<ChatMessage> preprocessChatMessages(List<ChatMessage> messages) {
        return messages;
    }
    
    /**
     * Called before generation starts
     */
    default void onGenerationStart(String prompt) {}
    
    /**
     * Called after generation completes
     */
    default void onGenerationComplete(GenerationResult result) {}
    
    /**
     * Called on each token generated (for streaming)
     */
    default void onTokenGenerated(String token) {}
    
    /**
     * Modify sampling temperature dynamically
     */
    default float adjustTemperature(float temperature, int tokenIndex) {
        return temperature;
    }
    
    // Override to return String[] instead of default implementation
    @Override
    default String[] getDependencies() {
        return new String[0];
    }
}
