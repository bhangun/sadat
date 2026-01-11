package tech.kayys.wayang.mcp.parser;

import io.quarkus.test.junit.QuarkusTest;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SchemaConverterTest {

    @Inject
    SchemaConverter schemaConverter;

    @Test
    void testConvertStringSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("string");
        schema.setDescription("A string field");

        Map<String, Object> converted = schemaConverter.convert(schema);

        assertNotNull(converted);
        assertEquals("string", converted.get("type"));
        assertEquals("A string field", converted.get("description"));
    }

    @Test
    void testConvertIntegerSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("integer");
        schema.setFormat("int32");

        Map<String, Object> converted = schemaConverter.convert(schema);

        assertNotNull(converted);
        assertEquals("integer", converted.get("type"));
        assertEquals("int32", converted.get("format"));
    }

    @Test
    void testConvertNumberSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("number");
        schema.setFormat("double");

        Map<String, Object> converted = schemaConverter.convert(schema);

        assertNotNull(converted);
        assertEquals("number", converted.get("type"));
    }

    @Test
    void testConvertBooleanSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("boolean");

        Map<String, Object> converted = schemaConverter.convert(schema);

        assertNotNull(converted);
        assertEquals("boolean", converted.get("type"));
    }

    @Test
    void testConvertArraySchema() {
        Schema<?> itemSchema = new Schema<>();
        itemSchema.setType("string");

        Schema<?> arraySchema = new Schema<>();
        arraySchema.setType("array");
        arraySchema.setItems(itemSchema);

        Map<String, Object> converted = schemaConverter.convert(arraySchema);

        assertNotNull(converted);
        assertEquals("array", converted.get("type"));
        assertNotNull(converted.get("items"));
    }

    @Test
    void testConvertObjectSchema() {
        Schema<?> nameSchema = new Schema<>();
        nameSchema.setType("string");

        Schema<?> ageSchema = new Schema<>();
        ageSchema.setType("integer");

        Schema<?> objectSchema = new Schema<>();
        objectSchema.setType("object");
        objectSchema.setProperties(Map.of(
                "name", nameSchema,
                "age", ageSchema));
        objectSchema.setRequired(java.util.List.of("name"));

        Map<String, Object> converted = schemaConverter.convert(objectSchema);

        assertNotNull(converted);
        assertEquals("object", converted.get("type"));
        assertNotNull(converted.get("properties"));
        assertTrue(converted.containsKey("required"));
    }

    @Test
    void testConvertSchemaWithEnum() {
        Schema schema = new Schema();
        schema.setType("string");
        schema.setEnum(java.util.List.of("active", "inactive", "pending"));

        Map<String, Object> converted = schemaConverter.convert(schema);

        assertNotNull(converted);
        assertEquals("string", converted.get("type"));
        assertNotNull(converted.get("enum"));
    }

    @Test
    void testConvertSchemaWithConstraints() {
        Schema<?> schema = new Schema<>();
        schema.setType("string");
        schema.setMinLength(5);
        schema.setMaxLength(100);
        schema.setPattern("^[a-zA-Z]+$");

        Map<String, Object> converted = schemaConverter.convert(schema);

        assertNotNull(converted);
        assertEquals("string", converted.get("type"));
        assertTrue(converted.containsKey("minLength"));
        assertTrue(converted.containsKey("maxLength"));
        assertTrue(converted.containsKey("pattern"));
    }

    @Test
    void testConvertSchemaWithDefault() {
        Schema<?> schema = new Schema<>();
        schema.setType("string");
        schema.setDefault("default-value");

        Map<String, Object> converted = schemaConverter.convert(schema);

        assertNotNull(converted);
        assertEquals("default-value", converted.get("default"));
    }

    @Test
    void testConvertNullSchema() {
        Map<String, Object> converted = schemaConverter.convert(null);

        assertNotNull(converted);
        assertTrue(converted.isEmpty() || converted.get("type") == null);
    }
}
