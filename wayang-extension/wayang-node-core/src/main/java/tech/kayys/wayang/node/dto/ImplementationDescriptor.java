
/**
 * Implementation details
 */
record ImplementationDescriptor(
    @NotNull ImplementationType type,
    @NotBlank String coordinate,
    @NotBlank String digest,
    Map<String, String> additionalInfo
) {
    public ImplementationDescriptor {
        additionalInfo = additionalInfo != null ? Map.copyOf(additionalInfo) : Map.of();
    }
}
