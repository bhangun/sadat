/**
 * Value object for tenant identification and isolation
 */
public record TenantId(String value) {
    public TenantId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be empty");
        }
    }
}
