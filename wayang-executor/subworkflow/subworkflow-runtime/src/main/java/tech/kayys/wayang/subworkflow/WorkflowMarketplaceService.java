package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Workflow Marketplace
 */
interface WorkflowMarketplaceService {

    /**
     * Publish workflow to marketplace
     */
    Uni<MarketplaceListing> publishWorkflow(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        ListingDetails details
    );

    /**
     * Search marketplace
     */
    Uni<List<MarketplaceListing>> searchMarketplace(
        String query,
        List<String> categories,
        int page,
        int size
    );

    /**
     * Install workflow from marketplace
     */
    Uni<tech.kayys.silat.core.domain.WorkflowDefinition> installFromMarketplace(
        String listingId,
        tech.kayys.silat.core.domain.TenantId tenantId
    );

    /**
     * Rate and review
     */
    Uni<Void> rateWorkflow(
        String listingId,
        int rating,
        String review
    );
}