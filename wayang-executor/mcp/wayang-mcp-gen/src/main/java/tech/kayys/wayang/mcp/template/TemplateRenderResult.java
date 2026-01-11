package tech.kayys.wayang.mcp.template;

public class TemplateRenderResult {

    private final boolean success;
    private final String content;
    private final String error;

    private TemplateRenderResult(boolean success, String content, String error) {
        this.success = success;
        this.content = content;
        this.error = error;
    }

    public static TemplateRenderResult success(String content) {
        return new TemplateRenderResult(true, content, null);
    }

    public static TemplateRenderResult error(String error) {
        return new TemplateRenderResult(false, null, error);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getContent() {
        return content;
    }

    public String getError() {
        return error;
    }
}
