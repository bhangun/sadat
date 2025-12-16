


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import tech.kayys.wayang.integration.auth.AuthConfig;

@Data
public class ConnectorConfig {
    
    @JsonProperty("component")
    private String component;
    
    @JsonProperty("uri")
    private String uri;
    
    @JsonProperty("direction")
    private String direction;
    
    @JsonProperty("authentication")
    private AuthConfig authentication;
}