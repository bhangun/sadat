package tech.kayys.wayang;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.service.EncryptionService;

import java.time.Instant;
import java.util.List;

@Path("/admin/consumers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConsumerAdminResource {

    @Inject
    EncryptionService encryptionService;

    @POST
    @Transactional
    public Response create(Consumer consumer) {
        if (consumer.id == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("ID is required").build();
        }
        consumer.createdAt = Instant.now();
        consumer.persist();
        return Response.status(Response.Status.CREATED).entity(consumer).build();
    }

    @GET
    public List<Consumer> list() {
        return Consumer.listAll();
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

    @POST
    @Path("/{id}/sensitive")
    @Transactional
    public Response addSensitiveData(@PathParam("id") String id, SensitiveDataRequest request) {
        Consumer consumer = Consumer.findById(id);
        if (consumer == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        ConsumerSensitiveData sensitive = ConsumerSensitiveData.findById(id);
        if (sensitive == null) {
            sensitive = new ConsumerSensitiveData();
            sensitive.consumer = consumer;
        }

        if (request.ktp != null) {
            sensitive.ktpEncrypted = encryptionService.encrypt(request.ktp);
        }
        if (request.taxId != null) {
            sensitive.taxIdEncrypted = encryptionService.encrypt(request.taxId);
        }
        
        sensitive.verificationLevel = request.verificationLevel;
        sensitive.verifiedAt = Instant.now();
        
        sensitive.persist();
        return Response.ok().build();
    }

    @GET
    @Path("/{id}/sensitive")
    public Response getSensitiveData(@PathParam("id") String id) {
        ConsumerSensitiveData sensitive = ConsumerSensitiveData.findById(id);
        if (sensitive == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Masking logic as per concern.md
        SensitiveDataResponse response = new SensitiveDataResponse();
        response.ktpMasked = mask(encryptionService.decrypt(sensitive.ktpEncrypted));
        response.taxIdMasked = mask(encryptionService.decrypt(sensitive.taxIdEncrypted));
        response.verificationLevel = sensitive.verificationLevel;
        response.verifiedAt = sensitive.verifiedAt;

        return Response.ok(response).build();
    }

    private String mask(String value) {
        if (value == null || value.length() < 4) return "****";
        return "****" + value.substring(value.length() - 4);
    }

    public static class SensitiveDataRequest {
        public String ktp;
        public String taxId;
        public String verificationLevel;
    }

    public static class SensitiveDataResponse {
        public String ktpMasked;
        public String taxIdMasked;
        public String verificationLevel;
        public Instant verifiedAt;
    }
}
