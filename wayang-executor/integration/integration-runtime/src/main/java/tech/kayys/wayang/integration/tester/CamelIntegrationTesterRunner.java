package tech.kayys.wayang.integration.tester;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.silat.executor.camel.CamelIntegrationExecutor;

/**
 * Runner for Camel Integration Tester
 */
@QuarkusMain
@ApplicationScoped
public class CamelIntegrationTesterRunner implements QuarkusApplication {

    private static final Logger LOG = LoggerFactory.getLogger(CamelIntegrationTesterRunner.class);

    @Inject
    CamelIntegrationExecutor executor;

    @Inject
    CamelContext camelContext;

    @Inject
    ProducerTemplate producerTemplate;

    @Override
    public int run(String... args) {
        LOG.info("Starting Camel Integration Executor Tester...");

        try {
            CamelIntegrationTester tester = new CamelIntegrationTester(executor, camelContext, producerTemplate);
            CamelIntegrationTester.TestResults results = tester.runAllTests();

            LOG.info("=== TEST RESULTS SUMMARY ===");
            LOG.info("Total Tests: {}", results.getTotalCount());
            LOG.info("Passed: {}", results.getPassedCount());
            LOG.info("Failed: {}", results.getFailedCount());
            LOG.info("Duration: {}ms", results.getDuration());
            LOG.info("Overall Status: {}", results.isAllPassed() ? "ALL PASSED" : "SOME FAILED");

            if (!results.isAllPassed()) {
                LOG.error("Some tests failed. Check individual test results above.");
                return 1; // Return error code
            } else {
                LOG.info("All tests passed successfully!");
                return 0; // Return success code
            }

        } catch (Exception e) {
            LOG.error("Error running tests", e);
            return 1; // Return error code
        }
    }
}