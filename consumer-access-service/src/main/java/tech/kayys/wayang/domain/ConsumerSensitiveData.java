package tech.kayys.wayang;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "consumer_sensitive_data")
public class ConsumerSensitiveData extends PanacheEntityBase {

    @Id
    @Column(name = "consumer_id")
    public String consumerId;

    @Column(name = "ktp_encrypted")
    public String ktpEncrypted;

    @Column(name = "tax_id_encrypted")
    public String taxIdEncrypted;

    public String verificationLevel;
    public Instant verifiedAt;

    @OneToOne
    @MapsId
    @JoinColumn(name = "consumer_id")
    public Consumer consumer;
}
