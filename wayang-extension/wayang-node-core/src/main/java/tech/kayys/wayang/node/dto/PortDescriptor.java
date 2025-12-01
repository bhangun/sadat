
/**
 * Port descriptor for inputs/outputs
 */
record PortDescriptor(
    @NotBlank String name,
    @NotBlank String type,
    boolean required,
    Object defaultValue,
    String description,
    Map<String, Object> schema
) {
    public PortDescriptor {
        schema = schema != null ? Map.copyOf(schema) : Map.of();
    }
}