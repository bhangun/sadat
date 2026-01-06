package tech.kayys.wayang.nodes.services;

import java.util.List;

public class EmbedResponse {
    private List<Double> embeddings;
    private int dimensions;
    private String model;

    public void setEmbeddings(List<Double> embeddings) { this.embeddings = embeddings; }
    public void setDimensions(int dimensions) { this.dimensions = dimensions; }
    public void setModel(String model) { this.model = model; }
}
