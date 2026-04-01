# BizAssert

一个轻量级、生产就绪的 Java 业务断言工具类。

灵感来源于 Spring 的 `org.springframework.util.Assert`，**BizAssert** 专为**业务逻辑校验**而设计。断言失败时抛出携带错误码的业务异常（而非 `IllegalArgumentException`），非常适合企业应用中 Service 层的参数与状态校验。

---

## ✨ 特性

- 🎯 **面向业务** — 默认抛出带错误码的 `BizException`，而非 `IllegalArgumentException`
- 🔌 **可插拔异常工厂** — 支持全局替换默认异常类型，也支持单次调用指定异常
- 🏷️ **Label 机制** — 避免重复编写完整错误消息（`notNullAs(userId, "userId")`）
- 📝 **占位符消息** — 支持 `{}`, `{}` 风格的消息格式化
- 📋 **错误码枚举** — 一等支持 `IErrorCode` 错误码枚举
- ↩️ **Pass-through 返回值** — `notNull`、`notEmpty`、`notBlank`、数值断言直接返回校验后的值
- 🧩 **丰富的断言方法** — 覆盖布尔、空值、字符串、集合、Map、数组、数值、正则、相等性等
- ⚡ **零依赖** — 纯 Java 实现，无任何第三方依赖

---

## 📦 安装

### Maven

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>biz-assert</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.example:biz-assert:1.0.0'
```

> **要求 Java 8+**

---

## 🚀 快速开始

```java
import com.example.common.assert_.BizAssert;

// 基本用法 — 为 null 时抛出 BizException
User user = BizAssert.notNull(userRepository.findById(id), "用户不存在");

// 指定错误码
BizAssert.notNull(userId, 10001, "userId 不能为空");

// 错误枚举
BizAssert.notNull(userId, BizError.USER_NOT_NULL);

// Label 机制 — 自动生成消息："userId must not be null"
BizAssert.notNullAs(userId, "userId");

// 占位符
BizAssert.notNull(userId, "{} 不能为空", "userId");
```

---

## 📖 详细使用说明

### 1. 全局异常工厂配置

默认情况下，所有断言失败都抛出 `BizException`。你可以在应用启动时**替换一次**：

```java
// 默认行为 — 抛出 BizException
BizAssert.notNull(null, "oops");
// → BizException{code=-1, message=oops}

// 全局替换（仅调用一次，例如在 main() 或 @PostConstruct 中）
BizAssert.setDefaultExceptionFactory(PaymentException::new);

// 现在所有断言都抛出 PaymentException
BizAssert.notNull(null, 10001, "oops");
// → PaymentException(10001, "oops")
```

> ⚠️ `setDefaultExceptionFactory` 只能调用**一次**，重复调用会抛出 `IllegalStateException`。

对于只接受 `(String message)` 构造函数的异常：

```java
BizAssert.setDefaultExceptionFactory(
    ExceptionFactory.ofMessage(IllegalStateException::new)
);
```

---

### 2. 消息风格

每个断言方法都支持 **7 种消息风格**：

```java
Object userId = null;

// ① 无消息 — 使用默认消息
BizAssert.notNull(userId);
// → "parameter must not be null"

// ② 显式消息
BizAssert.notNull(userId, "userId 不能为空");
// → "userId 不能为空"

// ③ 错误码 + 消息
BizAssert.notNull(userId, 10001, "userId 不能为空");
// → code=10001, "userId 不能为空"

// ④ 占位符消息（{}, {}, ...）
BizAssert.notNull(userId, "{} 不能为空", "userId");
// → "userId 不能为空"

// ⑤ 延迟消息（Supplier，仅在失败时求值）
BizAssert.notNull(userId, () -> entity.getName() + " 的 ID 为空");

// ⑥ 错误枚举
BizAssert.notNull(userId, BizError.USER_NOT_NULL);
// → code=10001, "user must not be null"

// ⑦ 错误枚举 + 占位符参数
BizAssert.notNull(userId, BizError.PARAM_INVALID, "userId");
// → code=10003, "userId is invalid"
```

#### Label 机制

对于常见的 `"xxx must not be null"` 模式，使用 `xxxAs` 简写：

```java
// 传统写法：
BizAssert.notNull(userId, "{} must not be null", "userId");

// Label 简写：
BizAssert.notNullAs(userId, "userId");
// → "userId must not be null"

// 同样支持：
BizAssert.notEmptyAs(name, "name");    // → "name must not be empty"
BizAssert.notBlankAs(email, "email");  // → "email must not be blank"
BizAssert.isPositiveAs(age, "age");    // → "age must be positive"
```

#### null 参数展示

占位符参数为 `null` 时，展示为 `<null>`：

```java
BizAssert.isTrue(false, "{} is invalid", (Object) null);
// → "<null> is invalid"
```

---

### 3. 单次调用指定自定义异常

#### 风格一：ExceptionFactory 参数

由框架构造异常。要求异常类有 `(int, String)` 构造函数：

```java
BizAssert.isTrue(order.isPaid(), "订单未支付", PaymentException::new);

BizAssert.isTrue(order.isPaid(), 20001, "订单未支付", PaymentException::new);

// 对于只有 (String) 构造函数的异常：
BizAssert.notNull(user, "未找到",
    ExceptionFactory.ofMessage(NotFoundException::new));
```

#### 风格二：直接传入异常实例（`xxxOrThrow`）

由调用方完全控制异常类型和内容，最大灵活性：

```java
BizAssert.isTrueOrThrow(order.isPaid(),
    () -> new PaymentException("订单未支付，ID=" + order.getId()));

User user = BizAssert.notNullOrThrow(userRepo.findById(id),
    () -> new NotFoundException("用户 " + id + " 不存在"));

String name = BizAssert.notBlankOrThrow(input,
    () -> new ValidationException("名称不能为空"));
```

---

### 4. Pass-through 返回值

校验值的断言方法**直接返回该值**，省去额外的变量赋值：

```java
// 传统写法
User user = userRepository.findById(id);
BizAssert.notNull(user, "用户不存在");
// 使用 user...

// Pass-through 写法 ✅
User user = BizAssert.notNull(userRepository.findById(id), "用户不存在");

// 可以直接串联使用
String name = BizAssert.notBlank(request.getName(), "名称不能为空");
List<Item> items = BizAssert.notEmpty(order.getItems(), "订单无商品");
int age = BizAssert.isPositive(request.getAge(), "年龄必须为正数");
```

**返回值规则：**

| 返回原值                                             | 返回 `void`            |
| ---------------------------------------------------- | ---------------------- |
| `notNull` → `T`                                      | `isTrue`               |
| `notEmpty` → `String` / `Collection` / `Map` / `T[]` | `isFalse`              |
| `notBlank` → `String`                                | `isEqual` / `notEqual` |
| `isPositive` → `int` / `long`                        | `state`                |
| `isNonNegative` → `int` / `long`                     |                        |
| `isBetween` → `int` / `long`                         |                        |
| `matches` → `String`                                 |                        |
| `fail` → `T`（永不返回）                             |                        |

---

### 5. 布尔断言

```java
BizAssert.isTrue(order.isPaid(), "发货前必须完成支付");

BizAssert.isFalse(account.isLocked(), "账号已被锁定");

BizAssert.isTrue(amount > 0, 20001, "金额必须大于零");

BizAssert.isTrue(user.isActive(), BizError.USER_NOT_ACTIVE);

BizAssert.isTrueOrThrow(order.isPaid(),
    () -> new PaymentException("未支付订单：" + order.getId()));
```

---

### 6. 空值断言

```java
// notNull — 返回校验后的值
User user = BizAssert.notNull(userRepo.findById(id), "用户不存在");

// notNullAs — Label 机制
Long userId = BizAssert.notNullAs(request.getUserId(), "userId");

// isNull
BizAssert.isNull(existingUser, "用户已存在");
```

---

### 7. 字符串断言

```java
// notEmpty — null 或 "" 不通过
String name = BizAssert.notEmpty(request.getName(), "名称不能为空");
// 注意："   "（纯空格）可以通过 notEmpty

// notBlank — null、""、纯空白字符都不通过
String email = BizAssert.notBlank(request.getEmail(), "邮箱不能为空");
// 返回原始值，不做 trim

// Label 变体
BizAssert.notEmptyAs(name, "name");   // → "name must not be empty"
BizAssert.notBlankAs(email, "email"); // → "email must not be blank"
```

---

### 8. 集合断言

```java
List<User> users = BizAssert.notEmpty(userRepo.findAll(), "未找到用户");

BizAssert.notEmpty(order.getItems(), BizError.ORDER_ITEMS_EMPTY);

List<String> tags = BizAssert.notEmptyAs(post.getTags(), "tags");
// → "tags must not be empty"

Set<Role> roles = BizAssert.notEmptyOrThrow(user.getRoles(),
    () -> new AuthException("用户无角色"));
```

---

### 9. Map 断言

```java
Map<String, Object> config = BizAssert.notEmpty(loadConfig(), "配置为空");

BizAssert.notEmptyAs(headers, "headers");
```

---

### 10. 数组断言

```java
String[] args = BizAssert.notEmpty(commandArgs, "未提供参数");

BizAssert.notEmptyAs(permissions, "permissions");
```

---

### 11. 相等性断言

```java
BizAssert.isEqual(actual.getStatus(), "ACTIVE", "状态必须为 ACTIVE");

BizAssert.isEqual(a, b, BizError.VALUE_MISMATCH, "a", "b");

BizAssert.notEqual(newPassword, oldPassword, "新密码不能与旧密码相同");

// null 安全：isEqual(null, null) 通过
```

---

### 12. 数值断言

所有数值断言都支持 **Pass-through** 和 `int`/`long` 两种类型：

```java
int quantity = BizAssert.isPositive(request.getQuantity(), "数量必须为正数");

long balance = BizAssert.isNonNegative(account.getBalance(), "余额不足");

int age = BizAssert.isBetween(request.getAge(), 1, 150, "年龄不合法");

// isBetween 的默认消息：
BizAssert.isBetween(age, 18, 65);
// → "value must be between 18 and 65, but was 16"

// Label 变体
BizAssert.isPositiveAs(amount, "amount");       // → "amount must be positive"
BizAssert.isNonNegativeAs(balance, "balance");  // → "balance must be non-negative"
BizAssert.isBetweenAs(age, 18, 65, "age");      // → "age must be between 18 and 65, but was 16"

// 错误枚举
BizAssert.isPositive(price, BizError.PRICE_MUST_POSITIVE);
```

---

### 13. 正则匹配

```java
String phone = BizAssert.matches(input, "^1[3-9]\\d{9}$", "手机号格式不正确");

BizAssert.matches(email, "^[\\w.]+@[\\w.]+$", BizError.EMAIL_FORMAT_INVALID);
```

---

### 14. 字符串包含/前缀/后缀

```java
BizAssert.contains(url, "://", "URL 必须包含协议");

BizAssert.doesNotContain(password, username, "密码不能包含用户名");

BizAssert.startsWith(orderNo, "ORD-", "订单号格式不正确");

BizAssert.endsWith(filename, ".pdf", "仅接受 PDF 文件");
```

---

### 15. 无 null 元素

```java
List<String> names = BizAssert.noNullElements(nameList, "名称列表包含 null");

String[] ids = BizAssert.noNullElements(idArray, "ID 数组包含 null");
```

---

### 16. 状态断言

语义上与 `isTrue` 相同，但表达的是**对象/系统状态校验**的意图：

```java
BizAssert.state(connection.isOpen(), "连接已关闭");

BizAssert.state(order.getStatus() == PENDING, BizError.ORDER_STATE_INVALID);

BizAssert.stateOrThrow(!engine.isShutdown(),
    () -> new EngineException("引擎已关闭"));
```

---

### 17. Fail（不可达分支）

`fail` 始终抛出异常，声明返回类型为 `T`，因此可在表达式和 `switch` 分支中使用：

```java
// 在 switch 中
switch (status) {
    case ACTIVE:  return handleActive();
    case CLOSED:  return handleClosed();
    default:      return BizAssert.fail("未知状态：" + status);
}

// 带错误码
return BizAssert.fail(50001, "内部处理错误");

// 带错误枚举
return BizAssert.fail(BizError.UNKNOWN_TYPE);

// 带自定义工厂
return BizAssert.fail("致命错误", ExceptionFactory.ofMessage(FatalException::new));
```

---

### 18. 错误码枚举

通过实现 `IErrorCode` 接口定义业务错误码：

```java
public enum BizError implements IErrorCode {

    USER_NOT_NULL(10001, "user must not be null"),
    USER_IS_NULL(10002, "{} is null"),
    PARAM_INVALID(10003, "{} is invalid"),
    ORDER_NOT_PAID(20001, "订单 {} 未支付"),
    ;

    private final int code;
    private final String message;

    BizError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
```

使用方式：

```java
// 直接使用
BizAssert.notNull(user, BizError.USER_NOT_NULL);
// → code=10001, message="user must not be null"

// 带占位符参数
BizAssert.isTrue(order.isPaid(), BizError.ORDER_NOT_PAID, order.getId());
// → code=20001, message="订单 ORD-12345 未支付"
```

---

### 19. 完整方法速查表

| 类别           | 方法                                  | 返回值          | 说明                         |
| -------------- | ------------------------------------- | --------------- | ---------------------------- |
| **布尔**       | `isTrue(...)`                         | `void`          | 断言表达式为 `true`          |
|                | `isTrueOrThrow(...)`                  | `void`          | 断言为 `true`，抛自定义异常  |
|                | `isFalse(...)`                        | `void`          | 断言表达式为 `false`         |
|                | `isFalseOrThrow(...)`                 | `void`          | 断言为 `false`，抛自定义异常 |
| **空值**       | `notNull(...)`                        | `T`             | 断言非 null，返回原值        |
|                | `notNullAs(obj, label)`               | `T`             | Label 简写                   |
|                | `notNullOrThrow(...)`                 | `T`             | 断言非 null，抛自定义异常    |
|                | `isNull(...)`                         | `void`          | 断言为 null                  |
| **字符串**     | `notEmpty(String, ...)`               | `String`        | 断言非 null 且非空           |
|                | `notEmptyAs(str, label)`              | `String`        | Label 简写                   |
|                | `notEmptyOrThrow(...)`                | `String`        | 抛自定义异常                 |
|                | `notBlank(...)`                       | `String`        | 断言非 null/空/空白          |
|                | `notBlankAs(str, label)`              | `String`        | Label 简写                   |
|                | `notBlankOrThrow(...)`                | `String`        | 抛自定义异常                 |
| **集合**       | `notEmpty(Collection, ...)`           | `T`             | 断言非 null 且非空           |
|                | `notEmptyAs(col, label)`              | `T`             | Label 简写                   |
|                | `notEmptyOrThrow(...)`                | `T`             | 抛自定义异常                 |
| **Map**        | `notEmpty(Map, ...)`                  | `T`             | 断言非 null 且非空           |
|                | `notEmptyAs(map, label)`              | `T`             | Label 简写                   |
|                | `notEmptyOrThrow(...)`                | `T`             | 抛自定义异常                 |
| **数组**       | `notEmpty(T[], ...)`                  | `T[]`           | 断言非 null 且非空           |
|                | `notEmptyAs(arr, label)`              | `T[]`           | Label 简写                   |
|                | `notEmptyOrThrow(...)`                | `T[]`           | 抛自定义异常                 |
| **相等**       | `isEqual(actual, expected, ...)`      | `void`          | 断言相等（null 安全）        |
|                | `notEqual(actual, unexpected, ...)`   | `void`          | 断言不相等                   |
| **数值**       | `isPositive(int/long, ...)`           | `int`/`long`    | 断言 > 0                     |
|                | `isPositiveAs(val, label)`            | `int`/`long`    | Label 简写                   |
|                | `isNonNegative(int/long, ...)`        | `int`/`long`    | 断言 >= 0                    |
|                | `isNonNegativeAs(val, label)`         | `int`/`long`    | Label 简写                   |
|                | `isBetween(val, min, max, ...)`       | `int`/`long`    | 断言在 [min, max] 内         |
|                | `isBetweenAs(val, min, max, label)`   | `int`/`long`    | Label 简写                   |
| **正则**       | `matches(str, regex, ...)`            | `String`        | 断言匹配正则                 |
| **字符串操作** | `contains(str, sub, ...)`             | `String`        | 断言包含子串                 |
|                | `doesNotContain(str, sub, ...)`       | `String`        | 断言不包含子串               |
|                | `startsWith(str, prefix, ...)`        | `String`        | 断言以前缀开头               |
|                | `endsWith(str, suffix, ...)`          | `String`        | 断言以后缀结尾               |
| **元素**       | `noNullElements(Collection/T[], ...)` | 原类型          | 断言无 null 元素             |
| **状态**       | `state(...)`                          | `void`          | 断言状态条件                 |
|                | `stateOrThrow(...)`                   | `void`          | 抛自定义异常                 |
| **失败**       | `fail(...)`                           | `T`（永不返回） | 直接抛异常，用于不可达代码   |

---

## 🏗️ 架构概览

```
┌─────────────────────────────────────────────────────┐
│                    BizAssert                        │
│                                                     │
│  ┌───────────────────────────────────────────────┐  │
│  │ 全局 ExceptionFactory（AtomicReference）       │  │
│  │ 默认: BizException::new                       │  │
│  │ 仅允许配置一次                                 │  │
│  └───────────────────────────────────────────────┘  │
│                                                     │
│  消息风格                                           │
│  ├── 默认消息（无参）                               │
│  ├── String message                                 │
│  ├── int code + String message                      │
│  ├── String pattern + Object... args（{} 占位符）  │
│  ├── Supplier<String>（延迟求值）                   │
│  ├── IErrorCode（错误枚举）                         │
│  ├── IErrorCode + Object... args                    │
│  └── Label 机制（xxxAs 方法）                       │
│                                                     │
│  异常风格                                           │
│  ├── 全局默认工厂                                   │
│  ├── 单次调用指定 ExceptionFactory                  │
│  └── xxxOrThrow(Supplier<RuntimeException>)         │
│                                                     │
│  返回值                                             │
│  ├── void: isTrue, isFalse, isEqual, state          │
│  └── T:    notNull, notEmpty, notBlank, 数值断言    │
└─────────────────────────────────────────────────────┘
```

---

## 📄 开源协议

[Apache License 2.0](LICENSE)

---

## 🤝 参与贡献

欢迎提交 Issue 和 Pull Request！