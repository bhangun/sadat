package tech.kayys.silat.executor.camel.blockchain;

record IPFSConfig(
    String host,
    int port,
    String gatewayUrl
) {}