package tech.kayys.wayang.billing.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import tech.kayys.wayang.billing.dto.AnomalySeverity;
import tech.kayys.wayang.billing.dto.AnomalyType;
import tech.kayys.wayang.organization.domain.Organization;

@Entity
@Table(name = "ai_anomalies")
public class Anomaly {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID anomalyId;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    public Organization organization;
    
    @Column(name = "detected_at")
    public Instant detectedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type")
    public AnomalyType anomalyType;
    
    @Column(name = "severity")
    @Enumerated(EnumType.STRING)
    public AnomalySeverity severity;
    
    @Column(name = "description")
    public String description;
    
    @Column(name = "metric_name")
    public String metricName;
    
    @Column(name = "expected_value")
    public double expectedValue;
    
    @Column(name = "actual_value")
    public double actualValue;
    
    @Column(name = "deviation_score")
    public double deviationScore;
    
    @Column(name = "is_investigated")
    public boolean isInvestigated = false;
    
    @Column(name = "is_false_positive")
    public Boolean isFalsePositive;
    
    @Column(name = "context", columnDefinition = "jsonb")
    public Map<String, Object> context;
}

