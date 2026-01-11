package tech.kayys.wayang.mcp.generator.testing;

import java.util.List;

public class TestConfiguration {

    private boolean enableContractTesting = true;
    private boolean enableLoadTesting = true;
    private boolean enableSecurityTesting = true;
    private boolean enableIntegrationTesting = true;
    private boolean enableChaosTesting = false;

    // Contract testing config
    private List<String> contractTestFramework = List.of("pact", "openapi");

    // Load testing config
    private List<String> loadTestFramework = List.of("jmeter", "k6");
    private int loadTestUsers = 10;
    private int loadTestDurationSeconds = 60;
    private int loadTestRampUpSeconds = 10;

    // Security testing config
    private List<String> securityTestCategories = List.of("OWASP-API", "Authentication", "Authorization");
    private boolean enablePenetrationTesting = false;

    // Integration testing config
    private String testEnvironment = "staging";
    private boolean enableDatabaseTesting = true;
    private boolean enableExternalServiceTesting = true;

    // Chaos testing config
    private List<String> chaosExperiments = List.of("network-latency", "service-failure", "resource-exhaustion");

    // Getters and setters
    public boolean isEnableContractTesting() { return enableContractTesting; }
    public void setEnableContractTesting(boolean enableContractTesting) { this.enableContractTesting = enableContractTesting; }

    public boolean isEnableLoadTesting() { return enableLoadTesting; }
    public void setEnableLoadTesting(boolean enableLoadTesting) { this.enableLoadTesting = enableLoadTesting; }

    public boolean isEnableSecurityTesting() { return enableSecurityTesting; }
    public void setEnableSecurityTesting(boolean enableSecurityTesting) { this.enableSecurityTesting = enableSecurityTesting; }

    public boolean isEnableIntegrationTesting() { return enableIntegrationTesting; }
    public void setEnableIntegrationTesting(boolean enableIntegrationTesting) { this.enableIntegrationTesting = enableIntegrationTesting; }

    public boolean isEnableChaosTesting() { return enableChaosTesting; }
    public void setEnableChaosTesting(boolean enableChaosTesting) { this.enableChaosTesting = enableChaosTesting; }

    public List<String> getContractTestFramework() { return contractTestFramework; }
    public void setContractTestFramework(List<String> contractTestFramework) { this.contractTestFramework = contractTestFramework; }

    public List<String> getLoadTestFramework() { return loadTestFramework; }
    public void setLoadTestFramework(List<String> loadTestFramework) { this.loadTestFramework = loadTestFramework; }

    public int getLoadTestUsers() { return loadTestUsers; }
    public void setLoadTestUsers(int loadTestUsers) { this.loadTestUsers = loadTestUsers; }

    public int getLoadTestDurationSeconds() { return loadTestDurationSeconds; }
    public void setLoadTestDurationSeconds(int loadTestDurationSeconds) { this.loadTestDurationSeconds = loadTestDurationSeconds; }

    public int getLoadTestRampUpSeconds() { return loadTestRampUpSeconds; }
    public void setLoadTestRampUpSeconds(int loadTestRampUpSeconds) { this.loadTestRampUpSeconds = loadTestRampUpSeconds; }

    public List<String> getSecurityTestCategories() { return securityTestCategories; }
    public void setSecurityTestCategories(List<String> securityTestCategories) { this.securityTestCategories = securityTestCategories; }

    public boolean isEnablePenetrationTesting() { return enablePenetrationTesting; }
    public void setEnablePenetrationTesting(boolean enablePenetrationTesting) { this.enablePenetrationTesting = enablePenetrationTesting; }

    public String getTestEnvironment() { return testEnvironment; }
    public void setTestEnvironment(String testEnvironment) { this.testEnvironment = testEnvironment; }

    public boolean isEnableDatabaseTesting() { return enableDatabaseTesting; }
    public void setEnableDatabaseTesting(boolean enableDatabaseTesting) { this.enableDatabaseTesting = enableDatabaseTesting; }

    public boolean isEnableExternalServiceTesting() { return enableExternalServiceTesting; }
    public void setEnableExternalServiceTesting(boolean enableExternalServiceTesting) { this.enableExternalServiceTesting = enableExternalServiceTesting; }

    public List<String> getChaosExperiments() { return chaosExperiments; }
    public void setChaosExperiments(List<String> chaosExperiments) { this.chaosExperiments = chaosExperiments; }
}