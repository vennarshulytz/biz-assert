package io.github.vennarshulytz.bizassert;

import io.github.vennarshulytz.bizassert.exception.BizException;
import io.github.vennarshulytz.bizassert.constants.ErrorCodes;
import io.github.vennarshulytz.bizassert.exception.ExceptionFactory;
import io.github.vennarshulytz.bizassert.exception.IErrorCode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
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
 *   <li>支持 {@code {}, {}} 占位符消息</li>
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
 * BizAssert.notNull(userId, "{} must not be null", "userId");
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
     * 默认异常工厂
     */
    private static volatile ExceptionFactory DEFAULT_FACTORY = BizException::new;

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
        synchronized (BizAssert.class) {
            if (factoryConfigured) {
                throw new IllegalStateException(
                        "Default ExceptionFactory has already been configured. It can only be set once.");
            }
            DEFAULT_FACTORY = factory;
            factoryConfigured = true;
        }
    }

    /**
     * 获取当前默认异常工厂（主要用于测试）
     */
    static ExceptionFactory getDefaultExceptionFactory() {
        return DEFAULT_FACTORY;
    }

    /**
     * 重置默认异常工厂为初始状态（仅用于测试）
     */
    static void resetDefaultExceptionFactory() {
        synchronized (BizAssert.class) {
            DEFAULT_FACTORY = BizException::new;
            factoryConfigured = false;
        }
    }

    // ==================== 消息格式化 ====================

    /**
     * 格式化消息，将 {}, {}, ... 替换为实际参数值
     * <p>null 参数展示为 {@code <null>}</p>
     */
    private static String formatMessage(String pattern, Object... args) {
        if (args == null || args.length == 0) return pattern;
        StringBuilder sb = new StringBuilder(pattern.length() + 16 * args.length);
        int pos = 0, argIdx = 0;
        while (argIdx < args.length) {
            int idx = pattern.indexOf("{}", pos);
            if (idx == -1) break;
            sb.append(pattern, pos, idx)
                    .append(args[argIdx] != null ? args[argIdx] : "<null>");
            pos = idx + 2;
            argIdx++;
        }
        sb.append(pattern, pos, pattern.length());
        return sb.toString();
    }

    // ==================== 异常抛出 ====================

    /**
     * 使用默认错误码、默认工厂创建并抛出异常
     * @param message 错误消息
     * @return RuntimeException
     */
    private static RuntimeException newException(String message) {
        return DEFAULT_FACTORY.create(ErrorCodes.UNSPECIFIED, message);
    }

    /**
     * 使用默认工厂创建并抛出异常
     * @param code    错误码
     * @param message 错误消息
     * @return RuntimeException
     */
    private static RuntimeException newException(int code, String message) {
        return DEFAULT_FACTORY.create(code, message);
    }

    /**
     * 使用默认错误码、指定工厂创建并抛出异常
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return RuntimeException
     */
    private static RuntimeException newException(String message, ExceptionFactory factory) {
        return factory.create(ErrorCodes.UNSPECIFIED, message);
    }

    /**
     * 使用指定工厂创建并抛出异常
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return RuntimeException
     */
    private static RuntimeException newException(int code, String message, ExceptionFactory factory) {
        return factory.create(code, message);
    }

    // ========================================================================
    //  isTrue / isFalse
    // ========================================================================

    // ---------- isTrue ----------

    /**
     * 断言表达式为 true（无消息，使用默认消息 "expression must be true"）。
     *
     * @param expression 布尔表达式
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrue(boolean expression) {
        isTrue(expression, "expression must be true");
    }

    /**
     * 断言表达式为 true。
     *
     * @param expression 布尔表达式
     * @param message    断言失败时的错误消息
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw newException(message);
        }
    }

    /**
     * 断言表达式为 true（占位符消息）。
     *
     * @param expression 布尔表达式
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrue(boolean expression, String message, Object... args) {
        if (!expression) {
            throw newException(formatMessage(message, args));
        }
    }

    /**
     * 断言表达式为 true（延迟构建消息）。
     * <p>消息仅在断言失败时求值，适用于消息构建代价较高的场景。</p>
     *
     * @param expression      布尔表达式
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrue(boolean expression, Supplier<String> messageSupplier) {
        if (!expression) {
            throw newException(nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言表达式为 true（延迟构建消息 + 占位符参数）。
     *
     * @param expression      布尔表达式
     * @param messageSupplier 错误消息模板提供者（仅在断言失败时调用）
     * @param args            占位符参数
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrue(boolean expression, Supplier<String> messageSupplier, Object... args) {
        if (!expression) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
    }

    /**
     * 断言表达式为 true（带错误码）。
     *
     * @param expression 布尔表达式
     * @param code       错误码
     * @param message    错误消息
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrue(boolean expression, int code, String message) {
        if (!expression) {
            throw newException(code, message);
        }
    }

    /**
     * 断言表达式为 true（带错误码 + 占位符参数）。
     *
     * @param expression 布尔表达式
     * @param code       错误码
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrue(boolean expression, int code, String message, Object... args) {
        if (!expression) {
            throw newException(code, formatMessage(message, args));
        }
    }

    /**
     * 断言表达式为 true（错误枚举）。
     *
     * @param expression 布尔表达式
     * @param errorCode  错误枚举，包含错误码与错误消息
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrue(boolean expression, IErrorCode errorCode) {
        if (!expression) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
    }

    /**
     * 断言表达式为 true（错误枚举 + 占位符参数）。
     *
     * @param expression 布尔表达式
     * @param errorCode  错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrue(boolean expression, IErrorCode errorCode, Object... args) {
        if (!expression) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
    }

    /**
     * 断言表达式为 true（指定异常工厂 + 默认消息）。
     *
     * @param expression 布尔表达式
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrue(boolean expression, ExceptionFactory factory) {
        if (!expression) {
            throw newException("expression must be true", factory);
        }
    }

    /**
     * 断言表达式为 true（指定异常工厂）。
     *
     * @param expression 布尔表达式
     * @param message    错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrue(boolean expression, String message, ExceptionFactory factory) {
        if (!expression) {
            throw newException(message, factory);
        }
    }

    /**
     * 断言表达式为 true（指定异常工厂 + 占位符参数）。
     *
     * @param expression 布尔表达式
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory    异常工厂，用于自定义异常类型
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrue(boolean expression, String message, ExceptionFactory factory, Object... args) {
        if (!expression) {
            throw newException(formatMessage(message, args), factory);
        }
    }

    /**
     * 断言表达式为 true（指定异常工厂 + 错误码）。
     *
     * @param expression 布尔表达式
     * @param code       错误码
     * @param message    错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrue(boolean expression, int code, String message, ExceptionFactory factory) {
        if (!expression) {
            throw newException(code, message, factory);
        }
    }

    /**
     * 断言表达式为 true（指定异常工厂 + 错误码 + 占位符参数）。
     *
     * @param expression 布尔表达式
     * @param code       错误码
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory    异常工厂，用于自定义异常类型
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrue(boolean expression, int code, String message, ExceptionFactory factory, Object... args) {
        if (!expression) {
            throw newException(code, formatMessage(message, args), factory);
        }
    }

    /**
     * 断言表达式为 true（指定异常工厂 + 错误枚举）。
     *
     * @param expression 布尔表达式
     * @param errorCode  错误枚举，包含错误码与错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrue(boolean expression, IErrorCode errorCode, ExceptionFactory factory) {
        if (!expression) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
    }

    /**
     * 断言表达式为 true（指定异常工厂 + 错误枚举 + 占位符参数）。
     *
     * @param expression 布尔表达式
     * @param errorCode  错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param factory    异常工厂，用于自定义异常类型
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrue(boolean expression, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (!expression) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
    }

    /**
     * 断言表达式为 true（直接传入异常实例，由调用方完全控制异常类型和内容）。
     *
     * <pre>{@code
     * BizAssert.isTrueOrThrow(order.isPaid(), () -> new PaymentException("订单未支付"));
     * }</pre>
     *
     * @param expression        布尔表达式
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @throws RuntimeException 若表达式为 false，抛出由 exceptionSupplier 提供的异常
     */
    public static void isTrueOrThrow(boolean expression, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!expression) {
            throw nullSafeGetException(exceptionSupplier);
        }
    }

    /**
     * 断言表达式为 true（label 机制）。
     * <p>自动生成消息：{label} must be true</p>
     *
     * <pre>{@code
     * BizAssert.isTrueAs(order.isPaid(), "paid");
     * // 断言失败时消息为："paid must be true"
     * }</pre>
     *
     * @param expression 布尔表达式
     * @param label      字段/参数名称，用于生成语义化错误消息
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrueAs(boolean expression, String label) {
        if (!expression) {
            throw newException(label + " must be true");
        }
    }

    /**
     * 断言表达式为 true（label 机制 + 错误码）。
     *
     * @param expression 布尔表达式
     * @param code       错误码
     * @param label      字段/参数名称，用于生成语义化错误消息
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrueAs(boolean expression, int code, String label) {
        if (!expression) {
            throw newException(code, label + " must be true");
        }
    }

    /**
     * 断言表达式为 true（label 机制 + 指定异常工厂）。
     * <p>自动生成消息：{label} must be true</p>
     *
     * @param expression 布尔表达式
     * @param label      字段/参数名称，用于生成语义化错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrueAs(boolean expression, String label, ExceptionFactory factory) {
        if (!expression) {
            throw newException(label + " must be true", factory);
        }
    }

    /**
     * 断言表达式为 true（label 机制 + 错误码 + 指定异常工厂）。
     *
     * @param expression 布尔表达式
     * @param code       错误码
     * @param label      字段/参数名称，用于生成语义化错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 false
     */
    public static void isTrueAs(boolean expression, int code, String label, ExceptionFactory factory) {
        if (!expression) {
            throw newException(code, label + " must be true", factory);
        }
    }

    // ---------- isFalse ----------

    /**
     * 断言表达式为 false（无消息，使用默认消息 "expression must be false"）。
     *
     * @param expression 布尔表达式
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalse(boolean expression) {
        isFalse(expression, "expression must be false");
    }

    /**
     * 断言表达式为 false。
     *
     * @param expression 布尔表达式
     * @param message    断言失败时的错误消息
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw newException(message);
        }
    }

    /**
     * 断言表达式为 false（占位符消息）。
     *
     * @param expression 布尔表达式
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalse(boolean expression, String message, Object... args) {
        if (expression) {
            throw newException(formatMessage(message, args));
        }
    }

    /**
     * 断言表达式为 false（延迟构建消息）。
     * <p>消息仅在断言失败时求值，适用于消息构建代价较高的场景。</p>
     *
     * @param expression      布尔表达式
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalse(boolean expression, Supplier<String> messageSupplier) {
        if (expression) {
            throw newException(nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言表达式为 false（延迟构建消息 + 占位符参数）。
     *
     * @param expression      布尔表达式
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @param args            占位符参数
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalse(boolean expression, Supplier<String> messageSupplier, Object... args) {
        if (expression) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
    }

    /**
     * 断言表达式为 false（带错误码）。
     *
     * @param expression 布尔表达式
     * @param code       错误码
     * @param message    错误消息
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalse(boolean expression, int code, String message) {
        if (expression) {
            throw newException(code, message);
        }
    }

    /**
     * 断言表达式为 false（带错误码 + 占位符参数）。
     *
     * @param expression 布尔表达式
     * @param code       错误码
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalse(boolean expression, int code, String message, Object... args) {
        if (expression) {
            throw newException(code, formatMessage(message, args));
        }
    }

    /**
     * 断言表达式为 false（错误枚举）。
     *
     * @param expression 布尔表达式
     * @param errorCode  错误枚举，包含错误码与错误消息
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalse(boolean expression, IErrorCode errorCode) {
        if (expression) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
    }

    /**
     * 断言表达式为 false（错误枚举 + 占位符参数）。
     *
     * @param expression 布尔表达式
     * @param errorCode  错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalse(boolean expression, IErrorCode errorCode, Object... args) {
        if (expression) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
    }

    /**
     * 断言表达式为 false（指定异常工厂 + 默认消息）。
     *
     * @param expression 布尔表达式
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalse(boolean expression, ExceptionFactory factory) {
        if (expression) {
            throw newException("expression must be false", factory);
        }
    }

    /**
     * 断言表达式为 false（指定异常工厂）。
     *
     * @param expression 布尔表达式
     * @param message    错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalse(boolean expression, String message, ExceptionFactory factory) {
        if (expression) {
            throw newException(message, factory);
        }
    }

    /**
     * 断言表达式为 false（指定异常工厂 + 占位符参数）。
     *
     * @param expression 布尔表达式
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory    异常工厂，用于自定义异常类型
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalse(boolean expression, String message, ExceptionFactory factory, Object... args) {
        if (expression) {
            throw newException(formatMessage(message, args), factory);
        }
    }

    /**
     * 断言表达式为 false（指定异常工厂 + 带错误码）。
     *
     * @param expression 布尔表达式
     * @param code       错误码
     * @param message    错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalse(boolean expression, int code, String message, ExceptionFactory factory) {
        if (expression) {
            throw newException(code, message, factory);
        }
    }

    /**
     * 断言表达式为 false（指定异常工厂 + 带错误码 + 占位符参数）。
     *
     * @param expression 布尔表达式
     * @param code       错误码
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory    异常工厂，用于自定义异常类型
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalse(boolean expression, int code, String message, ExceptionFactory factory, Object... args) {
        if (expression) {
            throw newException(code, formatMessage(message, args), factory);
        }
    }

    /**
     * 断言表达式为 false（指定异常工厂 + 错误枚举）。
     *
     * @param expression 布尔表达式
     * @param errorCode  错误枚举，包含错误码与错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalse(boolean expression, IErrorCode errorCode, ExceptionFactory factory) {
        if (expression) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
    }

    /**
     * 断言表达式为 false（指定异常工厂 + 错误枚举 + 占位符参数）。
     *
     * @param expression 布尔表达式
     * @param errorCode  错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param factory    异常工厂，用于自定义异常类型
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalse(boolean expression, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (expression) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
    }

    /**
     * 断言表达式为 false（直接传入异常实例）。
     *
     * @param expression        布尔表达式
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @throws RuntimeException 若表达式为 true，抛出由 exceptionSupplier 提供的异常
     */
    public static void isFalseOrThrow(boolean expression, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (expression) {
            throw nullSafeGetException(exceptionSupplier);
        }
    }

    /**
     * 断言表达式为 false（label 机制）。
     * <p>自动生成消息：{label} must be false</p>
     *
     * <pre>{@code
     * BizAssert.isFalseAs(order.isPaid(), "paid");
     * // 断言失败时消息为："paid must be false"
     * }</pre>
     *
     * @param expression 布尔表达式
     * @param label      字段/参数名称，用于生成语义化错误消息
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalseAs(boolean expression, String label) {
        if (expression) {
            throw newException(label + " must be false");
        }
    }

    /**
     * 断言表达式为 false（label 机制 + 错误码）。
     *
     * @param expression 布尔表达式
     * @param code       错误码
     * @param label      字段/参数名称，用于生成语义化错误消息
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalseAs(boolean expression, int code, String label) {
        if (expression) {
            throw newException(code, label + " must be false");
        }
    }

    /**
     * 断言表达式为 false（label 机制 + 指定异常工厂）。
     * <p>自动生成消息：{label} must be false</p>
     *
     * @param expression 布尔表达式
     * @param label      字段/参数名称，用于生成语义化错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalseAs(boolean expression, String label, ExceptionFactory factory) {
        if (expression) {
            throw newException(label + " must be false", factory);
        }
    }

    /**
     * 断言表达式为 false（label 机制 + 错误码 + 指定异常工厂）。
     *
     * @param expression 布尔表达式
     * @param code       错误码
     * @param label      字段/参数名称，用于生成语义化错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 true
     */
    public static void isFalseAs(boolean expression, int code, String label, ExceptionFactory factory) {
        if (expression) {
            throw newException(code, label + " must be false", factory);
        }
    }

    // ========================================================================
    //  notNull — Pass-through，返回非 null 的值
    // ========================================================================

    /**
     * 断言对象不为 null（无消息，使用默认消息 "parameter must not be null"）。
     *
     * @param object 待校验对象
     * @param <T>    对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNull(T object) {
        return notNull(object, "parameter must not be null");
    }

    /**
     * 断言对象不为 null（Pass-through，返回非 null 的值）。
     *
     * <pre>{@code
     * User user = BizAssert.notNull(userRepo.findById(id), "用户不存在");
     * }</pre>
     *
     * @param object  待校验对象
     * @param message 错误消息
     * @param <T>     对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNull(T object, String message) {
        if (object == null) {
            throw newException(message);
        }
        return object;
    }

    /**
     * 断言对象不为 null（占位符消息）。
     * <p><b>注意：</b>当只有一个额外参数且类型为 String 时，编译器会优先匹配此方法而非
     * {@link #notNull(Object, String)}。如果不需要占位符替换，请使用
     * {@link #notNull(Object, String)} 的精确两参数形式。</p>
     *
     * @param object  待校验对象
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @param <T>     对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNull(T object, String message, Object... args) {
        if (object == null) {
            throw newException(formatMessage(message, args));
        }
        return object;
    }

    /**
     * 断言对象不为 null（延迟构建消息）。
     *
     * @param object          待校验对象
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @param <T>             对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNull(T object, Supplier<String> messageSupplier) {
        if (object == null) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return object;
    }

    /**
     * 断言对象不为 null（延迟构建消息 + 占位符参数）。
     *
     * @param object          待校验对象
     * @param messageSupplier 错误消息模板提供者（仅在断言失败时调用）
     * @param args            占位符参数
     * @param <T>             对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNull(T object, Supplier<String> messageSupplier, Object... args) {
        if (object == null) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return object;
    }

    /**
     * 断言对象不为 null（带错误码）。
     *
     * @param object  待校验对象
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNull(T object, int code, String message) {
        if (object == null) {
            throw newException(code, message);
        }
        return object;
    }

    /**
     * 断言对象不为 null（带错误码 + 占位符参数）。
     *
     * @param object  待校验对象
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @param <T>     对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNull(T object, int code, String message, Object... args) {
        if (object == null) {
            throw newException(code, formatMessage(message, args));
        }
        return object;
    }

    /**
     * 断言对象不为 null（错误枚举）。
     *
     * @param object    待校验对象
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param <T>       对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNull(T object, IErrorCode errorCode) {
        if (object == null) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return object;
    }

    /**
     * 断言对象不为 null（错误枚举 + 占位符参数）。
     *
     * @param object    待校验对象
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param args      占位符参数
     * @param <T>       对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNull(T object, IErrorCode errorCode, Object... args) {
        if (object == null) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return object;
    }

    /**
     * 断言对象不为 null（指定异常工厂 + 默认消息）。
     *
     * @param object  待校验对象
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNull(T object, ExceptionFactory factory) {
        if (object == null) {
            throw newException("parameter must not be null", factory);
        }
        return object;
    }

    /**
     * 断言对象不为 null（指定异常工厂）。
     *
     * @param object  待校验对象
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNull(T object, String message, ExceptionFactory factory) {
        if (object == null) {
            throw newException(message, factory);
        }
        return object;
    }

    /**
     * 断言对象不为 null（指定异常工厂 + 占位符参数）。
     *
     * @param object  待校验对象
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @param <T>     对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNull(T object, String message, ExceptionFactory factory, Object... args) {
        if (object == null) {
            throw newException(formatMessage(message, args), factory);
        }
        return object;
    }

    /**
     * 断言对象不为 null（指定异常工厂 + 错误码）。
     *
     * @param object  待校验对象
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNull(T object, int code, String message, ExceptionFactory factory) {
        if (object == null) {
            throw newException(code, message, factory);
        }
        return object;
    }

    /**
     * 断言对象不为 null（指定异常工厂 + 错误码 + 占位符参数）。
     *
     * @param object  待校验对象
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @param <T>     对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNull(T object, int code, String message, ExceptionFactory factory, Object... args) {
        if (object == null) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return object;
    }

    /**
     * 断言对象不为 null（指定异常工厂 + 错误枚举）。
     *
     * @param object    待校验对象
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param factory   异常工厂，用于自定义异常类型
     * @param <T>       对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNull(T object, IErrorCode errorCode, ExceptionFactory factory) {
        if (object == null) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return object;
    }

    /**
     * 断言对象不为 null（指定异常工厂 + 错误枚举 + 占位符参数）。
     *
     * @param object    待校验对象
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param factory   异常工厂，用于自定义异常类型
     * @param args      占位符参数
     * @param <T>       对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNull(T object, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (object == null) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return object;
    }

    /**
     * 断言对象不为 null（直接传入异常实例）。
     *
     * @param object            待校验对象
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @param <T>               对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null，抛出由 exceptionSupplier 提供的异常
     */
    public static <T> T notNullOrThrow(T object, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (object == null) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return object;
    }

    /**
     * 断言对象不为 null（label 机制）。
     * <p>自动生成消息：{label} must not be null</p>
     *
     * <pre>{@code
     * BizAssert.notNullAs(userId, "userId");
     * // 断言失败时消息为："userId must not be null"
     * }</pre>
     *
     * @param object 待校验对象
     * @param label  字段/参数名称，用于生成语义化错误消息
     * @param <T>    对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNullAs(T object, String label) {
        if (object == null) {
            throw newException(label + " must not be null");
        }
        return object;
    }

    /**
     * 断言对象不为 null（label 机制 + 错误码）。
     *
     * @param object 待校验对象
     * @param code   错误码
     * @param label  字段/参数名称，用于生成语义化错误消息
     * @param <T>    对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNullAs(T object, int code, String label) {
        if (object == null) {
            throw newException(code, label + " must not be null");
        }
        return object;
    }

    /**
     * 断言对象不为 null（label 机制 + 指定异常工厂）。
     * <p>自动生成消息：{label} must not be null</p>
     *
     * @param object  待校验对象
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNullAs(T object, String label, ExceptionFactory factory) {
        if (object == null) {
            throw newException(label + " must not be null", factory);
        }
        return object;
    }

    /**
     * 断言对象不为 null（label 机制 + 错误码 + 指定异常工厂）。
     *
     * @param object  待校验对象
     * @param code    错误码
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     对象类型
     * @return 非 null 的原对象
     * @throws RuntimeException 若对象为 null
     */
    public static <T> T notNullAs(T object, int code, String label, ExceptionFactory factory) {
        if (object == null) {
            throw newException(code, label + " must not be null", factory);
        }
        return object;
    }

    // ---------- isNull ----------

    /**
     * 断言对象为 null（无消息，使用默认消息 "parameter must be null"）。
     *
     * @param object 待校验对象
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNull(Object object) {
        isNull(object, "parameter must be null");
    }

    /**
     * 断言对象为 null。
     *
     * @param object  待校验对象
     * @param message 错误消息
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNull(Object object, String message) {
        if (object != null) {
            throw newException(message);
        }
    }

    /**
     * 断言对象为 null（占位符消息）。
     *
     * @param object  待校验对象
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNull(Object object, String message, Object... args) {
        if (object != null) {
            throw newException(formatMessage(message, args));
        }
    }

    /**
     * 断言对象为 null（延迟构建消息）。
     *
     * @param object          待校验对象
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNull(Object object, Supplier<String> messageSupplier) {
        if (object != null) {
            throw newException(nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言对象为 null（延迟构建消息 + 占位符参数）。
     *
     * @param object          待校验对象
     * @param messageSupplier 错误消息模板提供者（仅在断言失败时调用）
     * @param args            占位符参数
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNull(Object object, Supplier<String> messageSupplier, Object... args) {
        if (object != null) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
    }

    /**
     * 断言对象为 null（带错误码）。
     *
     * @param object  待校验对象
     * @param code    错误码
     * @param message 错误消息
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNull(Object object, int code, String message) {
        if (object != null) {
            throw newException(code, message);
        }
    }

    /**
     * 断言对象为 null（带错误码 + 占位符参数）。
     *
     * @param object  待校验对象
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNull(Object object, int code, String message, Object... args) {
        if (object != null) {
            throw newException(code, formatMessage(message, args));
        }
    }

    /**
     * 断言对象为 null（错误枚举）。
     *
     * @param object    待校验对象
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNull(Object object, IErrorCode errorCode) {
        if (object != null) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
    }

    /**
     * 断言对象为 null（错误枚举 + 占位符参数）。
     *
     * @param object    待校验对象
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param args      占位符参数
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNull(Object object, IErrorCode errorCode, Object... args) {
        if (object != null) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
    }

    /**
     * 断言对象为 null（指定异常工厂 + 默认消息）。
     *
     * @param object  待校验对象
     * @param factory 异常工厂，用于自定义异常类型
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNull(Object object, ExceptionFactory factory) {
        if (object != null) {
            throw newException("parameter must be null", factory);
        }
    }

    /**
     * 断言对象为 null（指定异常工厂）。
     *
     * @param object  待校验对象
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNull(Object object, String message, ExceptionFactory factory) {
        if (object != null) {
            throw newException(message, factory);
        }
    }

    /**
     * 断言对象为 null（指定异常工厂 + 占位符参数）。
     *
     * @param object  待校验对象
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNull(Object object, String message, ExceptionFactory factory, Object... args) {
        if (object != null) {
            throw newException(formatMessage(message, args), factory);
        }
    }

    /**
     * 断言对象为 null（指定异常工厂 + 错误码）。
     *
     * @param object  待校验对象
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNull(Object object, int code, String message, ExceptionFactory factory) {
        if (object != null) {
            throw newException(code, message, factory);
        }
    }

    /**
     * 断言对象为 null（指定异常工厂 + 错误码 + 占位符参数）。
     *
     * @param object  待校验对象
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNull(Object object, int code, String message, ExceptionFactory factory, Object... args) {
        if (object != null) {
            throw newException(code, formatMessage(message, args), factory);
        }
    }

    /**
     * 断言对象为 null（指定异常工厂 + 错误枚举）。
     *
     * @param object    待校验对象
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param factory   异常工厂，用于自定义异常类型
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNull(Object object, IErrorCode errorCode, ExceptionFactory factory) {
        if (object != null) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
    }

    /**
     * 断言对象为 null（指定异常工厂 + 错误枚举 + 占位符参数）。
     *
     * @param object    待校验对象
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param factory   异常工厂，用于自定义异常类型
     * @param args      占位符参数
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNull(Object object, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (object != null) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
    }

    /**
     * 断言对象为 null（直接传入异常实例）。
     *
     * @param object            待校验对象
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @throws RuntimeException 若对象不为 null，抛出由 exceptionSupplier 提供的异常
     */
    public static void isNullOrThrow(Object object, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (object != null) {
            throw nullSafeGetException(exceptionSupplier);
        }
    }

    /**
     * 断言对象为 null（label 机制）。
     * <p>自动生成消息：{label} must be null</p>
     *
     * <pre>{@code
     * BizAssert.isNullAs(userId, "userId");
     * // 断言失败时消息为："userId must be null"
     * }</pre>
     *
     * @param object 待校验对象
     * @param label  字段/参数名称，用于生成语义化错误消息
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNullAs(Object object, String label) {
        if (object != null) {
            throw newException(label + " must be null");
        }
    }

    /**
     * 断言对象为 null（label 机制 + 错误码）。
     *
     * @param object 待校验对象
     * @param code   错误码
     * @param label  字段/参数名称，用于生成语义化错误消息
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNullAs(Object object, int code, String label) {
        if (object != null) {
            throw newException(code, label + " must be null");
        }
    }

    /**
     * 断言对象为 null（label 机制 + 指定异常工厂）。
     * <p>自动生成消息：{label} must be null</p>
     *
     * @param object  待校验对象
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNullAs(Object object, String label, ExceptionFactory factory) {
        if (object != null) {
            throw newException(label + " must be null", factory);
        }
    }

    /**
     * 断言对象为 null（label 机制 + 错误码 + 指定异常工厂）。
     *
     * @param object  待校验对象
     * @param code    错误码
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @throws RuntimeException 若对象不为 null
     */
    public static void isNullAs(Object object, int code, String label, ExceptionFactory factory) {
        if (object != null) {
            throw newException(code, label + " must be null", factory);
        }
    }


    // ========================================================================
    //  notEmpty (String) — Pass-through
    // ========================================================================

    /**
     * 断言字符串不为空（无消息，使用默认消息 "parameter must not be empty"）。
     * <p>空的定义：{@code null} 或长度为 0 的字符串。</p>
     *
     * @param text 待校验字符串
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmpty(String text) {
        return notEmpty(text, "parameter must not be empty");
    }

    /**
     * 断言字符串不为 null 且不为空字符串（Pass-through）。
     *
     * @param text    待校验字符串
     * @param message 错误消息
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmpty(String text, String message) {
        if (text == null || text.isEmpty()) {
            throw newException(message);
        }
        return text;
    }

    /**
     * 断言字符串不为空（占位符消息）。
     *
     * @param text    待校验字符串
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmpty(String text, String message, Object... args) {
        if (text == null || text.isEmpty()) {
            throw newException(formatMessage(message, args));
        }
        return text;
    }

    /**
     * 断言字符串不为空（延迟构建消息）。
     *
     * @param text            待校验字符串
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmpty(String text, Supplier<String> messageSupplier) {
        if (text == null || text.isEmpty()) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return text;
    }

    /**
     * 断言字符串不为空（延迟构建消息 + 占位符参数）。
     *
     * @param text            待校验字符串
     * @param messageSupplier 错误消息模板提供者（仅在断言失败时调用）
     * @param args            占位符参数
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmpty(String text, Supplier<String> messageSupplier, Object... args) {
        if (text == null || text.isEmpty()) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return text;
    }

    /**
     * 断言字符串不为空（带错误码）。
     *
     * @param text    待校验字符串
     * @param code    错误码
     * @param message 错误消息
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmpty(String text, int code, String message) {
        if (text == null || text.isEmpty()) {
            throw newException(code, message);
        }
        return text;
    }

    /**
     * 断言字符串不为空（带错误码 + 占位符参数）。
     *
     * @param text    待校验字符串
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmpty(String text, int code, String message, Object... args) {
        if (text == null || text.isEmpty()) {
            throw newException(code, formatMessage(message, args));
        }
        return text;
    }

    /**
     * 断言字符串不为空（错误枚举）。
     *
     * @param text      待校验字符串
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmpty(String text, IErrorCode errorCode) {
        if (text == null || text.isEmpty()) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return text;
    }

    /**
     * 断言字符串不为空（错误枚举 + 占位符参数）。
     *
     * @param text      待校验字符串
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param args      占位符参数
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmpty(String text, IErrorCode errorCode, Object... args) {
        if (text == null || text.isEmpty()) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return text;
    }

    /**
     * 断言字符串不为空（指定异常工厂 + 默认消息）。
     *
     * @param text    待校验字符串
     * @param factory 异常工厂，用于自定义异常类型
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmpty(String text, ExceptionFactory factory) {
        if (text == null || text.isEmpty()) {
            throw newException("parameter must not be empty", factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空（指定异常工厂）。
     *
     * @param text    待校验字符串
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmpty(String text, String message, ExceptionFactory factory) {
        if (text == null || text.isEmpty()) {
            throw newException(message, factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空（指定异常工厂 + 占位符参数）。
     *
     * @param text    待校验字符串
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmpty(String text, String message, ExceptionFactory factory, Object... args) {
        if (text == null || text.isEmpty()) {
            throw newException(formatMessage(message, args), factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空（指定异常工厂 + 错误码）。
     *
     * @param text    待校验字符串
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmpty(String text, int code, String message, ExceptionFactory factory) {
        if (text == null || text.isEmpty()) {
            throw newException(code, message, factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空（指定异常工厂 + 错误码 + 占位符参数）。
     *
     * @param text    待校验字符串
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmpty(String text, int code, String message, ExceptionFactory factory, Object... args) {
        if (text == null || text.isEmpty()) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空（指定异常工厂 + 错误枚举）。
     *
     * @param text      待校验字符串
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param factory   异常工厂，用于自定义异常类型
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmpty(String text, IErrorCode errorCode, ExceptionFactory factory) {
        if (text == null || text.isEmpty()) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空（指定异常工厂 + 错误枚举 + 占位符参数）。
     *
     * @param text      待校验字符串
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param factory   异常工厂，用于自定义异常类型
     * @param args      占位符参数
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmpty(String text, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (text == null || text.isEmpty()) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空（直接传入异常实例）。
     *
     * @param text              待校验字符串
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空，抛出由 exceptionSupplier 提供的异常
     */
    public static String notEmptyOrThrow(String text, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (text == null || text.isEmpty()) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return text;
    }

    /**
     * 断言字符串不为空（label 机制）。
     * <p>自动生成消息：{label} must not be empty</p>
     *
     * @param text  待校验字符串
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmptyAs(String text, String label) {
        if (text == null || text.isEmpty()) {
            throw newException(label + " must not be empty");
        }
        return text;
    }

    /**
     * 断言字符串不为空（label 机制 + 错误码）。
     *
     * @param text  待校验字符串
     * @param code  错误码
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmptyAs(String text, int code, String label) {
        if (text == null || text.isEmpty()) {
            throw newException(code, label + " must not be empty");
        }
        return text;
    }

    /**
     * 断言字符串不为空（label 机制 + 指定异常工厂）。
     *
     * @param text    待校验字符串
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmptyAs(String text, String label, ExceptionFactory factory) {
        if (text == null || text.isEmpty()) {
            throw newException(label + " must not be empty", factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空（label 机制 + 错误码 + 指定异常工厂）。
     *
     * @param text    待校验字符串
     * @param code    错误码
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 非空字符串
     * @throws RuntimeException 若字符串为 null 或空
     */
    public static String notEmptyAs(String text, int code, String label, ExceptionFactory factory) {
        if (text == null || text.isEmpty()) {
            throw newException(code, label + " must not be empty", factory);
        }
        return text;
    }

    // ========================================================================
    //  notEmpty (Collection) — Pass-through
    // ========================================================================

    /**
     * 断言集合不为空（无消息，使用默认消息 "collection must not be empty"）。
     * <p>空的定义：{@code null} 或 {@link Collection#isEmpty()} 为 {@code true}。</p>
     *
     * @param collection 待校验集合
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection) {
        return notEmpty(collection, "collection must not be empty");
    }

    /**
     * 断言集合不为 null 且不为空（Pass-through）。
     *
     * @param collection 待校验集合
     * @param message    错误消息
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw newException(message);
        }
        return collection;
    }

    /**
     * 断言集合不为空（占位符消息）。
     *
     * @param collection 待校验集合
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args       占位符参数
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, String message, Object... args) {
        if (collection == null || collection.isEmpty()) {
            throw newException(formatMessage(message, args));
        }
        return collection;
    }

    /**
     * 断言集合不为空（延迟构建消息）。
     *
     * @param collection      待校验集合
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @param <E>             集合元素类型
     * @param <T>             集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, Supplier<String> messageSupplier) {
        if (collection == null || collection.isEmpty()) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return collection;
    }

    /**
     * 断言集合不为空（延迟构建消息 + 占位符参数）。
     *
     * @param collection      待校验集合
     * @param messageSupplier 错误消息模板提供者（仅在断言失败时调用）
     * @param args            占位符参数
     * @param <E>             集合元素类型
     * @param <T>             集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, Supplier<String> messageSupplier, Object... args) {
        if (collection == null || collection.isEmpty()) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return collection;
    }

    /**
     * 断言集合不为空（带错误码）。
     *
     * @param collection 待校验集合
     * @param code       错误码
     * @param message    错误消息
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, int code, String message) {
        if (collection == null || collection.isEmpty()) {
            throw newException(code, message);
        }
        return collection;
    }

    /**
     * 断言集合不为空（带错误码 + 占位符参数）。
     *
     * @param collection 待校验集合
     * @param code       错误码
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args       占位符参数
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, int code, String message, Object... args) {
        if (collection == null || collection.isEmpty()) {
            throw newException(code, formatMessage(message, args));
        }
        return collection;
    }

    /**
     * 断言集合不为空（错误枚举）。
     *
     * @param collection 待校验集合
     * @param errorCode  错误枚举，包含错误码与错误消息
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, IErrorCode errorCode) {
        if (collection == null || collection.isEmpty()) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return collection;
    }

    /**
     * 断言集合不为空（错误枚举 + 占位符参数）。
     *
     * @param collection 待校验集合
     * @param errorCode  错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param args       占位符参数
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, IErrorCode errorCode, Object... args) {
        if (collection == null || collection.isEmpty()) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return collection;
    }

    /**
     * 断言集合不为空（指定异常工厂 + 默认消息）。
     *
     * @param collection 待校验集合
     * @param factory    异常工厂，用于自定义异常类型
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, ExceptionFactory factory) {
        if (collection == null || collection.isEmpty()) {
            throw newException("collection must not be empty", factory);
        }
        return collection;
    }

    /**
     * 断言集合不为空（指定异常工厂）。
     *
     * @param collection 待校验集合
     * @param message    错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, String message, ExceptionFactory factory) {
        if (collection == null || collection.isEmpty()) {
            throw newException(message, factory);
        }
        return collection;
    }

    /**
     * 断言集合不为空（指定异常工厂 + 占位符参数）。
     *
     * @param collection 待校验集合
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory    异常工厂，用于自定义异常类型
     * @param args       占位符参数
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, String message, ExceptionFactory factory, Object... args) {
        if (collection == null || collection.isEmpty()) {
            throw newException(formatMessage(message, args), factory);
        }
        return collection;
    }

    /**
     * 断言集合不为空（指定异常工厂 + 错误码）。
     *
     * @param collection 待校验集合
     * @param code       错误码
     * @param message    错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, int code, String message, ExceptionFactory factory) {
        if (collection == null || collection.isEmpty()) {
            throw newException(code, message, factory);
        }
        return collection;
    }

    /**
     * 断言集合不为空（指定异常工厂 + 错误码 + 占位符参数）。
     *
     * @param collection 待校验集合
     * @param code       错误码
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory    异常工厂，用于自定义异常类型
     * @param args       占位符参数
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, int code, String message, ExceptionFactory factory, Object... args) {
        if (collection == null || collection.isEmpty()) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return collection;
    }

    /**
     * 断言集合不为空（指定异常工厂 + 错误枚举）。
     *
     * @param collection 待校验集合
     * @param errorCode  错误枚举，包含错误码与错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, IErrorCode errorCode, ExceptionFactory factory) {
        if (collection == null || collection.isEmpty()) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return collection;
    }

    /**
     * 断言集合不为空（指定异常工厂 + 错误枚举 + 占位符参数）。
     *
     * @param collection 待校验集合
     * @param errorCode  错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param factory    异常工厂，用于自定义异常类型
     * @param args       占位符参数
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmpty(T collection, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (collection == null || collection.isEmpty()) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return collection;
    }

    /**
     * 断言集合不为空（直接传入异常实例）。
     *
     * @param collection        待校验集合
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @param <E>               集合元素类型
     * @param <T>               集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空，抛出由 exceptionSupplier 提供的异常
     */
    public static <E, T extends Collection<E>> T notEmptyOrThrow(T collection,
                                                                 Supplier<? extends RuntimeException> exceptionSupplier) {
        if (collection == null || collection.isEmpty()) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return collection;
    }

    /**
     * 断言集合不为空（label 机制）。
     * <p>自动生成消息：{label} must not be empty</p>
     *
     * @param collection 待校验集合
     * @param label      字段/参数名称，用于生成语义化错误消息
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmptyAs(T collection, String label) {
        if (collection == null || collection.isEmpty()) {
            throw newException(label + " must not be empty");
        }
        return collection;
    }

    /**
     * 断言集合不为空（label 机制 + 错误码）。
     *
     * @param collection 待校验集合
     * @param code       错误码
     * @param label      字段/参数名称，用于生成语义化错误消息
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmptyAs(T collection, int code, String label) {
        if (collection == null || collection.isEmpty()) {
            throw newException(code, label + " must not be empty");
        }
        return collection;
    }

    /**
     * 断言集合不为空（label 机制 + 指定异常工厂）。
     *
     * @param collection 待校验集合
     * @param label      字段/参数名称，用于生成语义化错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmptyAs(T collection, String label, ExceptionFactory factory) {
        if (collection == null || collection.isEmpty()) {
            throw newException(label + " must not be empty", factory);
        }
        return collection;
    }

    /**
     * 断言集合不为空（label 机制 + 错误码 + 指定异常工厂）。
     *
     * @param collection 待校验集合
     * @param code       错误码
     * @param label      字段/参数名称，用于生成语义化错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @param <E>        集合元素类型
     * @param <T>        集合类型
     * @return 非空集合
     * @throws RuntimeException 若集合为 null 或空
     */
    public static <E, T extends Collection<E>> T notEmptyAs(T collection, int code, String label, ExceptionFactory factory) {
        if (collection == null || collection.isEmpty()) {
            throw newException(code, label + " must not be empty", factory);
        }
        return collection;
    }

    // ========================================================================
    //  notEmpty (Map) — Pass-through
    // ========================================================================

    /**
     * 断言 Map 不为空（无消息，使用默认消息 "map must not be empty"）。
     * <p>空的定义：{@code null} 或 {@link Map#isEmpty()} 为 {@code true}。</p>
     *
     * @param map 待校验 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @param <T> Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map) {
        return notEmpty(map, "map must not be empty");
    }

    /**
     * 断言 Map 不为 null 且不为空（Pass-through）。
     *
     * @param map     待校验 Map
     * @param message 错误消息
     * @param <K>     键类型
     * @param <V>     值类型
     * @param <T>     Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, String message) {
        if (map == null || map.isEmpty()) {
            throw newException(message);
        }
        return map;
    }

    /**
     * 断言 Map 不为空（占位符消息）。
     *
     * @param map     待校验 Map
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @param <K>     键类型
     * @param <V>     值类型
     * @param <T>     Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, String message, Object... args) {
        if (map == null || map.isEmpty()) {
            throw newException(formatMessage(message, args));
        }
        return map;
    }

    /**
     * 断言 Map 不为空（延迟构建消息）。
     *
     * @param map             待校验 Map
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @param <K>             键类型
     * @param <V>             值类型
     * @param <T>             Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, Supplier<String> messageSupplier) {
        if (map == null || map.isEmpty()) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return map;
    }

    /**
     * 断言 Map 不为空（延迟构建消息 + 占位符参数）。
     *
     * @param map             待校验 Map
     * @param messageSupplier 错误消息模板提供者（仅在断言失败时调用）
     * @param args            占位符参数
     * @param <K>             键类型
     * @param <V>             值类型
     * @param <T>             Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, Supplier<String> messageSupplier, Object... args) {
        if (map == null || map.isEmpty()) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return map;
    }

    /**
     * 断言 Map 不为空（带错误码）。
     *
     * @param map     待校验 Map
     * @param code    错误码
     * @param message 错误消息
     * @param <K>     键类型
     * @param <V>     值类型
     * @param <T>     Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, int code, String message) {
        if (map == null || map.isEmpty()) {
            throw newException(code, message);
        }
        return map;
    }

    /**
     * 断言 Map 不为空（带错误码 + 占位符参数）。
     *
     * @param map     待校验 Map
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @param <K>     键类型
     * @param <V>     值类型
     * @param <T>     Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, int code, String message, Object... args) {
        if (map == null || map.isEmpty()) {
            throw newException(code, formatMessage(message, args));
        }
        return map;
    }

    /**
     * 断言 Map 不为空（错误枚举）。
     *
     * @param map       待校验 Map
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param <K>       键类型
     * @param <V>       值类型
     * @param <T>       Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, IErrorCode errorCode) {
        if (map == null || map.isEmpty()) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return map;
    }

    /**
     * 断言 Map 不为空（错误枚举 + 占位符参数）。
     *
     * @param map       待校验 Map
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param args      占位符参数
     * @param <K>       键类型
     * @param <V>       值类型
     * @param <T>       Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, IErrorCode errorCode, Object... args) {
        if (map == null || map.isEmpty()) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return map;
    }

    /**
     * 断言 Map 不为空（指定异常工厂 + 默认消息）。
     *
     * @param map     待校验 Map
     * @param factory 异常工厂，用于自定义异常类型
     * @param <K>     键类型
     * @param <V>     值类型
     * @param <T>     Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, ExceptionFactory factory) {
        if (map == null || map.isEmpty()) {
            throw newException("map must not be empty", factory);
        }
        return map;
    }

    /**
     * 断言 Map 不为空（指定异常工厂）。
     *
     * @param map     待校验 Map
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <K>     键类型
     * @param <V>     值类型
     * @param <T>     Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, String message, ExceptionFactory factory) {
        if (map == null || map.isEmpty()) {
            throw newException(message, factory);
        }
        return map;
    }

    /**
     * 断言 Map 不为空（指定异常工厂 + 占位符参数）。
     *
     * @param map     待校验 Map
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @param <K>     键类型
     * @param <V>     值类型
     * @param <T>     Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, String message, ExceptionFactory factory, Object... args) {
        if (map == null || map.isEmpty()) {
            throw newException(formatMessage(message, args), factory);
        }
        return map;
    }

    /**
     * 断言 Map 不为空（指定异常工厂 + 错误码）。
     *
     * @param map     待校验 Map
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <K>     键类型
     * @param <V>     值类型
     * @param <T>     Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, int code, String message, ExceptionFactory factory) {
        if (map == null || map.isEmpty()) {
            throw newException(code, message, factory);
        }
        return map;
    }

    /**
     * 断言 Map 不为空（指定异常工厂 + 错误码 + 占位符参数）。
     *
     * @param map     待校验 Map
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @param <K>     键类型
     * @param <V>     值类型
     * @param <T>     Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, int code, String message, ExceptionFactory factory, Object... args) {
        if (map == null || map.isEmpty()) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return map;
    }

    /**
     * 断言 Map 不为空（指定异常工厂 + 错误枚举）。
     *
     * @param map       待校验 Map
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param factory   异常工厂，用于自定义异常类型
     * @param <K>       键类型
     * @param <V>       值类型
     * @param <T>       Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, IErrorCode errorCode, ExceptionFactory factory) {
        if (map == null || map.isEmpty()) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return map;
    }

    /**
     * 断言 Map 不为空（指定异常工厂 + 错误枚举 + 占位符参数）。
     *
     * @param map       待校验 Map
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param factory   异常工厂，用于自定义异常类型
     * @param args      占位符参数
     * @param <K>       键类型
     * @param <V>       值类型
     * @param <T>       Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmpty(T map, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (map == null || map.isEmpty()) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return map;
    }

    /**
     * 断言 Map 不为空（直接传入异常实例）。
     *
     * @param map               待校验 Map
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @param <K>               键类型
     * @param <V>               值类型
     * @param <T>               Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空，抛出由 exceptionSupplier 提供的异常
     */
    public static <K, V, T extends Map<K, V>> T notEmptyOrThrow(T map,
                                                                Supplier<? extends RuntimeException> exceptionSupplier) {
        if (map == null || map.isEmpty()) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return map;
    }

    /**
     * 断言 Map 不为空（label 机制）。
     * <p>自动生成消息：{label} must not be empty</p>
     *
     * @param map   待校验 Map
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @param <K>   键类型
     * @param <V>   值类型
     * @param <T>   Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmptyAs(T map, String label) {
        if (map == null || map.isEmpty()) {
            throw newException(label + " must not be empty");
        }
        return map;
    }

    /**
     * 断言 Map 不为空（label 机制 + 错误码）。
     *
     * @param map   待校验 Map
     * @param code  错误码
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @param <K>   键类型
     * @param <V>   值类型
     * @param <T>   Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmptyAs(T map, int code, String label) {
        if (map == null || map.isEmpty()) {
            throw newException(code, label + " must not be empty");
        }
        return map;
    }

    /**
     * 断言 Map 不为空（label 机制 + 指定异常工厂）。
     *
     * @param map     待校验 Map
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <K>     键类型
     * @param <V>     值类型
     * @param <T>     Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmptyAs(T map, String label, ExceptionFactory factory) {
        if (map == null || map.isEmpty()) {
            throw newException(label + " must not be empty", factory);
        }
        return map;
    }

    /**
     * 断言 Map 不为空（label 机制 + 错误码 + 指定异常工厂）。
     *
     * @param map     待校验 Map
     * @param code    错误码
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <K>     键类型
     * @param <V>     值类型
     * @param <T>     Map 类型
     * @return 非空 Map
     * @throws RuntimeException 若 Map 为 null 或空
     */
    public static <K, V, T extends Map<K, V>> T notEmptyAs(T map, int code, String label, ExceptionFactory factory) {
        if (map == null || map.isEmpty()) {
            throw newException(code, label + " must not be empty", factory);
        }
        return map;
    }

    // ========================================================================
    //  notEmpty (Array) — Pass-through
    // ========================================================================

    /**
     * 断言数组不为空（无消息，使用默认消息 "array must not be empty"）。
     * <p>空的定义：{@code null} 或长度为 0 的数组。</p>
     *
     * @param array 待校验数组
     * @param <T>   数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmpty(T[] array) {
        return notEmpty(array, "array must not be empty");
    }

    /**
     * 断言数组不为 null 且不为空（Pass-through）。
     *
     * @param array   待校验数组
     * @param message 错误消息
     * @param <T>     数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmpty(T[] array, String message) {
        if (array == null || array.length == 0) {
            throw newException(message);
        }
        return array;
    }

    /**
     * 断言数组不为空（占位符消息）。
     *
     * @param array   待校验数组
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @param <T>     数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmpty(T[] array, String message, Object... args) {
        if (array == null || array.length == 0) {
            throw newException(formatMessage(message, args));
        }
        return array;
    }

    /**
     * 断言数组不为空（延迟构建消息）。
     *
     * @param array           待校验数组
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @param <T>             数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmpty(T[] array, Supplier<String> messageSupplier) {
        if (array == null || array.length == 0) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return array;
    }

    /**
     * 断言数组不为空（延迟构建消息 + 占位符参数）。
     *
     * @param array           待校验数组
     * @param messageSupplier 错误消息模板提供者（仅在断言失败时调用）
     * @param args            占位符参数
     * @param <T>             数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmpty(T[] array, Supplier<String> messageSupplier, Object... args) {
        if (array == null || array.length == 0) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return array;
    }

    /**
     * 断言数组不为空（带错误码）。
     *
     * @param array   待校验数组
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmpty(T[] array, int code, String message) {
        if (array == null || array.length == 0) {
            throw newException(code, message);
        }
        return array;
    }

    /**
     * 断言数组不为空（带错误码 + 占位符参数）。
     *
     * @param array   待校验数组
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @param <T>     数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmpty(T[] array, int code, String message, Object... args) {
        if (array == null || array.length == 0) {
            throw newException(code, formatMessage(message, args));
        }
        return array;
    }

    /**
     * 断言数组不为空（错误枚举）。
     *
     * @param array     待校验数组
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param <T>       数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmpty(T[] array, IErrorCode errorCode) {
        if (array == null || array.length == 0) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return array;
    }

    /**
     * 断言数组不为空（错误枚举 + 占位符参数）。
     *
     * @param array     待校验数组
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param args      占位符参数
     * @param <T>       数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmpty(T[] array, IErrorCode errorCode, Object... args) {
        if (array == null || array.length == 0) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return array;
    }

    /**
     * 断言数组不为空（指定异常工厂 + 默认消息）。
     *
     * @param array   待校验数组
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmpty(T[] array, ExceptionFactory factory) {
        if (array == null || array.length == 0) {
            throw newException("array must not be empty", factory);
        }
        return array;
    }

    /**
     * 断言数组不为空（指定异常工厂）。
     *
     * @param array   待校验数组
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmpty(T[] array, String message, ExceptionFactory factory) {
        if (array == null || array.length == 0) {
            throw newException(message, factory);
        }
        return array;
    }

    /**
     * 断言数组不为空（指定异常工厂 + 占位符参数）。
     *
     * @param array   待校验数组
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @param <T>     数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmpty(T[] array, String message, ExceptionFactory factory, Object... args) {
        if (array == null || array.length == 0) {
            throw newException(formatMessage(message, args), factory);
        }
        return array;
    }

    /**
     * 断言数组不为空（指定异常工厂 + 错误码）。
     *
     * @param array   待校验数组
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmpty(T[] array, int code, String message, ExceptionFactory factory) {
        if (array == null || array.length == 0) {
            throw newException(code, message, factory);
        }
        return array;
    }

    /**
     * 断言数组不为空（指定异常工厂 + 错误码 + 占位符参数）。
     *
     * @param array   待校验数组
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @param <T>     数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmpty(T[] array, int code, String message, ExceptionFactory factory, Object... args) {
        if (array == null || array.length == 0) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return array;
    }

    /**
     * 断言数组不为空（指定异常工厂 + 错误枚举）。
     *
     * @param array     待校验数组
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param factory   异常工厂，用于自定义异常类型
     * @param <T>       数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmpty(T[] array, IErrorCode errorCode, ExceptionFactory factory) {
        if (array == null || array.length == 0) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return array;
    }

    /**
     * 断言数组不为空（指定异常工厂 + 错误枚举 + 占位符参数）。
     *
     * @param array     待校验数组
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param factory   异常工厂，用于自定义异常类型
     * @param args      占位符参数
     * @param <T>       数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmpty(T[] array, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (array == null || array.length == 0) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return array;
    }

    /**
     * 断言数组不为空（直接传入异常实例）。
     *
     * @param array             待校验数组
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @param <T>               数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空，抛出由 exceptionSupplier 提供的异常
     */
    public static <T> T[] notEmptyOrThrow(T[] array, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (array == null || array.length == 0) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return array;
    }

    /**
     * 断言数组不为空（label 机制）。
     * <p>自动生成消息：{label} must not be empty</p>
     *
     * @param array 待校验数组
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @param <T>   数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmptyAs(T[] array, String label) {
        if (array == null || array.length == 0) {
            throw newException(label + " must not be empty");
        }
        return array;
    }

    /**
     * 断言数组不为空（label 机制 + 错误码）。
     *
     * @param array 待校验数组
     * @param code  错误码
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @param <T>   数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmptyAs(T[] array, int code, String label) {
        if (array == null || array.length == 0) {
            throw newException(code, label + " must not be empty");
        }
        return array;
    }

    /**
     * 断言数组不为空（label 机制 + 指定异常工厂）。
     *
     * @param array   待校验数组
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmptyAs(T[] array, String label, ExceptionFactory factory) {
        if (array == null || array.length == 0) {
            throw newException(label + " must not be empty", factory);
        }
        return array;
    }

    /**
     * 断言数组不为空（label 机制 + 错误码 + 指定异常工厂）。
     *
     * @param array   待校验数组
     * @param code    错误码
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     数组元素类型
     * @return 非空数组
     * @throws RuntimeException 若数组为 null 或空
     */
    public static <T> T[] notEmptyAs(T[] array, int code, String label, ExceptionFactory factory) {
        if (array == null || array.length == 0) {
            throw newException(code, label + " must not be empty", factory);
        }
        return array;
    }

    // ========================================================================
    //  notBlank (String) — Pass-through
    // ========================================================================

    /**
     * 断言字符串不为空白（无消息，使用默认消息 "parameter must not be blank"）。
     * <p>空白的定义：{@code null}、长度为 0 的字符串或仅包含空白字符的字符串。</p>
     *
     * @param text 待校验字符串
     * @return 非空白字符串（原始值，不做 trim）
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlank(String text) {
        return notBlank(text, "parameter must not be blank");
    }

    /**
     * 断言字符串不为 null、不为空、不全为空白字符（Pass-through）。
     * <p>返回原始值，不做 trim。</p>
     *
     * @param text    待校验字符串
     * @param message 错误消息
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlank(String text, String message) {
        if (isBlank(text)) {
            throw newException(message);
        }
        return text;
    }

    /**
     * 断言字符串不为空白（占位符消息）。
     *
     * @param text    待校验字符串
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlank(String text, String message, Object... args) {
        if (isBlank(text)) {
            throw newException(formatMessage(message, args));
        }
        return text;
    }

    /**
     * 断言字符串不为空白（延迟构建消息）。
     *
     * @param text            待校验字符串
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlank(String text, Supplier<String> messageSupplier) {
        if (isBlank(text)) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return text;
    }

    /**
     * 断言字符串不为空白（延迟构建消息 + 占位符参数）。
     *
     * @param text            待校验字符串
     * @param messageSupplier 错误消息模板提供者（仅在断言失败时调用）
     * @param args            占位符参数
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlank(String text, Supplier<String> messageSupplier, Object... args) {
        if (isBlank(text)) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return text;
    }

    /**
     * 断言字符串不为空白（带错误码）。
     *
     * @param text    待校验字符串
     * @param code    错误码
     * @param message 错误消息
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlank(String text, int code, String message) {
        if (isBlank(text)) {
            throw newException(code, message);
        }
        return text;
    }

    /**
     * 断言字符串不为空白（带错误码 + 占位符参数）。
     *
     * @param text    待校验字符串
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlank(String text, int code, String message, Object... args) {
        if (isBlank(text)) {
            throw newException(code, formatMessage(message, args));
        }
        return text;
    }

    /**
     * 断言字符串不为空白（错误枚举）。
     *
     * @param text      待校验字符串
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlank(String text, IErrorCode errorCode) {
        if (isBlank(text)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return text;
    }

    /**
     * 断言字符串不为空白（错误枚举 + 占位符参数）。
     *
     * @param text      待校验字符串
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param args      占位符参数
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlank(String text, IErrorCode errorCode, Object... args) {
        if (isBlank(text)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return text;
    }

    /**
     * 断言字符串不为空白（指定异常工厂 + 默认消息）。
     *
     * @param text    待校验字符串
     * @param factory 异常工厂，用于自定义异常类型
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlank(String text, ExceptionFactory factory) {
        if (isBlank(text)) {
            throw newException("parameter must not be blank", factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空白（指定异常工厂）。
     *
     * @param text    待校验字符串
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlank(String text, String message, ExceptionFactory factory) {
        if (isBlank(text)) {
            throw newException(message, factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空白（指定异常工厂 + 占位符参数）。
     *
     * @param text    待校验字符串
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlank(String text, String message, ExceptionFactory factory, Object... args) {
        if (isBlank(text)) {
            throw newException(formatMessage(message, args), factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空白（指定异常工厂 + 错误码）。
     *
     * @param text    待校验字符串
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlank(String text, int code, String message, ExceptionFactory factory) {
        if (isBlank(text)) {
            throw newException(code, message, factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空白（指定异常工厂 + 错误码 + 占位符参数）。
     *
     * @param text    待校验字符串
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlank(String text, int code, String message, ExceptionFactory factory, Object... args) {
        if (isBlank(text)) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空白（指定异常工厂 + 错误枚举）。
     *
     * @param text      待校验字符串
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param factory   异常工厂，用于自定义异常类型
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlank(String text, IErrorCode errorCode, ExceptionFactory factory) {
        if (isBlank(text)) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空白（指定异常工厂 + 错误枚举 + 占位符参数）。
     *
     * @param text      待校验字符串
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param factory   异常工厂，用于自定义异常类型
     * @param args      占位符参数
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlank(String text, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (isBlank(text)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空白（直接传入异常实例）。
     *
     * @param text              待校验字符串
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白，抛出由 exceptionSupplier 提供的异常
     */
    public static String notBlankOrThrow(String text, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (isBlank(text)) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return text;
    }

    /**
     * 断言字符串不为空白（label 机制）。
     * <p>自动生成消息：{label} must not be blank</p>
     *
     * @param text  待校验字符串
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlankAs(String text, String label) {
        if (isBlank(text)) {
            throw newException(label + " must not be blank");
        }
        return text;
    }

    /**
     * 断言字符串不为空白（label 机制 + 错误码）。
     *
     * @param text  待校验字符串
     * @param code  错误码
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlankAs(String text, int code, String label) {
        if (isBlank(text)) {
            throw newException(code, label + " must not be blank");
        }
        return text;
    }

    /**
     * 断言字符串不为空白（label 机制 + 指定异常工厂）。
     *
     * @param text    待校验字符串
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlankAs(String text, String label, ExceptionFactory factory) {
        if (isBlank(text)) {
            throw newException(label + " must not be blank", factory);
        }
        return text;
    }

    /**
     * 断言字符串不为空白（label 机制 + 错误码 + 指定异常工厂）。
     *
     * @param text    待校验字符串
     * @param code    错误码
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 非空白字符串
     * @throws RuntimeException 若字符串为 null、空或空白
     */
    public static String notBlankAs(String text, int code, String label, ExceptionFactory factory) {
        if (isBlank(text)) {
            throw newException(code, label + " must not be blank", factory);
        }
        return text;
    }

    // ========================================================================
    //  isEqual / notEqual
    // ========================================================================

    /**
     * 断言两个对象相等（无消息，使用默认消息 "actual must be equal to expected"）。
     * <p>比较方式：{@link Objects#equals(Object, Object)}。</p>
     *
     * @param actual   实际值
     * @param expected 期望值
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqual(T actual, T expected) {
        if (!Objects.equals(actual, expected)) {
            throw newException("actual must be equal to expected");
        }
    }

    /**
     * 断言两个对象相等（使用 {@link Objects#equals(Object, Object)}）。
     *
     * @param actual   实际值
     * @param expected 期望值
     * @param message  错误消息
     * @param <T>      对象类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqual(T actual, T expected, String message) {
        if (!Objects.equals(actual, expected)) {
            throw newException(message);
        }
    }

    /**
     * 断言两个对象相等（占位符消息）。
     *
     * @param actual   实际值
     * @param expected 期望值
     * @param message  错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args     占位符参数
     * @param <T>      对象类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqual(T actual, T expected, String message, Object... args) {
        if (!Objects.equals(actual, expected)) {
            throw newException(formatMessage(message, args));
        }
    }

    /**
     * 断言两个对象相等（延迟构建消息）。
     *
     * @param actual          实际值
     * @param expected        期望值
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @param <T>             对象类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqual(T actual, T expected, Supplier<String> messageSupplier) {
        if (!Objects.equals(actual, expected)) {
            throw newException(nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言两个对象相等（延迟构建消息 + 占位符参数）。
     *
     * @param actual          实际值
     * @param expected        期望值
     * @param messageSupplier 错误消息模板提供者（仅在断言失败时调用）
     * @param args            占位符参数
     * @param <T>             对象类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqual(T actual, T expected, Supplier<String> messageSupplier, Object... args) {
        if (!Objects.equals(actual, expected)) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
    }

    /**
     * 断言两个对象相等（带错误码）。
     *
     * @param actual   实际值
     * @param expected 期望值
     * @param code     错误码
     * @param message  错误消息
     * @param <T>      对象类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqual(T actual, T expected, int code, String message) {
        if (!Objects.equals(actual, expected)) {
            throw newException(code, message);
        }
    }

    /**
     * 断言两个对象相等（带错误码 + 占位符参数）。
     *
     * @param actual   实际值
     * @param expected 期望值
     * @param code     错误码
     * @param message  错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args     占位符参数
     * @param <T>      对象类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqual(T actual, T expected, int code, String message, Object... args) {
        if (!Objects.equals(actual, expected)) {
            throw newException(code, formatMessage(message, args));
        }
    }

    /**
     * 断言两个对象相等（错误枚举）。
     *
     * @param actual    实际值
     * @param expected  期望值
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param <T>       对象类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqual(T actual, T expected, IErrorCode errorCode) {
        if (!Objects.equals(actual, expected)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
    }

    /**
     * 断言两个对象相等（错误枚举 + 占位符参数）。
     *
     * @param actual    实际值
     * @param expected  期望值
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param args      占位符参数
     * @param <T>       对象类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqual(T actual, T expected, IErrorCode errorCode, Object... args) {
        if (!Objects.equals(actual, expected)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
    }

    /**
     * 断言两个对象相等（指定异常工厂 + 默认消息）。
     *
     * @param actual   实际值
     * @param expected 期望值
     * @param factory  异常工厂，用于自定义异常类型
     * @param <T>      对象类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqual(T actual, T expected, ExceptionFactory factory) {
        if (!Objects.equals(actual, expected)) {
            throw newException("actual must be equal to expected", factory);
        }
    }

    /**
     * 断言两个对象相等（指定异常工厂）。
     *
     * @param actual   实际值
     * @param expected 期望值
     * @param message  错误消息
     * @param factory  异常工厂，用于自定义异常类型
     * @param <T>      对象类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqual(T actual, T expected, String message, ExceptionFactory factory) {
        if (!Objects.equals(actual, expected)) {
            throw newException(message, factory);
        }
    }

    /**
     * 断言两个对象相等（指定异常工厂 + 占位符参数）。
     *
     * @param actual   实际值
     * @param expected 期望值
     * @param message  错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory  异常工厂，用于自定义异常类型
     * @param args     占位符参数
     * @param <T>      对象类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqual(T actual, T expected, String message, ExceptionFactory factory, Object... args) {
        if (!Objects.equals(actual, expected)) {
            throw newException(formatMessage(message, args), factory);
        }
    }

    /**
     * 断言两个对象相等（指定异常工厂 + 错误码）。
     *
     * @param actual   实际值
     * @param expected 期望值
     * @param code     错误码
     * @param message  错误消息
     * @param factory  异常工厂，用于自定义异常类型
     * @param <T>      对象类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqual(T actual, T expected, int code, String message, ExceptionFactory factory) {
        if (!Objects.equals(actual, expected)) {
            throw newException(code, message, factory);
        }
    }

    /**
     * 断言两个对象相等（指定异常工厂 + 错误码 + 占位符参数）。
     *
     * @param actual   实际值
     * @param expected 期望值
     * @param code     错误码
     * @param message  错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory  异常工厂，用于自定义异常类型
     * @param args     占位符参数
     * @param <T>      对象类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqual(T actual, T expected, int code, String message, ExceptionFactory factory, Object... args) {
        if (!Objects.equals(actual, expected)) {
            throw newException(code, formatMessage(message, args), factory);
        }
    }

    /**
     * 断言两个对象相等（指定异常工厂 + 错误枚举）。
     *
     * @param actual    实际值
     * @param expected  期望值
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param factory   异常工厂，用于自定义异常类型
     * @param <T>       对象类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqual(T actual, T expected, IErrorCode errorCode, ExceptionFactory factory) {
        if (!Objects.equals(actual, expected)) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
    }

    /**
     * 断言两个对象相等（指定异常工厂 + 错误枚举 + 占位符参数）。
     *
     * @param actual    实际值
     * @param expected  期望值
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param factory   异常工厂，用于自定义异常类型
     * @param args      占位符参数
     * @param <T>       对象类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqual(T actual, T expected, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (!Objects.equals(actual, expected)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
    }

    /**
     * 断言两个对象相等（直接传入异常实例）。
     *
     * @param actual            实际值
     * @param expected          期望值
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @throws RuntimeException 若两个对象不相等，抛出由 exceptionSupplier 提供的异常
     */
    public static <T> void isEqualOrThrow(T actual, T expected, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!Objects.equals(actual, expected)) {
            throw nullSafeGetException(exceptionSupplier);
        }
    }

    /**
     * 断言两个对象相等（label 机制）。
     * <p>自动生成消息：{actualLabel} must be equal to {expectedLabel}</p>
     *
     * @param actual        实际值
     * @param expected      期望值
     * @param actualLabel   实际值的字段/参数名称
     * @param expectedLabel 期望值的字段/参数名称
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqualAs(T actual, T expected, String actualLabel, String expectedLabel) {
        if (!Objects.equals(actual, expected)) {
            throw newException(actualLabel + " must be equal to " + expectedLabel);
        }
    }

    /**
     * 断言两个对象相等（label 机制 + 错误码）。
     *
     * @param actual        实际值
     * @param expected      期望值
     * @param code          错误码
     * @param actualLabel   实际值的字段/参数名称
     * @param expectedLabel 期望值的字段/参数名称
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqualAs(T actual, T expected, int code, String actualLabel, String expectedLabel) {
        if (!Objects.equals(actual, expected)) {
            throw newException(code, actualLabel + " must be equal to " + expectedLabel);
        }
    }

    /**
     * 断言两个对象相等（label 机制 + 指定异常工厂）。
     *
     * @param actual        实际值
     * @param expected      期望值
     * @param actualLabel   实际值的字段/参数名称
     * @param expectedLabel 期望值的字段/参数名称
     * @param factory       异常工厂，用于自定义异常类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqualAs(T actual, T expected, String actualLabel, String expectedLabel, ExceptionFactory factory) {
        if (!Objects.equals(actual, expected)) {
            throw newException(actualLabel + " must be equal to " + expectedLabel, factory);
        }
    }

    /**
     * 断言两个对象相等（label 机制 + 错误码 + 指定异常工厂）。
     *
     * @param actual        实际值
     * @param expected      期望值
     * @param code          错误码
     * @param actualLabel   实际值的字段/参数名称
     * @param expectedLabel 期望值的字段/参数名称
     * @param factory       异常工厂，用于自定义异常类型
     * @throws RuntimeException 若两个对象不相等
     */
    public static <T> void isEqualAs(T actual, T expected, int code, String actualLabel, String expectedLabel, ExceptionFactory factory) {
        if (!Objects.equals(actual, expected)) {
            throw newException(code, actualLabel + " must be equal to " + expectedLabel, factory);
        }
    }

    /**
     * 断言两个对象不相等（无消息，使用默认消息 "actual must not be equal to unexpected"）。
     * <p>比较方式：{@link Objects#equals(Object, Object)}。</p>
     *
     * @param actual     实际值
     * @param unexpected 不期望的值
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqual(Object actual, Object unexpected) {
        if (Objects.equals(actual, unexpected)) {
            throw newException("actual must not be equal to unexpected");
        }
    }

    /**
     * 断言两个对象不相等。
     *
     * @param actual     实际值
     * @param unexpected 不期望的值
     * @param message    错误消息
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqual(Object actual, Object unexpected, String message) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(message);
        }
    }

    /**
     * 断言两个对象不相等（占位符消息）。
     *
     * @param actual     实际值
     * @param unexpected 不期望的值
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args       占位符参数
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqual(Object actual, Object unexpected, String message, Object... args) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(formatMessage(message, args));
        }
    }

    /**
     * 断言两个对象不相等（延迟构建消息）。
     *
     * @param actual          实际值
     * @param unexpected      不期望的值
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqual(Object actual, Object unexpected, Supplier<String> messageSupplier) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(nullSafeGet(messageSupplier));
        }
    }

    /**
     * 断言两个对象不相等（延迟构建消息 + 占位符参数）。
     *
     * @param actual          实际值
     * @param unexpected      不期望的值
     * @param messageSupplier 错误消息模板提供者（仅在断言失败时调用）
     * @param args            占位符参数
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqual(Object actual, Object unexpected, Supplier<String> messageSupplier, Object... args) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
    }

    /**
     * 断言两个对象不相等（带错误码）。
     *
     * @param actual     实际值
     * @param unexpected 不期望的值
     * @param code       错误码
     * @param message    错误消息
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqual(Object actual, Object unexpected, int code, String message) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(code, message);
        }
    }

    /**
     * 断言两个对象不相等（带错误码 + 占位符参数）。
     *
     * @param actual     实际值
     * @param unexpected 不期望的值
     * @param code       错误码
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args       占位符参数
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqual(Object actual, Object unexpected, int code, String message, Object... args) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(code, formatMessage(message, args));
        }
    }

    /**
     * 断言两个对象不相等（错误枚举）。
     *
     * @param actual     实际值
     * @param unexpected 不期望的值
     * @param errorCode  错误枚举，包含错误码与错误消息
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqual(Object actual, Object unexpected, IErrorCode errorCode) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
    }

    /**
     * 断言两个对象不相等（错误枚举 + 占位符参数）。
     *
     * @param actual     实际值
     * @param unexpected 不期望的值
     * @param errorCode  错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param args       占位符参数
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqual(Object actual, Object unexpected, IErrorCode errorCode, Object... args) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
    }

    /**
     * 断言两个对象不相等（指定异常工厂 + 默认消息）。
     *
     * @param actual     实际值
     * @param unexpected 不期望的值
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqual(Object actual, Object unexpected, ExceptionFactory factory) {
        if (Objects.equals(actual, unexpected)) {
            throw newException("actual must not be equal to unexpected", factory);
        }
    }

    /**
     * 断言两个对象不相等（指定异常工厂）。
     *
     * @param actual     实际值
     * @param unexpected 不期望的值
     * @param message    错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqual(Object actual, Object unexpected, String message, ExceptionFactory factory) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(message, factory);
        }
    }

    /**
     * 断言两个对象不相等（指定异常工厂 + 占位符参数）。
     *
     * @param actual     实际值
     * @param unexpected 不期望的值
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory    异常工厂，用于自定义异常类型
     * @param args       占位符参数
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqual(Object actual, Object unexpected, String message, ExceptionFactory factory, Object... args) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(formatMessage(message, args), factory);
        }
    }

    /**
     * 断言两个对象不相等（指定异常工厂 + 错误码）。
     *
     * @param actual     实际值
     * @param unexpected 不期望的值
     * @param code       错误码
     * @param message    错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqual(Object actual, Object unexpected, int code, String message, ExceptionFactory factory) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(code, message, factory);
        }
    }

    /**
     * 断言两个对象不相等（指定异常工厂 + 错误码 + 占位符参数）。
     *
     * @param actual     实际值
     * @param unexpected 不期望的值
     * @param code       错误码
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory    异常工厂，用于自定义异常类型
     * @param args       占位符参数
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqual(Object actual, Object unexpected, int code, String message, ExceptionFactory factory, Object... args) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(code, formatMessage(message, args), factory);
        }
    }

    /**
     * 断言两个对象不相等（指定异常工厂 + 错误枚举）。
     *
     * @param actual     实际值
     * @param unexpected 不期望的值
     * @param errorCode  错误枚举，包含错误码与错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqual(Object actual, Object unexpected, IErrorCode errorCode, ExceptionFactory factory) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
    }

    /**
     * 断言两个对象不相等（指定异常工厂 + 错误枚举 + 占位符参数）。
     *
     * @param actual     实际值
     * @param unexpected 不期望的值
     * @param errorCode  错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param factory    异常工厂，用于自定义异常类型
     * @param args       占位符参数
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqual(Object actual, Object unexpected, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
    }

    /**
     * 断言两个对象不相等（直接传入异常实例）。
     *
     * @param actual            实际值
     * @param unexpected        不期望的值
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @throws RuntimeException 若两个对象相等，抛出由 exceptionSupplier 提供的异常
     */
    public static void notEqualOrThrow(Object actual, Object unexpected, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (Objects.equals(actual, unexpected)) {
            throw nullSafeGetException(exceptionSupplier);
        }
    }

    /**
     * 断言两个对象不相等（label 机制）。
     * <p>自动生成消息：{actualLabel} must not be equal to {unexpectedLabel}</p>
     *
     * @param actual          实际值
     * @param unexpected      不期望的值
     * @param actualLabel     实际值的字段/参数名称
     * @param unexpectedLabel 不期望值的字段/参数名称
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqualAs(Object actual, Object unexpected, String actualLabel, String unexpectedLabel) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(actualLabel + " must not be equal to " + unexpectedLabel);
        }
    }

    /**
     * 断言两个对象不相等（label 机制 + 错误码）。
     *
     * @param actual          实际值
     * @param unexpected      不期望的值
     * @param code            错误码
     * @param actualLabel     实际值的字段/参数名称
     * @param unexpectedLabel 不期望值的字段/参数名称
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqualAs(Object actual, Object unexpected, int code, String actualLabel, String unexpectedLabel) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(code, actualLabel + " must not be equal to " + unexpectedLabel);
        }
    }

    /**
     * 断言两个对象不相等（label 机制 + 指定异常工厂）。
     *
     * @param actual          实际值
     * @param unexpected      不期望的值
     * @param actualLabel     实际值的字段/参数名称
     * @param unexpectedLabel 不期望值的字段/参数名称
     * @param factory         异常工厂，用于自定义异常类型
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqualAs(Object actual, Object unexpected, String actualLabel, String unexpectedLabel, ExceptionFactory factory) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(actualLabel + " must not be equal to " + unexpectedLabel, factory);
        }
    }

    /**
     * 断言两个对象不相等（label 机制 + 错误码 + 指定异常工厂）。
     *
     * @param actual          实际值
     * @param unexpected      不期望的值
     * @param code            错误码
     * @param actualLabel     实际值的字段/参数名称
     * @param unexpectedLabel 不期望值的字段/参数名称
     * @param factory         异常工厂，用于自定义异常类型
     * @throws RuntimeException 若两个对象相等
     */
    public static void notEqualAs(Object actual, Object unexpected, int code, String actualLabel, String unexpectedLabel, ExceptionFactory factory) {
        if (Objects.equals(actual, unexpected)) {
            throw newException(code, actualLabel + " must not be equal to " + unexpectedLabel, factory);
        }
    }

    // ========================================================================
    //  数值断言 — Pass-through
    // ========================================================================

    /**
     * 断言 int 数值为正数（{@code > 0}）（无消息，使用默认消息 "value must be positive"）。
     *
     * @param value 待校验数值
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositive(int value) {
        return isPositive(value, "value must be positive");
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）。
     *
     * @param value   待校验数值
     * @param message 错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositive(int value, String message) {
        if (value <= 0) {
            throw newException(message);
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（占位符消息）。
     *
     * @param value   待校验数值
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositive(int value, String message, Object... args) {
        if (value <= 0) {
            throw newException(formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（延迟构建消息）。
     *
     * @param value           待校验数值
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositive(int value, Supplier<String> messageSupplier) {
        if (value <= 0) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（延迟构建消息 + 占位符参数）。
     *
     * @param value           待校验数值
     * @param messageSupplier 错误消息模板提供者（仅在断言失败时调用）
     * @param args            占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositive(int value, Supplier<String> messageSupplier, Object... args) {
        if (value <= 0) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（带错误码）。
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositive(int value, int code, String message) {
        if (value <= 0) {
            throw newException(code, message);
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（带错误码 + 占位符参数）。
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositive(int value, int code, String message, Object... args) {
        if (value <= 0) {
            throw newException(code, formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（错误枚举）。
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositive(int value, IErrorCode errorCode) {
        if (value <= 0) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（错误枚举 + 占位符参数）。
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param args      占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositive(int value, IErrorCode errorCode, Object... args) {
        if (value <= 0) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（指定异常工厂 + 默认消息）。
     *
     * @param value   待校验数值
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositive(int value, ExceptionFactory factory) {
        if (value <= 0) {
            throw newException("value must be positive", factory);
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（指定异常工厂）。
     *
     * @param value   待校验数值
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositive(int value, String message, ExceptionFactory factory) {
        if (value <= 0) {
            throw newException(message, factory);
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（指定异常工厂 + 占位符参数）。
     *
     * @param value   待校验数值
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositive(int value, String message, ExceptionFactory factory, Object... args) {
        if (value <= 0) {
            throw newException(formatMessage(message, args), factory);
        }
        return value;
    }


    /**
     * 断言 int 数值为正数（{@code > 0}）（指定异常工厂 + 带错误码）。
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositive(int value, int code, String message, ExceptionFactory factory) {
        if (value <= 0) {
            throw newException(code, message, factory);
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（指定异常工厂 + 带错误码 + 占位符参数）。
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositive(int value, int code, String message, ExceptionFactory factory, Object... args) {
        if (value <= 0) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（指定异常工厂 + 错误枚举）。
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param factory   异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositive(int value, IErrorCode errorCode, ExceptionFactory factory) {
        if (value <= 0) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（指定异常工厂 + 错误枚举 + 占位符参数）。
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param factory   异常工厂，用于自定义异常类型
     * @param args      占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositive(int value, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (value <= 0) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（直接传入异常实例）。
     *
     * @param value             待校验数值
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}，抛出由 exceptionSupplier 提供的异常
     */
    public static int isPositiveOrThrow(int value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value <= 0) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（label 机制）。
     * <p>自动生成消息：{label} must be positive</p>
     *
     * @param value 待校验数值
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositiveAs(int value, String label) {
        if (value <= 0) {
            throw newException(label + " must be positive");
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（label 机制 + 错误码）。
     *
     * @param value 待校验数值
     * @param code  错误码
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositiveAs(int value, int code, String label) {
        if (value <= 0) {
            throw newException(code, label + " must be positive");
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（label 机制 + 指定异常工厂）。
     *
     * @param value   待校验数值
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositiveAs(int value, String label, ExceptionFactory factory) {
        if (value <= 0) {
            throw newException(label + " must be positive", factory);
        }
        return value;
    }

    /**
     * 断言 int 数值为正数（{@code > 0}）（label 机制 + 错误码 + 指定异常工厂）。
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static int isPositiveAs(int value, int code, String label, ExceptionFactory factory) {
        if (value <= 0) {
            throw newException(code, label + " must be positive", factory);
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（无消息，使用默认消息 "value must be positive"）。
     *
     * @param value 待校验数值
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositive(long value) {
        return isPositive(value, "value must be positive");
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）。
     *
     * @param value   待校验数值
     * @param message 错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositive(long value, String message) {
        if (value <= 0L) {
            throw newException(message);
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（占位符消息）。
     *
     * @param value   待校验数值
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositive(long value, String message, Object... args) {
        if (value <= 0L) {
            throw newException(formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（延迟构建消息）。
     *
     * @param value           待校验数值
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositive(long value, Supplier<String> messageSupplier) {
        if (value <= 0) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（延迟构建消息 + 占位符参数）。
     *
     * @param value           待校验数值
     * @param messageSupplier 错误消息模板提供者（仅在断言失败时调用）
     * @param args            占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositive(long value, Supplier<String> messageSupplier, Object... args) {
        if (value <= 0) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（带错误码）。
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositive(long value, int code, String message) {
        if (value <= 0) {
            throw newException(code, message);
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（带错误码 + 占位符参数）。
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositive(long value, int code, String message, Object... args) {
        if (value <= 0) {
            throw newException(code, formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（错误枚举）。
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositive(long value, IErrorCode errorCode) {
        if (value <= 0L) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（错误枚举 + 占位符参数）。
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param args      占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositive(long value, IErrorCode errorCode, Object... args) {
        if (value <= 0L) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（指定异常工厂 + 默认消息）。
     *
     * @param value   待校验数值
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositive(long value, ExceptionFactory factory) {
        if (value <= 0) {
            throw newException("value must be positive", factory);
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（指定异常工厂）。
     *
     * @param value   待校验数值
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositive(long value, String message, ExceptionFactory factory) {
        if (value <= 0) {
            throw newException(message, factory);
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（指定异常工厂 + 占位符参数）。
     *
     * @param value   待校验数值
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositive(long value, String message, ExceptionFactory factory, Object... args) {
        if (value <= 0) {
            throw newException(formatMessage(message, args), factory);
        }
        return value;
    }


    /**
     * 断言 long 数值为正数（{@code > 0}）（指定异常工厂 + 带错误码）。
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositive(long value, int code, String message, ExceptionFactory factory) {
        if (value <= 0) {
            throw newException(code, message, factory);
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（指定异常工厂 + 带错误码 + 占位符参数）。
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositive(long value, int code, String message, ExceptionFactory factory, Object... args) {
        if (value <= 0) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（指定异常工厂 + 错误枚举）。
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param factory   异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositive(long value, IErrorCode errorCode, ExceptionFactory factory) {
        if (value <= 0) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（指定异常工厂 + 错误枚举 + 占位符参数）。
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param factory   异常工厂，用于自定义异常类型
     * @param args      占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositive(long value, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (value <= 0) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（直接传入异常实例）。
     *
     * @param value             待校验数值
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}，抛出由 exceptionSupplier 提供的异常
     */
    public static long isPositiveOrThrow(long value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value <= 0) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（label 机制）。
     * <p>自动生成消息：{label} must be positive</p>
     *
     * @param value 待校验数值
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositiveAs(long value, String label) {
        if (value <= 0L) {
            throw newException(label + " must be positive");
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（label 机制 + 错误码）。
     *
     * @param value 待校验数值
     * @param code  错误码
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositiveAs(long value, int code, String label) {
        if (value <= 0) {
            throw newException(code, label + " must be positive");
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（label 机制 + 指定异常工厂）。
     *
     * @param value   待校验数值
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositiveAs(long value, String label, ExceptionFactory factory) {
        if (value <= 0) {
            throw newException(label + " must be positive", factory);
        }
        return value;
    }

    /**
     * 断言 long 数值为正数（{@code > 0}）（label 机制 + 错误码 + 指定异常工厂）。
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 {@code <= 0}
     */
    public static long isPositiveAs(long value, int code, String label, ExceptionFactory factory) {
        if (value <= 0) {
            throw newException(code, label + " must be positive", factory);
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（无消息，使用默认消息）。
     * <p>支持所有 {@link Number} 子类（如 {@link Integer}、{@link Long}、
     * {@link Double}、{@link java.math.BigDecimal} 等），通过 {@code doubleValue()} 进行比较。</p>
     *
     * @param value 待校验数值，为 null 时视为断言失败
     * @param <T>   数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositive(T value) {
        return isPositive(value, "value must be positive");
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）。
     *
     * @param value   待校验数值，为 null 时视为断言失败
     * @param message 错误消息
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositive(T value, String message) {
        if (!isPositiveNumber(value)) {
            throw newException(message);
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（占位符消息）。
     *
     * @param value   待校验数值，为 null 时视为断言失败
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositive(T value, String message, Object... args) {
        if (!isPositiveNumber(value)) {
            throw newException(formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（延迟构建消息）。
     *
     * @param value           待校验数值，为 null 时视为断言失败
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @param <T>             数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositive(T value, Supplier<String> messageSupplier) {
        if (!isPositiveNumber(value)) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（延迟构建消息 + 占位符参数）。
     *
     * @param value           待校验数值，为 null 时视为断言失败
     * @param messageSupplier 错误消息模板提供者（仅在断言失败时调用）
     * @param args            占位符参数
     * @param <T>             数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositive(T value, Supplier<String> messageSupplier, Object... args) {
        if (!isPositiveNumber(value)) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（带错误码）。
     *
     * @param value   待校验数值，为 null 时视为断言失败
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositive(T value, int code, String message) {
        if (!isPositiveNumber(value)) {
            throw newException(code, message);
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（带错误码 + 占位符参数）。
     *
     * @param value   待校验数值，为 null 时视为断言失败
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositive(T value, int code, String message, Object... args) {
        if (!isPositiveNumber(value)) {
            throw newException(code, formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（错误枚举）。
     *
     * @param value     待校验数值，为 null 时视为断言失败
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param <T>       数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositive(T value, IErrorCode errorCode) {
        if (!isPositiveNumber(value)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（错误枚举 + 占位符参数）。
     *
     * @param value     待校验数值，为 null 时视为断言失败
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param args      占位符参数
     * @param <T>       数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositive(T value, IErrorCode errorCode, Object... args) {
        if (!isPositiveNumber(value)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（指定异常工厂 + 默认消息）。
     *
     * @param value   待校验数值，为 null 时视为断言失败
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositive(T value, ExceptionFactory factory) {
        return isPositive(value, "value must be positive", factory);
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（指定异常工厂）。
     *
     * @param value   待校验数值，为 null 时视为断言失败
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositive(T value, String message, ExceptionFactory factory) {
        if (!isPositiveNumber(value)) {
            throw newException(message, factory);
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（指定异常工厂 + 占位符参数）。
     *
     * @param value   待校验数值，为 null 时视为断言失败
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositive(T value, String message, ExceptionFactory factory, Object... args) {
        if (!isPositiveNumber(value)) {
            throw newException(formatMessage(message, args), factory);
        }
        return value;
    }


    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（指定异常工厂 + 带错误码）。
     *
     * @param value   待校验数值，为 null 时视为断言失败
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositive(T value, int code, String message, ExceptionFactory factory) {
        if (!isPositiveNumber(value)) {
            throw newException(code, message, factory);
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（指定异常工厂 + 带错误码 + 占位符参数）。
     *
     * @param value   待校验数值，为 null 时视为断言失败
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositive(T value, int code, String message, ExceptionFactory factory, Object... args) {
        if (!isPositiveNumber(value)) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（指定异常工厂 + 错误枚举）。
     *
     * @param value     待校验数值，为 null 时视为断言失败
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param factory   异常工厂，用于自定义异常类型
     * @param <T>       数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositive(T value, IErrorCode errorCode, ExceptionFactory factory) {
        if (!isPositiveNumber(value)) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（指定异常工厂 + 错误枚举 + 占位符参数）。
     *
     * @param value     待校验数值，为 null 时视为断言失败
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param factory   异常工厂，用于自定义异常类型
     * @param args      占位符参数
     * @param <T>       数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositive(T value, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (!isPositiveNumber(value)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（直接传入异常实例）。
     *
     * @param value             待校验数值，为 null 时视为断言失败
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @param <T>               数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}，抛出由 exceptionSupplier 提供的异常
     */
    public static <T extends Number> T isPositiveOrThrow(T value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!isPositiveNumber(value)) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（label 机制）。
     * <p>自动生成消息：{label} must be positive</p>
     *
     * @param value 待校验数值，为 null 时视为断言失败
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @param <T>   数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositiveAs(T value, String label) {
        if (!isPositiveNumber(value)) {
            throw newException(label + " must be positive");
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（label 机制 + 错误码）。
     *
     * @param value 待校验数值，为 null 时视为断言失败
     * @param code  错误码
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @param <T>   数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositiveAs(T value, int code, String label) {
        if (!isPositiveNumber(value)) {
            throw newException(code, label + " must be positive");
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（label 机制 + 指定异常工厂）。
     *
     * @param value   待校验数值，为 null 时视为断言失败
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositiveAs(T value, String label, ExceptionFactory factory) {
        if (!isPositiveNumber(value)) {
            throw newException(label + " must be positive", factory);
        }
        return value;
    }

    /**
     * 断言泛型 {@link Number} 数值为正数（{@code > 0}）（label 机制 + 错误码 + 指定异常工厂）。
     *
     * @param value   待校验数值，为 null 时视为断言失败
     * @param code    错误码
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值为 null 或 {@code <= 0}
     */
    public static <T extends Number> T isPositiveAs(T value, int code, String label, ExceptionFactory factory) {
        if (!isPositiveNumber(value)) {
            throw newException(code, label + " must be positive", factory);
        }
        return value;
    }

    // ===================== isNonNegative(int) =====================

    /**
     * 断言数值为非负数（>= 0）
     *
     * @param value 待校验数值
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegative(int value) {
        return isNonNegative(value, "value must be non-negative");
    }

    /**
     * 断言数值为非负数（>= 0）
     *
     * @param value   待校验数值
     * @param message 错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegative(int value, String message) {
        if (value < 0) {
            throw newException(message);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（占位符消息）
     *
     * @param value   待校验数值
     * @param message 错误消息模板（支持占位符）
     * @param args    占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegative(int value, String message, Object... args) {
        if (value < 0) {
            throw newException(formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（延迟构建消息）
     *
     * @param value           待校验数值
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegative(int value, Supplier<String> messageSupplier) {
        if (value < 0) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（延迟构建消息 + 占位符消息）
     *
     * @param value           待校验数值
     * @param messageSupplier 错误消息模板提供者（支持占位符，仅在断言失败时调用）
     * @param args            占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegative(int value, Supplier<String> messageSupplier, Object... args) {
        if (value < 0) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（带错误码）
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegative(int value, int code, String message) {
        if (value < 0) {
            throw newException(code, message);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（带错误码 + 占位符消息）
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息模板（支持占位符）
     * @param args    占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegative(int value, int code, String message, Object... args) {
        if (value < 0) {
            throw newException(code, formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（错误枚举）
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegative(int value, IErrorCode errorCode) {
        if (value < 0) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（错误枚举 + 占位符参数）
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，包含错误码与错误消息模板
     * @param args      占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegative(int value, IErrorCode errorCode, Object... args) {
        if (value < 0) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 默认消息）
     *
     * @param value   待校验数值
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegative(int value, ExceptionFactory factory) {
        if (value < 0) {
            throw newException("value must be non-negative", factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂）
     *
     * @param value   待校验数值
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegative(int value, String message, ExceptionFactory factory) {
        if (value < 0) {
            throw newException(message, factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 占位符参数）
     *
     * @param value   待校验数值
     * @param message 错误消息模板（支持占位符）
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegative(int value, String message, ExceptionFactory factory, Object... args) {
        if (value < 0) {
            throw newException(formatMessage(message, args), factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 带错误码）
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegative(int value, int code, String message, ExceptionFactory factory) {
        if (value < 0) {
            throw newException(code, message, factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 带错误码 + 占位符消息）
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息模板（支持占位符）
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegative(int value, int code, String message, ExceptionFactory factory, Object... args) {
        if (value < 0) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 错误枚举）
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param factory   异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegative(int value, IErrorCode errorCode, ExceptionFactory factory) {
        if (value < 0) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 错误枚举 + 占位符参数）
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，包含错误码与错误消息模板
     * @param factory   异常工厂，用于自定义异常类型
     * @param args      占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegative(int value, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (value < 0) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（直接传入异常实例）
     *
     * @param value             待校验数值
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @return 原数值
     * @throws RuntimeException 若数值 < 0，抛出由 exceptionSupplier 提供的异常
     */
    public static int isNonNegativeOrThrow(int value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value < 0) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（label 机制）
     *
     * @param value 待校验数值
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegativeAs(int value, String label) {
        if (value < 0) {
            throw newException(label + " must be non-negative");
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（label 机制 + 带错误码）
     *
     * @param value 待校验数值
     * @param code  错误码
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegativeAs(int value, int code, String label) {
        if (value < 0) {
            throw newException(code, label + " must be non-negative");
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（label 机制 + 指定异常工厂）
     *
     * @param value   待校验数值
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegativeAs(int value, String label, ExceptionFactory factory) {
        if (value < 0) {
            throw newException(label + " must be non-negative", factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（label 机制 + 错误码 + 指定异常工厂）
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static int isNonNegativeAs(int value, int code, String label, ExceptionFactory factory) {
        if (value < 0) {
            throw newException(code, label + " must be non-negative", factory);
        }
        return value;
    }


// ===================== isNonNegative(long) =====================

    /**
     * 断言数值为非负数（>= 0）
     *
     * @param value 待校验数值
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegative(long value) {
        return isNonNegative(value, "value must be non-negative");
    }

    /**
     * 断言数值为非负数（>= 0）
     *
     * @param value   待校验数值
     * @param message 错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegative(long value, String message) {
        if (value < 0) {
            throw newException(message);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（占位符消息）
     *
     * @param value   待校验数值
     * @param message 错误消息模板（支持占位符）
     * @param args    占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegative(long value, String message, Object... args) {
        if (value < 0) {
            throw newException(formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（延迟构建消息）
     *
     * @param value           待校验数值
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegative(long value, Supplier<String> messageSupplier) {
        if (value < 0) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（延迟构建消息 + 占位符消息）
     *
     * @param value           待校验数值
     * @param messageSupplier 错误消息模板提供者（支持占位符，仅在断言失败时调用）
     * @param args            占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegative(long value, Supplier<String> messageSupplier, Object... args) {
        if (value < 0) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（带错误码）
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegative(long value, int code, String message) {
        if (value < 0) {
            throw newException(code, message);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（带错误码 + 占位符消息）
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息模板（支持占位符）
     * @param args    占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegative(long value, int code, String message, Object... args) {
        if (value < 0) {
            throw newException(code, formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（错误枚举）
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegative(long value, IErrorCode errorCode) {
        if (value < 0) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（错误枚举 + 占位符参数）
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，包含错误码与错误消息模板
     * @param args      占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegative(long value, IErrorCode errorCode, Object... args) {
        if (value < 0) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 默认消息）
     *
     * @param value   待校验数值
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegative(long value, ExceptionFactory factory) {
        if (value < 0) {
            throw newException("value must be non-negative", factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂）
     *
     * @param value   待校验数值
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegative(long value, String message, ExceptionFactory factory) {
        if (value < 0) {
            throw newException(message, factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 占位符参数）
     *
     * @param value   待校验数值
     * @param message 错误消息模板（支持占位符）
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegative(long value, String message, ExceptionFactory factory, Object... args) {
        if (value < 0) {
            throw newException(formatMessage(message, args), factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 带错误码）
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegative(long value, int code, String message, ExceptionFactory factory) {
        if (value < 0) {
            throw newException(code, message, factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 带错误码 + 占位符消息）
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息模板（支持占位符）
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegative(long value, int code, String message, ExceptionFactory factory, Object... args) {
        if (value < 0) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 错误枚举）
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param factory   异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegative(long value, IErrorCode errorCode, ExceptionFactory factory) {
        if (value < 0) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 错误枚举 + 占位符参数）
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，包含错误码与错误消息模板
     * @param factory   异常工厂，用于自定义异常类型
     * @param args      占位符参数
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegative(long value, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (value < 0) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（直接传入异常实例）
     *
     * @param value             待校验数值
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @return 原数值
     * @throws RuntimeException 若数值 < 0，抛出由 exceptionSupplier 提供的异常
     */
    public static long isNonNegativeOrThrow(long value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value < 0) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（label 机制）
     *
     * @param value 待校验数值
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegativeAs(long value, String label) {
        if (value < 0) {
            throw newException(label + " must be non-negative");
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（label 机制 + 带错误码）
     *
     * @param value 待校验数值
     * @param code  错误码
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegativeAs(long value, int code, String label) {
        if (value < 0) {
            throw newException(code, label + " must be non-negative");
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（label 机制 + 指定异常工厂）
     *
     * @param value   待校验数值
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegativeAs(long value, String label, ExceptionFactory factory) {
        if (value < 0) {
            throw newException(label + " must be non-negative", factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（label 机制 + 错误码 + 指定异常工厂）
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static long isNonNegativeAs(long value, int code, String label, ExceptionFactory factory) {
        if (value < 0) {
            throw newException(code, label + " must be non-negative", factory);
        }
        return value;
    }


// ===================== isNonNegative(<T extends Number>) =====================

    /**
     * 断言数值为非负数（>= 0）
     * <p>支持所有 {@link Number} 子类（如 {@link Integer}、{@link Long}、
     * {@link Double}、{@link java.math.BigDecimal} 等），通过 {@code doubleValue()} 进行比较。</p>
     *
     * @param value 待校验数值
     * @param <T>   数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegative(T value) {
        return isNonNegative(value, "value must be non-negative");
    }

    /**
     * 断言数值为非负数（>= 0）
     *
     * @param value   待校验数值
     * @param message 错误消息
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegative(T value, String message) {
        if (!isNonNegativeNumber(value)) {
            throw newException(message);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（占位符消息）
     *
     * @param value   待校验数值
     * @param message 错误消息模板（支持占位符）
     * @param args    占位符参数
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegative(T value, String message, Object... args) {
        if (!isNonNegativeNumber(value)) {
            throw newException(formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（延迟构建消息）
     *
     * @param value           待校验数值
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @param <T>             数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegative(T value, Supplier<String> messageSupplier) {
        if (!isNonNegativeNumber(value)) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（延迟构建消息 + 占位符消息）
     *
     * @param value           待校验数值
     * @param messageSupplier 错误消息模板提供者（支持占位符，仅在断言失败时调用）
     * @param args            占位符参数
     * @param <T>             数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegative(T value, Supplier<String> messageSupplier, Object... args) {
        if (!isNonNegativeNumber(value)) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（带错误码）
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegative(T value, int code, String message) {
        if (!isNonNegativeNumber(value)) {
            throw newException(code, message);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（带错误码 + 占位符消息）
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息模板（支持占位符）
     * @param args    占位符参数
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegative(T value, int code, String message, Object... args) {
        if (!isNonNegativeNumber(value)) {
            throw newException(code, formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（错误枚举）
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param <T>       数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegative(T value, IErrorCode errorCode) {
        if (!isNonNegativeNumber(value)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（错误枚举 + 占位符参数）
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，包含错误码与错误消息模板
     * @param args      占位符参数
     * @param <T>       数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegative(T value, IErrorCode errorCode, Object... args) {
        if (!isNonNegativeNumber(value)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 默认消息）
     *
     * @param value   待校验数值
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegative(T value, ExceptionFactory factory) {
        return isNonNegative(value, "value must be non-negative", factory);
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂）
     *
     * @param value   待校验数值
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegative(T value, String message, ExceptionFactory factory) {
        if (!isNonNegativeNumber(value)) {
            throw newException(message, factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 占位符参数）
     *
     * @param value   待校验数值
     * @param message 错误消息模板（支持占位符）
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegative(T value, String message, ExceptionFactory factory, Object... args) {
        if (!isNonNegativeNumber(value)) {
            throw newException(formatMessage(message, args), factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 带错误码）
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegative(T value, int code, String message, ExceptionFactory factory) {
        if (!isNonNegativeNumber(value)) {
            throw newException(code, message, factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 带错误码 + 占位符消息）
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param message 错误消息模板（支持占位符）
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegative(T value, int code, String message, ExceptionFactory factory, Object... args) {
        if (!isNonNegativeNumber(value)) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 错误枚举）
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param factory   异常工厂，用于自定义异常类型
     * @param <T>       数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegative(T value, IErrorCode errorCode, ExceptionFactory factory) {
        if (!isNonNegativeNumber(value)) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（指定异常工厂 + 错误枚举 + 占位符参数）
     *
     * @param value     待校验数值
     * @param errorCode 错误枚举，包含错误码与错误消息模板
     * @param factory   异常工厂，用于自定义异常类型
     * @param args      占位符参数
     * @param <T>       数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegative(T value, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (!isNonNegativeNumber(value)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（直接传入异常实例）
     *
     * @param value             待校验数值
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @param <T>               数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0，抛出由 exceptionSupplier 提供的异常
     */
    public static <T extends Number> T isNonNegativeOrThrow(T value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!isNonNegativeNumber(value)) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（label 机制）
     *
     * @param value 待校验数值
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @param <T>   数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegativeAs(T value, String label) {
        if (!isNonNegativeNumber(value)) {
            throw newException(label + " must be non-negative");
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（label 机制 + 带错误码）
     *
     * @param value 待校验数值
     * @param code  错误码
     * @param label 字段/参数名称，用于生成语义化错误消息
     * @param <T>   数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegativeAs(T value, int code, String label) {
        if (!isNonNegativeNumber(value)) {
            throw newException(code, label + " must be non-negative");
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（label 机制 + 指定异常工厂）
     *
     * @param value   待校验数值
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegativeAs(T value, String label, ExceptionFactory factory) {
        if (!isNonNegativeNumber(value)) {
            throw newException(label + " must be non-negative", factory);
        }
        return value;
    }

    /**
     * 断言数值为非负数（>= 0）（label 机制 + 错误码 + 指定异常工厂）
     *
     * @param value   待校验数值
     * @param code    错误码
     * @param label   字段/参数名称，用于生成语义化错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     数值类型，必须为 {@link Number} 的子类
     * @return 原数值
     * @throws RuntimeException 若数值 < 0
     */
    public static <T extends Number> T isNonNegativeAs(T value, int code, String label, ExceptionFactory factory) {
        if (!isNonNegativeNumber(value)) {
            throw newException(code, label + " must be non-negative", factory);
        }
        return value;
    }

    // ===================== int 类型 =====================

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）
     *
     * @param value 待校验数值
     * @param min   最小值（含）
     * @param max   最大值（含）
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max) {
        return isBetween(value, min, max, "value must be between " + min + " and " + max + ", but was " + value);
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param message 错误消息
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max, String message) {
        if (value < min || value > max) {
            throw newException(message);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（占位符消息）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param message 错误消息模板，支持 {} 占位符
     * @param args    占位符参数
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max, String message, Object... args) {
        if (value < min || value > max) {
            throw newException(formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（延迟构建消息）
     *
     * @param value           待校验数值
     * @param min             最小值（含）
     * @param max             最大值（含）
     * @param messageSupplier 错误消息提供者，仅在断言失败时调用
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max, Supplier<String> messageSupplier) {
        if (value < min || value > max) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（延迟构建消息 + 占位符）
     *
     * @param value           待校验数值
     * @param min             最小值（含）
     * @param max             最大值（含）
     * @param messageSupplier 错误消息模板提供者，支持 {} 占位符
     * @param args            占位符参数
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max, Supplier<String> messageSupplier, Object... args) {
        if (value < min || value > max) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（带错误码）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param code    错误码
     * @param message 错误消息
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max, int code, String message) {
        if (value < min || value > max) {
            throw newException(code, message);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（带错误码 + 占位符消息）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param code    错误码
     * @param message 错误消息模板，支持 {} 占位符
     * @param args    占位符参数
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max, int code, String message, Object... args) {
        if (value < min || value > max) {
            throw newException(code, formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（错误枚举）
     *
     * @param value     待校验数值
     * @param min       最小值（含）
     * @param max       最大值（含）
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max, IErrorCode errorCode) {
        if (value < min || value > max) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（错误枚举 + 占位符参数）
     *
     * @param value     待校验数值
     * @param min       最小值（含）
     * @param max       最大值（含）
     * @param errorCode 错误枚举，消息模板支持 {} 占位符
     * @param args      占位符参数
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max, IErrorCode errorCode, Object... args) {
        if (value < min || value > max) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 默认消息）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param factory 异常工厂，用于创建自定义异常类型
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max, ExceptionFactory factory) {
        if (value < min || value > max) {
            throw newException("value must be between " + min + " and " + max + ", but was " + value, factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param message 错误消息
     * @param factory 异常工厂，用于创建自定义异常类型
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max, String message, ExceptionFactory factory) {
        if (value < min || value > max) {
            throw newException(message, factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 占位符参数）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param message 错误消息模板，支持 {} 占位符
     * @param factory 异常工厂，用于创建自定义异常类型
     * @param args    占位符参数
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max, String message, ExceptionFactory factory, Object... args) {
        if (value < min || value > max) {
            throw newException(formatMessage(message, args), factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 带错误码）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于创建自定义异常类型
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max, int code, String message, ExceptionFactory factory) {
        if (value < min || value > max) {
            throw newException(code, message, factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 带错误码 + 占位符消息）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param code    错误码
     * @param message 错误消息模板，支持 {} 占位符
     * @param factory 异常工厂，用于创建自定义异常类型
     * @param args    占位符参数
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max, int code, String message, ExceptionFactory factory, Object... args) {
        if (value < min || value > max) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 错误枚举）
     *
     * @param value     待校验数值
     * @param min       最小值（含）
     * @param max       最大值（含）
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param factory   异常工厂，用于创建自定义异常类型
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max, IErrorCode errorCode, ExceptionFactory factory) {
        if (value < min || value > max) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 错误枚举 + 占位符参数）
     *
     * @param value     待校验数值
     * @param min       最小值（含）
     * @param max       最大值（含）
     * @param errorCode 错误枚举，消息模板支持 {} 占位符
     * @param factory   异常工厂，用于创建自定义异常类型
     * @param args      占位符参数
     * @return 原数值
     */
    public static int isBetween(int value, int min, int max, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (value < min || value > max) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（直接传入异常实例）
     *
     * @param value             待校验数值
     * @param min               最小值（含）
     * @param max               最大值（含）
     * @param exceptionSupplier 异常提供者，仅在断言失败时调用
     * @return 原数值
     */
    public static int isBetweenOrThrow(int value, int min, int max, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value < min || value > max) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（label 机制）
     *
     * @param value 待校验数值
     * @param min   最小值（含）
     * @param max   最大值（含）
     * @param label 字段/参数标签，用于生成语义化错误消息，如 "age must be between 1 and 120"
     * @return 原数值
     */
    public static int isBetweenAs(int value, int min, int max, String label) {
        if (value < min || value > max) {
            throw newException(label + " must be between " + min + " and " + max + ", but was " + value);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（label 机制 + 带错误码）
     *
     * @param value 待校验数值
     * @param min   最小值（含）
     * @param max   最大值（含）
     * @param code  错误码
     * @param label 字段/参数标签，用于生成语义化错误消息
     * @return 原数值
     */
    public static int isBetweenAs(int value, int min, int max, int code, String label) {
        if (value < min || value > max) {
            throw newException(code, label + " must be between " + min + " and " + max + ", but was " + value);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（label 机制 + 指定异常工厂）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param label   字段/参数标签，用于生成语义化错误消息
     * @param factory 异常工厂，用于创建自定义异常类型
     * @return 原数值
     */
    public static int isBetweenAs(int value, int min, int max, String label, ExceptionFactory factory) {
        if (value < min || value > max) {
            throw newException(label + " must be between " + min + " and " + max + ", but was " + value, factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（label 机制 + 错误码 + 指定异常工厂）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param code    错误码
     * @param label   字段/参数标签，用于生成语义化错误消息
     * @param factory 异常工厂，用于创建自定义异常类型
     * @return 原数值
     */
    public static int isBetweenAs(int value, int min, int max, int code, String label, ExceptionFactory factory) {
        if (value < min || value > max) {
            throw newException(code, label + " must be between " + min + " and " + max + ", but was " + value, factory);
        }
        return value;
    }


// ===================== long 类型 =====================

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）
     *
     * @param value 待校验数值
     * @param min   最小值（含）
     * @param max   最大值（含）
     * @return 原数值
     */
    public static long isBetween(long value, long min, long max) {
        return isBetween(value, min, max, "value must be between " + min + " and " + max + ", but was " + value);
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param message 错误消息
     * @return 原数值
     */
    public static long isBetween(long value, long min, long max, String message) {
        if (value < min || value > max) {
            throw newException(message);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（占位符消息）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param message 错误消息模板，支持 {} 占位符
     * @param args    占位符参数
     * @return 原数值
     */
    public static long isBetween(long value, long min, long max, String message, Object... args) {
        if (value < min || value > max) {
            throw newException(formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（延迟构建消息）
     *
     * @param value           待校验数值
     * @param min             最小值（含）
     * @param max             最大值（含）
     * @param messageSupplier 错误消息提供者，仅在断言失败时调用
     * @return 原数值
     */
    public static long isBetween(long value, long min, long max, Supplier<String> messageSupplier) {
        if (value < min || value > max) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（延迟构建消息 + 占位符）
     *
     * @param value           待校验数值
     * @param min             最小值（含）
     * @param max             最大值（含）
     * @param messageSupplier 错误消息模板提供者，支持 {} 占位符
     * @param args            占位符参数
     * @return 原数值
     */
    public static long isBetween(long value, long min, long max, Supplier<String> messageSupplier, Object... args) {
        if (value < min || value > max) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（带错误码）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param code    错误码
     * @param message 错误消息
     * @return 原数值
     */
    public static long isBetween(long value, long min, long max, int code, String message) {
        if (value < min || value > max) {
            throw newException(code, message);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（带错误码 + 占位符消息）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param code    错误码
     * @param message 错误消息模板，支持 {} 占位符
     * @param args    占位符参数
     * @return 原数值
     */
    public static long isBetween(long value, long min, long max, int code, String message, Object... args) {
        if (value < min || value > max) {
            throw newException(code, formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（错误枚举）
     *
     * @param value     待校验数值
     * @param min       最小值（含）
     * @param max       最大值（含）
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @return 原数值
     */
    public static long isBetween(long value, long min, long max, IErrorCode errorCode) {
        if (value < min || value > max) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（错误枚举 + 占位符参数）
     *
     * @param value     待校验数值
     * @param min       最小值（含）
     * @param max       最大值（含）
     * @param errorCode 错误枚举，消息模板支持 {} 占位符
     * @param args      占位符参数
     * @return 原数值
     */
    public static long isBetween(long value, long min, long max, IErrorCode errorCode, Object... args) {
        if (value < min || value > max) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 默认消息）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param factory 异常工厂，用于创建自定义异常类型
     * @return 原数值
     */
    public static long isBetween(long value, long min, long max, ExceptionFactory factory) {
        if (value < min || value > max) {
            throw newException("value must be between " + min + " and " + max + ", but was " + value, factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param message 错误消息
     * @param factory 异常工厂，用于创建自定义异常类型
     * @return 原数值
     */
    public static long isBetween(long value, long min, long max, String message, ExceptionFactory factory) {
        if (value < min || value > max) {
            throw newException(message, factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 占位符参数）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param message 错误消息模板，支持 {} 占位符
     * @param factory 异常工厂，用于创建自定义异常类型
     * @param args    占位符参数
     * @return 原数值
     */
    public static long isBetween(long value, long min, long max, String message, ExceptionFactory factory, Object... args) {
        if (value < min || value > max) {
            throw newException(formatMessage(message, args), factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 带错误码）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于创建自定义异常类型
     * @return 原数值
     */
    public static long isBetween(long value, long min, long max, int code, String message, ExceptionFactory factory) {
        if (value < min || value > max) {
            throw newException(code, message, factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 带错误码 + 占位符消息）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param code    错误码
     * @param message 错误消息模板，支持 {} 占位符
     * @param factory 异常工厂，用于创建自定义异常类型
     * @param args    占位符参数
     * @return 原数值
     */
    public static long isBetween(long value, long min, long max, int code, String message, ExceptionFactory factory, Object... args) {
        if (value < min || value > max) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 错误枚举）
     *
     * @param value     待校验数值
     * @param min       最小值（含）
     * @param max       最大值（含）
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param factory   异常工厂，用于创建自定义异常类型
     * @return 原数值
     */
    public static long isBetween(long value, long min, long max, IErrorCode errorCode, ExceptionFactory factory) {
        if (value < min || value > max) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 错误枚举 + 占位符参数）
     *
     * @param value     待校验数值
     * @param min       最小值（含）
     * @param max       最大值（含）
     * @param errorCode 错误枚举，消息模板支持 {} 占位符
     * @param factory   异常工厂，用于创建自定义异常类型
     * @param args      占位符参数
     * @return 原数值
     */
    public static long isBetween(long value, long min, long max, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (value < min || value > max) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（直接传入异常实例）
     *
     * @param value             待校验数值
     * @param min               最小值（含）
     * @param max               最大值（含）
     * @param exceptionSupplier 异常提供者，仅在断言失败时调用
     * @return 原数值
     */
    public static long isBetweenOrThrow(long value, long min, long max, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value < min || value > max) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（label 机制）
     *
     * @param value 待校验数值
     * @param min   最小值（含）
     * @param max   最大值（含）
     * @param label 字段/参数标签，用于生成语义化错误消息，如 "timestamp must be between 0 and 9999999999"
     * @return 原数值
     */
    public static long isBetweenAs(long value, long min, long max, String label) {
        if (value < min || value > max) {
            throw newException(label + " must be between " + min + " and " + max + ", but was " + value);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（label 机制 + 带错误码）
     *
     * @param value 待校验数值
     * @param min   最小值（含）
     * @param max   最大值（含）
     * @param code  错误码
     * @param label 字段/参数标签，用于生成语义化错误消息
     * @return 原数值
     */
    public static long isBetweenAs(long value, long min, long max, int code, String label) {
        if (value < min || value > max) {
            throw newException(code, label + " must be between " + min + " and " + max + ", but was " + value);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（label 机制 + 指定异常工厂）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param label   字段/参数标签，用于生成语义化错误消息
     * @param factory 异常工厂，用于创建自定义异常类型
     * @return 原数值
     */
    public static long isBetweenAs(long value, long min, long max, String label, ExceptionFactory factory) {
        if (value < min || value > max) {
            throw newException(label + " must be between " + min + " and " + max + ", but was " + value, factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（label 机制 + 错误码 + 指定异常工厂）
     *
     * @param value   待校验数值
     * @param min     最小值（含）
     * @param max     最大值（含）
     * @param code    错误码
     * @param label   字段/参数标签，用于生成语义化错误消息
     * @param factory 异常工厂，用于创建自定义异常类型
     * @return 原数值
     */
    public static long isBetweenAs(long value, long min, long max, int code, String label, ExceptionFactory factory) {
        if (value < min || value > max) {
            throw newException(code, label + " must be between " + min + " and " + max + ", but was " + value, factory);
        }
        return value;
    }


    // ===================== 泛型 Number 类型 =====================

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）
     * <p>使用 {@link Comparable#compareTo} 进行比较，适用于 {@link Integer}、{@link Long}、
     * {@link Double}、{@link java.math.BigDecimal} 等所有实现了 {@link Comparable} 的 {@link Number} 子类。</p>
     *
     * @param value 待校验数值，不可为 null
     * @param min   最小值（含），不可为 null
     * @param max   最大值（含），不可为 null
     * @param <T>   数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetween(T value, T min, T max) {
        return isBetween(value, min, max, "value must be between " + min + " and " + max + ", but was " + value);
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）
     *
     * @param value   待校验数值，不可为 null
     * @param min     最小值（含），不可为 null
     * @param max     最大值（含），不可为 null
     * @param message 错误消息
     * @param <T>     数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetween(T value, T min, T max, String message) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(message);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（占位符消息）
     *
     * @param value   待校验数值，不可为 null
     * @param min     最小值（含），不可为 null
     * @param max     最大值（含），不可为 null
     * @param message 错误消息模板，支持 {} 占位符
     * @param args    占位符参数
     * @param <T>     数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetween(T value, T min, T max, String message, Object... args) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（延迟构建消息）
     *
     * @param value           待校验数值，不可为 null
     * @param min             最小值（含），不可为 null
     * @param max             最大值（含），不可为 null
     * @param messageSupplier 错误消息提供者，仅在断言失败时调用
     * @param <T>             数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetween(T value, T min, T max, Supplier<String> messageSupplier) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（延迟构建消息 + 占位符）
     *
     * @param value           待校验数值，不可为 null
     * @param min             最小值（含），不可为 null
     * @param max             最大值（含），不可为 null
     * @param messageSupplier 错误消息模板提供者，支持 {} 占位符
     * @param args            占位符参数
     * @param <T>             数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetween(T value, T min, T max, Supplier<String> messageSupplier, Object... args) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（带错误码）
     *
     * @param value   待校验数值，不可为 null
     * @param min     最小值（含），不可为 null
     * @param max     最大值（含），不可为 null
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetween(T value, T min, T max, int code, String message) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(code, message);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（带错误码 + 占位符消息）
     *
     * @param value   待校验数值，不可为 null
     * @param min     最小值（含），不可为 null
     * @param max     最大值（含），不可为 null
     * @param code    错误码
     * @param message 错误消息模板，支持 {} 占位符
     * @param args    占位符参数
     * @param <T>     数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetween(T value, T min, T max, int code, String message, Object... args) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(code, formatMessage(message, args));
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（错误枚举）
     *
     * @param value     待校验数值，不可为 null
     * @param min       最小值（含），不可为 null
     * @param max       最大值（含），不可为 null
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param <T>       数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetween(T value, T min, T max, IErrorCode errorCode) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（错误枚举 + 占位符参数）
     *
     * @param value     待校验数值，不可为 null
     * @param min       最小值（含），不可为 null
     * @param max       最大值（含），不可为 null
     * @param errorCode 错误枚举，消息模板支持 {} 占位符
     * @param args      占位符参数
     * @param <T>       数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetween(T value, T min, T max, IErrorCode errorCode, Object... args) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 默认消息）
     *
     * @param value   待校验数值，不可为 null
     * @param min     最小值（含），不可为 null
     * @param max     最大值（含），不可为 null
     * @param factory 异常工厂，用于创建自定义异常类型
     * @param <T>     数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetween(T value, T min, T max, ExceptionFactory factory) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException("value must be between " + min + " and " + max + ", but was " + value, factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂）
     *
     * @param value   待校验数值，不可为 null
     * @param min     最小值（含），不可为 null
     * @param max     最大值（含），不可为 null
     * @param message 错误消息
     * @param factory 异常工厂，用于创建自定义异常类型
     * @param <T>     数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetween(T value, T min, T max, String message, ExceptionFactory factory) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(message, factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 占位符参数）
     *
     * @param value   待校验数值，不可为 null
     * @param min     最小值（含），不可为 null
     * @param max     最大值（含），不可为 null
     * @param message 错误消息模板，支持 {} 占位符
     * @param factory 异常工厂，用于创建自定义异常类型
     * @param args    占位符参数
     * @param <T>     数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetween(T value, T min, T max, String message, ExceptionFactory factory, Object... args) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(formatMessage(message, args), factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 带错误码）
     *
     * @param value   待校验数值，不可为 null
     * @param min     最小值（含），不可为 null
     * @param max     最大值（含），不可为 null
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于创建自定义异常类型
     * @param <T>     数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetween(T value, T min, T max, int code, String message, ExceptionFactory factory) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(code, message, factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 带错误码 + 占位符消息）
     *
     * @param value   待校验数值，不可为 null
     * @param min     最小值（含），不可为 null
     * @param max     最大值（含），不可为 null
     * @param code    错误码
     * @param message 错误消息模板，支持 {} 占位符
     * @param factory 异常工厂，用于创建自定义异常类型
     * @param args    占位符参数
     * @param <T>     数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetween(T value, T min, T max, int code, String message, ExceptionFactory factory, Object... args) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 错误枚举）
     *
     * @param value     待校验数值，不可为 null
     * @param min       最小值（含），不可为 null
     * @param max       最大值（含），不可为 null
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param factory   异常工厂，用于创建自定义异常类型
     * @param <T>       数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetween(T value, T min, T max, IErrorCode errorCode, ExceptionFactory factory) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（指定异常工厂 + 错误枚举 + 占位符参数）
     *
     * @param value     待校验数值，不可为 null
     * @param min       最小值（含），不可为 null
     * @param max       最大值（含），不可为 null
     * @param errorCode 错误枚举，消息模板支持 {} 占位符
     * @param factory   异常工厂，用于创建自定义异常类型
     * @param args      占位符参数
     * @param <T>       数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetween(T value, T min, T max, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（直接传入异常实例）
     *
     * @param value             待校验数值，不可为 null
     * @param min               最小值（含），不可为 null
     * @param max               最大值（含），不可为 null
     * @param exceptionSupplier 异常提供者，仅在断言失败时调用
     * @param <T>               数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetweenOrThrow(T value, T min, T max, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（label 机制）
     *
     * @param value 待校验数值，不可为 null
     * @param min   最小值（含），不可为 null
     * @param max   最大值（含），不可为 null
     * @param label 字段/参数标签，用于生成语义化错误消息，如 "score must be between 0.0 and 100.0"
     * @param <T>   数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetweenAs(T value, T min, T max, String label) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(label + " must be between " + min + " and " + max + ", but was " + value);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（label 机制 + 带错误码）
     *
     * @param value 待校验数值，不可为 null
     * @param min   最小值（含），不可为 null
     * @param max   最大值（含），不可为 null
     * @param code  错误码
     * @param label 字段/参数标签，用于生成语义化错误消息
     * @param <T>   数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetweenAs(T value, T min, T max, int code, String label) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(code, label + " must be between " + min + " and " + max + ", but was " + value);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（label 机制 + 指定异常工厂）
     *
     * @param value   待校验数值，不可为 null
     * @param min     最小值（含），不可为 null
     * @param max     最大值（含），不可为 null
     * @param label   字段/参数标签，用于生成语义化错误消息
     * @param factory 异常工厂，用于创建自定义异常类型
     * @param <T>     数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetweenAs(T value, T min, T max, String label, ExceptionFactory factory) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(label + " must be between " + min + " and " + max + ", but was " + value, factory);
        }
        return value;
    }

    /**
     * 断言数值在指定范围内（闭区间 [min, max]）（label 机制 + 错误码 + 指定异常工厂）
     *
     * @param value   待校验数值，不可为 null
     * @param min     最小值（含），不可为 null
     * @param max     最大值（含），不可为 null
     * @param code    错误码
     * @param label   字段/参数标签，用于生成语义化错误消息
     * @param factory 异常工厂，用于创建自定义异常类型
     * @param <T>     数值类型，须同时继承 {@link Number} 并实现 {@link Comparable}
     * @return 原数值
     */
    public static <T extends Number & Comparable<T>> T isBetweenAs(T value, T min, T max, int code, String label, ExceptionFactory factory) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw newException(code, label + " must be between " + min + " and " + max + ", but was " + value, factory);
        }
        return value;
    }

    // ============================================================
    // matches：断言字符串匹配正则表达式
    // ============================================================

    /**
     * 断言字符串匹配正则表达式（默认消息）
     *
     * @param text  待校验字符串
     * @param regex 正则表达式
     * @return 原字符串
     */
    public static String matches(String text, String regex) {
        return matches(text, regex, "text must match regex: " + regex);
    }

    /**
     * 断言字符串匹配正则表达式
     *
     * @param text    待校验字符串
     * @param regex   正则表达式
     * @param message 错误消息
     * @return 原字符串
     */
    public static String matches(String text, String regex, String message) {
        notNull(text, message);
        if (!text.matches(regex)) {
            throw newException(message);
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（占位符消息）
     *
     * @param text    待校验字符串
     * @param regex   正则表达式
     * @param message 错误消息模板（支持占位符）
     * @param args    占位符参数
     * @return 原字符串
     */
    public static String matches(String text, String regex, String message, Object... args) {
        notNull(text, message, args);
        if (!text.matches(regex)) {
            throw newException(formatMessage(message, args));
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（延迟构建消息）
     *
     * @param text            待校验字符串
     * @param regex           正则表达式
     * @param messageSupplier 错误消息供应者
     * @return 原字符串
     */
    public static String matches(String text, String regex, Supplier<String> messageSupplier) {
        notNull(text, messageSupplier);
        if (!text.matches(regex)) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（延迟构建消息 + 占位符）
     *
     * @param text            待校验字符串
     * @param regex           正则表达式
     * @param messageSupplier 错误消息供应者（支持占位符）
     * @param args            占位符参数
     * @return 原字符串
     */
    public static String matches(String text, String regex, Supplier<String> messageSupplier, Object... args) {
        notNull(text, messageSupplier, args);
        if (!text.matches(regex)) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（带错误码）
     *
     * @param text    待校验字符串
     * @param regex   正则表达式
     * @param code    错误码
     * @param message 错误消息
     * @return 原字符串
     */
    public static String matches(String text, String regex, int code, String message) {
        notNull(text, code, message);
        if (!text.matches(regex)) {
            throw newException(code, message);
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（带错误码 + 占位符消息）
     *
     * @param text    待校验字符串
     * @param regex   正则表达式
     * @param code    错误码
     * @param message 错误消息模板（支持占位符）
     * @param args    占位符参数
     * @return 原字符串
     */
    public static String matches(String text, String regex, int code, String message, Object... args) {
        notNull(text, code, message, args);
        if (!text.matches(regex)) {
            throw newException(code, formatMessage(message, args));
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（错误枚举）
     *
     * @param text      待校验字符串
     * @param regex     正则表达式
     * @param errorCode 错误枚举
     * @return 原字符串
     */
    public static String matches(String text, String regex, IErrorCode errorCode) {
        notNull(text, errorCode);
        if (!text.matches(regex)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（错误枚举 + 占位符参数）
     *
     * @param text      待校验字符串
     * @param regex     正则表达式
     * @param errorCode 错误枚举
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String matches(String text, String regex, IErrorCode errorCode, Object... args) {
        notNull(text, errorCode, args);
        if (!text.matches(regex)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（指定异常工厂 + 默认消息）
     *
     * @param text    待校验字符串
     * @param regex   正则表达式
     * @param factory 异常工厂
     * @return 原字符串
     */
    public static String matches(String text, String regex, ExceptionFactory factory) {
        notNull(text, factory);
        if (!text.matches(regex)) {
            throw newException("text must match regex: " + regex, factory);
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（指定异常工厂）
     *
     * @param text    待校验字符串
     * @param regex   正则表达式
     * @param message 错误消息
     * @param factory 异常工厂
     * @return 原字符串
     */
    public static String matches(String text, String regex, String message, ExceptionFactory factory) {
        notNull(text, message, factory);
        if (!text.matches(regex)) {
            throw newException(message, factory);
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（指定异常工厂 + 占位符参数）
     *
     * @param text    待校验字符串
     * @param regex   正则表达式
     * @param message 错误消息模板（支持占位符）
     * @param factory 异常工厂
     * @param args    占位符参数
     * @return 原字符串
     */
    public static String matches(String text, String regex, String message, ExceptionFactory factory, Object... args) {
        notNull(text, message, factory, args);
        if (!text.matches(regex)) {
            throw newException(formatMessage(message, args), factory);
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（指定异常工厂 + 带错误码）
     *
     * @param text    待校验字符串
     * @param regex   正则表达式
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂
     * @return 原字符串
     */
    public static String matches(String text, String regex, int code, String message, ExceptionFactory factory) {
        notNull(text, code, message, factory);
        if (!text.matches(regex)) {
            throw newException(code, message, factory);
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（指定异常工厂 + 带错误码 + 占位符消息）
     *
     * @param text    待校验字符串
     * @param regex   正则表达式
     * @param code    错误码
     * @param message 错误消息模板（支持占位符）
     * @param factory 异常工厂
     * @param args    占位符参数
     * @return 原字符串
     */
    public static String matches(String text, String regex, int code, String message, ExceptionFactory factory, Object... args) {
        notNull(text, code, message, factory, args);
        if (!text.matches(regex)) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（指定异常工厂 + 错误枚举）
     *
     * @param text      待校验字符串
     * @param regex     正则表达式
     * @param errorCode 错误枚举
     * @param factory   异常工厂
     * @return 原字符串
     */
    public static String matches(String text, String regex, IErrorCode errorCode, ExceptionFactory factory) {
        notNull(text, errorCode, factory);
        if (!text.matches(regex)) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（指定异常工厂 + 错误枚举 + 占位符参数）
     *
     * @param text      待校验字符串
     * @param regex     正则表达式
     * @param errorCode 错误枚举
     * @param factory   异常工厂
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String matches(String text, String regex, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        notNull(text, errorCode, factory, args);
        if (!text.matches(regex)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（直接传入异常实例）
     *
     * @param text              待校验字符串
     * @param regex             正则表达式
     * @param exceptionSupplier 异常实例供应者
     * @return 原字符串
     */
    public static String matchesOrThrow(String text, String regex, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (text == null || !text.matches(regex)) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（label 机制）
     *
     * @param text  待校验字符串
     * @param regex 正则表达式
     * @param label 字段标签
     * @return 原字符串
     */
    public static String matchesAs(String text, String regex, String label) {
        notNullAs(text, label);
        if (!text.matches(regex)) {
            throw newException(label + " must match regex: " + regex);
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（label 机制 + 带错误码）
     *
     * @param text  待校验字符串
     * @param regex 正则表达式
     * @param code  错误码
     * @param label 字段标签
     * @return 原字符串
     */
    public static String matchesAs(String text, String regex, int code, String label) {
        notNullAs(text, code, label);
        if (!text.matches(regex)) {
            throw newException(code, label + " must match regex: " + regex);
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（label 机制 + 指定异常工厂）
     *
     * @param text    待校验字符串
     * @param regex   正则表达式
     * @param label   字段标签
     * @param factory 异常工厂
     * @return 原字符串
     */
    public static String matchesAs(String text, String regex, String label, ExceptionFactory factory) {
        notNullAs(text, label, factory);
        if (!text.matches(regex)) {
            throw newException(label + " must match regex: " + regex, factory);
        }
        return text;
    }

    /**
     * 断言字符串匹配正则表达式（label 机制 + 错误码 + 指定异常工厂）
     *
     * @param text    待校验字符串
     * @param regex   正则表达式
     * @param code    错误码
     * @param label   字段标签
     * @param factory 异常工厂
     * @return 原字符串
     */
    public static String matchesAs(String text, String regex, int code, String label, ExceptionFactory factory) {
        notNullAs(text, label, factory);
        if (!text.matches(regex)) {
            throw newException(code, label + " must match regex: " + regex, factory);
        }
        return text;
    }


    // ============================================================
    // doesNotContain：断言字符串不包含指定子串
    // ============================================================

    /**
     * 断言字符串不包含指定子串（默认消息）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @return 原字符串
     */
    public static String doesNotContain(String text, String substring) {
        return doesNotContain(text, substring, "text must not contain: " + substring);
    }

    /**
     * 断言字符串不包含指定子串
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param message   错误消息
     * @return 原字符串
     */
    public static String doesNotContain(String text, String substring, String message) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(message);
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（占位符消息）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param message   错误消息模板（支持占位符）
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String doesNotContain(String text, String substring, String message, Object... args) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(formatMessage(message, args));
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（延迟构建消息）
     *
     * @param text            待校验字符串
     * @param substring       不允许出现的子串
     * @param messageSupplier 错误消息供应者
     * @return 原字符串
     */
    public static String doesNotContain(String text, String substring, Supplier<String> messageSupplier) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（延迟构建消息 + 占位符）
     *
     * @param text            待校验字符串
     * @param substring       不允许出现的子串
     * @param messageSupplier 错误消息供应者（支持占位符）
     * @param args            占位符参数
     * @return 原字符串
     */
    public static String doesNotContain(String text, String substring, Supplier<String> messageSupplier, Object... args) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（带错误码）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param code      错误码
     * @param message   错误消息
     * @return 原字符串
     */
    public static String doesNotContain(String text, String substring, int code, String message) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(code, message);
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（带错误码 + 占位符消息）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param code      错误码
     * @param message   错误消息模板（支持占位符）
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String doesNotContain(String text, String substring, int code, String message, Object... args) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(code, formatMessage(message, args));
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（错误枚举）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param errorCode 错误枚举
     * @return 原字符串
     */
    public static String doesNotContain(String text, String substring, IErrorCode errorCode) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（错误枚举 + 占位符参数）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param errorCode 错误枚举
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String doesNotContain(String text, String substring, IErrorCode errorCode, Object... args) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（指定异常工厂 + 默认消息）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param factory   异常工厂
     * @return 原字符串
     */
    public static String doesNotContain(String text, String substring, ExceptionFactory factory) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException("text must not contain: " + substring, factory);
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（指定异常工厂）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param message   错误消息
     * @param factory   异常工厂
     * @return 原字符串
     */
    public static String doesNotContain(String text, String substring, String message, ExceptionFactory factory) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(message, factory);
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（指定异常工厂 + 占位符参数）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param message   错误消息模板（支持占位符）
     * @param factory   异常工厂
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String doesNotContain(String text, String substring, String message, ExceptionFactory factory, Object... args) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(formatMessage(message, args), factory);
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（指定异常工厂 + 带错误码）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param code      错误码
     * @param message   错误消息
     * @param factory   异常工厂
     * @return 原字符串
     */
    public static String doesNotContain(String text, String substring, int code, String message, ExceptionFactory factory) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(code, message, factory);
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（指定异常工厂 + 带错误码 + 占位符消息）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param code      错误码
     * @param message   错误消息模板（支持占位符）
     * @param factory   异常工厂
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String doesNotContain(String text, String substring, int code, String message, ExceptionFactory factory, Object... args) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（指定异常工厂 + 错误枚举）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param errorCode 错误枚举
     * @param factory   异常工厂
     * @return 原字符串
     */
    public static String doesNotContain(String text, String substring, IErrorCode errorCode, ExceptionFactory factory) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（指定异常工厂 + 错误枚举 + 占位符参数）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param errorCode 错误枚举
     * @param factory   异常工厂
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String doesNotContain(String text, String substring, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（直接传入异常实例）
     *
     * @param text              待校验字符串
     * @param substring         不允许出现的子串
     * @param exceptionSupplier 异常实例供应者
     * @return 原字符串
     */
    public static String doesNotContainOrThrow(String text, String substring, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (text != null && substring != null && text.contains(substring)) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（label 机制）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param label     字段标签
     * @return 原字符串
     */
    public static String doesNotContainAs(String text, String substring, String label) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(label + " must not contain: " + substring);
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（label 机制 + 带错误码）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param code      错误码
     * @param label     字段标签
     * @return 原字符串
     */
    public static String doesNotContainAs(String text, String substring, int code, String label) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(code, label + " must not contain: " + substring);
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（label 机制 + 指定异常工厂）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param label     字段标签
     * @param factory   异常工厂
     * @return 原字符串
     */
    public static String doesNotContainAs(String text, String substring, String label, ExceptionFactory factory) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(label + " must not contain: " + substring, factory);
        }
        return text;
    }

    /**
     * 断言字符串不包含指定子串（label 机制 + 错误码 + 指定异常工厂）
     *
     * @param text      待校验字符串
     * @param substring 不允许出现的子串
     * @param code      错误码
     * @param label     字段标签
     * @param factory   异常工厂
     * @return 原字符串
     */
    public static String doesNotContainAs(String text, String substring, int code, String label, ExceptionFactory factory) {
        if (text != null && substring != null && text.contains(substring)) {
            throw newException(code, label + " must not contain: " + substring, factory);
        }
        return text;
    }


    // ============================================================
    // contains：断言字符串包含指定子串
    // ============================================================

    /**
     * 断言字符串包含指定子串（默认消息）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @return 原字符串
     */
    public static String contains(String text, String substring) {
        return contains(text, substring, "text must contain: " + substring);
    }

    /**
     * 断言字符串包含指定子串
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param message   错误消息
     * @return 原字符串
     */
    public static String contains(String text, String substring, String message) {
        notNull(text, message);
        if (substring != null && !text.contains(substring)) {
            throw newException(message);
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（占位符消息）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param message   错误消息模板（支持占位符）
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String contains(String text, String substring, String message, Object... args) {
        notNull(text, message, args);
        if (substring != null && !text.contains(substring)) {
            throw newException(formatMessage(message, args));
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（延迟构建消息）
     *
     * @param text            待校验字符串
     * @param substring       必须包含的子串
     * @param messageSupplier 错误消息供应者
     * @return 原字符串
     */
    public static String contains(String text, String substring, Supplier<String> messageSupplier) {
        notNull(text, messageSupplier);
        if (substring != null && !text.contains(substring)) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（延迟构建消息 + 占位符）
     *
     * @param text            待校验字符串
     * @param substring       必须包含的子串
     * @param messageSupplier 错误消息供应者（支持占位符）
     * @param args            占位符参数
     * @return 原字符串
     */
    public static String contains(String text, String substring, Supplier<String> messageSupplier, Object... args) {
        notNull(text, formatMessage(nullSafeGet(messageSupplier), args));
        if (substring != null && !text.contains(substring)) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（带错误码）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param code      错误码
     * @param message   错误消息
     * @return 原字符串
     */
    public static String contains(String text, String substring, int code, String message) {
        notNull(text, message);
        if (substring != null && !text.contains(substring)) {
            throw newException(code, message);
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（带错误码 + 占位符消息）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param code      错误码
     * @param message   错误消息模板（支持占位符）
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String contains(String text, String substring, int code, String message, Object... args) {
        notNull(text, formatMessage(message, args));
        if (substring != null && !text.contains(substring)) {
            throw newException(code, formatMessage(message, args));
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（错误枚举）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param errorCode 错误枚举
     * @return 原字符串
     */
    public static String contains(String text, String substring, IErrorCode errorCode) {
        notNull(text, errorCode.getMessage());
        if (substring != null && !text.contains(substring)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（错误枚举 + 占位符参数）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param errorCode 错误枚举
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String contains(String text, String substring, IErrorCode errorCode, Object... args) {
        notNull(text, formatMessage(errorCode.getMessage(), args));
        if (substring != null && !text.contains(substring)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（指定异常工厂 + 默认消息）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param factory   异常工厂
     * @return 原字符串
     */
    public static String contains(String text, String substring, ExceptionFactory factory) {
        notNull(text, "text must contain: " + substring, factory);
        if (substring != null && !text.contains(substring)) {
            throw newException("text must contain: " + substring, factory);
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（指定异常工厂）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param message   错误消息
     * @param factory   异常工厂
     * @return 原字符串
     */
    public static String contains(String text, String substring, String message, ExceptionFactory factory) {
        notNull(text, message, factory);
        if (substring != null && !text.contains(substring)) {
            throw newException(message, factory);
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（指定异常工厂 + 占位符参数）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param message   错误消息模板（支持占位符）
     * @param factory   异常工厂
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String contains(String text, String substring, String message, ExceptionFactory factory, Object... args) {
        notNull(text, formatMessage(message, args), factory);
        if (substring != null && !text.contains(substring)) {
            throw newException(formatMessage(message, args), factory);
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（指定异常工厂 + 带错误码）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param code      错误码
     * @param message   错误消息
     * @param factory   异常工厂
     * @return 原字符串
     */
    public static String contains(String text, String substring, int code, String message, ExceptionFactory factory) {
        notNull(text, message, factory);
        if (substring != null && !text.contains(substring)) {
            throw newException(code, message, factory);
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（指定异常工厂 + 带错误码 + 占位符消息）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param code      错误码
     * @param message   错误消息模板（支持占位符）
     * @param factory   异常工厂
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String contains(String text, String substring, int code, String message, ExceptionFactory factory, Object... args) {
        notNull(text, formatMessage(message, args), factory);
        if (substring != null && !text.contains(substring)) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（指定异常工厂 + 错误枚举）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param errorCode 错误枚举
     * @param factory   异常工厂
     * @return 原字符串
     */
    public static String contains(String text, String substring, IErrorCode errorCode, ExceptionFactory factory) {
        notNull(text, errorCode.getMessage(), factory);
        if (substring != null && !text.contains(substring)) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（指定异常工厂 + 错误枚举 + 占位符参数）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param errorCode 错误枚举
     * @param factory   异常工厂
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String contains(String text, String substring, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        notNull(text, formatMessage(errorCode.getMessage(), args), factory);
        if (substring != null && !text.contains(substring)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（直接传入异常实例）
     *
     * @param text              待校验字符串
     * @param substring         必须包含的子串
     * @param exceptionSupplier 异常实例供应者
     * @return 原字符串
     */
    public static String containsOrThrow(String text, String substring, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (text == null || (substring != null && !text.contains(substring))) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（label 机制）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param label     字段标签
     * @return 原字符串
     */
    public static String containsAs(String text, String substring, String label) {
        notNull(text, label + " must not be null");
        if (substring != null && !text.contains(substring)) {
            throw newException(label + " must contain: " + substring);
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（label 机制 + 带错误码）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param code      错误码
     * @param label     字段标签
     * @return 原字符串
     */
    public static String containsAs(String text, String substring, int code, String label) {
        notNull(text, label + " must not be null");
        if (substring != null && !text.contains(substring)) {
            throw newException(code, label + " must contain: " + substring);
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（label 机制 + 指定异常工厂）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param label     字段标签
     * @param factory   异常工厂
     * @return 原字符串
     */
    public static String containsAs(String text, String substring, String label, ExceptionFactory factory) {
        notNull(text, label + " must not be null", factory);
        if (substring != null && !text.contains(substring)) {
            throw newException(label + " must contain: " + substring, factory);
        }
        return text;
    }

    /**
     * 断言字符串包含指定子串（label 机制 + 错误码 + 指定异常工厂）
     *
     * @param text      待校验字符串
     * @param substring 必须包含的子串
     * @param code      错误码
     * @param label     字段标签
     * @param factory   异常工厂
     * @return 原字符串
     */
    public static String containsAs(String text, String substring, int code, String label, ExceptionFactory factory) {
        notNull(text, label + " must not be null", factory);
        if (substring != null && !text.contains(substring)) {
            throw newException(code, label + " must contain: " + substring, factory);
        }
        return text;
    }


    // ============================================================
    // startsWith：断言字符串以指定前缀开头
    // ============================================================

    /**
     * 断言字符串以指定前缀开头（默认消息）
     *
     * @param text   待校验字符串
     * @param prefix 必须匹配的前缀
     * @return 原字符串
     */
    public static String startsWith(String text, String prefix) {
        return startsWith(text, prefix, "text must start with: " + prefix);
    }

    /**
     * 断言字符串以指定前缀开头
     *
     * @param text    待校验字符串
     * @param prefix  必须匹配的前缀
     * @param message 错误消息
     * @return 原字符串
     */
    public static String startsWith(String text, String prefix, String message) {
        notNull(text, message);
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(message);
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（占位符消息）
     *
     * @param text    待校验字符串
     * @param prefix  必须匹配的前缀
     * @param message 错误消息模板（支持占位符）
     * @param args    占位符参数
     * @return 原字符串
     */
    public static String startsWith(String text, String prefix, String message, Object... args) {
        notNull(text, formatMessage(message, args));
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(formatMessage(message, args));
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（延迟构建消息）
     *
     * @param text            待校验字符串
     * @param prefix          必须匹配的前缀
     * @param messageSupplier 错误消息供应者
     * @return 原字符串
     */
    public static String startsWith(String text, String prefix, Supplier<String> messageSupplier) {
        notNull(text, messageSupplier);
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（延迟构建消息 + 占位符）
     *
     * @param text            待校验字符串
     * @param prefix          必须匹配的前缀
     * @param messageSupplier 错误消息供应者（支持占位符）
     * @param args            占位符参数
     * @return 原字符串
     */
    public static String startsWith(String text, String prefix, Supplier<String> messageSupplier, Object... args) {
        notNull(text, formatMessage(nullSafeGet(messageSupplier), args));
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（带错误码）
     *
     * @param text    待校验字符串
     * @param prefix  必须匹配的前缀
     * @param code    错误码
     * @param message 错误消息
     * @return 原字符串
     */
    public static String startsWith(String text, String prefix, int code, String message) {
        notNull(text, message);
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(code, message);
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（带错误码 + 占位符消息）
     *
     * @param text    待校验字符串
     * @param prefix  必须匹配的前缀
     * @param code    错误码
     * @param message 错误消息模板（支持占位符）
     * @param args    占位符参数
     * @return 原字符串
     */
    public static String startsWith(String text, String prefix, int code, String message, Object... args) {
        notNull(text, formatMessage(message, args));
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(code, formatMessage(message, args));
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（错误枚举）
     *
     * @param text      待校验字符串
     * @param prefix    必须匹配的前缀
     * @param errorCode 错误枚举
     * @return 原字符串
     */
    public static String startsWith(String text, String prefix, IErrorCode errorCode) {
        notNull(text, errorCode.getMessage());
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（错误枚举 + 占位符参数）
     *
     * @param text      待校验字符串
     * @param prefix    必须匹配的前缀
     * @param errorCode 错误枚举
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String startsWith(String text, String prefix, IErrorCode errorCode, Object... args) {
        notNull(text, formatMessage(errorCode.getMessage(), args));
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（指定异常工厂 + 默认消息）
     *
     * @param text    待校验字符串
     * @param prefix  必须匹配的前缀
     * @param factory 异常工厂
     * @return 原字符串
     */
    public static String startsWith(String text, String prefix, ExceptionFactory factory) {
        notNull(text, "text must start with: " + prefix, factory);
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException("text must start with: " + prefix, factory);
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（指定异常工厂）
     *
     * @param text    待校验字符串
     * @param prefix  必须匹配的前缀
     * @param message 错误消息
     * @param factory 异常工厂
     * @return 原字符串
     */
    public static String startsWith(String text, String prefix, String message, ExceptionFactory factory) {
        notNull(text, message, factory);
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(message, factory);
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（指定异常工厂 + 占位符参数）
     *
     * @param text    待校验字符串
     * @param prefix  必须匹配的前缀
     * @param message 错误消息模板（支持占位符）
     * @param factory 异常工厂
     * @param args    占位符参数
     * @return 原字符串
     */
    public static String startsWith(String text, String prefix, String message, ExceptionFactory factory, Object... args) {
        notNull(text, formatMessage(message, args), factory);
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(formatMessage(message, args), factory);
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（指定异常工厂 + 带错误码）
     *
     * @param text    待校验字符串
     * @param prefix  必须匹配的前缀
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂
     * @return 原字符串
     */
    public static String startsWith(String text, String prefix, int code, String message, ExceptionFactory factory) {
        notNull(text, message, factory);
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(code, message, factory);
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（指定异常工厂 + 带错误码 + 占位符消息）
     *
     * @param text    待校验字符串
     * @param prefix  必须匹配的前缀
     * @param code    错误码
     * @param message 错误消息模板（支持占位符）
     * @param factory 异常工厂
     * @param args    占位符参数
     * @return 原字符串
     */
    public static String startsWith(String text, String prefix, int code, String message, ExceptionFactory factory, Object... args) {
        notNull(text, formatMessage(message, args), factory);
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（指定异常工厂 + 错误枚举）
     *
     * @param text      待校验字符串
     * @param prefix    必须匹配的前缀
     * @param errorCode 错误枚举
     * @param factory   异常工厂
     * @return 原字符串
     */
    public static String startsWith(String text, String prefix, IErrorCode errorCode, ExceptionFactory factory) {
        notNull(text, errorCode.getMessage(), factory);
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（指定异常工厂 + 错误枚举 + 占位符参数）
     *
     * @param text      待校验字符串
     * @param prefix    必须匹配的前缀
     * @param errorCode 错误枚举
     * @param factory   异常工厂
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String startsWith(String text, String prefix, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        notNull(text, formatMessage(errorCode.getMessage(), args), factory);
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（直接传入异常实例）
     *
     * @param text              待校验字符串
     * @param prefix            必须匹配的前缀
     * @param exceptionSupplier 异常实例供应者
     * @return 原字符串
     */
    public static String startsWithOrThrow(String text, String prefix, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (text == null || (prefix != null && !text.startsWith(prefix))) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（label 机制）
     *
     * @param text   待校验字符串
     * @param prefix 必须匹配的前缀
     * @param label  字段标签
     * @return 原字符串
     */
    public static String startsWithAs(String text, String prefix, String label) {
        notNull(text, label + " must not be null");
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(label + " must start with: " + prefix);
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（label 机制 + 带错误码）
     *
     * @param text   待校验字符串
     * @param prefix 必须匹配的前缀
     * @param code   错误码
     * @param label  字段标签
     * @return 原字符串
     */
    public static String startsWithAs(String text, String prefix, int code, String label) {
        notNull(text, label + " must not be null");
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(code, label + " must start with: " + prefix);
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（label 机制 + 指定异常工厂）
     *
     * @param text    待校验字符串
     * @param prefix  必须匹配的前缀
     * @param label   字段标签
     * @param factory 异常工厂
     * @return 原字符串
     */
    public static String startsWithAs(String text, String prefix, String label, ExceptionFactory factory) {
        notNull(text, label + " must not be null", factory);
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(label + " must start with: " + prefix, factory);
        }
        return text;
    }

    /**
     * 断言字符串以指定前缀开头（label 机制 + 错误码 + 指定异常工厂）
     *
     * @param text    待校验字符串
     * @param prefix  必须匹配的前缀
     * @param code    错误码
     * @param label   字段标签
     * @param factory 异常工厂
     * @return 原字符串
     */
    public static String startsWithAs(String text, String prefix, int code, String label, ExceptionFactory factory) {
        notNull(text, label + " must not be null", factory);
        if (prefix != null && !text.startsWith(prefix)) {
            throw newException(code, label + " must start with: " + prefix, factory);
        }
        return text;
    }


    // ============================================================
    // endsWith：断言字符串以指定后缀结尾
    // ============================================================

    /**
     * 断言字符串以指定后缀结尾（默认消息）
     *
     * @param text   待校验字符串
     * @param suffix 必须匹配的后缀
     * @return 原字符串
     */
    public static String endsWith(String text, String suffix) {
        return endsWith(text, suffix, "text must end with: " + suffix);
    }

    /**
     * 断言字符串以指定后缀结尾
     *
     * @param text    待校验字符串
     * @param suffix  必须匹配的后缀
     * @param message 错误消息
     * @return 原字符串
     */
    public static String endsWith(String text, String suffix, String message) {
        notNull(text, message);
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(message);
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（占位符消息）
     *
     * @param text    待校验字符串
     * @param suffix  必须匹配的后缀
     * @param message 错误消息模板（支持占位符）
     * @param args    占位符参数
     * @return 原字符串
     */
    public static String endsWith(String text, String suffix, String message, Object... args) {
        notNull(text, formatMessage(message, args));
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(formatMessage(message, args));
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（延迟构建消息）
     *
     * @param text            待校验字符串
     * @param suffix          必须匹配的后缀
     * @param messageSupplier 错误消息供应者
     * @return 原字符串
     */
    public static String endsWith(String text, String suffix, Supplier<String> messageSupplier) {
        notNull(text, messageSupplier);
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(nullSafeGet(messageSupplier));
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（延迟构建消息 + 占位符）
     *
     * @param text            待校验字符串
     * @param suffix          必须匹配的后缀
     * @param messageSupplier 错误消息供应者（支持占位符）
     * @param args            占位符参数
     * @return 原字符串
     */
    public static String endsWith(String text, String suffix, Supplier<String> messageSupplier, Object... args) {
        notNull(text, formatMessage(nullSafeGet(messageSupplier), args));
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(formatMessage(nullSafeGet(messageSupplier), args));
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（带错误码）
     *
     * @param text    待校验字符串
     * @param suffix  必须匹配的后缀
     * @param code    错误码
     * @param message 错误消息
     * @return 原字符串
     */
    public static String endsWith(String text, String suffix, int code, String message) {
        notNull(text, message);
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(code, message);
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（带错误码 + 占位符消息）
     *
     * @param text    待校验字符串
     * @param suffix  必须匹配的后缀
     * @param code    错误码
     * @param message 错误消息模板（支持占位符）
     * @param args    占位符参数
     * @return 原字符串
     */
    public static String endsWith(String text, String suffix, int code, String message, Object... args) {
        notNull(text, formatMessage(message, args));
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(code, formatMessage(message, args));
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（错误枚举）
     *
     * @param text      待校验字符串
     * @param suffix    必须匹配的后缀
     * @param errorCode 错误枚举
     * @return 原字符串
     */
    public static String endsWith(String text, String suffix, IErrorCode errorCode) {
        notNull(text, errorCode.getMessage());
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(errorCode.getCode(), errorCode.getMessage());
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（错误枚举 + 占位符参数）
     *
     * @param text      待校验字符串
     * @param suffix    必须匹配的后缀
     * @param errorCode 错误枚举
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String endsWith(String text, String suffix, IErrorCode errorCode, Object... args) {
        notNull(text, formatMessage(errorCode.getMessage(), args));
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（指定异常工厂 + 默认消息）
     *
     * @param text    待校验字符串
     * @param suffix  必须匹配的后缀
     * @param factory 异常工厂
     * @return 原字符串
     */
    public static String endsWith(String text, String suffix, ExceptionFactory factory) {
        notNull(text, "text must end with: " + suffix, factory);
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException("text must end with: " + suffix, factory);
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（指定异常工厂）
     *
     * @param text    待校验字符串
     * @param suffix  必须匹配的后缀
     * @param message 错误消息
     * @param factory 异常工厂
     * @return 原字符串
     */
    public static String endsWith(String text, String suffix, String message, ExceptionFactory factory) {
        notNull(text, message, factory);
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(message, factory);
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（指定异常工厂 + 占位符参数）
     *
     * @param text    待校验字符串
     * @param suffix  必须匹配的后缀
     * @param message 错误消息模板（支持占位符）
     * @param factory 异常工厂
     * @param args    占位符参数
     * @return 原字符串
     */
    public static String endsWith(String text, String suffix, String message, ExceptionFactory factory, Object... args) {
        notNull(text, formatMessage(message, args), factory);
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(formatMessage(message, args), factory);
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（指定异常工厂 + 带错误码）
     *
     * @param text    待校验字符串
     * @param suffix  必须匹配的后缀
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂
     * @return 原字符串
     */
    public static String endsWith(String text, String suffix, int code, String message, ExceptionFactory factory) {
        notNull(text, message, factory);
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(code, message, factory);
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（指定异常工厂 + 带错误码 + 占位符消息）
     *
     * @param text    待校验字符串
     * @param suffix  必须匹配的后缀
     * @param code    错误码
     * @param message 错误消息模板（支持占位符）
     * @param factory 异常工厂
     * @param args    占位符参数
     * @return 原字符串
     */
    public static String endsWith(String text, String suffix, int code, String message, ExceptionFactory factory, Object... args) {
        notNull(text, formatMessage(message, args), factory);
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(code, formatMessage(message, args), factory);
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（指定异常工厂 + 错误枚举）
     *
     * @param text      待校验字符串
     * @param suffix    必须匹配的后缀
     * @param errorCode 错误枚举
     * @param factory   异常工厂
     * @return 原字符串
     */
    public static String endsWith(String text, String suffix, IErrorCode errorCode, ExceptionFactory factory) {
        notNull(text, errorCode.getMessage(), factory);
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（指定异常工厂 + 错误枚举 + 占位符参数）
     *
     * @param text      待校验字符串
     * @param suffix    必须匹配的后缀
     * @param errorCode 错误枚举
     * @param factory   异常工厂
     * @param args      占位符参数
     * @return 原字符串
     */
    public static String endsWith(String text, String suffix, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        notNull(text, formatMessage(errorCode.getMessage(), args), factory);
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（直接传入异常实例）
     *
     * @param text              待校验字符串
     * @param suffix            必须匹配的后缀
     * @param exceptionSupplier 异常实例供应者
     * @return 原字符串
     */
    public static String endsWithOrThrow(String text, String suffix, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (text == null || (suffix != null && !text.endsWith(suffix))) {
            throw nullSafeGetException(exceptionSupplier);
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（label 机制）
     *
     * @param text   待校验字符串
     * @param suffix 必须匹配的后缀
     * @param label  字段标签
     * @return 原字符串
     */
    public static String endsWithAs(String text, String suffix, String label) {
        notNull(text, label + " must not be null");
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(label + " must end with: " + suffix);
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（label 机制 + 带错误码）
     *
     * @param text   待校验字符串
     * @param suffix 必须匹配的后缀
     * @param code   错误码
     * @param label  字段标签
     * @return 原字符串
     */
    public static String endsWithAs(String text, String suffix, int code, String label) {
        notNull(text, label + " must not be null");
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(code, label + " must end with: " + suffix);
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（label 机制 + 指定异常工厂）
     *
     * @param text    待校验字符串
     * @param suffix  必须匹配的后缀
     * @param label   字段标签
     * @param factory 异常工厂
     * @return 原字符串
     */
    public static String endsWithAs(String text, String suffix, String label, ExceptionFactory factory) {
        notNull(text, label + " must not be null", factory);
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(label + " must end with: " + suffix, factory);
        }
        return text;
    }

    /**
     * 断言字符串以指定后缀结尾（label 机制 + 错误码 + 指定异常工厂）
     *
     * @param text    待校验字符串
     * @param suffix  必须匹配的后缀
     * @param code    错误码
     * @param label   字段标签
     * @param factory 异常工厂
     * @return 原字符串
     */
    public static String endsWithAs(String text, String suffix, int code, String label, ExceptionFactory factory) {
        notNull(text, label + " must not be null", factory);
        if (suffix != null && !text.endsWith(suffix)) {
            throw newException(code, label + " must end with: " + suffix, factory);
        }
        return text;
    }

    // ======================== noNullElements - Collection ========================

    /**
     * 断言集合中不含 null 元素（默认消息）
     *
     * @param collection 待校验集合
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection) {
        return noNullElements(collection, "collection must not contain null elements");
    }

    /**
     * 断言集合中不含 null 元素
     *
     * @param collection 待校验集合
     * @param message    错误消息
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection, String message) {
        notNull(collection, message);
        for (E element : collection) {
            if (element == null) {
                throw newException(message);
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（占位符消息）
     *
     * @param collection 待校验集合
     * @param message    错误消息模板（支持占位符）
     * @param args       占位符参数
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection, String message, Object... args) {
        notNull(collection, message, args);
        for (E element : collection) {
            if (element == null) {
                throw newException(formatMessage(message, args));
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（延迟构建消息）
     *
     * @param collection      待校验集合
     * @param messageSupplier 错误消息提供者（延迟求值）
     * @param <E>             元素类型
     * @param <T>             集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection, Supplier<String> messageSupplier) {
        notNull(collection, messageSupplier);
        for (E element : collection) {
            if (element == null) {
                throw newException(nullSafeGet(messageSupplier));
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（延迟构建消息 + 占位符参数）
     *
     * @param collection      待校验集合
     * @param messageSupplier 错误消息模板提供者（延迟求值）
     * @param args            占位符参数
     * @param <E>             元素类型
     * @param <T>             集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection, Supplier<String> messageSupplier, Object... args) {
        notNull(collection, messageSupplier, args);
        for (E element : collection) {
            if (element == null) {
                throw newException(formatMessage(nullSafeGet(messageSupplier), args));
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（带错误码）
     *
     * @param collection 待校验集合
     * @param code       错误码
     * @param message    错误消息
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection, int code, String message) {
        notNull(collection, code, message);
        for (E element : collection) {
            if (element == null) {
                throw newException(code, message);
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（带错误码 + 占位符消息）
     *
     * @param collection 待校验集合
     * @param code       错误码
     * @param message    错误消息模板（支持占位符）
     * @param args       占位符参数
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection, int code, String message, Object... args) {
        notNull(collection, code, message, args);
        for (E element : collection) {
            if (element == null) {
                throw newException(code, formatMessage(message, args));
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（错误枚举）
     *
     * @param collection 待校验集合
     * @param errorCode  错误枚举
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
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
     * 断言集合中不含 null 元素（错误枚举 + 占位符参数）
     *
     * @param collection 待校验集合
     * @param errorCode  错误枚举
     * @param args       占位符参数
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection, IErrorCode errorCode, Object... args) {
        notNull(collection, errorCode, args);
        for (E element : collection) {
            if (element == null) {
                throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（指定异常工厂 + 默认消息）
     *
     * @param collection 待校验集合
     * @param factory    异常工厂
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection, ExceptionFactory factory) {
        notNull(collection, factory);
        for (E element : collection) {
            if (element == null) {
                throw newException("collection must not contain null elements", factory);
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（指定异常工厂）
     *
     * @param collection 待校验集合
     * @param message    错误消息
     * @param factory    异常工厂
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection, String message, ExceptionFactory factory) {
        notNull(collection, message, factory);
        for (E element : collection) {
            if (element == null) {
                throw newException(message, factory);
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（指定异常工厂 + 占位符参数）
     *
     * @param collection 待校验集合
     * @param message    错误消息模板（支持占位符）
     * @param factory    异常工厂
     * @param args       占位符参数
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection, String message, ExceptionFactory factory, Object... args) {
        notNull(collection, message, factory, args);
        for (E element : collection) {
            if (element == null) {
                throw newException(formatMessage(message, args), factory);
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（指定异常工厂 + 带错误码）
     *
     * @param collection 待校验集合
     * @param code       错误码
     * @param message    错误消息
     * @param factory    异常工厂
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection, int code, String message, ExceptionFactory factory) {
        notNull(collection, code, message, factory);
        for (E element : collection) {
            if (element == null) {
                throw newException(code, message, factory);
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（指定异常工厂 + 带错误码 + 占位符消息）
     *
     * @param collection 待校验集合
     * @param code       错误码
     * @param message    错误消息模板（支持占位符）
     * @param factory    异常工厂
     * @param args       占位符参数
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection, int code, String message, ExceptionFactory factory, Object... args) {
        notNull(collection, code, message, factory, args);
        for (E element : collection) {
            if (element == null) {
                throw newException(code, formatMessage(message, args), factory);
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（指定异常工厂 + 错误枚举）
     *
     * @param collection 待校验集合
     * @param errorCode  错误枚举
     * @param factory    异常工厂
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection, IErrorCode errorCode, ExceptionFactory factory) {
        notNull(collection, errorCode, factory);
        for (E element : collection) {
            if (element == null) {
                throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（指定异常工厂 + 错误枚举 + 占位符参数）
     *
     * @param collection 待校验集合
     * @param errorCode  错误枚举
     * @param factory    异常工厂
     * @param args       占位符参数
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElements(T collection, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        notNull(collection, errorCode, factory, args);
        for (E element : collection) {
            if (element == null) {
                throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（直接传入异常实例）
     *
     * @param collection        待校验集合
     * @param exceptionSupplier 异常提供者
     * @param <E>               元素类型
     * @param <T>               集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElementsOrThrow(T collection, Supplier<? extends RuntimeException> exceptionSupplier) {
        notNullOrThrow(collection, exceptionSupplier);
        for (E element : collection) {
            if (element == null) {
                throw nullSafeGetException(exceptionSupplier);
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（label 机制）
     *
     * @param collection 待校验集合
     * @param label      字段标签，用于生成友好错误消息（如 "userList"）
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElementsAs(T collection, String label) {
        notNull(collection, label + " must not be null");
        for (E element : collection) {
            if (element == null) {
                throw newException(label + " must not contain null elements");
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（label 机制 + 带错误码）
     *
     * @param collection 待校验集合
     * @param code       错误码
     * @param label      字段标签，用于生成友好错误消息
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElementsAs(T collection, int code, String label) {
        notNull(collection, code, label + " must not be null");
        for (E element : collection) {
            if (element == null) {
                throw newException(code, label + " must not contain null elements");
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（label 机制 + 指定异常工厂）
     *
     * @param collection 待校验集合
     * @param label      字段标签，用于生成友好错误消息
     * @param factory    异常工厂
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElementsAs(T collection, String label, ExceptionFactory factory) {
        notNull(collection, label + " must not be null", factory);
        for (E element : collection) {
            if (element == null) {
                throw newException(label + " must not contain null elements", factory);
            }
        }
        return collection;
    }

    /**
     * 断言集合中不含 null 元素（label 机制 + 错误码 + 指定异常工厂）
     *
     * @param collection 待校验集合
     * @param code       错误码
     * @param label      字段标签，用于生成友好错误消息
     * @param factory    异常工厂
     * @param <E>        元素类型
     * @param <T>        集合类型
     * @return 原集合
     */
    public static <E, T extends Collection<E>> T noNullElementsAs(T collection, int code, String label, ExceptionFactory factory) {
        notNull(collection, code, label + " must not be null", factory);
        for (E element : collection) {
            if (element == null) {
                throw newException(code, label + " must not contain null elements", factory);
            }
        }
        return collection;
    }


// ======================== noNullElements - Array ========================

    /**
     * 断言数组中不含 null 元素（默认消息）
     *
     * @param array 待校验数组
     * @param <T>   元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElements(T[] array) {
        return noNullElements(array, "array must not contain null elements");
    }

    /**
     * 断言数组中不含 null 元素
     *
     * @param array   待校验数组
     * @param message 错误消息
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElements(T[] array, String message) {
        notNull(array, message);
        for (T element : array) {
            if (element == null) {
                throw newException(message);
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（占位符消息）
     *
     * @param array   待校验数组
     * @param message 错误消息模板（支持占位符）
     * @param args    占位符参数
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElements(T[] array, String message, Object... args) {
        notNull(array, message, args);
        for (T element : array) {
            if (element == null) {
                throw newException(formatMessage(message, args));
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（延迟构建消息）
     *
     * @param array           待校验数组
     * @param messageSupplier 错误消息提供者（延迟求值）
     * @param <T>             元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElements(T[] array, Supplier<String> messageSupplier) {
        notNull(array, messageSupplier);
        for (T element : array) {
            if (element == null) {
                throw newException(nullSafeGet(messageSupplier));
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（延迟构建消息 + 占位符参数）
     *
     * @param array           待校验数组
     * @param messageSupplier 错误消息模板提供者（延迟求值）
     * @param args            占位符参数
     * @param <T>             元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElements(T[] array, Supplier<String> messageSupplier, Object... args) {
        notNull(array, messageSupplier, args);
        for (T element : array) {
            if (element == null) {
                throw newException(formatMessage(nullSafeGet(messageSupplier), args));
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（带错误码）
     *
     * @param array   待校验数组
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElements(T[] array, int code, String message) {
        notNull(array, code, message);
        for (T element : array) {
            if (element == null) {
                throw newException(code, message);
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（带错误码 + 占位符消息）
     *
     * @param array   待校验数组
     * @param code    错误码
     * @param message 错误消息模板（支持占位符）
     * @param args    占位符参数
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElements(T[] array, int code, String message, Object... args) {
        notNull(array, code, message, args);
        for (T element : array) {
            if (element == null) {
                throw newException(code, formatMessage(message, args));
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（错误枚举）
     *
     * @param array     待校验数组
     * @param errorCode 错误枚举
     * @param <T>       元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElements(T[] array, IErrorCode errorCode) {
        notNull(array, errorCode);
        for (T element : array) {
            if (element == null) {
                throw newException(errorCode.getCode(), errorCode.getMessage());
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（错误枚举 + 占位符参数）
     *
     * @param array     待校验数组
     * @param errorCode 错误枚举
     * @param args      占位符参数
     * @param <T>       元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElements(T[] array, IErrorCode errorCode, Object... args) {
        notNull(array, errorCode, args);
        for (T element : array) {
            if (element == null) {
                throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（指定异常工厂 + 默认消息）
     *
     * @param array   待校验数组
     * @param factory 异常工厂
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElements(T[] array, ExceptionFactory factory) {
        notNull(array, factory);
        for (T element : array) {
            if (element == null) {
                throw newException("array must not contain null elements", factory);
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（指定异常工厂）
     *
     * @param array   待校验数组
     * @param message 错误消息
     * @param factory 异常工厂
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElements(T[] array, String message, ExceptionFactory factory) {
        notNull(array, message, factory);
        for (T element : array) {
            if (element == null) {
                throw newException(message, factory);
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（指定异常工厂 + 占位符参数）
     *
     * @param array   待校验数组
     * @param message 错误消息模板（支持占位符）
     * @param factory 异常工厂
     * @param args    占位符参数
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElements(T[] array, String message, ExceptionFactory factory, Object... args) {
        notNull(array, message, factory, args);
        for (T element : array) {
            if (element == null) {
                throw newException(formatMessage(message, args), factory);
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（指定异常工厂 + 带错误码）
     *
     * @param array   待校验数组
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElements(T[] array, int code, String message, ExceptionFactory factory) {
        notNull(array, code, message, factory);
        for (T element : array) {
            if (element == null) {
                throw newException(code, message, factory);
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（指定异常工厂 + 带错误码 + 占位符消息）
     *
     * @param array   待校验数组
     * @param code    错误码
     * @param message 错误消息模板（支持占位符）
     * @param factory 异常工厂
     * @param args    占位符参数
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElements(T[] array, int code, String message, ExceptionFactory factory, Object... args) {
        notNull(array, code, message, factory, args);
        for (T element : array) {
            if (element == null) {
                throw newException(code, formatMessage(message, args), factory);
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（指定异常工厂 + 错误枚举）
     *
     * @param array     待校验数组
     * @param errorCode 错误枚举
     * @param factory   异常工厂
     * @param <T>       元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElements(T[] array, IErrorCode errorCode, ExceptionFactory factory) {
        notNull(array, errorCode, factory);
        for (T element : array) {
            if (element == null) {
                throw newException(errorCode.getCode(), errorCode.getMessage(), factory);
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（指定异常工厂 + 错误枚举 + 占位符参数）
     *
     * @param array     待校验数组
     * @param errorCode 错误枚举
     * @param factory   异常工厂
     * @param args      占位符参数
     * @param <T>       元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElements(T[] array, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        notNull(array, errorCode, factory, args);
        for (T element : array) {
            if (element == null) {
                throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args), factory);
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（直接传入异常实例）
     *
     * @param array             待校验数组
     * @param exceptionSupplier 异常提供者
     * @param <T>               元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElementsOrThrow(T[] array, Supplier<? extends RuntimeException> exceptionSupplier) {
        notNullOrThrow(array, exceptionSupplier);
        for (T element : array) {
            if (element == null) {
                throw nullSafeGetException(exceptionSupplier);
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（label 机制）
     *
     * @param array 待校验数组
     * @param label 字段标签，用于生成友好错误消息（如 "ids"）
     * @param <T>   元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElementsAs(T[] array, String label) {
        notNull(array, label + " must not be null");
        for (T element : array) {
            if (element == null) {
                throw newException(label + " must not contain null elements");
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（label 机制 + 带错误码）
     *
     * @param array 待校验数组
     * @param code  错误码
     * @param label 字段标签，用于生成友好错误消息
     * @param <T>   元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElementsAs(T[] array, int code, String label) {
        notNull(array, code, label + " must not be null");
        for (T element : array) {
            if (element == null) {
                throw newException(code, label + " must not contain null elements");
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（label 机制 + 指定异常工厂）
     *
     * @param array   待校验数组
     * @param label   字段标签，用于生成友好错误消息
     * @param factory 异常工厂
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElementsAs(T[] array, String label, ExceptionFactory factory) {
        notNull(array, label + " must not be null", factory);
        for (T element : array) {
            if (element == null) {
                throw newException(label + " must not contain null elements", factory);
            }
        }
        return array;
    }

    /**
     * 断言数组中不含 null 元素（label 机制 + 错误码 + 指定异常工厂）
     *
     * @param array   待校验数组
     * @param code    错误码
     * @param label   字段标签，用于生成友好错误消息
     * @param factory 异常工厂
     * @param <T>     元素类型
     * @return 原数组
     */
    public static <T> T[] noNullElementsAs(T[] array, int code, String label, ExceptionFactory factory) {
        notNull(array, code, label + " must not be null", factory);
        for (T element : array) {
            if (element == null) {
                throw newException(code, label + " must not contain null elements", factory);
            }
        }
        return array;
    }

    // ========================================================================
    //  state（语义同 isTrue，但用于状态校验场景）
    // ========================================================================

    /**
     * 断言状态条件为 true（无消息，使用默认消息 "state check failed"）。
     * <p>语义上用于校验对象/业务状态，与 {@link #isTrue} （参数校验）区分，便于在代码中表达不同的校验意图。</p>
     *
     * @param expression 状态条件表达式
     * @throws RuntimeException 若表达式为 false
     */
    public static void state(boolean expression) {
        isTrue(expression, "state check failed");
    }

    /**
     * 断言状态条件为 true。
     *
     * @param expression 状态条件表达式
     * @param message    错误消息
     * @throws RuntimeException 若表达式为 false
     */
    public static void state(boolean expression, String message) {
        isTrue(expression, message);
    }

    /**
     * 断言状态条件为 true（占位符消息）。
     *
     * @param expression 状态条件表达式
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 false
     */
    public static void state(boolean expression, String message, Object... args) {
        isTrue(expression, message, args);
    }

    /**
     * 断言状态条件为 true（延迟构建消息）。
     *
     * @param expression      状态条件表达式
     * @param messageSupplier 错误消息提供者（仅在断言失败时调用）
     * @throws RuntimeException 若表达式为 false
     */
    public static void state(boolean expression, Supplier<String> messageSupplier) {
        isTrue(expression, messageSupplier);
    }

    /**
     * 断言状态条件为 true（延迟构建消息 + 占位符参数）。
     *
     * @param expression      状态条件表达式
     * @param messageSupplier 错误消息模板提供者（仅在断言失败时调用）
     * @param args            占位符参数
     * @throws RuntimeException 若表达式为 false
     */
    public static void state(boolean expression, Supplier<String> messageSupplier, Object... args) {
        isTrue(expression, messageSupplier, args);
    }

    /**
     * 断言状态条件为 true（带错误码）。
     *
     * @param expression 状态条件表达式
     * @param code       错误码
     * @param message    错误消息
     * @throws RuntimeException 若表达式为 false
     */
    public static void state(boolean expression, int code, String message) {
        isTrue(expression, code, message);
    }

    /**
     * 断言状态条件为 true（带错误码 + 占位符参数）。
     *
     * @param expression 状态条件表达式
     * @param code       错误码
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 false
     */
    public static void state(boolean expression, int code, String message, Object... args) {
        isTrue(expression, code, message, args);
    }

    /**
     * 断言状态条件为 true（错误枚举）。
     *
     * @param expression 状态条件表达式
     * @param errorCode  错误枚举，包含错误码与错误消息
     * @throws RuntimeException 若表达式为 false
     */
    public static void state(boolean expression, IErrorCode errorCode) {
        isTrue(expression, errorCode);
    }

    /**
     * 断言状态条件为 true（错误枚举 + 占位符参数）。
     *
     * @param expression 状态条件表达式
     * @param errorCode  错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 false
     */
    public static void state(boolean expression, IErrorCode errorCode, Object... args) {
        isTrue(expression, errorCode, args);
    }

    /**
     * 断言状态条件为 true（指定异常工厂 + 默认消息）。
     *
     * @param expression 状态条件表达式
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 false
     */
    public static void state(boolean expression, ExceptionFactory factory) {
        isTrue(expression, "state check failed", factory);
    }

    /**
     * 断言状态条件为 true（指定异常工厂）。
     *
     * @param expression 状态条件表达式
     * @param message    错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 false
     */
    public static void state(boolean expression, String message, ExceptionFactory factory) {
        isTrue(expression, message, factory);
    }

    /**
     * 断言状态条件为 true（指定异常工厂 + 占位符参数）。
     *
     * @param expression 状态条件表达式
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory    异常工厂，用于自定义异常类型
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 false
     */
    public static void state(boolean expression, String message, ExceptionFactory factory, Object... args) {
        isTrue(expression, message, factory, args);
    }

    /**
     * 断言状态条件为 true（指定异常工厂 + 错误码）。
     *
     * @param expression 状态条件表达式
     * @param code       错误码
     * @param message    错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 false
     */
    public static void state(boolean expression, int code, String message, ExceptionFactory factory) {
        isTrue(expression, code, message, factory);
    }

    /**
     * 断言状态条件为 true（指定异常工厂 + 错误码 + 占位符参数）。
     *
     * @param expression 状态条件表达式
     * @param code       错误码
     * @param message    错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory    异常工厂，用于自定义异常类型
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 false
     */
    public static void state(boolean expression, int code, String message, ExceptionFactory factory, Object... args) {
        isTrue(expression, code, message, factory, args);
    }

    /**
     * 断言状态条件为 true（指定异常工厂 + 错误枚举）。
     *
     * @param expression 状态条件表达式
     * @param errorCode  错误枚举，包含错误码与错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 false
     */
    public static void state(boolean expression, IErrorCode errorCode, ExceptionFactory factory) {
        isTrue(expression, errorCode, factory);
    }

    /**
     * 断言状态条件为 true（指定异常工厂 + 错误枚举 + 占位符参数）。
     *
     * @param expression 状态条件表达式
     * @param errorCode  错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param factory    异常工厂，用于自定义异常类型
     * @param args       占位符参数
     * @throws RuntimeException 若表达式为 false
     */
    public static void state(boolean expression, IErrorCode errorCode, ExceptionFactory factory, Object... args) {
        isTrue(expression, errorCode, factory, args);
    }

    /**
     * 断言状态条件为 true（直接传入异常实例）。
     *
     * @param expression        状态条件表达式
     * @param exceptionSupplier 异常提供者（仅在断言失败时调用）
     * @throws RuntimeException 若表达式为 false，抛出由 exceptionSupplier 提供的异常
     */
    public static void stateOrThrow(boolean expression, Supplier<? extends RuntimeException> exceptionSupplier) {
        isTrueOrThrow(expression, exceptionSupplier);
    }

    /**
     * 断言状态条件为 true（label 机制）。
     * <p>自动生成消息：{label} check failed</p>
     *
     * @param expression 状态条件表达式
     * @param label      状态/字段名称，用于生成语义化错误消息
     * @throws RuntimeException 若表达式为 false
     */
    public static void stateAs(boolean expression, String label) {
        if (!expression) {
            throw newException(ErrorCodes.UNSPECIFIED, label + " check failed");
        }
    }

    /**
     * 断言状态条件为 true（label 机制 + 错误码）。
     *
     * @param expression 状态条件表达式
     * @param code       错误码
     * @param label      状态/字段名称，用于生成语义化错误消息
     * @throws RuntimeException 若表达式为 false
     */
    public static void stateAs(boolean expression, int code, String label) {
        if (!expression) {
            throw newException(code, label + " check failed");
        }
    }

    /**
     * 断言状态条件为 true（label 机制 + 指定异常工厂）。
     *
     * @param expression 状态条件表达式
     * @param label      状态/字段名称，用于生成语义化错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 false
     */
    public static void stateAs(boolean expression, String label, ExceptionFactory factory) {
        if (!expression) {
            throw newException(ErrorCodes.UNSPECIFIED, label + " check failed", factory);
        }
    }

    /**
     * 断言状态条件为 true（label 机制 + 错误码 + 指定异常工厂）。
     *
     * @param expression 状态条件表达式
     * @param code       错误码
     * @param label      状态/字段名称，用于生成语义化错误消息
     * @param factory    异常工厂，用于自定义异常类型
     * @throws RuntimeException 若表达式为 false
     */
    public static void stateAs(boolean expression, int code, String label, ExceptionFactory factory) {
        if (!expression) {
            throw newException(code, label + " check failed", factory);
        }
    }

    // ========================================================================
    //  fail — 直接抛出异常（用于不可达分支等场景）
    // ========================================================================

    /**
     * 直接抛出异常（用于不可达分支、switch default 等场景）。
     * <p>声明泛型返回类型 {@code <T>} 以便在表达式中使用（实际永远不会返回）。</p>
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
     * @param <T>     声明返回类型以便在表达式中使用（实际不会返回）
     * @return 声明返回类型以便在表达式中使用（实际不会返回）
     * @throws RuntimeException 总是抛出
     */
    public static <T> T fail(String message) {
        throw newException(message);
    }

    /**
     * 直接抛出异常（占位符消息）。
     *
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @param <T>     声明返回类型以便在表达式中使用（实际不会返回）
     * @return 声明返回类型以便在表达式中使用（实际不会返回）
     * @throws RuntimeException 总是抛出
     */
    public static <T> T fail(String message, Object... args) {
        throw newException(formatMessage(message, args));
    }

    /**
     * 直接抛出异常（带错误码）。
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     声明返回类型以便在表达式中使用（实际不会返回）
     * @return 声明返回类型以便在表达式中使用（实际不会返回）
     * @throws RuntimeException 总是抛出
     */
    public static <T> T fail(int code, String message) {
        throw newException(code, message);
    }

    /**
     * 直接抛出异常（带错误码 + 占位符参数）。
     *
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param args    占位符参数
     * @param <T>     声明返回类型以便在表达式中使用（实际不会返回）
     * @return 声明返回类型以便在表达式中使用（实际不会返回）
     * @throws RuntimeException 总是抛出
     */
    public static <T> T fail(int code, String message, Object... args) {
        throw newException(code, formatMessage(message, args));
    }

    /**
     * 直接抛出异常（错误枚举）。
     *
     * @param errorCode 错误枚举，包含错误码与错误消息
     * @param <T>       声明返回类型以便在表达式中使用（实际不会返回）
     * @return 声明返回类型以便在表达式中使用（实际不会返回）
     * @throws RuntimeException 总是抛出
     */
    public static <T> T fail(IErrorCode errorCode) {
        throw newException(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 直接抛出异常（错误枚举 + 占位符参数）。
     *
     * @param errorCode 错误枚举，消息模板支持 {@code {}, {}, ...} 占位符
     * @param args      占位符参数
     * @param <T>       声明返回类型以便在表达式中使用（实际不会返回）
     * @return 声明返回类型以便在表达式中使用（实际不会返回）
     * @throws RuntimeException 总是抛出
     */
    public static <T> T fail(IErrorCode errorCode, Object... args) {
        throw newException(errorCode.getCode(), formatMessage(errorCode.getMessage(), args));
    }

    /**
     * 直接抛出异常（指定异常工厂）。
     *
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     声明返回类型以便在表达式中使用（实际不会返回）
     * @return 声明返回类型以便在表达式中使用（实际不会返回）
     * @throws RuntimeException 总是抛出
     */
    public static <T> T fail(String message, ExceptionFactory factory) {
        throw newException(message, factory);
    }

    /**
     * 直接抛出异常（指定异常工厂 + 占位符参数）。
     *
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @param <T>     声明返回类型以便在表达式中使用（实际不会返回）
     * @return 声明返回类型以便在表达式中使用（实际不会返回）
     * @throws RuntimeException 总是抛出
     */
    public static <T> T fail(String message, ExceptionFactory factory, Object... args) {
        throw newException(formatMessage(message, args), factory);
    }

    /**
     * 直接抛出异常（指定异常工厂 + 错误码）。
     *
     * @param code    错误码
     * @param message 错误消息
     * @param factory 异常工厂，用于自定义异常类型
     * @param <T>     声明返回类型以便在表达式中使用（实际不会返回）
     * @return 声明返回类型以便在表达式中使用（实际不会返回）
     * @throws RuntimeException 总是抛出
     */
    public static <T> T fail(int code, String message, ExceptionFactory factory) {
        throw newException(code, message, factory);
    }

    /**
     * 直接抛出异常（指定异常工厂 + 错误码 + 占位符参数）。
     *
     * @param code    错误码
     * @param message 错误消息模板，支持 {@code {}, {}, ...} 占位符
     * @param factory 异常工厂，用于自定义异常类型
     * @param args    占位符参数
     * @param <T>     声明返回类型以便在表达式中使用（实际不会返回）
     * @return 声明返回类型以便在表达式中使用（实际不会返回）
     * @throws RuntimeException 总是抛出
     */
    public static <T> T fail(int code, String message, ExceptionFactory factory, Object... args) {
        throw newException(code, formatMessage(message, args), factory);
    }

    // ========================================================================
    //  内部工具方法
    // ========================================================================

    private static boolean isPositiveNumber(Number value) {

        if (value == null) {
            return false;
        }

        if (value instanceof Integer) {
            return value.intValue() > 0;
        }
        if (value instanceof Long) {
            return value.longValue() > 0;
        }

        // BigDecimal / BigInteger：必须走 compareTo，避免精度丢失
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).compareTo(BigDecimal.ZERO) > 0;
        }

        if (value instanceof Short) {
            return value.shortValue() > 0;
        }
        if (value instanceof Byte) {
            return value.byteValue() > 0;
        }

        if (value instanceof BigInteger) {
            return ((BigInteger) value).compareTo(BigInteger.ZERO) > 0;
        }

        if (value instanceof Float) {
            return value.floatValue() > 0f;
        }
        return Double.compare(value.doubleValue(), 0d) > 0;
    }

    private static boolean isNonNegativeNumber(Number value) {

        if (value == null) {
            return false;
        }

        if (value instanceof Integer) {
            return value.intValue() >= 0;
        }
        if (value instanceof Long) {
            return value.longValue() >= 0;
        }

        // BigDecimal / BigInteger：必须走 compareTo，避免精度丢失
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).compareTo(BigDecimal.ZERO) > -1;
        }

        if (value instanceof Short) {
            return value.shortValue() >= 0;
        }
        if (value instanceof Byte) {
            return value.byteValue() >= 0;
        }

        if (value instanceof BigInteger) {
            return ((BigInteger) value).compareTo(BigInteger.ZERO) > -1;
        }

        if (value instanceof Float) {
            return value.floatValue() >= 0f;
        }
        return Double.compare(value.doubleValue(), 0d) > -1;
    }

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