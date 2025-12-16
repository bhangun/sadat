


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AggregatorConfig {
    
    @JsonProperty("correlationExpression")
    private String correlationExpression;
    
    @JsonProperty("completionPredicate")
    private String completionPredicate;
    
    @JsonProperty("completionTimeout")
    private Integer completionTimeout;
    
    @JsonProperty("completionSize")
    private Integer completionSize;
    
    @JsonProperty("aggregationStrategy")
    private String aggregationStrategy;
    
    @JsonProperty("repositoryRef")
    private String repositoryRef;
}