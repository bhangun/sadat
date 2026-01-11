package tech.kayys.silat.executor.camel.blockchain;

record EthereumConfig(
    String nodeUrl,
    String network,
    String privateKey
) {}