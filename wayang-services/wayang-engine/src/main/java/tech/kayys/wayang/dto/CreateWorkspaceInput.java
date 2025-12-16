package tech.kayys.wayang.dto;

import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.NonNull;

/**
 * Input types for mutations
 */
@Input("CreateWorkspaceInput")
public class CreateWorkspaceInput {
    @NonNull
    public String name;
    public String description;
    public String metadata;
}
