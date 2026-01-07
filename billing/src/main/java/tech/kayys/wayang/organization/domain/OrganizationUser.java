package tech.kayys.wayang.organization.domain;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;
import tech.kayys.wayang.billing.model.UserRole;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "organization_users", indexes = {
    @Index(name = "idx_user_org", columnList = "organization_id"),
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_active", columnList = "active")
})
public class OrganizationUser extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    public UUID userId;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    public Organization organization;
    
    @Column(name = "email", nullable = false)
    public String email;
    
    @Column(name = "first_name")
    public String firstName;
    
    @Column(name = "last_name")
    public String lastName;
    
    @Column(name = "password_hash")
    public String passwordHash;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    public UserRole role = UserRole.MEMBER;
    
    @Column(name = "permissions", columnDefinition = "jsonb")
    public List<String> permissions = new ArrayList<>();
    
    @Column(name = "active")
    public boolean active = true;
    
    @Column(name = "email_verified")
    public boolean emailVerified = false;
    
    @Column(name = "mfa_enabled")
    public boolean mfaEnabled = false;
    
    @Column(name = "mfa_secret")
    public String mfaSecret;
    
    @Column(name = "last_login")
    public Instant lastLogin;
    
    @Column(name = "created_at")
    public Instant createdAt;
    
    @Column(name = "updated_at")
    public Instant updatedAt;
    
    @Column(name = "invited_by")
    public UUID invitedBy;
    
    @Column(name = "accepted_invite_at")
    public Instant acceptedInviteAt;
    
    @Column(name = "sso_provisioned")
    public boolean ssoProvisioned = false;
    
    @Column(name = "external_id")
    public String externalId;
    
    @Column(name = "metadata", columnDefinition = "jsonb")
    public Map<String, Object> metadata = new HashMap<>();
    
    // Helper methods
    
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return email;
    }
    
    public boolean hasPermission(String permission) {
        return permissions.contains(permission) || role == UserRole.OWNER;
    }
    
    public boolean isAdmin() {
        return role == UserRole.OWNER || role == UserRole.ADMIN;
    }
}
