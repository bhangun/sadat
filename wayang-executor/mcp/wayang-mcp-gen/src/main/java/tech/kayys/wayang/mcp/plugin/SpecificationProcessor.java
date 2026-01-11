package tech.kayys.wayang.mcp.plugin;

import tech.kayys.wayang.mcp.model.ApiSpecification;
import java.io.InputStream;

public interface SpecificationProcessor {

    String getSpecificationType();

    void initialize() throws PluginException;

    boolean canProcess(String content, String filename);

    ApiSpecification processSpecification(InputStream content, String filename,
            PluginExecutionContext context) throws PluginException;

    ValidationResult validateSpecification(InputStream content, String filename,
            PluginExecutionContext context) throws PluginException;
}
