package tech.kayys.wayang.graphql;

import io.smallrye.graphql.api.ErrorExtensionProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.JsonValue;
import tech.kayys.wayang.exception.DesignerException;

@ApplicationScoped
public class GraphQLErrorHandler implements ErrorExtensionProvider {

    @Override
    public String getKey() {
        return "wayang-error-handler";
    }

    @Override
    public JsonValue mapValueFrom(Throwable exception) {
        var builder = jakarta.json.Json.createObjectBuilder();

        if (exception instanceof DesignerException de) {
            builder.add("code", de.getCode());

            if (de.getMetadata() != null && !de.getMetadata().isEmpty()) {
                var metaBuilder = jakarta.json.Json.createObjectBuilder();
                de.getMetadata().forEach((k, v) -> {
                    if (v == null)
                        return;
                    if (v instanceof String)
                        metaBuilder.add(k, (String) v);
                    else if (v instanceof Integer)
                        metaBuilder.add(k, (Integer) v);
                    else if (v instanceof Long)
                        metaBuilder.add(k, (Long) v);
                    else if (v instanceof Double)
                        metaBuilder.add(k, (Double) v);
                    else if (v instanceof Boolean)
                        metaBuilder.add(k, (Boolean) v);
                    else
                        metaBuilder.add(k, v.toString());
                });
                builder.add("metadata", metaBuilder);
            }
        } else {
            builder.add("code", "INTERNAL_ERROR");
            if (exception.getMessage() != null) {
                builder.add("message", exception.getMessage());
            }
        }

        return builder.build();
    }
}
