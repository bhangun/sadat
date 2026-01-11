package tech.kayys.silat.executor.subworkflow;

/**
 * Sub-workflow exception
 */
class SubWorkflowException extends RuntimeException {
    private final String code;

    public SubWorkflowException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}