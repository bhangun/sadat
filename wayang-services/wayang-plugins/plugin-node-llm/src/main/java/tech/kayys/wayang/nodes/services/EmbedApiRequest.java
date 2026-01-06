package tech.kayys.wayang.nodes.services;

import java.util.List;

public class EmbedApiRequest {
    private String model;
    private List<String> texts;
    private boolean normalize;

    public void setModel(String model) { this.model = model; }
    public void setTexts(List<String> texts) { this.texts = texts; }
    public void setNormalize(boolean normalize) { this.normalize = normalize; }
}
