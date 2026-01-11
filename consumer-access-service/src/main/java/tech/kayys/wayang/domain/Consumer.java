package tech.kayys.wayang;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "consumers")
public class Consumer extends PanacheEntityBase {

    @Id
    public String id; // Linked to Keycloak user ID or generated

    @Enumerated(EnumType.STRING)
    public ConsumerType type;

    public String name;
    public String legalName;
    public String email;
    public String billingEmail;
    public String phone;

    public String country;
    public String address;

    @Column(name = "tenant_id", unique = true)
    public String tenantId;

    @Enumerated(EnumType.STRING)
    public ConsumerStatus status;

    public Instant createdAt;

    public enum ConsumerType {
        INDIVIDUAL, COMPANY
    }

    public enum ConsumerStatus {
        ACTIVE, PENDING, SUSPENDED, DELETED
    }
}
