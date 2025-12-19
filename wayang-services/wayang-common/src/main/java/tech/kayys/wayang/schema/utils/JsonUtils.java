package tech.kayys.wayang.schema.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

import tech.kayys.wayang.schema.node.PluginDescriptor;
import tech.kayys.wayang.schema.node.PluginImplementation;
import tech.kayys.wayang.schema.node.PortData;
import tech.kayys.wayang.schema.node.PortDescriptor;

/**
 * JSON utility class without external dependencies
 * Supports basic JSON parsing and serialization for the Unified Low-Code AI
 * Agent & Workflow Schema
 */
public class JsonUtils {

    /**
     * Parse JSON string into Java objects
     */
    public static Object parse(String json) {
        return parseValue(new JsonTokenizer(json));
    }

    /**
     * Convert Java object to JSON string
     */
    public static String stringify(Object obj) {
        StringBuilder sb = new StringBuilder();
        serializeValue(obj, sb, 0);
        return sb.toString();
    }

    /**
     * Parse JSON string into a specific class instance
     */
    public static <T> T parse(String json, Class<T> clazz) {
        Object parsed = parse(json);
        if (parsed instanceof Map) {
            return mapToObject((Map<String, Object>) parsed, clazz);
        }
        return clazz.cast(parsed);
    }

    // Basic JSON tokenizer
    private static class JsonTokenizer {
        private final String json;
        private int pos = 0;

        public JsonTokenizer(String json) {
            this.json = json;
        }

        public char peek() {
            return pos < json.length() ? json.charAt(pos) : '\0';
        }

        public char next() {
            return json.charAt(pos++);
        }

        public void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }

        public boolean hasMore() {
            return pos < json.length();
        }

        public String substring(int start) {
            return json.substring(start, pos);
        }
    }

    // Parse JSON value based on current token
    private static Object parseValue(JsonTokenizer tokenizer) {
        tokenizer.skipWhitespace();
        char c = tokenizer.peek();

        if (c == '{') {
            return parseObject(tokenizer);
        } else if (c == '[') {
            return parseArray(tokenizer);
        } else if (c == '"' || c == '\'') {
            return parseString(tokenizer);
        } else if (c == '-' || (c >= '0' && c <= '9')) {
            return parseNumber(tokenizer);
        } else if (c == 't' || c == 'f') {
            return parseBoolean(tokenizer);
        } else if (c == 'n') {
            return parseNull(tokenizer);
        }
        throw new RuntimeException("Invalid JSON at position " + tokenizer.pos);
    }

    private static Map<String, Object> parseObject(JsonTokenizer tokenizer) {
        Map<String, Object> obj = new LinkedHashMap<>();
        tokenizer.next(); // Skip '{'
        tokenizer.skipWhitespace();

        if (tokenizer.peek() == '}') {
            tokenizer.next();
            return obj;
        }

        while (true) {
            tokenizer.skipWhitespace();
            String key = parseString(tokenizer).toString();
            tokenizer.skipWhitespace();

            if (tokenizer.next() != ':') {
                throw new RuntimeException("Expected ':' after key");
            }

            tokenizer.skipWhitespace();
            Object value = parseValue(tokenizer);
            obj.put(key, value);

            tokenizer.skipWhitespace();
            char c = tokenizer.next();

            if (c == '}') {
                break;
            } else if (c != ',') {
                throw new RuntimeException("Expected ',' or '}' in object");
            }
        }

        return obj;
    }

    private static List<Object> parseArray(JsonTokenizer tokenizer) {
        List<Object> array = new ArrayList<>();
        tokenizer.next(); // Skip '['
        tokenizer.skipWhitespace();

        if (tokenizer.peek() == ']') {
            tokenizer.next();
            return array;
        }

        while (true) {
            tokenizer.skipWhitespace();
            Object value = parseValue(tokenizer);
            array.add(value);

            tokenizer.skipWhitespace();
            char c = tokenizer.next();

            if (c == ']') {
                break;
            } else if (c != ',') {
                throw new RuntimeException("Expected ',' or ']' in array");
            }
        }

        return array;
    }

    private static String parseString(JsonTokenizer tokenizer) {
        char quote = tokenizer.next();
        StringBuilder sb = new StringBuilder();

        while (tokenizer.hasMore()) {
            char c = tokenizer.next();

            if (c == quote) {
                break;
            } else if (c == '\\') {
                char escaped = tokenizer.next();
                switch (escaped) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\'':
                        sb.append('\'');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        String hex = tokenizer.json.substring(tokenizer.pos, tokenizer.pos + 4);
                        tokenizer.pos += 4;
                        sb.append((char) Integer.parseInt(hex, 16));
                        break;
                    default:
                        sb.append(escaped);
                }
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private static Object parseNumber(JsonTokenizer tokenizer) {
        int start = tokenizer.pos;

        while (tokenizer.hasMore()) {
            char c = tokenizer.peek();
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                tokenizer.next();
            } else {
                break;
            }
        }

        String numStr = tokenizer.substring(start);

        try {
            // Try parsing as integer first
            if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                return Double.parseDouble(numStr);
            } else {
                long longValue = Long.parseLong(numStr);
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                    return (int) longValue;
                }
                return longValue;
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid number: " + numStr);
        }
    }

    private static Boolean parseBoolean(JsonTokenizer tokenizer) {
        if (tokenizer.peek() == 't') {
            expect(tokenizer, "true");
            return true;
        } else {
            expect(tokenizer, "false");
            return false;
        }
    }

    private static Object parseNull(JsonTokenizer tokenizer) {
        expect(tokenizer, "null");
        return null;
    }

    private static void expect(JsonTokenizer tokenizer, String expected) {
        for (int i = 0; i < expected.length(); i++) {
            if (tokenizer.next() != expected.charAt(i)) {
                throw new RuntimeException("Expected '" + expected + "'");
            }
        }
    }

    // Serialization methods
    private static void serializeValue(Object value, StringBuilder sb, int indent) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            serializeString((String) value, sb);
        } else if (value instanceof Number) {
            sb.append(value.toString());
        } else if (value instanceof Boolean) {
            sb.append(value.toString());
        } else if (value instanceof Map) {
            serializeObject((Map<?, ?>) value, sb, indent);
        } else if (value instanceof Collection) {
            serializeArray((Collection<?>) value, sb, indent);
        } else if (value instanceof Object[]) {
            serializeArray(Arrays.asList((Object[]) value), sb, indent);
        } else if (value.getClass().isArray()) {
            serializePrimitiveArray(value, sb, indent);
        } else if (value instanceof Date) {
            serializeString(DateTimeFormatter.ISO_INSTANT.format(
                    ((Date) value).toInstant()), sb);
        } else if (value instanceof LocalDateTime) {
            serializeString(((LocalDateTime) value).format(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME), sb);
        } else if (value instanceof OffsetDateTime) {
            serializeString(((OffsetDateTime) value).format(
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME), sb);
        } else {
            // Try to convert object to map
            serializeObject(objectToMap(value), sb, indent);
        }
    }

    private static void serializeString(String str, StringBuilder sb) {
        sb.append('"');
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    private static void serializeObject(Map<?, ?> map, StringBuilder sb, int indent) {
        sb.append('{');
        boolean first = true;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            if (indent > 0) {
                sb.append('\n').append("  ".repeat(indent));
            }

            serializeString(entry.getKey().toString(), sb);
            sb.append(':');
            if (indent > 0) {
                sb.append(' ');
            }

            serializeValue(entry.getValue(), sb, indent + 1);
            first = false;
        }

        if (indent > 0 && !map.isEmpty()) {
            sb.append('\n').append("  ".repeat(indent - 1));
        }
        sb.append('}');
    }

    private static void serializeArray(Collection<?> collection, StringBuilder sb, int indent) {
        sb.append('[');
        boolean first = true;

        for (Object item : collection) {
            if (!first) {
                sb.append(',');
            }
            if (indent > 0) {
                sb.append('\n').append("  ".repeat(indent));
            }

            serializeValue(item, sb, indent + 1);
            first = false;
        }

        if (indent > 0 && !collection.isEmpty()) {
            sb.append('\n').append("  ".repeat(indent - 1));
        }
        sb.append(']');
    }

    private static void serializePrimitiveArray(Object array, StringBuilder sb, int indent) {
        sb.append('[');

        if (array instanceof int[]) {
            int[] intArray = (int[]) array;
            for (int i = 0; i < intArray.length; i++) {
                if (i > 0)
                    sb.append(',');
                sb.append(intArray[i]);
            }
        } else if (array instanceof long[]) {
            long[] longArray = (long[]) array;
            for (int i = 0; i < longArray.length; i++) {
                if (i > 0)
                    sb.append(',');
                sb.append(longArray[i]);
            }
        } else if (array instanceof double[]) {
            double[] doubleArray = (double[]) array;
            for (int i = 0; i < doubleArray.length; i++) {
                if (i > 0)
                    sb.append(',');
                sb.append(doubleArray[i]);
            }
        } else if (array instanceof boolean[]) {
            boolean[] boolArray = (boolean[]) array;
            for (int i = 0; i < boolArray.length; i++) {
                if (i > 0)
                    sb.append(',');
                sb.append(boolArray[i]);
            }
        } else if (array instanceof String[]) {
            String[] strArray = (String[]) array;
            for (int i = 0; i < strArray.length; i++) {
                if (i > 0)
                    sb.append(',');
                serializeString(strArray[i], sb);
            }
        }

        sb.append(']');
    }

    // Convert Java object to Map for serialization
    private static Map<String, Object> objectToMap(Object obj) {
        Map<String, Object> map = new LinkedHashMap<>();
        Class<?> clazz = obj.getClass();

        // Get all fields including inherited
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);

                    if (value != null) {
                        String fieldName = field.getName();
                        map.put(fieldName, value);
                    }
                } catch (IllegalAccessException e) {
                    // Skip inaccessible fields
                }
            }
            clazz = clazz.getSuperclass();
        }

        // Also include getter methods
        clazz = obj.getClass();
        for (Method method : clazz.getMethods()) {
            String methodName = method.getName();
            if (method.getParameterCount() == 0 && methodName.startsWith("get") &&
                    !methodName.equals("getClass")) {
                try {
                    Object value = method.invoke(obj);
                    if (value != null) {
                        String fieldName = methodName.substring(3, 4).toLowerCase() +
                                methodName.substring(4);
                        map.put(fieldName, value);
                    }
                } catch (Exception e) {
                    // Skip methods that throw exceptions
                }
            }
        }

        return map;
    }

    // Convert Map to Java object
    private static <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();

                try {
                    Field field = getField(clazz, fieldName);
                    if (field != null) {
                        field.setAccessible(true);

                        // Convert value to field type
                        Object convertedValue = convertValue(value, field.getType());
                        field.set(instance, convertedValue);
                    }
                } catch (Exception e) {
                    // Try setter method
                    String setterName = "set" + fieldName.substring(0, 1).toUpperCase() +
                            fieldName.substring(1);

                    for (Method method : clazz.getMethods()) {
                        if (method.getName().equals(setterName) &&
                                method.getParameterCount() == 1) {
                            Object convertedValue = convertValue(value,
                                    method.getParameterTypes()[0]);
                            method.invoke(instance, convertedValue);
                            break;
                        }
                    }
                }
            }

            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        }
    }

    private static Field getField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (targetType == String.class) {
            return value.toString();
        }

        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
        }

        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else if (value instanceof String) {
                return Long.parseLong((String) value);
            }
        }

        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
        }

        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) {
                return value;
            } else if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
        }

        if (targetType == BigDecimal.class) {
            if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            } else if (value instanceof String) {
                return new BigDecimal((String) value);
            }
        }

        if (targetType == List.class || targetType == Collection.class) {
            if (value instanceof Collection) {
                return new ArrayList<>((Collection<?>) value);
            }
        }

        if (targetType == Map.class) {
            if (value instanceof Map) {
                return new LinkedHashMap<>((Map<?, ?>) value);
            }
        }

        if (targetType == Pattern.class && value instanceof String) {
            return Pattern.compile((String) value);
        }

        // Handle nested objects
        if (value instanceof Map) {
            return mapToObject((Map<String, Object>) value, targetType);
        }

        return value;
    }

    /**
     * Example usage
     */
    public static void main(String[] args) {
        // Create a sample plugin descriptor
        PluginDescriptor plugin = new PluginDescriptor();
        plugin.setId("openai/chat-completion");
        plugin.setName("OpenAI Chat Completion");
        plugin.setVersion("1.0.0");
        plugin.setDescription("OpenAI chat completion plugin");

        PluginImplementation impl = new PluginImplementation();
        impl.setType("python");
        impl.setCoordinate("openai:chat-completion:1.0.0");
        impl.setDigest("sha256:abc123...");
        plugin.setImplementation(impl);

        List<PortDescriptor> inputs = new ArrayList<>();
        PortDescriptor input1 = new PortDescriptor();
        input1.setName("prompt");
        input1.setDisplayName("Prompt");
        input1.setDescription("The prompt to send to OpenAI");

        PortData inputData1 = new PortData();
        inputData1.setType("string");
        inputData1.setRequired(true);
        input1.setData(inputData1);
        inputs.add(input1);

        plugin.setInputs(inputs);

        // Convert to JSON
        String json = JsonUtils.stringify(plugin);
        System.out.println("Serialized JSON:");
        System.out.println(json);

        // Parse JSON back to object
        PluginDescriptor parsedPlugin = JsonUtils.parse(json, PluginDescriptor.class);
        System.out.println("\nParsed plugin name: " + parsedPlugin.getName());

        // Parse generic JSON
        String sampleJson = "{\"name\":\"test\",\"version\":\"1.0.0\",\"numbers\":[1,2,3]}";
        Object parsed = JsonUtils.parse(sampleJson);
        System.out.println("\nParsed generic JSON: " + parsed);
    }

}