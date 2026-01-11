package tech.kayys.silat.executor.camel.ai;

import java.util.Map;

record ONNXModelConfig(
    String modelPath,
    Map<String, Object> inputSpec
) {}