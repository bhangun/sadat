



import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EnrichmentConfig {
    
    @JsonProperty("resourceUri")
    private String resourceUri;
    
    @JsonProperty("strategy")
    private String strategy;
    
    @JsonProperty("cacheEnabled")
    private Boolean cacheEnabled;
    
    @JsonProperty("cacheTtl")
    private Integer cacheTtl;
}