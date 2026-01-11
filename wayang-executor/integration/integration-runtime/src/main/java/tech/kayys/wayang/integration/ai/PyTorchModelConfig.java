package tech.kayys.silat.executor.camel.ai;

record PyTorchModelConfig(
    String servingUrl,
    String modelName
) {}