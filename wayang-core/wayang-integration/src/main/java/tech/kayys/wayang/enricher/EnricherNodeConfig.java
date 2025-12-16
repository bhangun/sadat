


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EnricherNodeConfig extends IntegrationNodeConfig {
    
    @JsonProperty("enrichment")
    private EnrichmentConfig enrichment;
}