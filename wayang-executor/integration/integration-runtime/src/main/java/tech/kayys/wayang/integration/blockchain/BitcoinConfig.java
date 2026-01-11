package tech.kayys.silat.executor.camel.blockchain;

record BitcoinConfig(
    String rpcUrl,
    String rpcUser,
    String rpcPassword,
    String network
) {}