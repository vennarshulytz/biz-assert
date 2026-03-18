package io.github.vennarshulytz.bizassert.exception;

import io.github.vennarshulytz.bizassert.constants.ErrorCodes;

/**
 * 通用业务异常
 * <p>作为 BizAssert 的默认抛出异常类型</p>
 *
 * @author vennarshulytz
 * @since 1.0.0
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public BizException(String message) {
        this(ErrorCodes.UNSPECIFIED, message);
    }

    public BizException(String message, Throwable cause) {
        this(ErrorCodes.UNSPECIFIED, message, cause);
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "BizException{code=" + code + ", message=" + getMessage() + "}";
    }
}