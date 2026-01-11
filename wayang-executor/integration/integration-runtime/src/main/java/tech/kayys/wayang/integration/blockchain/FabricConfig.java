package tech.kayys.silat.executor.camel.blockchain;

record FabricConfig(
    String networkConfigPath,
    String walletPath,
    String organizationName,
    String userName
) {}