package tech.kayys.silat.executor.camel.modern;

record GRPCConfig(
    String host,
    int port,
    boolean useTLS,
    String certPath
) {}