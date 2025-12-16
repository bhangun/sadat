package tech.kayys.wayang.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.websocket.Decoder;
import jakarta.websocket.EndpointConfig;

public class ResourceEventDecoder implements Decoder.Text<ResourceEvent> {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public ResourceEvent decode(String s) {
        try {
            return objectMapper.readValue(s, ResourceEvent.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode resource event", e);
        }
    }

    @Override
    public boolean willDecode(String s) {
        return s != null;
    }

    @Override
    public void init(EndpointConfig config) {
    }

    @Override
    public void destroy() {
    }
}
