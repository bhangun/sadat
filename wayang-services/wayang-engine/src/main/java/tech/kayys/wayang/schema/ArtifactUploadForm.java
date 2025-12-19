package tech.kayys.wayang.schema;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.PartType;
import java.io.File;

/**
 * Multipart form for artifact uploads
 */
public class ArtifactUploadForm {

    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public File file;

    @FormParam("name")
    @PartType(MediaType.TEXT_PLAIN)
    public String name;

    @FormParam("description")
    @PartType(MediaType.TEXT_PLAIN)
    public String description;

    @FormParam("type")
    @PartType(MediaType.TEXT_PLAIN)
    public String type;
}
