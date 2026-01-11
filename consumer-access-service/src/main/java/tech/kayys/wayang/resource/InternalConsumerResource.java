package tech.kayys.wayang;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.service.EncryptionService;

import java.time.Instant;

@Path("/internal/consumers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InternalConsumerResource {

    @Inject
    EncryptionService encryptionService;

    @POST
    @Transactional
    public Response register(InternalRegistrationRequest request) {
        if (request.consumer == null || (request.consumer.id == null && request.consumer.tenantId == null)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Consumer and ID/TenantID are required").build();
        }

        Consumer registration = request.consumer;
        Consumer consumer = Consumer.findById(registration.id);
        if (consumer == null) {
            consumer = registration;
            if (consumer.createdAt == null) {
                consumer.createdAt = Instant.now();
            }
            consumer.status = Consumer.ConsumerStatus.ACTIVE;
            consumer.persist();
        } else {
            // Update existing
            consumer.name = registration.name;
            consumer.legalName = registration.legalName;
            consumer.email = registration.email;
            consumer.billingEmail = registration.billingEmail;
            consumer.address = registration.address;
            consumer.country = registration.country;
            consumer.tenantId = registration.tenantId;
        }

        if (request.ktp != null || request.taxId != null) {
            ConsumerSensitiveData sensitive = ConsumerSensitiveData.findById(consumer.id);
            if (sensitive == null) {
                sensitive = new ConsumerSensitiveData();
                sensitive.consumer = consumer;
                sensitive.consumerId = consumer.id;
            }
            if (request.ktp != null) sensitive.ktpEncrypted = encryptionService.encrypt(request.ktp);
            if (request.taxId != null) sensitive.taxIdEncrypted = encryptionService.encrypt(request.taxId);
            sensitive.persist();
        }

        return Response.ok(consumer).build();
    }

    public static class InternalRegistrationRequest {
        public Consumer consumer;
        public String ktp;
        public String taxId;
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") String id) {
        Consumer consumer = Consumer.findById(id);
        if (consumer == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(consumer).build();
    }

    @GET
    @Path("/tenant/{tenantId}")
    public Response getByTenant(@PathParam("tenantId") String tenantId) {
        Consumer consumer = Consumer.find("tenantId", tenantId).firstResult();
        if (consumer == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(consumer).build();
    }

    @GET
    @Path("/{id}/sensitive/decrypted")
    public Response getSensitiveDecrypted(@PathParam("id") String id) {
        ConsumerSensitiveData sensitive = ConsumerSensitiveData.findById(id);
        if (sensitive == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        DecryptedSensitiveData response = new DecryptedSensitiveData();
        response.ktp = encryptionService.decrypt(sensitive.ktpEncrypted);
        response.taxId = encryptionService.decrypt(sensitive.taxIdEncrypted);
        
        return Response.ok(response).build();
    }

    public static class DecryptedSensitiveData {
        public String ktp;
        public String taxId;
    }
}
