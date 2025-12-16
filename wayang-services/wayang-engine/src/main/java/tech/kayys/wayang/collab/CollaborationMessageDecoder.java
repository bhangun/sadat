package tech.kayys.wayang.collab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EndpointConfig;

/**
 * CollaborationMessageDecoder - Decode JSON to collaboration messages
 */
public class CollaborationMessageDecoder implements Decoder.Text<CollaborationMessage> {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public CollaborationMessage decode(String message) throws DecodeException {
        try {
            return objectMapper.readValue(message, CollaborationMessage.class);
        } catch (Exception e) {
            throw new DecodeException(message, "Failed to decode message", e);
        }
    }

    @Override
    public boolean willDecode(String message) {
        try {
            objectMapper.readTree(message);
            return true;
        } catch (Exception e) {
            return false;
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