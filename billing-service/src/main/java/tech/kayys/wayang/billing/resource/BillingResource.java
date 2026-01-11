package tech.kayys.wayang.billing.resource;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.billing.dto.BillingSummary;
import tech.kayys.wayang.billing.dto.CreateCreditNoteRequest;
import tech.kayys.wayang.billing.dto.PaymentRequest;
import tech.kayys.wayang.billing.dto.PlatformOverview;
import tech.kayys.wayang.billing.dto.RevenueMetrics;
import tech.kayys.wayang.billing.dto.TenantHealthOverview;
import tech.kayys.wayang.billing.dto.UsageMetrics;
import tech.kayys.wayang.billing.service.AdminDashboardService;
import tech.kayys.wayang.billing.service.BillingService;
import tech.kayys.wayang.invoice.domain.Invoice;
import tech.kayys.wayang.invoice.model.InvoiceStatus;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.payment.dto.PaymentResult;

@Path("/api/v1/management/billing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@SecurityRequirement(name = "bearer")
@Tag(name = "Billing", description = "Billing and invoicing")
public class BillingResource {

    @Inject
    BillingService billingService;

    /**
     * Get billing summary
     */
    @GET
    @Path("/organizations/{organizationId}/summary")
    @Operation(summary = "Get billing summary")
    public Uni<BillingSummary> getBillingSummary(
            @PathParam("organizationId") UUID organizationId) {

        return Organization.<Organization>findById(organizationId)
                .flatMap(billingService::getBillingSummary);
    }

    /**
     * List invoices
     */
    @GET
    @Path("/organizations/{organizationId}/invoices")
    @Operation(summary = "List invoices")
    public Uni<List<Invoice>> listInvoices(
            @PathParam("organizationId") UUID organizationId,
            @QueryParam("status") InvoiceStatus status,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        return Invoice.<Invoice>find(
                status != null ? "organization.organizationId = ?1 and status = ?2"
                        : "organization.organizationId = ?1",
                status != null ? List.of(organizationId, status).toArray() : organizationId).page(page, size).list();
    }

    /**
     * Get invoice
     */
    @GET
    @Path("/invoices/{invoiceId}")
    @Operation(summary = "Get invoice")
    public Uni<RestResponse<Invoice>> getInvoice(
            @PathParam("invoiceId") UUID invoiceId) {

        return Invoice.<Invoice>findById(invoiceId)
                .map(inv -> inv != null ? RestResponse.ok(inv) : RestResponse.notFound());
    }

    /**
     * Pay invoice
     */
    @POST
    @Path("/invoices/{invoiceId}/pay")
    @Operation(summary = "Pay invoice")
    public Uni<RestResponse<PaymentResult>> payInvoice(
            @PathParam("invoiceId") UUID invoiceId,
            @Valid PaymentRequest request) {

        return Invoice.<Invoice>findById(invoiceId)
                .flatMap(invoice -> {
                    if (invoice == null) {
                        return Uni.createFrom().item(
                                RestResponse.<PaymentResult>notFound());
                    }

                    return billingService.processPayment(invoice)
                            .map(RestResponse::ok);
                });
    }

    /**
     * Download invoice PDF
     */
    @GET
    @Path("/invoices/{invoiceId}/pdf")
    @Produces("application/pdf")
    @Operation(summary = "Download invoice PDF")
    public Uni<RestResponse<byte[]>> downloadInvoicePdf(
            @PathParam("invoiceId") UUID invoiceId) {

        // Generate and return PDF
        return Uni.createFrom().item(
                RestResponse.notImplemented());
    }

    /**
     * Create credit note
     */
    @POST
    @Path("/invoices/{invoiceId}/credit-note")
    @Operation(summary = "Create credit note")
    public Uni<RestResponse<Invoice>> createCreditNote(
            @PathParam("invoiceId") UUID invoiceId,
            @Valid CreateCreditNoteRequest request) {

        return Invoice.<Invoice>findById(invoiceId)
                .flatMap(invoice -> billingService.createCreditNote(
                        invoice,
                        request.amount(),
                        request.reason()))
                .map(RestResponse::ok);
    }
}
