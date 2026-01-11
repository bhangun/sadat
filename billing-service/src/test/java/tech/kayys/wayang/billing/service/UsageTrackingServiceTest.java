package tech.kayys.wayang.billing.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.billing.dto.RecordUsageRequest;
import tech.kayys.wayang.billing.model.UsageType;

class UsageTrackingServiceTest {

    private UsageTrackingService usageTrackingService;
    private Organization organization;
    private RecordUsageRequest usageRequest;

    @BeforeEach
    void setUp() {
        usageTrackingService = new UsageTrackingService();

        // Setup test organization
        organization = new Organization();
        organization.organizationId = UUID.randomUUID();
        organization.name = "Test Organization";

        // Setup test usage request
        usageRequest = new RecordUsageRequest(
                organization.organizationId.toString(),
                UsageType.API_CALL,
                100L,
                "calls",
                "test-resource",
                Map.of());
    }

    @Test
    void testRecordUsage_ValidRequest_RecordsUsage() {
        // When
        var result = usageTrackingService.recordUsage(usageRequest);

        // Then
        assertNotNull(result);
    }

    @Test
    void testGetCurrentPeriodUsage_ValidOrganization_ReturnsUsage() {
        // Given
        YearMonth yearMonth = YearMonth.now();

        // When
        var result = usageTrackingService.getCurrentPeriodUsage(organization.organizationId, yearMonth);

        // Then
        assertNotNull(result);
    }

    @Test
    void testGetUsageHistory_ValidOrganization_ReturnsHistory() {
        // Given
        YearMonth start = YearMonth.now().minusMonths(1);
        YearMonth end = YearMonth.now();

        // When
        var result = usageTrackingService.getUsageHistory(organization.organizationId, start, end);

        // Then
        assertNotNull(result);
    }

    @Test
    void testGetQuotaStatus_ValidOrganization_ReturnsStatus() {
        // When
        var result = usageTrackingService.getQuotaStatus(organization.organizationId);

        // Then
        assertNotNull(result);
    }
}