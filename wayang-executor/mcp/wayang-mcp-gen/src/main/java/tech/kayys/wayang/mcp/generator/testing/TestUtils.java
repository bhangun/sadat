package tech.kayys.wayang.mcp.generator.testing;

import java.util.Random;

public class TestUtils {
    
    public static String generateExpiredToken() {
        // This is a mock implementation for generating an expired token
        Random random = new Random();
        return "expired_token_" + random.nextInt(10000);
    }
}