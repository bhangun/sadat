package tech.kayys.silat.executor.camel.ai;

record TensorFlowModelConfig(
    String servingUrl,
    String modelName,
    String signatureName
) {}