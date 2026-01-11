package tech.kayys.silat.executor.camel.modern;

record GraphQLConfig(
    String endpoint,
    String websocketEndpoint,
    String authToken
) {}