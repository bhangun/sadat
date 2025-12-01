/**
 * Property descriptor for node configuration
 */
record PropertyDescriptor(
    @NotBlank String name,
    @NotBlank String type,
    Object defaultValue,
    boolean required,
    String description,
    Map<String, Object> validation
) {
    public PropertyDescriptor {
        validation = validation != null ? Map.copyOf(validation) : Map.of();
    }
}