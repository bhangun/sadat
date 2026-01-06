package tech.kayys.wayang.nodes.services;

import java.util.List;

public class EmbedRequest {
    private String model;
    private List<String> texts;
    private boolean normalize;

    public String getModel() { return model; }
    public List<String> getTexts() { return texts; }
    public boolean isNormalize() { return normalize; }
}
