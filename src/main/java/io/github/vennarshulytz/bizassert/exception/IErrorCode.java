package io.github.vennarshulytz.bizassert.exception;

/**
 * 错误码接口 - 所有业务错误码枚举需实现此接口
 *
 * @author vennarshulytz
 * @since 1.0.0
 */
public interface IErrorCode {

    /**
     * 错误码
     */
    int getCode();

    /**
     * 错误消息（支持占位符 {}, {}, ...）
     */
    String getMessage();
}