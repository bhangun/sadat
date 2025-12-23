package tech.kayys.wayang.workflow.service.backup;

import java.time.Instant;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * System configuration entity
 */
@Entity
@Table(name = "system_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {
    @Id
    private String id;

    @Column(name = "config_key")
    private String key;

    @Column(name = "config_value")
    private String value;

    @Column(name = "modified_at")
    private Instant modifiedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private Map<String, Object> metadata;

    @Column(name = "tenant_id")
    private String tenantId;

    public void setLastModified(Instant lastModified) {
        this.modifiedAt = lastModified;
    }

    public String getConfigKey() {
        return key;
    }
}
