

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ConnectorNodeConfig extends IntegrationNodeConfig {
    
    @JsonProperty("connector")
    private ConnectorConfig connector;
}