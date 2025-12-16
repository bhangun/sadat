package tech.kayys.wayang.collab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

/**
 * CollaborationMessageEncoder - Encode collaboration messages to JSON
 */
public class CollaborationMessageEncoder implements Encoder.Text<CollaborationMessage> {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public String encode(CollaborationMessage message) throws EncodeException {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            throw new EncodeException(message, "Failed to encode message", e);
        }
    }

    @Override
    public void init(EndpointConfig config) {
        // No initialization needed
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}
