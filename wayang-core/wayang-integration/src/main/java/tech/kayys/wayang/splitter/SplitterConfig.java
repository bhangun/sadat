

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SplitterConfig {
    
    @JsonProperty("expression")
    private String expression;
    
    @JsonProperty("streaming")
    private Boolean streaming;
    
    @JsonProperty("parallel")
    private Boolean parallel;
    
    @JsonProperty("aggregationStrategy")
    private String aggregationStrategy;
    
    @JsonProperty("stopOnException")
    private Boolean stopOnException;
}