package tech.kayys.wayang.billing.service;

import java.math.BigDecimal;
import java.util.UUID;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.billing.dto.RevenueShare;
import tech.kayys.wayang.marketplace.domain.MarketplaceListing;

/**
 * Revenue share calculator
 */
@ApplicationScoped
public class RevenueShareCalculator {
    
    public Uni<RevenueShare> calculateMonthlyRevenue(
            UUID publisherOrgId,
            java.time.YearMonth month) {
        
        return MarketplaceListing.<MarketplaceListing>find(
            "publisher.organizationId = ?1", publisherOrgId
        ).list()
        .map(listings -> {
            BigDecimal totalRevenue = BigDecimal.ZERO;
            BigDecimal platformFee = BigDecimal.ZERO;
            BigDecimal publisherShare = BigDecimal.ZERO;
            
            for (MarketplaceListing listing : listings) {
                BigDecimal listingRevenue = listing.totalRevenue;
                BigDecimal share = listingRevenue.multiply(
                    listing.revenueSharePercent.divide(
                        BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP
                    )
                );
                
                totalRevenue = totalRevenue.add(listingRevenue);
                publisherShare = publisherShare.add(share);
                platformFee = platformFee.add(listingRevenue.subtract(share));
            }
            
            return new RevenueShare(
                publisherOrgId,
                month,
                totalRevenue,
                publisherShare,
                platformFee,
                listings.size()
            );
        });
    }
}