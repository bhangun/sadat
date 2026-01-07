package tech.kayys.wayang.billing.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.kayys.wayang.billing.model.UsageType;
import tech.kayys.wayang.billing.domain.Address;
import tech.kayys.wayang.organization.domain.Organization;
class CostCalculatorTest {

    private CostCalculator costCalculator;

    private Organization organization;

    @BeforeEach
    void setUp() {
        costCalculator = new CostCalculator();

        // Setup test organization
        organization = new Organization();
        organization.organizationId = UUID.randomUUID();
        organization.tenantId = UUID.randomUUID().toString();
        organization.name = "Test Organization";

        // Create billing address
        Address address = new Address();
        address.country = "US";
        organization.billingAddress = address;
    }

    @Test
    void testGetUnitPrice_ValidUsageType_ReturnsBasePrice() {
        // Given
        UsageType usageType = UsageType.WORKFLOW_EXECUTION;

        // When
        BigDecimal unitPrice = costCalculator.getUnitPrice(organization, usageType);

        // Then
        assertEquals(BigDecimal.valueOf(0.01), unitPrice);
    }

    @Test
    void testGetUnitPrice_InvalidUsageType_ReturnsZero() {
        // When
        BigDecimal unitPrice = costCalculator.getUnitPrice(organization, null);

        // Then
        assertEquals(BigDecimal.ZERO, unitPrice);
    }

    @Test
    void testCalculateCost_NoTieredPricing_ReturnsSimpleCalculation() {
        // Given
        UsageType usageType = UsageType.API_CALL;
        long quantity = 1000;

        // When
        BigDecimal cost = costCalculator.calculateCost(organization, usageType, quantity, 0);

        // Then
        assertEquals(BigDecimal.valueOf(1.00), cost); // 1000 * 0.001
    }

    @Test
    void testCalculateCost_TieredPricing_FirstTier() {
        // Given
        UsageType usageType = UsageType.WORKFLOW_EXECUTION;
        long quantity = 500; // Within first tier (0-1000)

        // When
        BigDecimal cost = costCalculator.calculateCost(organization, usageType, quantity, 0);

        // Then
        assertEquals(BigDecimal.valueOf(5.00), cost); // 500 * 0.01
    }

    @Test
    void testCalculateCost_TieredPricing_SecondTier() {
        // Given
        UsageType usageType = UsageType.WORKFLOW_EXECUTION;
        long quantity = 2000;
        long previousUsage = 1000; // Start in second tier

        // When
        BigDecimal cost = costCalculator.calculateCost(organization, usageType, quantity, previousUsage);

        // Then
        // 1000 units at $0.008 = $8.00
        assertEquals(BigDecimal.valueOf(16.00), cost); // 2000 * 0.008
    }

    @Test
    void testCalculateCost_TieredPricing_MultipleTiers() {
        // Given
        UsageType usageType = UsageType.WORKFLOW_EXECUTION;
        long quantity = 15000;
        long previousUsage = 500; // Start in first tier

        // When
        BigDecimal cost = costCalculator.calculateCost(organization, usageType, quantity, previousUsage);

        // Then
        // 500 units at $0.01 = $5.00
        // 9500 units at $0.008 = $76.00
        // 5000 units at $0.005 = $25.00
        // Total: $106.00
        assertEquals(BigDecimal.valueOf(106.00), cost);
    }

    @Test
    void testCalculateCost_AI_TokenUsage_TieredPricing() {
        // Given
        UsageType usageType = UsageType.AI_TOKEN_USAGE;
        long quantity = 500000; // Within second tier

        // When
        BigDecimal cost = costCalculator.calculateCost(organization, usageType, quantity, 0);

        // Then
        // First 100000 at $0.00002 = $2.00
        // Next 400000 at $0.000015 = $6.00
        // Total: $8.00
        assertEquals(BigDecimal.valueOf(8.00), cost);
    }

    @Test
    void testGetPricingTier_ValidUsageType() {
        // When
        var tier = costCalculator.getPricingTier(UsageType.WORKFLOW_EXECUTION);

        // Then
        assertNotNull(tier);
        assertEquals(BigDecimal.valueOf(0.01), tier.basePrice());
        assertFalse(tier.brackets().isEmpty());
    }

    @Test
    void testGetPricingTier_InvalidUsageType() {
        // When
        var tier = costCalculator.getPricingTier(null);

        // Then
        assertNull(tier);
    }

    @Test
    void testHasTieredPricing_WorkflowExecution_ReturnsTrue() {
        // When
        boolean hasTiered = costCalculator.hasTieredPricing(UsageType.WORKFLOW_EXECUTION);

        // Then
        assertTrue(hasTiered);
    }

    @Test
    void testHasTieredPricing_APICall_ReturnsFalse() {
        // When
        boolean hasTiered = costCalculator.hasTieredPricing(UsageType.API_CALL);

        // Then
        assertFalse(hasTiered);
    }

    @Test
    void testGetEffectivePrice_NoTiers_ReturnsBasePrice() {
        // When
        BigDecimal price = costCalculator.getEffectivePrice(UsageType.API_CALL, 1000);

        // Then
        assertEquals(BigDecimal.valueOf(0.001), price);
    }

    @Test
    void testGetEffectivePrice_FirstTier() {
        // When
        BigDecimal price = costCalculator.getEffectivePrice(UsageType.WORKFLOW_EXECUTION, 500);

        // Then
        assertEquals(BigDecimal.valueOf(0.01), price);
    }

    @Test
    void testGetEffectivePrice_SecondTier() {
        // When
        BigDecimal price = costCalculator.getEffectivePrice(UsageType.WORKFLOW_EXECUTION, 5000);

        // Then
        assertEquals(BigDecimal.valueOf(0.008), price);
    }

    @Test
    void testGetEffectivePrice_BeyondLastTier() {
        // When
        BigDecimal price = costCalculator.getEffectivePrice(UsageType.WORKFLOW_EXECUTION, 50000);

        // Then
        assertEquals(BigDecimal.valueOf(0.005), price);
    }

    @Test
    void testGetEffectivePrice_InvalidUsageType() {
        // When
        BigDecimal price = costCalculator.getEffectivePrice(null, 1000);

        // Then
        assertEquals(BigDecimal.ZERO, price);
    }
}