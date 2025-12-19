package tech.kayys.wayang.schema.node;

import java.util.List;

public class Outputs {
    private List<OutputChannel> channels;
    private PortDescriptor defaultOutput;
    private boolean streaming = false;

    // Getters and setters
    public List<OutputChannel> getChannels() {
        return channels;
    }

    public void setChannels(List<OutputChannel> channels) {
        this.channels = channels;
    }

    public PortDescriptor getDefault() {
        return defaultOutput;
    }

    public void setDefault(PortDescriptor defaultOutput) {
        this.defaultOutput = defaultOutput;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
    }
}