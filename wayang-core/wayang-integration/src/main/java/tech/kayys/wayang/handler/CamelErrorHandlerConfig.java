



import io.quarkus.runtime.Startup;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.spi.ErrorHandlerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

/**
 * Global Camel error handlers
 */
@Slf4j
@ApplicationScoped
public class CamelErrorHandlerConfig {
    
    @Produces
    @Named("defaultErrorHandler")
    @Startup
    public ErrorHandlerFactory defaultErrorHandler() {
        return DefaultErrorHandlerBuilder.defaultErrorHandler()
            .maximumRedeliveries(3)
            .redeliveryDelay(1000)
            .backOffMultiplier(2)
            .useExponentialBackOff()
            .maximumRedeliveryDelay(10000)
            .retryAttemptedLogLevel(LoggingLevel.WARN)
            .retriesExhaustedLogLevel(LoggingLevel.ERROR)
            .onExceptionOccurred(exchange -> {
                // Mark exchange for error-as-input handling
                exchange.setProperty("errorOccurred", true);
                log.error("Error occurred in exchange: {}", exchange.getExchangeId());
            })
            .onRedelivery(exchange -> {
                int redeliveryCount = exchange.getIn().getHeader("CamelRedeliveryCounter", Integer.class);
                log.warn("Redelivery attempt {} for exchange: {}", 
                    redeliveryCount, exchange.getExchangeId());
            });
    }
    
    @Produces
    @Named("noRetryErrorHandler")
    public ErrorHandlerFactory noRetryErrorHandler() {
        return DefaultErrorHandlerBuilder.defaultErrorHandler()
            .maximumRedeliveries(0)
            .logStackTrace(true);
    }
    
    @Produces
    @Named("deadLetterChannel")
    public ErrorHandlerFactory deadLetterChannel() {
        return DeadLetterChannelBuilder.deadLetterChannel("direct:dead-letter-queue")
            .maximumRedeliveries(0)
            .useOriginalMessage()
            .onPrepareFailure(exchange -> {
                // Log to audit before sending to DLQ
                log.error("Sending exchange {} to dead letter queue", exchange.getExchangeId());
            });
    }
}