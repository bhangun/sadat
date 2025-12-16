


import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@RegisterForReflection
public class CustomAggregationStrategy implements AggregationStrategy {
    
    @Override
    @SuppressWarnings("unchecked")
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) {
            List<Object> list = new ArrayList<>();
            list.add(newExchange.getIn().getBody());
            newExchange.getIn().setBody(list);
            return newExchange;
        }
        
        List<Object> list = oldExchange.getIn().getBody(List.class);
        list.add(newExchange.getIn().getBody());
        return oldExchange;
    }
}