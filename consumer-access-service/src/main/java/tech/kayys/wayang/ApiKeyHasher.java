package tech.kayys.wayang;

import com.google.common.hash.Hashing;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class ApiKeyHasher {

    public String hash(String raw) {
        return Hashing.sha256()
            .hashString(raw, StandardCharsets.UTF_8)
            .toString();
    }
}
