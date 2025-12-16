package tech.kayys.wayang.dto;

import org.eclipse.microprofile.graphql.Input;

@Input("UpdateWorkspaceInput")
public class UpdateWorkspaceInput {
    public String name;
    public String description;
    public String metadata;
}