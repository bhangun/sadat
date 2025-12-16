


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SplitterNodeConfig extends IntegrationNodeConfig {
    
    @JsonProperty("splitter")
    private SplitterConfig splitter;
}