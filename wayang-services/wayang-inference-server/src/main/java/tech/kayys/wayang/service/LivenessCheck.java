package tech.kayys.wayang.service;



public class LivenessCheck implements HealthCheck {
    
    @Inject
    ModelManager modelManager;
    
    @Override
    public HealthCheckResponse call() {
        try {
            // Check if model manager is responsive
            var model = modelManager.getActiveModel();
            
            return HealthCheckResponse
                .named("model-liveness")
                .status(model != null)
                .withData("model_loaded", model != null)
                .build();
                
        } catch (Exception e) {
            return HealthCheckResponse
                .named("model-liveness")
                .down()
                .withData("error", e.getMessage())
                .build();
        }
    }
}
