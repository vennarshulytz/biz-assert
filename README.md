# BizAssert

[![Maven Central](https://img.shields.io/maven-central/v/io.github.vennarshulytz/biz-assert.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.vennarshulytz/biz-assert)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-8%2B-green.svg)](https://www.oracle.com/java/technologies/javase-downloads.html)

##### 📖 English Documentation | 📖 [中文文档](README_zh.md)

A lightweight, production-ready business assertion utility for Java applications.

Inspired by Spring's `org.springframework.util.Assert`, **BizAssert** is purpose-built for **business logic validation**. It throws customizable business exceptions (with error codes) instead of generic `IllegalArgumentException`, making it ideal for service-layer validation in enterprise applications.

---
## 🔗 Project Repository

- **GitHub**：[vennarshulytz/biz-assert: BizAssert is a lightweight and enterprise-grade assertion library for Java, focused on business logic validation. Unlike standard assertion utilities, it throws customizable business exceptions with error codes, enabling consistent error handling and clearer service-layer validation across complex applications.](https://github.com/vennarshulytz/biz-assert/)

---

- If you encounter any issues while using this project, feel free to open an Issue. Pull Requests are always welcome and highly appreciated.
- If you find this project helpful, please consider giving it a ⭐ Star on GitHub.
- Your support means a lot and helps keep the project actively maintained and improved.

---

## ✨ Features

- 🎯 **Business-Oriented** — Throws `BizException` with error code by default, not `IllegalArgumentException`
- 🔌 **Pluggable Exception Factory** — Replace the global default exception, or specify per-call
- 🏷️ **Label Mechanism** — Avoid repetitive boilerplate messages (`notNullAs(userId, "userId")`)
- 📝 **Placeholder Messages** — `{}`, `{}` style message formatting (`MessageFormat`)
- 📋 **Error Code Enum Support** — First-class support for `IErrorCode` enums
- ↩️ **Pass-through Return Values** — `notNull`, `notEmpty`, `notBlank`, numeric assertions return the validated value
- 🧩 **Rich Assertion Methods** — Boolean, null, string, collection, map, array, numeric, pattern, equality, and more
- ⚡ **Zero Dependencies** — Pure Java, no third-party libraries required

---

## 📦 Installation

### Maven

```xml
<dependency>
    <groupId>io.github.vennarshulytz</groupId>
    <artifactId>biz-assert</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.vennarshulytz:biz-assert:1.0.0'
```

> **Requires Java 8+**

---

## 🚀 Quick Start

```java
import io.github.vennarshulytz.common.assert_.BizAssert;

// Basic — throws BizException if null
User user = BizAssert.notNull(userRepository.findById(id), "User not found");

// With error code
BizAssert.notNull(userId, 10001, "userId must not be null");

// Error enum
BizAssert.notNull(userId, BizError.USER_NOT_NULL);

// Label mechanism — message auto-generated: "userId must not be null"
BizAssert.notNullAs(userId, "userId");

// Placeholder
BizAssert.notNull(userId, "{} must not be null", "userId");
```

---

## 📖 Detailed Usage

### 1. Global Exception Factory

By default, all assertion failures throw `BizException`. You can replace it **once** at application startup:

```java
// Default behavior — throws BizException
BizAssert.notNull(null, "oops");
// → BizException{code=-1, message=oops}

// Replace globally (call once, e.g., in main() or @PostConstruct)
BizAssert.setDefaultExceptionFactory(PaymentException::new);

// Now all assertions throw PaymentException
BizAssert.notNull(null, 10001, "oops");
// → PaymentException(10001, "oops")
```

> ⚠️ `setDefaultExceptionFactory` can only be called **once**. A second call throws `IllegalStateException`.

For exceptions that only accept `(String message)` constructor:

```java
BizAssert.setDefaultExceptionFactory(
    ExceptionFactory.ofMessage(IllegalStateException::new)
);
```

---

### 2. Message Styles

BizAssert supports **7 message styles** for every assertion method:

```java
Object userId = null;

// ① No message — uses default
BizAssert.notNull(userId);
// → "parameter must not be null"

// ② Explicit message
BizAssert.notNull(userId, "userId must not be null");
// → "userId must not be null"

// ③ Error code + message
BizAssert.notNull(userId, 10001, "userId must not be null");
// → code=10001, "userId must not be null"

// ④ Placeholder message ({}, {}, ...)
BizAssert.notNull(userId, "{} must not be null", "userId");
// → "userId must not be null"

// ⑤ Lazy message (Supplier, evaluated only on failure)
BizAssert.notNull(userId, () -> "ID for " + entity.getName() + " is null");

// ⑥ Error enum
BizAssert.notNull(userId, BizError.USER_NOT_NULL);
// → code=10001, "user must not be null"

// ⑦ Error enum + placeholder args
BizAssert.notNull(userId, BizError.PARAM_INVALID, "userId");
// → code=10003, "userId is invalid"
```

#### Label Mechanism

For the common pattern `"xxx must not be null"`, use the `xxxAs` shorthand:

```java
// Instead of writing:
BizAssert.notNull(userId, "{} must not be null", "userId");

// Just write:
BizAssert.notNullAs(userId, "userId");
// → "userId must not be null"

// Also available:
BizAssert.notEmptyAs(name, "name");    // → "name must not be empty"
BizAssert.notBlankAs(email, "email");  // → "email must not be blank"
BizAssert.isPositiveAs(age, "age");    // → "age must be positive"
```

#### Null Placeholder Args

When a placeholder argument is `null`, it displays as `<null>`:

```java
BizAssert.isTrue(false, "{} is invalid", (Object) null);
// → "<null> is invalid"
```

---

### 3. Custom Exception Per Call

#### Style 1: ExceptionFactory parameter

The framework constructs the exception. Your exception class needs an `(int, String)` constructor:

```java
BizAssert.isTrue(order.isPaid(), "Order not paid", PaymentException::new);

BizAssert.isTrue(order.isPaid(), 20001, "Order not paid", PaymentException::new);

// For exceptions with only (String) constructor:
BizAssert.notNull(user, "not found",
    ExceptionFactory.ofMessage(NotFoundException::new));
```

#### Style 2: Direct exception instance (`xxxOrThrow`)

You construct the exception yourself — maximum flexibility:

```java
BizAssert.isTrueOrThrow(order.isPaid(),
    () -> new PaymentException("Order not paid, id=" + order.getId()));

User user = BizAssert.notNullOrThrow(userRepo.findById(id),
    () -> new NotFoundException("User " + id + " not found"));

String name = BizAssert.notBlankOrThrow(input,
    () -> new ValidationException("Name is required"));
```

---

### 4. Pass-through Return Values

Assertions that validate a value **return that value**, eliminating extra variable assignments:

```java
// Without pass-through (traditional)
User user = userRepository.findById(id);
BizAssert.notNull(user, "User not found");
// use user...

// With pass-through ✅
User user = BizAssert.notNull(userRepository.findById(id), "User not found");

// Chain-friendly
String name = BizAssert.notBlank(request.getName(), "Name required");
List<Item> items = BizAssert.notEmpty(order.getItems(), "Order has no items");
int age = BizAssert.isPositive(request.getAge(), "Age must be positive");
```

**Return value rules:**

| Returns value | Returns `void` |
|---|---|
| `notNull` → `T` | `isTrue` |
| `notEmpty` → `String` / `Collection` / `Map` / `T[]` | `isFalse` |
| `notBlank` → `String` | `isEqual` / `notEqual` |
| `isPositive` → `int` / `long` | `state` |
| `isNonNegative` → `int` / `long` | |
| `isBetween` → `int` / `long` | |
| `matches` → `String` | |
| `fail` → `T` (never returns) | |

---

### 5. Boolean Assertions

```java
BizAssert.isTrue(order.isPaid(), "Order must be paid before shipping");

BizAssert.isFalse(account.isLocked(), "Account is locked");

BizAssert.isTrue(amount > 0, 20001, "Amount must be positive");

BizAssert.isTrue(user.isActive(), BizError.USER_NOT_ACTIVE);

BizAssert.isTrueOrThrow(order.isPaid(),
    () -> new PaymentException("Unpaid order: " + order.getId()));
```

---

### 6. Null Assertions

```java
// notNull — returns the validated value
User user = BizAssert.notNull(userRepo.findById(id), "User not found");

// notNullAs — label mechanism
Long userId = BizAssert.notNullAs(request.getUserId(), "userId");

// isNull
BizAssert.isNull(existingUser, "User already exists");
```

---

### 7. String Assertions

```java
// notEmpty — null or "" fails
String name = BizAssert.notEmpty(request.getName(), "Name is required");
// Note: "   " (whitespace only) PASSES notEmpty

// notBlank — null, "", or whitespace-only fails
String email = BizAssert.notBlank(request.getEmail(), "Email is required");
// Returns original value (no trim)

// Label variants
BizAssert.notEmptyAs(name, "name");   // → "name must not be empty"
BizAssert.notBlankAs(email, "email"); // → "email must not be blank"
```

---

### 8. Collection Assertions

```java
List<User> users = BizAssert.notEmpty(userRepo.findAll(), "No users found");

BizAssert.notEmpty(order.getItems(), BizError.ORDER_ITEMS_EMPTY);

List<String> tags = BizAssert.notEmptyAs(post.getTags(), "tags");
// → "tags must not be empty"

Set<Role> roles = BizAssert.notEmptyOrThrow(user.getRoles(),
    () -> new AuthException("User has no roles"));
```

---

### 9. Map Assertions

```java
Map<String, Object> config = BizAssert.notEmpty(loadConfig(), "Config is empty");

BizAssert.notEmptyAs(headers, "headers");
```

---

### 10. Array Assertions

```java
String[] args = BizAssert.notEmpty(commandArgs, "No arguments provided");

BizAssert.notEmptyAs(permissions, "permissions");
```

---

### 11. Equality Assertions

```java
BizAssert.isEqual(actual.getStatus(), "ACTIVE", "Status must be ACTIVE");

BizAssert.isEqual(a, b, BizError.VALUE_MISMATCH, "a", "b");

BizAssert.notEqual(newPassword, oldPassword, "New password must differ from old");

// null-safe: isEqual(null, null) passes
```

---

### 12. Numeric Assertions

All numeric assertions support **pass-through** and both `int`/`long`:

```java
int quantity = BizAssert.isPositive(request.getQuantity(), "Quantity must be positive");

long balance = BizAssert.isNonNegative(account.getBalance(), "Insufficient balance");

int age = BizAssert.isBetween(request.getAge(), 1, 150, "Invalid age");

// Default message for isBetween:
BizAssert.isBetween(age, 18, 65);
// → "value must be between 18 and 65, but was 16"

// Label variants
BizAssert.isPositiveAs(amount, "amount");       // → "amount must be positive"
BizAssert.isNonNegativeAs(balance, "balance");  // → "balance must be non-negative"
BizAssert.isBetweenAs(age, 18, 65, "age");      // → "age must be between 18 and 65, but was 16"

// Error enum
BizAssert.isPositive(price, BizError.PRICE_MUST_POSITIVE);
```

---

### 13. Pattern Matching

```java
String phone = BizAssert.matches(input, "^1[3-9]\\d{9}$", "Invalid phone number");

BizAssert.matches(email, "^[\\w.]+@[\\w.]+$", BizError.EMAIL_FORMAT_INVALID);
```

---

### 14. String Contains / Prefix / Suffix

```java
BizAssert.contains(url, "://", "URL must contain protocol");

BizAssert.doesNotContain(password, username, "Password must not contain username");

BizAssert.startsWith(orderNo, "ORD-", "Invalid order number format");

BizAssert.endsWith(filename, ".pdf", "Only PDF files accepted");
```

---

### 15. No Null Elements

```java
List<String> names = BizAssert.noNullElements(nameList, "Name list contains null");

String[] ids = BizAssert.noNullElements(idArray, "ID array contains null");
```

---

### 16. State Assertions

Semantically identical to `isTrue`, but communicates **object/system state validation** intent:

```java
BizAssert.state(connection.isOpen(), "Connection is closed");

BizAssert.state(order.getStatus() == PENDING, BizError.ORDER_STATE_INVALID);

BizAssert.stateOrThrow(!engine.isShutdown(),
    () -> new EngineException("Engine is shut down"));
```

---

### 17. Fail (Unreachable Branches)

`fail` always throws and is declared to return `T`, so it works in expressions and `switch` branches:

```java
// In switch
switch (status) {
    case ACTIVE:  return handleActive();
    case CLOSED:  return handleClosed();
    default:      return BizAssert.fail("Unknown status: " + status);
}

// With error code
return BizAssert.fail(50001, "Internal processing error");

// With error enum
return BizAssert.fail(BizError.UNKNOWN_TYPE);

// With custom factory
return BizAssert.fail("fatal", ExceptionFactory.ofMessage(FatalException::new));
```

---

### 18. Error Code Enum

Define your business error codes by implementing `IErrorCode`:

```java
public enum BizError implements IErrorCode {

    USER_NOT_NULL(10001, "user must not be null"),
    USER_IS_NULL(10002, "{} is null"),
    PARAM_INVALID(10003, "{} is invalid"),
    ORDER_NOT_PAID(20001, "Order {} is not paid"),
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

Usage:

```java
// Direct
BizAssert.notNull(user, BizError.USER_NOT_NULL);
// → code=10001, message="user must not be null"

// With placeholder args
BizAssert.isTrue(order.isPaid(), BizError.ORDER_NOT_PAID, order.getId());
// → code=20001, message="Order ORD-12345 is not paid"
```

---

### 19. Complete Method Reference

| Category | Method | Return | Description |
|----------|--------|--------|-------------|
| **Boolean** | `isTrue(...)` | `void` | Assert expression is `true` |
| | `isTrueOrThrow(...)` | `void` | Assert `true`, throw custom exception |
| | `isFalse(...)` | `void` | Assert expression is `false` |
| | `isFalseOrThrow(...)` | `void` | Assert `false`, throw custom exception |
| **Null** | `notNull(...)` | `T` | Assert not null, return value |
| | `notNullAs(obj, label)` | `T` | Label shorthand |
| | `notNullOrThrow(...)` | `T` | Assert not null, throw custom exception |
| | `isNull(...)` | `void` | Assert is null |
| **String** | `notEmpty(String, ...)` | `String` | Assert not null/empty |
| | `notEmptyAs(str, label)` | `String` | Label shorthand |
| | `notEmptyOrThrow(...)` | `String` | Throw custom exception |
| | `notBlank(...)` | `String` | Assert not null/empty/blank |
| | `notBlankAs(str, label)` | `String` | Label shorthand |
| | `notBlankOrThrow(...)` | `String` | Throw custom exception |
| **Collection** | `notEmpty(Collection, ...)` | `T` | Assert not null/empty |
| | `notEmptyAs(col, label)` | `T` | Label shorthand |
| | `notEmptyOrThrow(...)` | `T` | Throw custom exception |
| **Map** | `notEmpty(Map, ...)` | `T` | Assert not null/empty |
| | `notEmptyAs(map, label)` | `T` | Label shorthand |
| | `notEmptyOrThrow(...)` | `T` | Throw custom exception |
| **Array** | `notEmpty(T[], ...)` | `T[]` | Assert not null/empty |
| | `notEmptyAs(arr, label)` | `T[]` | Label shorthand |
| | `notEmptyOrThrow(...)` | `T[]` | Throw custom exception |
| **Equality** | `isEqual(actual, expected, ...)` | `void` | Assert equals (null-safe) |
| | `notEqual(actual, unexpected, ...)` | `void` | Assert not equals |
| **Numeric** | `isPositive(int/long, ...)` | `int`/`long` | Assert > 0 |
| | `isPositiveAs(val, label)` | `int`/`long` | Label shorthand |
| | `isNonNegative(int/long, ...)` | `int`/`long` | Assert >= 0 |
| | `isNonNegativeAs(val, label)` | `int`/`long` | Label shorthand |
| | `isBetween(val, min, max, ...)` | `int`/`long` | Assert in [min, max] |
| | `isBetweenAs(val, min, max, label)` | `int`/`long` | Label shorthand |
| **Pattern** | `matches(str, regex, ...)` | `String` | Assert regex match |
| **String Ops** | `contains(str, sub, ...)` | `String` | Assert contains substring |
| | `doesNotContain(str, sub, ...)` | `String` | Assert does not contain |
| | `startsWith(str, prefix, ...)` | `String` | Assert starts with |
| | `endsWith(str, suffix, ...)` | `String` | Assert ends with |
| **Elements** | `noNullElements(Collection/T[], ...)` | original | Assert no null elements |
| **State** | `state(...)` | `void` | Assert state condition |
| | `stateOrThrow(...)` | `void` | Throw custom exception |
| **Fail** | `fail(...)` | `T` (never) | Always throw, for unreachable code |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                    BizAssert                        │
│                                                     │
│  ┌───────────────────────────────────────────────┐  │
│  │ Global ExceptionFactory (AtomicReference)     │  │
│  │ Default: BizException::new                    │  │
│  │ One-time configuration only                   │  │
│  └───────────────────────────────────────────────┘  │
│                                                     │
│  Message Styles                                     │
│  ├── Default message (no args)                      │
│  ├── String message                                 │
│  ├── int code + String message                      │
│  ├── String pattern + Object... args ({} style)    │
│  ├── Supplier<String> (lazy evaluation)             │
│  ├── IErrorCode (error enum)                        │
│  ├── IErrorCode + Object... args                    │
│  └── Label mechanism (xxxAs methods)                │
│                                                     │
│  Exception Styles                                   │
│  ├── Global default factory                         │
│  ├── Per-call ExceptionFactory parameter            │
│  └── xxxOrThrow(Supplier<RuntimeException>)         │
│                                                     │
│  Return Values                                      │
│  ├── void: isTrue, isFalse, isEqual, state          │
│  └── T:    notNull, notEmpty, notBlank, numeric     │
└─────────────────────────────────────────────────────┘
```

---

## 📄 License

[Apache License 2.0](LICENSE)

---

## 🤝 Contributing

Issues and Pull Requests are welcome!
