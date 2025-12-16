package tech.kayys.wayang.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * MetadataDiff - Workflow metadata changes
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadataDiff {
    public boolean changed = false;
    public boolean nameChanged = false;
    public boolean descriptionChanged = false;
    public boolean tagsChanged = false;
    public boolean authorChanged = false;

    public String oldName;
    public String newName;
    public String oldDescription;
    public String newDescription;
}
