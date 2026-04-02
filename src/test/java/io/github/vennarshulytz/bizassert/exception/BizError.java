package io.github.vennarshulytz.bizassert.exception;

import sun.misc.Unsafe;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 通用业务错误码（示例）
 *
 * @author vennarshulytz
 * @since 1.0.0
 */
public enum BizError implements IErrorCode {

    USER_NOT_NULL(10001, "user must not be null"),
    USER_IS_NULL(10002, "{0} is null"),
    PARAM_INVALID(10003, "{0} is invalid"),
    ;

    private final int code;
    private final String message;

    BizError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

}