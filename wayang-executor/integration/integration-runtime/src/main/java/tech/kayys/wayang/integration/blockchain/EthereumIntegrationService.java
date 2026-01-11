package tech.kayys.silat.executor.camel.blockchain;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Ethereum and EVM-compatible blockchain integration
 */
@ApplicationScoped
public class EthereumIntegrationService {

    private static final Logger LOG = LoggerFactory.getLogger(EthereumIntegrationService.class);

    @Inject
    CamelContext camelContext;

    @Inject
    ProducerTemplate producerTemplate;

    /**
     * Get account balance
     */
    public Uni<BlockchainBalance> getBalance(
            String address,
            EthereumConfig config,
            String tenantId) {

        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<BlockchainBalance> future = new CompletableFuture<>();

            try {
                String routeId = "eth-balance-" + UUID.randomUUID();

                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)
                            .setHeader("tenantId", constant(tenantId))

                            // Use Web3j component
                            .toD("web3j://eth_getBalance?address=" + address +
                                 "&nodeAddress=" + config.nodeUrl() +
                                 "&operation=ETH_GET_BALANCE")

                            .process(exchange -> {
                                BigInteger weiBalance = exchange.getIn().getBody(BigInteger.class);
                                BigDecimal ethBalance = new BigDecimal(weiBalance)
                                    .divide(new BigDecimal("1000000000000000000"));

                                BlockchainBalance balance = new BlockchainBalance(
                                    address,
                                    "ETH",
                                    ethBalance,
                                    weiBalance.toString(),
                                    config.network(),
                                    Instant.now()
                                );

                                future.complete(balance);
                            });
                    }
                });

                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, null);

            } catch (Exception e) {
                LOG.error("Failed to get Ethereum balance", e);
                future.completeExceptionally(e);
            }

            return future;
        });
    }

    /**
     * Send transaction
     */
    public Uni<TransactionReceipt> sendTransaction(
            String fromAddress,
            String toAddress,
            BigDecimal amount,
            EthereumConfig config,
            String tenantId) {

        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<TransactionReceipt> future = new CompletableFuture<>();

            try {
                String routeId = "eth-send-" + UUID.randomUUID();

                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)

                            .process(exchange -> {
                                // Convert ETH to Wei
                                BigInteger weiAmount = amount
                                    .multiply(new BigDecimal("1000000000000000000"))
                                    .toBigInteger();

                                exchange.getIn().setHeader("toAddress", toAddress);
                                exchange.getIn().setHeader("fromAddress", fromAddress);
                                exchange.getIn().setHeader("value", weiAmount);
                            })

                            .toD("web3j://eth_sendTransaction?" +
                                 "nodeAddress=" + config.nodeUrl() +
                                 "&operation=ETH_SEND_RAW_TRANSACTION" +
                                 "&privateKey=" + config.privateKey())

                            .process(exchange -> {
                                String txHash = exchange.getIn().getBody(String.class);

                                TransactionReceipt receipt = new TransactionReceipt(
                                    txHash,
                                    fromAddress,
                                    toAddress,
                                    amount,
                                    "ETH",
                                    "PENDING",
                                    null,
                                    config.network(),
                                    Instant.now()
                                );

                                future.complete(receipt);
                            });
                    }
                });

                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, null);

            } catch (Exception e) {
                LOG.error("Failed to send Ethereum transaction", e);
                future.completeExceptionally(e);
            }

            return future;
        });
    }

    /**
     * Call smart contract function
     */
    public Uni<SmartContractResult> callContract(
            String contractAddress,
            String functionName,
            List<Object> parameters,
            EthereumConfig config,
            String tenantId) {

        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<SmartContractResult> future = new CompletableFuture<>();

            try {
                String routeId = "eth-contract-" + UUID.randomUUID();

                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)

                            .process(exchange -> {
                                exchange.getIn().setHeader("contractAddress", contractAddress);
                                exchange.getIn().setHeader("functionName", functionName);
                                exchange.getIn().setBody(parameters);
                            })

                            .toD("web3j://contract?" +
                                 "address=" + contractAddress +
                                 "&nodeAddress=" + config.nodeUrl() +
                                 "&operation=CONTRACT_CALL" +
                                 "&method=" + functionName)

                            .process(exchange -> {
                                Object result = exchange.getIn().getBody();

                                SmartContractResult contractResult = new SmartContractResult(
                                    contractAddress,
                                    functionName,
                                    parameters,
                                    result,
                                    null,
                                    config.network(),
                                    Instant.now()
                                );

                                future.complete(contractResult);
                            });
                    }
                });

                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, null);

            } catch (Exception e) {
                LOG.error("Failed to call smart contract", e);
                future.completeExceptionally(e);
            }

            return future;
        });
    }

    /**
     * Get transaction receipt
     */
    public Uni<TransactionReceipt> getTransactionReceipt(
            String txHash,
            EthereumConfig config,
            String tenantId) {

        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<TransactionReceipt> future = new CompletableFuture<>();

            try {
                String routeId = "eth-receipt-" + UUID.randomUUID();

                camelContext.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() {
                        from("direct:" + routeId)
                            .routeId(routeId)

                            .toD("web3j://eth_getTransactionReceipt?" +
                                 "transactionHash=" + txHash +
                                 "&nodeAddress=" + config.nodeUrl())

                            .unmarshal().json()
                            .process(exchange -> {
                                Map<String, Object> receipt = exchange.getIn().getBody(Map.class);

                                TransactionReceipt txReceipt = new TransactionReceipt(
                                    txHash,
                                    (String) receipt.get("from"),
                                    (String) receipt.get("to"),
                                    null,
                                    "ETH",
                                    (String) receipt.get("status"),
                                    ((Number) receipt.get("blockNumber")).longValue(),
                                    config.network(),
                                    Instant.now()
                                );

                                future.complete(txReceipt);
                            });
                    }
                });

                camelContext.getRouteController().startRoute(routeId);
                producerTemplate.sendBody("direct:" + routeId, null);

            } catch (Exception e) {
                future.completeExceptionally(e);
            }

            return future;
        });
    }
}