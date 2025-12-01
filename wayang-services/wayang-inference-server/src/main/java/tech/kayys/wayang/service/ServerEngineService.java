package tech.kayys.wayang.service;

import java.util.List;

import org.jboss.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.engine.LlamaEngine;

@ApplicationScoped
public class ServerEngineService {

    private static final Logger log = Logger.getLogger(ServerEngineService.class);
    @Inject
    ServerLlamaConfig serverConfig;

    private LlamaEngine engine;

    @PostConstruct
    void initialize() {
        // Initialize core engine
        this.engine = new LlamaEngine(serverConfig);

        // Handle warmup in server layer
        if (serverConfig.warmupModel()) {
            warmupModel();
        }
    }

    private void warmupModel() {
        try {
            log.infof("Warming up model with {} tokens", serverConfig.warmupTokens());
            String warmupPrompt = "Hello, this is a warmup prompt to initialize the model.";
            engine.generate(warmupPrompt, serverConfig.sampling(),
                    Math.min(10, serverConfig.warmupTokens()), List.of(), null, false);
            log.info("Model warmup completed");
        } catch (Exception e) {
            log.warn("Model warmup failed, continuing anyway", e);
        }
    }
}