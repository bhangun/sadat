package tech.kayys.wayang.schema;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.PartType;
import java.io.File;

/**
 * Multipart form for workflow import
 */
public class WorkflowImportForm {

    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public File file;

    @FormParam("overwrite")
    @PartType(MediaType.TEXT_PLAIN)
    public boolean overwrite;
}
