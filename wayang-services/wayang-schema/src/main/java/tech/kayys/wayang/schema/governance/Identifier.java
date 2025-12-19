package tech.kayys.wayang.schema.governance;

public class Identifier {
    private String value;

    public Identifier() {
    }

    public Identifier(String value) {
        if (!value.matches("^[a-z0-9_.-]+(/[a-z0-9_.-]+)?$")) {
            throw new IllegalArgumentException("Invalid identifier format");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
