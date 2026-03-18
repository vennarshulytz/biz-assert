package io.github.vennarshulytz.bizassert.exception;

import java.util.function.Function;

/**
 * 异常工厂接口 - 用于创建自定义异常
 * <p>通过实现此接口可以自定义抛出的异常类型</p>
 *
 * <pre>{@code
 * // 方法引用（要求目标异常有 (int, String) 构造函数）
 * ExceptionFactory factory = PaymentException::new;
 *
 * // 仅需 message 的异常
 * ExceptionFactory factory = ExceptionFactory.ofMessage(IllegalStateException::new);
 * }</pre>
 *
 * @author vennarshulytz
 * @since 1.0.0
 */
@FunctionalInterface
public interface ExceptionFactory {

    /**
     * 创建异常实例
     *
     * @param code    错误码
     * @param message 已解析的最终消息（占位符替换后的）
     * @return 运行时异常
     */
    RuntimeException create(int code, String message);

    /**
     * 适配只需要 message 的异常构造函数
     *
     * <pre>{@code
     * ExceptionFactory factory = ExceptionFactory.ofMessage(IllegalStateException::new);
     * }</pre>
     *
     * @param factory 接收 message 返回异常的函数
     * @return ExceptionFactory 实例
     */
    static ExceptionFactory ofMessage(Function<String, RuntimeException> factory) {
        return (code, message) -> factory.apply(message);
    }
}