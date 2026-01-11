package tech.kayys.wayang.billing.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tech.kayys.wayang.billing.model.PlanTier;
import tech.kayys.wayang.billing.model.PricingModel;
import tech.kayys.wayang.billing.model.Requirements;
import tech.kayys.wayang.billing.model.WorkflowCategory;

/**
 * Request DTO for publishing a workflow to the marketplace.
 * Contains all the necessary information to create a new workflow listing.
 */
public record PublishWorkflowRequest(
    @NotBlank(message = "Workflow name is required")
    @Size(max = 200, message = "Workflow name must not exceed 200 characters")
    String name,

    @NotBlank(message = "Tagline is required")
    @Size(max = 500, message = "Tagline must not exceed 500 characters")
    String tagline,

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,

    @NotNull(message = "Category is required")
    WorkflowCategory category,

    @Size(max = 10, message = "Tags list must not exceed 10 items")
    List<@NotBlank(message = "Tag cannot be blank") @Size(max = 50, message = "Tag must not exceed 50 characters") String> tags,

    @NotNull(message = "Pricing model is required")
    PricingModel pricingModel,

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    BigDecimal price,

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    String currency,

    @Size(max = 500, message = "Icon URL must not exceed 500 characters")
    String iconUrl,

    @Size(max = 10, message = "Screenshots list must not exceed 10 items")
    List<@Size(max = 500, message = "Screenshot URL must not exceed 500 characters") String> screenshots,

    @Size(max = 500, message = "Demo video URL must not exceed 500 characters")
    String demoVideoUrl,

    @Valid
    @NotNull(message = "Requirements are required")
    Requirements requirements,

    List<@NotNull(message = "Compatible plan cannot be null") PlanTier> compatiblePlans
) {}
