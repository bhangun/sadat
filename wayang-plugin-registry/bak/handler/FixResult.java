package tech.kayys.wayang.plugin.runtime.handler;

import lombok.Data;

/**
 * Fix Result from self-healing
 */
@Data
class FixResult {
    private boolean success;
    private Object result;
    private String message;

    public static FixResult success(Object result) {
        FixResult fix = new FixResult();
        fix.success = true;
        fix.result = result;
        return fix;
    }

    public static FixResult failed(String message) {
        FixResult fix = new FixResult();
        fix.success = false;
        fix.message = message;
        return fix;
    }
}
