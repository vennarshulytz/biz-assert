package io.github.vennarshulytz.bizassert;

import io.github.vennarshulytz.bizassert.exception.BizException;
import io.github.vennarshulytz.bizassert.constants.ErrorCodes;
import io.github.vennarshulytz.bizassert.exception.ExceptionFactory;
import io.github.vennarshulytz.bizassert.exception.IErrorCode;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 业务断言工具类
 *
 * <p>提供丰富的断言方法用于业务参数/状态校验，断言失败时抛出业务异常。</p>
 *
 * <h3>核心特性</h3>
 * <ul>
 *   <li>全局可配置默认异常工厂（仅允许设置一次）</li>
 *   <li>每次调用可指定自定义异常工厂</li>
 *   <li>支持错误枚举 {@link IErrorCode}</li>
 *   <li>支持 {@code {0}, {1}} 占位符消息</li>
 *   <li>支持 label 机制（{@code notNullAs} 系列方法）</li>
 *   <li>支持 Pass-through 返回值（notNull/notEmpty/notBlank 等）</li>
 *   <li>支持直接传入异常实例（{@code isTrueOrThrow} 系列方法）</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 全局配置（应用启动时，仅一次）
 * BizAssert.setDefaultExceptionFactory(PaymentException::new);
 *
 * // 基本使用
 * BizAssert.notNull(userId, "userId must not be null");
 *
 * // Pass-through
 * User user = BizAssert.notNull(userRepository.findById(id), "用户不存在");
 *
 * // label 机制
 * BizAssert.notNullAs(userId, "userId");
 *
 * // 错误枚举
 * BizAssert.notNull(userId, BizError.USER_NOT_NULL);
 *
 * // 占位符
 * BizAssert.notNull(userId, "{0} must not be null", "userId");
 *
 * // 自定义异常工厂
 * BizAssert.isTrue(order.isPaid(), "订单未支付", PaymentException::new);
 *
 * // 直接传入异常
 * BizAssert.isTrueOrThrow(order.isPaid(), () -> new PaymentException("订单未支付"));
 * }</pre>
 *
 * @author vennarshulytz
 * @since 1.0.0
 */
public final class BizAssert {

    // ==================== 默认异常工厂 ====================

    /**
     * 默认异常工厂（原子引用，保证线程安全）
     */
    private static final AtomicReference<ExceptionFactory> DEFAULT_FACTORY =
            new AtomicReference<>(BizException::new);

    /**
     * 标记是否已被自定义设置过（仅允许一次）
     */
    private static volatile boolean factoryConfigured = false;

    private BizAssert() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 设置全局默认异常工厂（仅允许调用一次）
     *
     * @param factory 异常工厂
     * @throws IllegalStateException 如果重复设置
     */
    public static void setDefaultExceptionFactory(ExceptionFactory factory) {
        Objects.requireNonNull(factory, "ExceptionFactory must not be null");
        synchronized (DEFAULT_FACTORY) {
            if (factoryConfigured) {
                throw new IllegalStateException(
                        "Default ExceptionFactory has already been configured. It can only be set once.");
            }
            DEFAULT_FACTORY.set(factory);
            factoryConfigured = true;
        }
    }

    /**
     * 获取当前默认异常工厂（主要用于测试）
     */
    static ExceptionFactory getDefaultExceptionFactory() {
        return DEFAULT_FACTORY.get();
    }

    /**
     * 重置默认异常工厂为初始状态（仅用于测试）
     */
    static void resetDefaultExceptionFactory() {
        synchronized (DEFAULT_FACTORY) {
            DEFAULT_FACTORY.set(BizException::new);
            factoryConfigured = false;
        }
    }

    // ==================== 消息格式化 ====================

    /**
     * 格式化消息，将 {0}, {1}, ... 替换为实际参数值
     * <p>null 参数展示为 {@code <null>}</p>
     */
    private static String formatMessage(String pattern, Object... args) {
        if (args == null || args.length == 0) {
            return pattern;
        }
        // 将 null 替换为 "<null>" 展示
        Object[] safeArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            safeArgs[i] = args[i] != null ? args[i] : "<null>";
        }
        return MessageFormat.format(pattern, safeArgs);
    }

    // ==================== 异常抛出 ====================

    /**
     * 使用默认工厂创建并抛出异常
     */
    private static RuntimeException newException(int code, String message) {
        return DEFAULT_FACTORY.get().create(code, message);
    }

    /**
     * 使用指定工厂创建并抛出异常
     */
    private static RuntimeException newException(int code, String message, ExceptionFactory factory) {
        return factory.create(code, message);
    }

    // ========================================================================
    //  isTrue / isFalse
    // ========================================================================

    // ---------- isTrue ----------

    /**
     * 断言表达式为 true（无消息，使用默认）
     */
    public static void isTrue(boolean expression) {
        isTrue(expression, "expression must be true");
    }

    /**
     * 断言表达式为 true
     *
     * @param expression 布尔表达式
     * @param message    错误消息
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
    }

    /**
     * 断言表达式为 true（占位符消息）
     */
    public static void isTrue(boolean expression, String message, Object... args) {
        if (!expression) {
            throw newException(ErrorCodes.UNSPECIFIED, formatMessage(message, args));
        }
    }

    /**
     * 断言表达式为 true（延迟构建消息）
     */
    public static void isTrue(boolean expression, Supplier<String> messageSupplier) {
        if (!expression) {
            throw newException(ErrorCodes.UNSPECIFIED, nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言表达式为 true（延迟构建消息 + 占位符参数）
     */
    public static void isTrue(boolean expression, Supplier<String> messageSupplier, Object... args) {
        if (!expression) {
            throw newException(ErrorCodes.UNSPECIFIED, formatMessage(nullSafeGet(messageSupplier), args));
        }
    }

    /**
     * 断言表达式为 true（带错误码）
     */
    public static void isTrue(boolean expression, int code, String message) {
        if (!expression) {
            throw newException(code, message);
        }
    }

    /**
     * 断言表达式为 true（带错误码 + 占位符参数）
     */
    public static void isTrue(boolean expression, int code, String message, Object... args) {
        if (!expression) {
            throw newException(code, formatMessage(message, args));
        }
    }

    /**
     * 断言表达式为 true（错误枚举）
     */
    public static void isTrue(boolean expression, IErrorCode errorCode) {
        if (!expression) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
    }

    /**
     * 断言表达式为 true（错误枚举 + 占位符参数）
     */
    public static void isTrue(boolean expression, IErrorCode errorCode, Object... args) {
        if (!expression) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
    }

    /**
     * 断言表达式为 true（指定异常工厂）
     */
    public static void isTrue(boolean expression, String message, ExceptionFactory factory) {
        if (!expression) {
            throw newException(ErrorCodes.UNSPECIFIED, message, factory);
        }
    }

    /**
     * 断言表达式为 true（指定异常工厂 + 占位符参数）
     */
    public static void isTrue(boolean expression, String message, ExceptionFactory factory, Object... args) {
        if (!expression) {
            throw newException(ErrorCodes.UNSPECIFIED, formatMessage(message, args), factory);
        }
    }

    /**
     * 断言表达式为 true（指定异常工厂 + 错误码）
     */
    public static void isTrue(boolean expression, int code, String message, ExceptionFactory factory) {
        if (!expression) {
            throw newException(code, message, factory);
        }
    }

    /**
     * 断言表达式为 true（指定异常工厂 + 错误码 + 占位符参数）
     */
    public static void isTrue(boolean expression, int code, String message, ExceptionFactory factory, Object... args) {
        if (!expression) {
            throw newException(code, formatMessage(message, args), factory);
        }
    }

    /**
     * 断言表达式为 true（指定异常工厂 + 错误枚举）
     */
    public static void isTrue(boolean expression, IErrorCode errorCode, ExceptionFactory factory) {
        if (!expression) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
    }

    /**
     * 断言表达式为 true（指定异常工厂 + 错误枚举 + 占位符参数）
     */
    public static void isTrue(boolean expression, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (!expression) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
    }

    /**
     * 断言表达式为 true（直接传入异常实例，由调用方完全控制异常类型和内容）
     *
     * <pre>{@code
     * BizAssert.isTrueOrThrow(order.isPaid(), () -> new PaymentException("订单未支付"));
     * }</pre>
     */
    public static void isTrueOrThrow(boolean expression, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!expression) {
            throw nullSafeGetException(exceptionSupplier);
        }
    }

    /**
     * 断言表达式为 true（label 机制）
     * <p>自动生成消息：{label} must be true</p>
     *
     * <pre>{@code
     * BizAssert.isTrueAs(order.isPaid(), "paid");
     * // 等价于 BizAssert.isTrue(userId, "{0} must be true", "paid");
     * }</pre>
     */
    public static void isTrueAs(boolean expression, String label) {
        if (!expression) {
            throw newException(ErrorCodes.UNSPECIFIED, label + " must be true");
        }
    }

    /**
     * 断言表达式为 true（label 机制 + 错误码）
     */
    public static void isTrueAs(boolean expression, int code, String label) {
        if (!expression) {
            throw newException(code, label + " must be true");
        }
    }

    // ---------- isFalse ----------

    /**
     * 断言表达式为 false（无消息）
     */
    public static void isFalse(boolean expression) {
        isFalse(expression, "expression must be false");
    }

    /**
     * 断言表达式为 false
     */
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
    }

    /**
     * 断言表达式为 false（占位符消息）
     */
    public static void isFalse(boolean expression, String message, Object... args) {
        if (expression) {
            throw newException(ErrorCodes.UNSPECIFIED, formatMessage(message, args));
        }
    }

    /**
     * 断言表达式为 false（延迟构建消息）
     */
    public static void isFalse(boolean expression, Supplier<String> messageSupplier) {
        if (expression) {
            throw newException(ErrorCodes.UNSPECIFIED, nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言表达式为 false（带错误码）
     */
    public static void isFalse(boolean expression, int code, String message) {
        if (expression) {
            throw newException(code, message);
        }
    }

    /**
     * 断言表达式为 false（带错误码 + 占位符参数）
     */
    public static void isFalse(boolean expression, int code, String message, Object... args) {
        if (expression) {
            throw newException(code, formatMessage(message, args));
        }
    }

    /**
     * 断言表达式为 false（错误枚举）
     */
    public static void isFalse(boolean expression, IErrorCode errorCode) {
        if (expression) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
    }

    /**
     * 断言表达式为 false（错误枚举 + 占位符参数）
     */
    public static void isFalse(boolean expression, IErrorCode errorCode, Object... args) {
        if (expression) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
    }

    /**
     * 断言表达式为 false（指定异常工厂）
     */
    public static void isFalse(boolean expression, String message, ExceptionFactory factory) {
        if (expression) {
            throw newException(ErrorCodes.UNSPECIFIED, message, factory);
        }
    }

    /**
     * 断言表达式为 false（指定异常工厂 + 占位符参数）
     */
    public static void isFalse(boolean expression, String message, ExceptionFactory factory, Object... args) {
        if (expression) {
            throw newException(ErrorCodes.UNSPECIFIED, formatMessage(message, args), factory);
        }
    }

    /**
     * 断言表达式为 false（指定异常工厂 + 带错误码）
     */
    public static void isFalse(boolean expression, int code, String message, ExceptionFactory factory) {
        if (expression) {
            throw newException(code, message, factory);
        }
    }

    /**
     * 断言表达式为 false（指定异常工厂 + 带错误码 + 占位符参数）
     */
    public static void isFalse(boolean expression, int code, String message, ExceptionFactory factory, Object... args) {
        if (expression) {
            throw newException(code, formatMessage(message, args), factory);
        }
    }

    /**
     * 断言表达式为 false（指定异常工厂 + 错误枚举）
     */
    public static void isFalse(boolean expression, IErrorCode errorCode, ExceptionFactory factory) {
        if (expression) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
    }

    /**
     * 断言表达式为 false（指定异常工厂 + 错误枚举 + 占位符参数）
     */
    public static void isFalse(boolean expression, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (expression) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
    }

    /**
     * 断言表达式为 false（直接传入异常实例）
     */
    public static void isFalseOrThrow(boolean expression, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (expression) {
            throw nullSafeGetException(exceptionSupplier);
        }
    }

    /**
     * 断言表达式为 false（label 机制）
     * <p>自动生成消息：{label} must be false</p>
     *
     * <pre>{@code
     * BizAssert.isFalseAs(order.isPaid(), "paid");
     * // 等价于 BizAssert.isFalse(userId, "{0} must be false", "paid");
     * }</pre>
     */
    public static void isFalseAs(boolean expression, String label) {
        if (expression) {
            throw newException(ErrorCodes.UNSPECIFIED, label + " must be false");
        }
    }

    /**
     * 断言表达式为 false（label 机制 + 错误码）
     */
    public static void isFalseAs(boolean expression, int code, String label) {
        if (expression) {
            throw newException(code, label + " must be false");
        }
    }

    // ========================================================================
    //  notNull — Pass-through，返回非 null 的值
    // ========================================================================

    /**
     * 断言对象不为 null（无消息）
     */
    public static <T> T notNull(T object) {
        return notNull(object, "parameter must not be null");
    }

    /**
     * 断言对象不为 null（Pass-through）
     *
     * <pre>{@code
     * User user = BizAssert.notNull(userRepo.findById(id), "用户不存在");
     * }</pre>
     *
     * @param object  待校验对象
     * @param message 错误消息
     * @param <T>     对象类型
     * @return 非 null 的原对象
     */
    public static <T> T notNull(T object, String message) {
        if (object == null) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return object;
    }

    /**
     * 断言对象不为 null（占位符消息）
     * <p><b>注意：</b>当只有一个额外参数且类型为 String 时，编译器会优先匹配此方法而非
     * {@link #notNull(Object, String)} 。如果不需要占位符替换，请使用
     * {@link #notNull(Object, String)} 的精确两参数形式。</p>
     */
    public static <T> T notNull(T object, String message, Object... args) {
        if (object == null) {
            throw newException(ErrorCodes.UNSPECIFIED, formatMessage(message, args));
        }
        return object;
    }

    /**
     * 断言对象不为 null（延迟构建消息）
     */
    public static <T> T notNull(T object, Supplier<String> messageSupplier) {
        if (object == null) {
            throw newException(ErrorCodes.UNSPECIFIED, nullSafeGet(messageSupplier));
        }
        return object;
    }

    /**
     * 断言对象不为 null（延迟构建消息 + 占位符参数）
     */
    public static <T> T notNull(T object, Supplier<String> messageSupplier, Object... args) {
        if (object == null) {
            throw newException(ErrorCodes.UNSPECIFIED, formatMessage(nullSafeGet(messageSupplier), args));
        }
        return object;
    }

    /**
     * 断言对象不为 null（带错误码）
     */
    public static <T> T notNull(T object, int code, String message) {
        if (object == null) {
            throw newException(code, message);
        }
        return object;
    }

    /**
     * 断言对象不为 null（带错误码 + 占位符参数）
     */
    public static <T> T notNull(T object, int code, String message, Object... args) {
        if (object == null) {
            throw newException(code, formatMessage(message, args));
        }
        return object;
    }

    /**
     * 断言对象不为 null（错误枚举）
     */
    public static <T> T notNull(T object, IErrorCode errorCode) {
        if (object == null) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return object;
    }

    /**
     * 断言对象不为 null（错误枚举 + 占位符参数）
     */
    public static <T> T notNull(T object, IErrorCode errorCode, Object... args) {
        if (object == null) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return object;
    }

    /**
     * 断言对象不为 null（指定异常工厂）
     */
    public static <T> T notNull(T object, String message, ExceptionFactory factory) {
        if (object == null) {
            throw newException(ErrorCodes.UNSPECIFIED, message, factory);
        }
        return object;
    }

    /**
     * 断言对象不为 null（指定异常工厂 + 占位符参数）
     */
    public static <T> T notNull(T object, String message, ExceptionFactory factory, Object... args) {
        if (object == null) {
            throw newException(ErrorCodes.UNSPECIFIED, formatMessage(message, args), factory);
        }
        return object;
    }

    /**
     * 断言对象不为 null（指定异常工厂 + 错误码）
     */
    public static <T> T notNull(T object, int code, String message, ExceptionFactory factory) {
        if (object == null) {
            throw newException(code, message, factory);
        }
        return object;
    }

    /**
     * 断言对象不为 null（指定异常工厂 + 错误码 + 占位符参数）
     */
    public static <T> T notNull(T object, int code, String message, ExceptionFactory factory, Object... args) {
        if (object == null) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return object;
    }

    /**
     * 断言对象不为 null（指定异常工厂 + 错误枚举）
     */
    public static <T> T notNull(T object, IErrorCode errorCode, ExceptionFactory factory) {
        if (object == null) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return object;
    }

    /**
     * 断言对象不为 null（指定异常工厂 + 错误枚举 + 占位符参数）
     */
    public static <T> T notNull(T object, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (object == null) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return object;
    }

    /**
     * 断言对象不为 null（直接传入异常实例）
     */
    public static <T> T notNullOrThrow(T object, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (object == null) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return object;
    }

    /**
     * 断言对象不为 null（label 机制）
     * <p>自动生成消息：{label} must not be null</p>
     *
     * <pre>{@code
     * BizAssert.notNullAs(userId, "userId");
     * // 等价于 BizAssert.notNull(userId, "{0} must not be null", "userId");
     * }</pre>
     */
    public static <T> T notNullAs(T object, String label) {
        if (object == null) {
            throw newException(ErrorCodes.UNSPECIFIED, label + " must not be null");
        }
        return object;
    }

    /**
     * 断言对象不为 null（label 机制 + 错误码）
     */
    public static <T> T notNullAs(T object, int code, String label) {
        if (object == null) {
            throw newException(code, label + " must not be null");
        }
        return object;
    }

    // ---------- isNull ----------

    /**
     * 断言对象为 null（无消息）
     */
    public static void isNull(Object object) {
        isNull(object, "parameter must be null");
    }

    /**
     * 断言对象为 null
     */
    public static void isNull(Object object, String message) {
        if (object != null) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
    }

    /**
     * 断言对象为 null（占位符消息）
     */
    public static void isNull(Object object, String message, Object... args) {
        if (object != null) {
            throw newException(ErrorCodes.UNSPECIFIED, formatMessage(message, args));
        }
    }

    /**
     * 断言对象为 null（延迟构建消息）
     */
    public static void isNull(Object object, Supplier<String> messageSupplier) {
        if (object != null) {
            throw newException(ErrorCodes.UNSPECIFIED, nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言对象为 null（延迟构建消息 + 占位符参数）
     */
    public static void isNull(Object object, Supplier<String> messageSupplier, Object... args) {
        if (object != null) {
            throw newException(ErrorCodes.UNSPECIFIED, formatMessage(nullSafeGet(messageSupplier), args));
        }
    }

    /**
     * 断言对象为 null（带错误码）
     */
    public static void isNull(Object object, int code, String message) {
        if (object != null) {
            throw newException(code, message);
        }
    }

    /**
     * 断言对象为 null（带错误码 + 占位符参数）
     */
    public static void isNull(Object object, int code, String message, Object... args) {
        if (object != null) {
            throw newException(code, formatMessage(message, args));
        }
    }

    /**
     * 断言对象为 null（错误枚举）
     */
    public static void isNull(Object object, IErrorCode errorCode) {
        if (object != null) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
    }

    /**
     * 断言对象为 null（错误枚举 + 占位符参数）
     */
    public static void isNull(Object object, IErrorCode errorCode, Object... args) {
        if (object != null) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
    }

    /**
     * 断言对象为 null（指定异常工厂）
     */
    public static void isNull(Object object, String message, ExceptionFactory factory) {
        if (object != null) {
            throw newException(ErrorCodes.UNSPECIFIED, message, factory);
        }
    }

    /**
     * 断言对象为 null（指定异常工厂 + 占位符参数）
     */
    public static void isNull(Object object, String message, ExceptionFactory factory, Object... args) {
        if (object != null) {
            throw newException(ErrorCodes.UNSPECIFIED, formatMessage(message, args), factory);
        }
    }

    /**
     * 断言对象为 null（指定异常工厂 + 错误码）
     */
    public static void isNull(Object object, int code, String message, ExceptionFactory factory) {
        if (object != null) {
            throw newException(code, message, factory);
        }
    }

    /**
     * 断言对象为 null（指定异常工厂 + 错误码 + 占位符参数）
     */
    public static void isNull(Object object, int code, String message, ExceptionFactory factory, Object... args) {
        if (object != null) {
            throw newException(code, formatMessage(message, args), factory);
        }
    }

    /**
     * 断言对象为 null（指定异常工厂 + 错误枚举）
     */
    public static void isNull(Object object, IErrorCode errorCode, ExceptionFactory factory) {
        if (object != null) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
    }

    /**
     * 断言对象为 null（指定异常工厂 + 错误枚举 + 占位符参数）
     */
    public static void isNull(Object object, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (object != null) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
    }

    /**
     * 断言对象为 null（直接传入异常实例）
     */
    public static void isNullOrThrow(Object object, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (object != null) {
            throw nullSafeGetException(exceptionSupplier);
        }
    }

    /**
     * 断言对象为 null（label 机制）
     * <p>自动生成消息：{label} must be null</p>
     *
     * <pre>{@code
     * BizAssert.isNullAs(userId, "userId");
     * // 等价于 BizAssert.isNull(userId, "{0} must be null", "userId");
     * }</pre>
     */
    public static void isNullAs(Object object, String label) {
        if (object != null) {
            throw newException(ErrorCodes.UNSPECIFIED, label + " must be null");
        }
    }

    /**
     * 断言对象为 null（label 机制 + 错误码）
     */
    public static void isNullAs(Object object, int code, String label) {
        if (object != null) {
            throw newException(code, label + " must be null");
        }
    }


    // ========================================================================
    //  notEmpty (String) — Pass-through
    // ========================================================================

    /**
     * 断言字符串不为空（无消息）
     */
    public static String notEmpty(String text) {
        return notEmpty(text, "parameter must not be empty");
    }

    /**
     * 断言字符串不为 null 且不为空字符串（Pass-through）
     *
     * @param text    待校验字符串
     * @param message 错误消息
     * @return 非空字符串
     */
    public static String notEmpty(String text, String message) {
        if (text == null || text.isEmpty()) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return text;
    }



    /**
     * 断言字符串不为空（延迟构建消息）
     */
    public static String notEmpty(String text, Supplier<String> messageSupplier) {
        if (text == null || text.isEmpty()) {
            throw newException(ErrorCodes.UNSPECIFIED, nullSafeGet(messageSupplier));
        }
        return text;
    }

    /**
     * 断言字符串不为空（带错误码）
     */
    public static String notEmpty(String text, int code, String message) {
        if (text == null || text.isEmpty()) {
            throw newException(code, message);
        }
        return text;
    }

    /**
     * 断言字符串不为空（占位符消息）
     */
    public static String notEmpty(String text, String message, Object... args) {
        if (text == null || text.isEmpty()) {
            throw newException(ErrorCodes.UNSPECIFIED, formatMessage(message, args));
        }
        return text;
    }

    /**
     * 断言字符串不为空（错误枚举）
     */
    public static String notEmpty(String text, IErrorCode errorCode) {
        if (text == null || text.isEmpty()) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return text;
    }

    /**
     * 断言字符串不为空（错误枚举 + 占位符参数）
     */
    public static String notEmpty(String text, IErrorCode errorCode, Object... args) {
        if (text == null || text.isEmpty()) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return text;
    }

    /**
     * 断言字符串不为空（指定异常工厂）
     */
    public static String notEmpty(String text, String message, ExceptionFactory factory) {
        if (text == null || text.isEmpty()) {
            throw newException(ErrorCodes.UNSPECIFIED, message, factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空（直接传入异常实例）
     */
    public static String notEmptyOrThrow(String text, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (text == null || text.isEmpty()) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return text;
    }

    /**
     * 断言字符串不为空（label 机制）
     */
    public static String notEmptyAs(String text, String label) {
        if (text == null || text.isEmpty()) {
            throw newException(ErrorCodes.UNSPECIFIED, label + " must not be empty");
        }
        return text;
    }

    // ========================================================================
    //  notEmpty (Collection) — Pass-through
    // ========================================================================

    /**
     * 断言集合不为 null 且不为空（Pass-through）
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return collection;
    }

    /**
     * 断言集合不为空（无消息）
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection) {
        return notEmpty(collection, "collection must not be empty");
    }

    /**
     * 断言集合不为空（延迟构建消息）
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, Supplier<String> messageSupplier) {
        if (collection == null || collection.isEmpty()) {
            throw newException(ErrorCodes.UNSPECIFIED, nullSafeGet(messageSupplier));
        }
        return collection;
    }

    /**
     * 断言集合不为空（带错误码）
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, int code, String message) {
        if (collection == null || collection.isEmpty()) {
            throw newException(code, message);
        }
        return collection;
    }

    /**
     * 断言集合不为空（错误枚举）
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, IErrorCode errorCode) {
        if (collection == null || collection.isEmpty()) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return collection;
    }

    /**
     * 断言集合不为空（错误枚举 + 占位符参数）
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, IErrorCode errorCode, Object... args) {
        if (collection == null || collection.isEmpty()) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return collection;
    }

    /**
     * 断言集合不为空（指定异常工厂）
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, String message, ExceptionFactory factory) {
        if (collection == null || collection.isEmpty()) {
            throw newException(ErrorCodes.UNSPECIFIED, message, factory);
        }
        return collection;
    }

    /**
     * 断言集合不为空（直接传入异常实例）
     */
    public static <E, T extends Collection<E>> T notEmptyOrThrow(T collection,
                                                                 Supplier<? extends RuntimeException> exceptionSupplier) {
        if (collection == null || collection.isEmpty()) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return collection;
    }

    /**
     * 断言集合不为空（label 机制）
     */
    public static <E, T extends Collection<E>> T notEmptyAs(T collection, String label) {
        if (collection == null || collection.isEmpty()) {
            throw newException(ErrorCodes.UNSPECIFIED, label + " must not be empty");
        }
        return collection;
    }

    // ========================================================================
    //  notEmpty (Map) — Pass-through
    // ========================================================================

    /**
     * 断言 Map 不为 null 且不为空（Pass-through）
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, String message) {
        if (map == null || map.isEmpty()) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return map;
    }

    /**
     * 断言 Map 不为空（无消息）
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map) {
        return notEmpty(map, "map must not be empty");
    }

    /**
     * 断言 Map 不为空（延迟构建消息）
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, Supplier<String> messageSupplier) {
        if (map == null || map.isEmpty()) {
            throw newException(ErrorCodes.UNSPECIFIED, nullSafeGet(messageSupplier));
        }
        return map;
    }

    /**
     * 断言 Map 不为空（带错误码）
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, int code, String message) {
        if (map == null || map.isEmpty()) {
            throw newException(code, message);
        }
        return map;
    }

    /**
     * 断言 Map 不为空（错误枚举）
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, IErrorCode errorCode) {
        if (map == null || map.isEmpty()) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return map;
    }

    /**
     * 断言 Map 不为空（指定异常工厂）
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, String message, ExceptionFactory factory) {
        if (map == null || map.isEmpty()) {
            throw newException(ErrorCodes.UNSPECIFIED, message, factory);
        }
        return map;
    }

    /**
     * 断言 Map 不为空（直接传入异常实例）
     */
    public static <K, V, T extends Map<K, V>> T notEmptyOrThrow(T map,
                                                                Supplier<? extends RuntimeException> exceptionSupplier) {
        if (map == null || map.isEmpty()) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return map;
    }

    /**
     * 断言 Map 不为空（label 机制）
     */
    public static <K, V, T extends Map<K, V>> T notEmptyAs(T map, String label) {
        if (map == null || map.isEmpty()) {
            throw newException(ErrorCodes.UNSPECIFIED, label + " must not be empty");
        }
        return map;
    }

    // ========================================================================
    //  notEmpty (Array) — Pass-through
    // ========================================================================

    /**
     * 断言数组不为 null 且不为空（Pass-through）
     */
    public static <T> T[] notEmpty(T[] array, String message) {
        if (array == null || array.length == 0) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return array;
    }

    /**
     * 断言数组不为空（无消息）
     */
    public static <T> T[] notEmpty(T[] array) {
        return notEmpty(array, "array must not be empty");
    }

    /**
     * 断言数组不为空（延迟构建消息）
     */
    public static <T> T[] notEmpty(T[] array, Supplier<String> messageSupplier) {
        if (array == null || array.length == 0) {
            throw newException(ErrorCodes.UNSPECIFIED, nullSafeGet(messageSupplier));
        }
        return array;
    }

    /**
     * 断言数组不为空（带错误码）
     */
    public static <T> T[] notEmpty(T[] array, int code, String message) {
        if (array == null || array.length == 0) {
            throw newException(code, message);
        }
        return array;
    }

    /**
     * 断言数组不为空（错误枚举）
     */
    public static <T> T[] notEmpty(T[] array, IErrorCode errorCode) {
        if (array == null || array.length == 0) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return array;
    }

    /**
     * 断言数组不为空（指定异常工厂）
     */
    public static <T> T[] notEmpty(T[] array, String message, ExceptionFactory factory) {
        if (array == null || array.length == 0) {
            throw newException(ErrorCodes.UNSPECIFIED, message, factory);
        }
        return array;
    }

    /**
     * 断言数组不为空（直接传入异常实例）
     */
    public static <T> T[] notEmptyOrThrow(T[] array, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (array == null || array.length == 0) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return array;
    }

    /**
     * 断言数组不为空（label 机制）
     */
    public static <T> T[] notEmptyAs(T[] array, String label) {
        if (array == null || array.length == 0) {
            throw newException(ErrorCodes.UNSPECIFIED, label + " must not be empty");
        }
        return array;
    }

    // ========================================================================
    //  notBlank (String) — Pass-through
    // ========================================================================

    /**
     * 断言字符串不为 null、不为空、不全为空白字符（Pass-through）
     * <p>返回原始值，不做 trim</p>
     */
    public static String notBlank(String text, String message) {
        if (isBlank(text)) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return text;
    }

    /**
     * 断言字符串不为空白（无消息）
     */
    public static String notBlank(String text) {
        return notBlank(text, "parameter must not be blank");
    }

    /**
     * 断言字符串不为空白（延迟构建消息）
     */
    public static String notBlank(String text, Supplier<String> messageSupplier) {
        if (isBlank(text)) {
            throw newException(ErrorCodes.UNSPECIFIED, nullSafeGet(messageSupplier));
        }
        return text;
    }

    /**
     * 断言字符串不为空白（带错误码）
     */
    public static String notBlank(String text, int code, String message) {
        if (isBlank(text)) {
            throw newException(code, message);
        }
        return text;
    }

    /**
     * 断言字符串不为空白（占位符消息）
     */
    public static String notBlank(String text, String message, Object... args) {
        if (isBlank(text)) {
            throw newException(ErrorCodes.UNSPECIFIED, formatMessage(message, args));
        }
        return text;
    }

    /**
     * 断言字符串不为空白（错误枚举）
     */
    public static String notBlank(String text, IErrorCode errorCode) {
        if (isBlank(text)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return text;
    }

    /**
     * 断言字符串不为空白（错误枚举 + 占位符参数）
     */
    public static String notBlank(String text, IErrorCode errorCode, Object... args) {
        if (isBlank(text)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return text;
    }

    /**
     * 断言字符串不为空白（指定异常工厂）
     */
    public static String notBlank(String text, String message, ExceptionFactory factory) {
        if (isBlank(text)) {
            throw newException(ErrorCodes.UNSPECIFIED, message, factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空白（直接传入异常实例）
     */
    public static String notBlankOrThrow(String text, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (isBlank(text)) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return text;
    }

    /**
     * 断言字符串不为空白（label 机制）
     */
    public static String notBlankAs(String text, String label) {
        if (isBlank(text)) {
            throw newException(ErrorCodes.UNSPECIFIED, label + " must not be blank");
        }
        return text;
    }

    // ========================================================================
    //  isEqual / notEqual
    // ========================================================================

    /**
     * 断言两个对象相等（使用 Objects.equals）
     */
    public static void isEqual(Object actual, Object expected, String message) {
        if (!Objects.equals(actual, expected)) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
    }

    /**
     * 断言两个对象相等（占位符消息）
     */
    public static void isEqual(Object actual, Object expected, String message, Object... args) {
        if (!Objects.equals(actual, expected)) {
            throw newException(ErrorCodes.UNSPECIFIED, formatMessage(message, args));
        }
    }

    /**
     * 断言两个对象相等（错误枚举）
     */
    public static void isEqual(Object actual, Object expected, IErrorCode errorCode) {
        if (!Objects.equals(actual, expected)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
    }

    /**
     * 断言两个对象相等（错误枚举 + 占位符参数）
     */
    public static void isEqual(Object actual, Object expected, IErrorCode errorCode, Object... args) {
        if (!Objects.equals(actual, expected)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
    }

    /**
     * 断言两个对象相等（指定异常工厂）
     */
    public static void isEqual(Object actual, Object expected, String message, ExceptionFactory factory) {
        if (!Objects.equals(actual, expected)) {
            throw newException(ErrorCodes.UNSPECIFIED, message, factory);
        }
    }

    /**
     * 断言两个对象不相等
     */
    public static void notEqual(Object actual, Object unexpected, String message) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
    }

    /**
     * 断言两个对象不相等（占位符消息）
     */
    public static void notEqual(Object actual, Object unexpected, String message, Object... args) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(ErrorCodes.UNSPECIFIED, formatMessage(message, args));
        }
    }

    /**
     * 断言两个对象不相等（错误枚举）
     */
    public static void notEqual(Object actual, Object unexpected, IErrorCode errorCode) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
    }

    /**
     * 断言两个对象不相等（指定异常工厂）
     */
    public static void notEqual(Object actual, Object unexpected, String message, ExceptionFactory factory) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(ErrorCodes.UNSPECIFIED, message, factory);
        }
    }

    // ========================================================================
    //  数值断言 — Pass-through
    // ========================================================================

    /**
     * 断言数值为正数（> 0）
     *
     * @param value   待校验数值
     * @param message 错误消息
     * @return 原数值
     */
    public static int isPositive(int value, String message) {
        if (value <= 0) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return value;
    }

    public static int isPositive(int value) {
        return isPositive(value, "value must be positive");
    }

    public static int isPositive(int value, IErrorCode errorCode) {
        if (value <= 0) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return value;
    }

    public static long isPositive(long value, String message) {
        if (value <= 0L) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return value;
    }

    public static long isPositive(long value) {
        return isPositive(value, "value must be positive");
    }

    public static long isPositive(long value, IErrorCode errorCode) {
        if (value <= 0L) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return value;
    }

    /**
     * 断言数值为正数（> 0）— label 机制
     */
    public static int isPositiveAs(int value, String label) {
        if (value <= 0) {
            throw newException(ErrorCodes.UNSPECIFIED, label + " must be positive");
        }
        return value;
    }

    public static long isPositiveAs(long value, String label) {
        if (value <= 0L) {
            throw newException(ErrorCodes.UNSPECIFIED, label + " must be positive");
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）
     */
    public static int isNonNegative(int value, String message) {
        if (value < 0) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return value;
    }

    public static int isNonNegative(int value) {
        return isNonNegative(value, "value must be non-negative");
    }

    public static int isNonNegative(int value, IErrorCode errorCode) {
        if (value < 0) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return value;
    }

    public static long isNonNegative(long value, String message) {
        if (value < 0L) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return value;
    }

    public static long isNonNegative(long value) {
        return isNonNegative(value, "value must be non-negative");
    }

    public static long isNonNegative(long value, IErrorCode errorCode) {
        if (value < 0L) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）— label 机制
     */
    public static int isNonNegativeAs(int value, String label) {
        if (value < 0) {
            throw newException(ErrorCodes.UNSPECIFIED, label + " must be non-negative");
        }
        return value;
    }

    public static long isNonNegativeAs(long value, String label) {
        if (value < 0L) {
            throw newException(ErrorCodes.UNSPECIFIED, label + " must be non-negative");
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）
     *
     * @param value 待校验数值
     * @param min   最小值（含）
     * @param max   最大值（含）
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max, String message) {
        if (value < min || value > max) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return value;
    }

    public static int isBetween(int value, int min, int max) {
        return isBetween(value, min, max,
                "value must be between " + min + " and " + max + ", but was " + value);
    }

    public static long isBetween(long value, long min, long max, String message) {
        if (value < min || value > max) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return value;
    }

    public static long isBetween(long value, long min, long max) {
        return isBetween(value, min, max,
                "value must be between " + min + " and " + max + ", but was " + value);
    }

    public static int isBetween(int value, int min, int max, IErrorCode errorCode) {
        if (value < min || value > max) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return value;
    }

    public static long isBetween(long value, long min, long max, IErrorCode errorCode) {
        if (value < min || value > max) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return value;
    }

    /**
     * 断言数值在范围内 — label 机制
     */
    public static int isBetweenAs(int value, int min, int max, String label) {
        if (value < min || value > max) {
            throw newException(ErrorCodes.UNSPECIFIED,
                    label + " must be between " + min + " and " + max + ", but was " + value);
        }
        return value;
    }

    public static long isBetweenAs(long value, long min, long max, String label) {
        if (value < min || value > max) {
            throw newException(ErrorCodes.UNSPECIFIED,
                    label + " must be between " + min + " and " + max + ", but was " + value);
        }
        return value;
    }

    // ========================================================================
    //  字符串模式匹配 — Pass-through
    // ========================================================================

    /**
     * 断言字符串匹配正则表达式
     *
     * @param text    待校验字符串（不能为 null）
     * @param regex   正则表达式
     * @param message 错误消息
     * @return 原字符串
     */
    public static String matches(String text, String regex, String message) {
        notNull(text, message);
        if (!text.matches(regex)) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（错误枚举）
     */
    public static String matches(String text, String regex, IErrorCode errorCode) {
        notNull(text, errorCode);
        if (!text.matches(regex)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（指定异常工厂）
     */
    public static String matches(String text, String regex, String message, ExceptionFactory factory) {
        notNull(text, message, factory);
        if (!text.matches(regex)) {
            throw newException(ErrorCodes.UNSPECIFIED, message, factory);
        }
        return text;
    }

    // ========================================================================
    //  doesNotContain
    // ========================================================================

    /**
     * 断言字符串不包含指定子串
     */
    public static String doesNotContain(String text, String substring, String message) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（错误枚举）
     */
    public static String doesNotContain(String text, String substring, IErrorCode errorCode) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return text;
    }

    // ========================================================================
    //  contains
    // ========================================================================

    /**
     * 断言字符串包含指定子串
     */
    public static String contains(String text, String substring, String message) {
        notNull(text, message);
        if (substring != null && !text.contains(substring)) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（错误枚举）
     */
    public static String contains(String text, String substring, IErrorCode errorCode) {
        notNull(text, errorCode);
        if (substring != null && !text.contains(substring)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return text;
    }

    // ========================================================================
    //  startsWith / endsWith
    // ========================================================================

    /**
     * 断言字符串以指定前缀开头
     */
    public static String startsWith(String text, String prefix, String message) {
        notNull(text, message);
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（错误枚举）
     */
    public static String startsWith(String text, String prefix, IErrorCode errorCode) {
        notNull(text, errorCode);
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾
     */
    public static String endsWith(String text, String suffix, String message) {
        notNull(text, message);
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（错误枚举）
     */
    public static String endsWith(String text, String suffix, IErrorCode errorCode) {
        notNull(text, errorCode);
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return text;
    }

    // ========================================================================
    //  noNullElements — Pass-through
    // ========================================================================

    /**
     * 断言集合中不含 null 元素
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection, String message) {
        notNull(collection, message);
        for (E element : collection) {
            if (element == null) {
                throw newException(ErrorCodes.UNSPECIFIED, message);
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（错误枚举）
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection, IErrorCode errorCode) {
        notNull(collection, errorCode);
        for (E element : collection) {
            if (element == null) {
                throw newException(errorCode.getCode(), errorCode.getMessage());
            }
        }
        return collection;
    }

    /**
     * 断言数组中不含 null 元素
     */
    public static <T> T[] noNullElements(T[] array, String message) {
        notNull(array, message);
        for (T element : array) {
            if (element == null) {
                throw newException(ErrorCodes.UNSPECIFIED, message);
            }
        }
        return array;
    }

    // ========================================================================
    //  state（语义同 isTrue，但用于状态校验场景）
    // ========================================================================

    /**
     * 断言状态条件为 true
     * <p>语义上用于校验对象状态，与 isTrue（参数校验）区分</p>
     */
    public static void state(boolean expression, String message) {
        if (!expression) {
            throw newException(ErrorCodes.UNSPECIFIED, message);
        }
    }

    public static void state(boolean expression) {
        state(expression, "state check failed");
    }

    public static void state(boolean expression, Supplier<String> messageSupplier) {
        if (!expression) {
            throw newException(ErrorCodes.UNSPECIFIED, nullSafeGet(messageSupplier));
        }
    }

    public static void state(boolean expression, int code, String message) {
        if (!expression) {
            throw newException(code, message);
        }
    }

    public static void state(boolean expression, IErrorCode errorCode) {
        if (!expression) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
    }

    public static void state(boolean expression, IErrorCode errorCode, Object... args) {
        if (!expression) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
    }

    public static void state(boolean expression, String message, ExceptionFactory factory) {
        if (!expression) {
            throw newException(ErrorCodes.UNSPECIFIED, message, factory);
        }
    }

    public static void stateOrThrow(boolean expression, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!expression) {
            throw nullSafeGetException(exceptionSupplier);
        }
    }

    // ========================================================================
    //  fail — 直接抛出异常（用于不可达分支等场景）
    // ========================================================================

    /**
     * 直接抛出异常
     *
     * <pre>{@code
     * switch (status) {
     *     case ACTIVE: ...
     *     case CLOSED: ...
     *     default: BizAssert.fail("Unknown status: " + status);
     * }
     * }</pre>
     *
     * @param message 错误消息
     * @return 声明返回类型以便在表达式中使用（实际不会返回）
     */
    public static <T> T fail(String message) {
        throw newException(ErrorCodes.UNSPECIFIED, message);
    }

    public static <T> T fail(int code, String message) {
        throw newException(code, message);
    }

    public static <T> T fail(IErrorCode errorCode) {
        throw newException(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> T fail(IErrorCode errorCode, Object... args) {
        throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
    }

    public static <T> T fail(String message, ExceptionFactory factory) {
        throw newException(ErrorCodes.UNSPECIFIED, message, factory);
    }

    // ========================================================================
    //  内部工具方法
    // ========================================================================

    /**
     * 判断字符串是否为空白（null、空、全空白字符）
     * <p>内联实现避免外部依赖</p>
     */
    private static boolean isBlank(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        for (int i = 0, len = text.length(); i < len; i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 安全获取 Supplier 值
     */
    private static String nullSafeGet(Supplier<String> messageSupplier) {
        return messageSupplier != null ? messageSupplier.get() : "assertion failed";
    }

    /**
     * 安全获取异常 Supplier 值
     */
    private static RuntimeException nullSafeGetException(Supplier<? extends RuntimeException> exceptionSupplier) {
        RuntimeException ex = (exceptionSupplier != null) ? exceptionSupplier.get() : null;
        if (ex == null) {
            return new BizException(ErrorCodes.UNSPECIFIED, "assertion failed (exception supplier returned null)");
        }
        return ex;
    }
}