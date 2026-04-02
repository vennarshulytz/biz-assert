package io.github.vennarshulytz.bizassert;

import io.github.vennarshulytz.bizassert.constants.ErrorCodes;
import io.github.vennarshulytz.bizassert.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BizAssert 单元测试
 *
 * @author vennarshulytz
 * @since 1.0.0
 */
class BizAssertTest {

    @BeforeEach
    void resetFactory() {
        BizAssert.resetDefaultExceptionFactory();
    }

    // ==================== 全局配置 ====================

    @Nested
    @DisplayName("全局异常工厂配置")
    class GlobalFactoryTest {

        @Test
        @DisplayName("默认工厂抛出 BizException")
        void defaultFactory() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notNull(null, "test"));
            assertEquals("test", ex.getMessage());
            assertEquals(ErrorCodes.UNSPECIFIED, ex.getCode());
        }

        @Test
        @DisplayName("设置自定义工厂后生效")
        void customFactory() {
            BizAssert.setDefaultExceptionFactory((code, msg) ->
                    new IllegalStateException("[" + code + "] " + msg));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> BizAssert.notNull(null, 10001, "test"));
            assertEquals("[10001] test", ex.getMessage());
        }

        @Test
        @DisplayName("重复设置抛出 IllegalStateException")
        void duplicateSet() {
            BizAssert.setDefaultExceptionFactory(BizException::new);
            assertThrows(IllegalStateException.class,
                    () -> BizAssert.setDefaultExceptionFactory(BizException::new));
        }

        @Test
        @DisplayName("工厂不可为 null")
        void nullFactory() {
            assertThrows(NullPointerException.class,
                    () -> BizAssert.setDefaultExceptionFactory(null));
        }
    }

    // ==================== isTrue / isFalse ====================

    @Nested
    @DisplayName("isTrue / isFalse")
    class IsTrueIsFalseTest {

        @Test
        void isTrue_pass() {
            assertDoesNotThrow(() -> BizAssert.isTrue(true));
            assertDoesNotThrow(() -> BizAssert.isTrue(true, "msg"));
        }

        @Test
        void isTrue_fail_defaultMessage() {
            BizException ex = assertThrows(BizException.class, () -> BizAssert.isTrue(false));
            assertEquals("expression must be true", ex.getMessage());
        }

        @Test
        void isTrue_fail_customMessage() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.isTrue(false, "order invalid"));
            assertEquals("order invalid", ex.getMessage());
        }

        @Test
        void isTrue_fail_withCode() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.isTrue(false, 10001, "order invalid"));
            assertEquals(10001, ex.getCode());
        }

        @Test
        void isTrue_fail_supplier() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.isTrue(false, () -> "lazy msg"));
            assertEquals("lazy msg", ex.getMessage());
        }

        @Test
        void isTrue_fail_errorCode() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.isTrue(false, BizError.USER_NOT_NULL));
            assertEquals(10001, ex.getCode());
            assertEquals("user must not be null", ex.getMessage());
        }

        @Test
        void isTrue_fail_errorCodeWithArgs() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.isTrue(false, BizError.USER_IS_NULL, "userId"));
            assertEquals("userId is null", ex.getMessage());
        }

        @Test
        void isTrue_fail_customFactory() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> BizAssert.isTrue(false, "bad",
                            ExceptionFactory.ofMessage(IllegalArgumentException::new)));
            assertEquals("bad", ex.getMessage());
        }

        @Test
        void isTrueOrThrow_fail() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> BizAssert.isTrueOrThrow(false,
                            () -> new IllegalStateException("custom")));
            assertEquals("custom", ex.getMessage());
        }

        @Test
        void isFalse_pass() {
            assertDoesNotThrow(() -> BizAssert.isFalse(false));
        }

        @Test
        void isFalse_fail() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.isFalse(true, "should be false"));
            assertEquals("should be false", ex.getMessage());
        }

        @Test
        void isFalse_fail_supplier() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.isFalse(true, () -> "lazy false msg"));
            assertEquals("lazy false msg", ex.getMessage());
        }

        @Test
        void isFalseOrThrow_fail() {
            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                    () -> BizAssert.isFalseOrThrow(true,
                            () -> new UnsupportedOperationException("nope")));
            assertEquals("nope", ex.getMessage());
        }
    }

    // ==================== notNull / isNull ====================

    @Nested
    @DisplayName("notNull / isNull")
    class NotNullTest {

        @Test
        void notNull_pass_returnValue() {
            String result = BizAssert.notNull("hello", "msg");
            assertEquals("hello", result);
        }

        @Test
        void notNull_pass_noMessage() {
            Object obj = new Object();
            assertSame(obj, BizAssert.notNull(obj));
        }

        @Test
        void notNull_fail_defaultMessage() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notNull(null));
            assertEquals("parameter must not be null", ex.getMessage());
        }

        @Test
        void notNull_fail_customMessage() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notNull(null, "user is null"));
            assertEquals("user is null", ex.getMessage());
        }

        @Test
        void notNull_fail_withCode() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notNull(null, 10001, "user is null"));
            assertEquals(10001, ex.getCode());
        }

        @Test
        void notNull_fail_supplier() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notNull(null, () -> "lazy null msg"));
            assertEquals("lazy null msg", ex.getMessage());
        }

        @Test
        void notNull_fail_placeholder() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notNull(null, "{} must not be null", "userId"));
            assertEquals("userId must not be null", ex.getMessage());
        }

        @Test
        void notNull_fail_placeholder_nullArg() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notNull(null, "{} is missing", (Object) null));
            assertEquals("<null> is missing", ex.getMessage());
        }

        @Test
        void notNull_fail_errorCode() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notNull(null, BizError.USER_NOT_NULL));
            assertEquals(10001, ex.getCode());
            assertEquals("user must not be null", ex.getMessage());
        }

        @Test
        void notNull_fail_errorCodeWithArgs() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notNull(null, BizError.USER_IS_NULL, "admin"));
            assertEquals("admin is null", ex.getMessage());
        }

        @Test
        void notNull_fail_customFactory() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> BizAssert.notNull(null, "bad",
                            ExceptionFactory.ofMessage(IllegalArgumentException::new)));
            assertEquals("bad", ex.getMessage());
        }

        @Test
        void notNullOrThrow_fail() {
            ArithmeticException ex = assertThrows(ArithmeticException.class,
                    () -> BizAssert.notNullOrThrow(null,
                            () -> new ArithmeticException("custom")));
            assertEquals("custom", ex.getMessage());
        }

        @Test
        void notNullOrThrow_pass() {
            String result = BizAssert.notNullOrThrow("hello",
                    () -> new ArithmeticException("custom"));
            assertEquals("hello", result);
        }

        @Test
        void notNullAs_pass() {
            assertEquals("value", BizAssert.notNullAs("value", "param"));
        }

        @Test
        void notNullAs_fail() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notNullAs(null, "userId"));
            assertEquals("userId must not be null", ex.getMessage());
        }

        @Test
        void notNullAs_fail_withCode() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notNullAs(null, 10001, "userId"));
            assertEquals(10001, ex.getCode());
            assertEquals("userId must not be null", ex.getMessage());
        }

        @Test
        void isNull_pass() {
            assertDoesNotThrow(() -> BizAssert.isNull(null));
            assertDoesNotThrow(() -> BizAssert.isNull(null, "msg"));
        }

        @Test
        void isNull_fail() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.isNull("not null", "should be null"));
            assertEquals("should be null", ex.getMessage());
        }
    }

    // ==================== notEmpty (String) ====================

    @Nested
    @DisplayName("notEmpty (String)")
    class NotEmptyStringTest {

        @Test
        void pass() {
            assertEquals("hello", BizAssert.notEmpty("hello", "msg"));
        }

        @Test
        void fail_null() {
            assertThrows(BizException.class, () -> BizAssert.notEmpty((String) null, "msg"));
        }

        @Test
        void fail_empty() {
            assertThrows(BizException.class, () -> BizAssert.notEmpty("", "msg"));
        }

        @Test
        void fail_defaultMessage() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notEmpty((String) null));
            assertEquals("parameter must not be empty", ex.getMessage());
        }

        @Test
        void pass_withBlankString() {
            // notEmpty 只校验空和 null，空格字符串应该通过
            assertEquals("  ", BizAssert.notEmpty("  ", "msg"));
        }

        @Test
        void label() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notEmptyAs((String) null, "name"));
            assertEquals("name must not be empty", ex.getMessage());
        }

        @Test
        void orThrow() {
            assertThrows(IllegalStateException.class,
                    () -> BizAssert.notEmptyOrThrow("",
                            () -> new IllegalStateException("empty")));
        }
    }

    // ==================== notEmpty (Collection) ====================

    @Nested
    @DisplayName("notEmpty (Collection)")
    class NotEmptyCollectionTest {

        @Test
        void pass() {
            List<String> list = Arrays.asList("a", "b");
            assertSame(list, BizAssert.notEmpty(list, "msg"));
        }

        @Test
        void fail_null() {
            assertThrows(BizException.class,
                    () -> BizAssert.notEmpty((Collection<?>) null, "msg"));
        }

        @Test
        void fail_empty() {
            assertThrows(BizException.class,
                    () -> BizAssert.notEmpty(Collections.emptyList(), "msg"));
        }

        @Test
        void fail_defaultMessage() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notEmpty((Collection<?>) null));
            assertEquals("collection must not be empty", ex.getMessage());
        }

        @Test
        void label() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notEmptyAs(Collections.emptyList(), "userList"));
            assertEquals("userList must not be empty", ex.getMessage());
        }
    }

    // ==================== notEmpty (Map) ====================

    @Nested
    @DisplayName("notEmpty (Map)")
    class NotEmptyMapTest {

        @Test
        void pass() {
            Map<String, String> map = Collections.singletonMap("k", "v");
            assertSame(map, BizAssert.notEmpty(map, "msg"));
        }

        @Test
        void fail_null() {
            assertThrows(BizException.class,
                    () -> BizAssert.notEmpty((Map<?, ?>) null, "msg"));
        }

        @Test
        void fail_empty() {
            assertThrows(BizException.class,
                    () -> BizAssert.notEmpty(Collections.emptyMap(), "msg"));
        }

        @Test
        void fail_defaultMessage() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notEmpty((Map<?, ?>) null));
            assertEquals("map must not be empty", ex.getMessage());
        }
    }

    // ==================== notEmpty (Array) ====================

    @Nested
    @DisplayName("notEmpty (Array)")
    class NotEmptyArrayTest {

        @Test
        void pass() {
            String[] arr = {"a", "b"};
            assertSame(arr, BizAssert.notEmpty(arr, "msg"));
        }

        @Test
        void fail_null() {
            assertThrows(BizException.class,
                    () -> BizAssert.notEmpty((Object[]) null, "msg"));
        }

        @Test
        void fail_empty() {
            assertThrows(BizException.class,
                    () -> BizAssert.notEmpty(new String[0], "msg"));
        }

        @Test
        void fail_defaultMessage() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notEmpty((Object[]) null));
            assertEquals("array must not be empty", ex.getMessage());
        }
    }

    // ==================== notBlank ====================

    @Nested
    @DisplayName("notBlank")
    class NotBlankTest {

        @Test
        void pass() {
            assertEquals("hello", BizAssert.notBlank("hello", "msg"));
        }

        @Test
        void pass_notTrimmed() {
            // 不做 trim，返回原值
            assertEquals("  hello  ", BizAssert.notBlank("  hello  ", "msg"));
        }

        @Test
        void fail_null() {
            assertThrows(BizException.class, () -> BizAssert.notBlank(null, "msg"));
        }

        @Test
        void fail_empty() {
            assertThrows(BizException.class, () -> BizAssert.notBlank("", "msg"));
        }

        @Test
        void fail_blank() {
            assertThrows(BizException.class, () -> BizAssert.notBlank("   ", "msg"));
        }

        @Test
        void fail_tab() {
            assertThrows(BizException.class, () -> BizAssert.notBlank("\t\n", "msg"));
        }

        @Test
        void fail_defaultMessage() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notBlank(null));
            assertEquals("parameter must not be blank", ex.getMessage());
        }

        @Test
        void label() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notBlankAs("  ", "username"));
            assertEquals("username must not be blank", ex.getMessage());
        }

        @Test
        void orThrow() {
            assertThrows(IllegalStateException.class,
                    () -> BizAssert.notBlankOrThrow("  ",
                            () -> new IllegalStateException("blank")));
        }
    }

    // ==================== isEqual / notEqual ====================

    @Nested
    @DisplayName("isEqual / notEqual")
    class EqualTest {

        @Test
        void isEqual_pass() {
            assertDoesNotThrow(() -> BizAssert.isEqual("a", "a", "msg"));
            assertDoesNotThrow(() -> BizAssert.isEqual(null, null, "msg"));
        }

        @Test
        void isEqual_fail() {
            assertThrows(BizException.class,
                    () -> BizAssert.isEqual("a", "b", "not equal"));
        }

        @Test
        void isEqual_fail_errorCode() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.isEqual("a", "b", BizError.PARAM_INVALID));
            assertEquals(10003, ex.getCode());
        }

        @Test
        void notEqual_pass() {
            assertDoesNotThrow(() -> BizAssert.notEqual("a", "b", "msg"));
        }

        @Test
        void notEqual_fail() {
            assertThrows(BizException.class,
                    () -> BizAssert.notEqual("a", "a", "should differ"));
        }
    }

    // ==================== 数值断言 ====================

    @Nested
    @DisplayName("数值断言")
    class NumericTest {

        @Test
        void isPositive_int_pass() {
            assertEquals(5, BizAssert.isPositive(5, "msg"));
        }

        @Test
        void isPositive_int_fail_zero() {
            assertThrows(BizException.class, () -> BizAssert.isPositive(0, "msg"));
        }

        @Test
        void isPositive_int_fail_negative() {
            assertThrows(BizException.class, () -> BizAssert.isPositive(-1, "msg"));
        }

        @Test
        void isPositive_long_pass() {
            assertEquals(100L, BizAssert.isPositive(100L, "msg"));
        }

        @Test
        void isPositive_label() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.isPositiveAs(0, "amount"));
            assertEquals("amount must be positive", ex.getMessage());
        }

        @Test
        void isNonNegative_int_pass() {
            assertEquals(0, BizAssert.isNonNegative(0, "msg"));
            assertEquals(5, BizAssert.isNonNegative(5, "msg"));
        }

        @Test
        void isNonNegative_int_fail() {
            assertThrows(BizException.class, () -> BizAssert.isNonNegative(-1, "msg"));
        }

        @Test
        void isNonNegative_label() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.isNonNegativeAs(-1, "balance"));
            assertEquals("balance must be non-negative", ex.getMessage());
        }

        @Test
        void isBetween_int_pass() {
            assertEquals(5, BizAssert.isBetween(5, 1, 10, "msg"));
            assertEquals(1, BizAssert.isBetween(1, 1, 10, "msg"));
            assertEquals(10, BizAssert.isBetween(10, 1, 10, "msg"));
        }

        @Test
        void isBetween_int_fail_below() {
            assertThrows(BizException.class, () -> BizAssert.isBetween(0, 1, 10, "msg"));
        }

        @Test
        void isBetween_int_fail_above() {
            assertThrows(BizException.class, () -> BizAssert.isBetween(11, 1, 10, "msg"));
        }

        @Test
        void isBetween_defaultMessage() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.isBetween(0, 1, 10));
            assertTrue(ex.getMessage().contains("between"));
            assertTrue(ex.getMessage().contains("0"));
        }

        @Test
        void isBetween_label() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.isBetweenAs(0, 1, 10, "age"));
            assertTrue(ex.getMessage().startsWith("age"));
        }
    }

    // ==================== 字符串模式匹配 ====================

    @Nested
    @DisplayName("字符串模式匹配")
    class PatternTest {

        @Test
        void matches_pass() {
            assertEquals("abc123", BizAssert.matches("abc123", "[a-z]+\\d+", "msg"));
        }

        @Test
        void matches_fail() {
            assertThrows(BizException.class,
                    () -> BizAssert.matches("abc", "\\d+", "must be digits"));
        }

        @Test
        void matches_null() {
            assertThrows(BizException.class,
                    () -> BizAssert.matches(null, "\\d+", "msg"));
        }
    }

    // ==================== contains / doesNotContain / startsWith / endsWith ====================

    @Nested
    @DisplayName("字符串包含/前缀/后缀")
    class StringContainsTest {

        @Test
        void contains_pass() {
            assertEquals("hello world", BizAssert.contains("hello world", "world", "msg"));
        }

        @Test
        void contains_fail() {
            assertThrows(BizException.class,
                    () -> BizAssert.contains("hello", "world", "msg"));
        }

        @Test
        void doesNotContain_pass() {
            assertDoesNotThrow(() -> BizAssert.doesNotContain("hello", "world", "msg"));
        }

        @Test
        void doesNotContain_fail() {
            assertThrows(BizException.class,
                    () -> BizAssert.doesNotContain("hello world", "world", "msg"));
        }

        @Test
        void startsWith_pass() {
            assertEquals("hello", BizAssert.startsWith("hello", "hel", "msg"));
        }

        @Test
        void startsWith_fail() {
            assertThrows(BizException.class,
                    () -> BizAssert.startsWith("hello", "world", "msg"));
        }

        @Test
        void endsWith_pass() {
            assertEquals("hello", BizAssert.endsWith("hello", "llo", "msg"));
        }

        @Test
        void endsWith_fail() {
            assertThrows(BizException.class,
                    () -> BizAssert.endsWith("hello", "world", "msg"));
        }
    }

    // ==================== noNullElements ====================

    @Nested
    @DisplayName("noNullElements")
    class NoNullElementsTest {

        @Test
        void collection_pass() {
            List<String> list = Arrays.asList("a", "b");
            assertSame(list, BizAssert.noNullElements(list, "msg"));
        }

        @Test
        void collection_fail() {
            assertThrows(BizException.class,
                    () -> BizAssert.noNullElements(Arrays.asList("a", null), "msg"));
        }

        @Test
        void array_pass() {
            String[] arr = {"a", "b"};
            assertSame(arr, BizAssert.noNullElements(arr, "msg"));
        }

        @Test
        void array_fail() {
            assertThrows(BizException.class,
                    () -> BizAssert.noNullElements(new String[]{"a", null}, "msg"));
        }
    }

    // ==================== state ====================

    @Nested
    @DisplayName("state")
    class StateTest {

        @Test
        void pass() {
            assertDoesNotThrow(() -> BizAssert.state(true));
            assertDoesNotThrow(() -> BizAssert.state(true, "msg"));
        }

        @Test
        void fail() {
            BizException ex = assertThrows(BizException.class, () -> BizAssert.state(false));
            assertEquals("state check failed", ex.getMessage());
        }

        @Test
        void fail_customMessage() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.state(false, "invalid state"));
            assertEquals("invalid state", ex.getMessage());
        }

        @Test
        void fail_errorCode() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.state(false, BizError.PARAM_INVALID, "status"));
            assertTrue(ex.getMessage().contains("status"));
        }

        @Test
        void stateOrThrow() {
            assertThrows(IllegalStateException.class,
                    () -> BizAssert.stateOrThrow(false,
                            () -> new IllegalStateException("bad state")));
        }
    }

    // ==================== fail ====================

    @Nested
    @DisplayName("fail")
    class FailTest {

        @Test
        void fail_message() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.fail("unreachable"));
            assertEquals("unreachable", ex.getMessage());
        }

        @Test
        void fail_withCode() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.fail(50001, "internal error"));
            assertEquals(50001, ex.getCode());
        }

        @Test
        void fail_errorCode() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.fail(BizError.PARAM_INVALID));
            assertEquals(10003, ex.getCode());
        }

        @Test
        void fail_customFactory() {
            assertThrows(UnsupportedOperationException.class,
                    () -> BizAssert.fail("nope",
                            ExceptionFactory.ofMessage(UnsupportedOperationException::new)));
        }

        @Test
        @DisplayName("fail 可用于 switch default 等不可达分支")
        void fail_inExpression() {
            String status = "UNKNOWN";
            assertThrows(BizException.class, () -> {
                @SuppressWarnings("unused")
                String result = processStatus(status);
            });
        }

        private String processStatus(String status) {
            switch (status) {
                case "ACTIVE":
                    return "active";
                case "CLOSED":
                    return "closed";
                default:
                    return BizAssert.fail("Unknown status: " + status);
            }
        }
    }

    // ==================== ExceptionFactory.ofMessage ====================

    @Nested
    @DisplayName("ExceptionFactory.ofMessage")
    class ExceptionFactoryTest {

        @Test
        void ofMessage() {
            ExceptionFactory factory = ExceptionFactory.ofMessage(IllegalArgumentException::new);
            RuntimeException ex = factory.create(10001, "test message");
            assertInstanceOf(IllegalArgumentException.class, ex);
            assertEquals("test message", ex.getMessage());
        }
    }

    // ==================== 占位符格式化 ====================

    @Nested
    @DisplayName("消息占位符")
    class PlaceholderTest {

        @Test
        void singlePlaceholder() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.isTrue(false, "{} is required", "email"));
            assertEquals("email is required", ex.getMessage());
        }

        @Test
        void multiplePlaceholders() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.isTrue(false, "{} must be between {} and {}", "age", 18, 65));
            assertEquals("age must be between 18 and 65", ex.getMessage());
        }

        @Test
        void nullArgShowsPlaceholder() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.isTrue(false, "{} is invalid", (Object) null));
            assertEquals("<null> is invalid", ex.getMessage());
        }

        @Test
        void errorCodeWithPlaceholders() {
            BizException ex = assertThrows(BizException.class,
                    () -> BizAssert.notNull(null, BizError.PARAM_INVALID, "orderId"));
            assertEquals("orderId is invalid", ex.getMessage());
            assertEquals(10003, ex.getCode());
        }
    }
}