package tech.kayys.wayang.dto;

import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.NonNull;

@Input("CreateWorkflowInput")
public class CreateWorkflowInput {
    @NonNull
    public String name;
    public String description;
    public String version;
    public String logic;
    public String ui;
    public String runtime;
    public String metadata;
}