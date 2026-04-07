package io.github.vennarshulytz.bizassert.utils;

/**
 * 格式化消息工具类
 *
 * @author vennarshulytz
 * @since 1.1.0
 */
public final class MessageFormatter {

    private MessageFormatter() {
        throw new UnsupportedOperationException("MessageFormatter class");
    }

    /**
     * 格式化消息，将 {}, {}, ... 替换为实际参数值
     * <p>null 参数展示为 {@code <null>}</p>
     * @param pattern 消息模板
     * @param args    占位符参数
     * @return        格式化后的消息
     */
    public static String format(String pattern, Object... args) {
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
}
