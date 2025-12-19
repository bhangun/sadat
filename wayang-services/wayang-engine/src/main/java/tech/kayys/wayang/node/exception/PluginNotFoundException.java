package tech.kayys.wayang.node.exception;

class PluginNotFoundException extends RuntimeException {
    public PluginNotFoundException(String message) {
        super(message);
    }
}