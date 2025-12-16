package tech.kayys.wayang.model;

/**
 * ResourceProfile - Resource requirements
 */
public class ResourceProfile {
    public String cpu = "100m";
    public String memory = "128Mi";
    public int gpu = 0;
    public String ephemeralStorage;
    public long timeoutMs = 30000;
}
