package tech.kayys.silat.ui;

import jakarta.persistence.*;
import java.util.*;

/**
 * Node UI configuration schema
 * Used by frontend to render nodes correctly
 */
@Entity
@Table(name = "node_ui_schemas")
class NodeUISchema {

    @Id
    private String nodeType;

    @Column(columnDefinition = "jsonb")
    private tech.kayys.silat.ui.schema.UIMetadata metadata;

    @Column(columnDefinition = "jsonb")
    private List<tech.kayys.silat.ui.schema.UIPort> inputPorts;

    @Column(columnDefinition = "jsonb")
    private List<tech.kayys.silat.ui.schema.UIPort> outputPorts;

    @Column(columnDefinition = "jsonb")
    private tech.kayys.silat.ui.schema.UIConfiguration configuration;

    @Column(columnDefinition = "jsonb")
    private tech.kayys.silat.ui.schema.UIValidation validation;
    
    // Getters and setters
    public String nodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    
    public tech.kayys.silat.ui.schema.UIMetadata metadata() { return metadata; }
    public void setMetadata(tech.kayys.silat.ui.schema.UIMetadata metadata) { this.metadata = metadata; }
    
    public List<tech.kayys.silat.ui.schema.UIPort> inputPorts() { return inputPorts; }
    public void setInputPorts(List<tech.kayys.silat.ui.schema.UIPort> inputPorts) { this.inputPorts = inputPorts; }
    
    public List<tech.kayys.silat.ui.schema.UIPort> outputPorts() { return outputPorts; }
    public void setOutputPorts(List<tech.kayys.silat.ui.schema.UIPort> outputPorts) { this.outputPorts = outputPorts; }
    
    public tech.kayys.silat.ui.schema.UIConfiguration configuration() { return configuration; }
    public void setConfiguration(tech.kayys.silat.ui.schema.UIConfiguration configuration) { this.configuration = configuration; }
    
    public tech.kayys.silat.ui.schema.UIValidation validation() { return validation; }
    public void setValidation(tech.kayys.silat.ui.schema.UIValidation validation) { this.validation = validation; }
}