package tech.kayys.wayang.schema.llm;

public class Choice {
    private LLMMessage message;
    private String finishReason;

    public LLMMessage getMessage() {
        return message;
    }

    public void setMessage(LLMMessage message) {
        this.message = message;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }
}
