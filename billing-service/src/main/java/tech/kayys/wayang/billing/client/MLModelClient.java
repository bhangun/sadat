package tech.kayys.wayang.billing.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.billing.model.MLPredictionResponse;

/**
 * ML Model REST client (connects to Python model server)
 */
@RegisterRestClient(configKey = "ml-model")
public interface MLModelClient {
    
    @jakarta.ws.rs.POST
    @jakarta.ws.rs.Path("/predict/churn")
    Uni<MLPredictionResponse> predict(double[] features);
}