



import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Configuration for integration nodes
 */
@Data
public class IntegrationNodeConfig {
    
    @JsonProperty("camelConfig")
    private CamelConfig camelConfig;
    
    @JsonProperty("dataFormat")
    private DataFormatConfig dataFormat;
    
    private Integer timeout;
    private boolean transacted;
    private boolean streamCaching;
}