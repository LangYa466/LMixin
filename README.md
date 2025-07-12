# LMixin

LMixin 是基于 ASM 的 Kotlin 库 通过写 "Mixin" 类轻松修改或增强 Java 字节码 无需改动原始代码 比Mixin更轻量

## 功能
LMixin 支持以下核心功能：
- **映射支持**: 灵活的名称映射能力
- **字节码注入**: 在指定位置精确插入代码
- **方法覆盖**: 完全替换目标类中的方法实现
- **字段和方法引用**: 通过@Shadow注解安全访问目标类成员
- **Java Agent 集成**: 无缝集成到 JVM 启动过程

## 核心注解

### `@Mixin`
用于标记Mixin类，指定目标类：
```kotlin
@Mixin(TargetClass::class)
class MyMixin {
    // Mixin实现
}
```

### `@Shadow`
`@Shadow` 注解用于在 Mixin 类中引用目标类的字段和方法。它允许你访问目标类的私有或受保护成员，而无需直接继承或修改目标类。

#### 基本语法
```kotlin
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Shadow(
    val value: String = "",      // 目标成员名（可选）
    val remap: Boolean = true    // 是否启用重映射（可选）
)
```

#### 字段引用
```kotlin
@Mixin(TargetClass::class)
class MyMixin {
    // 基本用法 - 引用同名字段
    @Shadow
    private val targetField: String = ""
    
    // 指定目标字段名
    @Shadow("differentFieldName")
    private val myField: String = ""
    
    // 禁用重映射
    @Shadow(remap = false)
    private val directField: String = ""
}
```

#### 方法引用
```kotlin
@Mixin(TargetClass::class)
class MyMixin {
    // 基本用法 - 引用同名方法
    @Shadow
    private fun targetMethod(): String = ""
    
    // 指定目标方法名
    @Shadow("differentMethodName")
    private fun myMethod(): String = ""
    
    // 带参数的方法引用
    @Shadow("methodWithParams")
    private fun myMethodWithParams(param: Int): String = ""
}
```

#### 参数说明

**value 参数**
- **类型**: `String`
- **默认值**: `""` (空字符串)
- **作用**: 指定目标类中成员的实际名称
- **使用场景**: 当 Mixin 中的成员名与目标类中的成员名不同时

**remap 参数**
- **类型**: `Boolean`
- **默认值**: `true`
- **作用**: 控制是否启用名称重映射
- **使用场景**: 当目标成员名已知且不需要混淆时

#### 使用场景

**1. 访问私有成员**
```kotlin
class TargetClass {
    private val secretValue = "secret"
    private fun secretMethod() = "secret method"
}

@Mixin(TargetClass::class)
class AccessMixin {
    @Shadow
    private val secretValue: String = ""
    
    @Shadow
    private fun secretMethod(): String = ""
    
    fun accessSecrets() {
        println("Secret value: $secretValue")
        println("Secret method: ${secretMethod()}")
    }
}
```

**2. 在重写方法中使用**
```kotlin
@Mixin(TargetClass::class)
class OverrideMixin {
    @Shadow
    private fun originalMethod(): String = ""
    
    @Overwrite
    fun publicMethod(): String {
        // 在重写的方法中调用原始方法
        val original = originalMethod()
        return "Modified: $original"
    }
}
```

**3. 在注入代码中使用**
```kotlin
@Mixin(TargetClass::class)
class InjectMixin {
    @Shadow
    private val counter: Int = 0
    
    @Inject(method = "someMethod", at = At.HEAD)
    fun injectCode() {
        // 在注入的代码中访问 Shadow 字段
        println("Counter value: $counter")
    }
}
```

#### 最佳实践

**1. 提供默认值**
```kotlin
// ✅ 好的做法
@Shadow
private val field: String = ""

@Shadow
private fun method(): String = ""

// ❌ 避免这样做
@Shadow
private val field: String

@Shadow
private fun method(): String
```

**2. 使用私有访问修饰符**
```kotlin
// ✅ 好的做法
@Shadow
private val targetField: String = ""

// ❌ 避免使用 public
@Shadow
public val targetField: String = ""
```

**3. 明确指定目标成员名**
```kotlin
// ✅ 好的做法
@Shadow("actualFieldName")
private val myField: String = ""

// ❌ 避免依赖默认行为
@Shadow
private val actualFieldName: String = ""
```

**4. 在文档中说明用途**
```kotlin
@Mixin(TargetClass::class)
class DocumentedMixin {
    // 引用目标类的内部计数器字段
    @Shadow
    private val counter: Int = 0
    
    // 引用目标类的内部验证方法
    @Shadow
    private fun validateInput(input: String): Boolean = false
}
```

**5. 合理使用 remap 参数**
```kotlin
@Mixin(TargetClass::class)
class RemapMixin {
    // 需要重映射（目标成员可能被混淆）
    @Shadow
    private val field1: String = ""
    
    // 不需要重映射（目标成员名已知且稳定）
    @Shadow(remap = false)
    private val field2: String = ""
}
```

#### 注意事项

**1. 类型匹配**
确保 `@Shadow` 成员的类型与目标类中对应成员的类型匹配：
```kotlin
// 目标类
class TargetClass {
    private val field: String = "value"
    private fun method(): Int = 42
}

// Mixin 类
@Mixin(TargetClass::class)
class MyMixin {
    // ✅ 类型匹配
    @Shadow
    private val field: String = ""
    
    @Shadow
    private fun method(): Int = 0
    
    // ❌ 类型不匹配
    @Shadow
    private val field: Int = 0  // 错误：应该是 String 类型
    
    @Shadow
    private fun method(): String = ""  // 错误：应该是 Int 类型
}
```

**2. 访问修饰符**
`@Shadow` 成员不会影响目标类中对应成员的访问修饰符：
```kotlin
class TargetClass {
    private val privateField = "private"
    protected val protectedField = "protected"
}

@Mixin(TargetClass::class)
class MyMixin {
    // 即使在这里声明为 public，仍然只能访问 private 字段
    @Shadow
    public val privateField: String = ""
    
    // 即使在这里声明为 private，仍然可以访问 protected 字段
    @Shadow
    private val protectedField: String = ""
}
```

**3. 初始化**
`@Shadow` 字段的默认值在运行时会被忽略，实际值来自目标类：
```kotlin
@Mixin(TargetClass::class)
class MyMixin {
    @Shadow
    private val field: String = "default value"  // 这个默认值会被忽略
    
    fun printField() {
        println(field)  // 输出目标类中 field 的实际值
    }
}
```

### `@Overwrite` 方法覆盖示例

假如你有一个目标类 `TargetClass` 和一个 Mixin 类 `SimpleMixin`：

```kotlin:src/main/kotlin/cn/langya/lmixin/TargetClass.kt
package cn.langya.lmixin

class TargetClass {
    fun originalMethod(): String {
        return "Original"
    }
}
```

```kotlin:src/main/kotlin/cn/langya/lmixin/SimpleMixin.kt
package cn.langya.lmixin

import cn.langya.lmixin.Mixin
import cn.langya.lmixin.Overwrite

@Mixin(TargetClass::class)
class SimpleMixin {
    @Overwrite
    fun originalMethod(): String {
        return "Overwritten"
    }
}
```

当 LMixin Agent 运行时，`TargetClass` 的 `originalMethod` 会被 `SimpleMixin` 的方法覆盖，然后返回 "Overwritten"

### `@Inject` 代码注入示例

`@Inject` 注解允许你在目标方法的指定位置插入代码你可以控制注入的位置和匹配的索引

**`At` 枚举类型**：
- `HEAD`: 注入到方法的头部
- `TAIL`: 注入到方法的尾部（return 语句之前）
- `BEFORE`: 注入到指定方法调用或指定行号之前需要配合 `method` 或 `ordinal` 参数使用
- `AFTER`: 注入到指定方法调用或指定行号之后需要配合 `method` 或 `ordinal` 参数使用

**`@Inject` 参数**：
- `method`: 目标方法名称（`BEFORE`/`AFTER` 模式下使用）
- `at`: 注入位置（默认为 `HEAD`）
- `ordinal`: 注入位置的索引（默认为 0，表示第一个匹配项当 `at` 为 `BEFORE` 或 `AFTER` 且 `method` 为空时，此参数表示行号）

示例：

```kotlin:src/main/kotlin/cn/langya/lmixin/TargetClass.kt
package cn.langya.lmixin

class TargetClass {
    fun methodToInject(): String {
        println("Inside original methodToInject")
        return "OriginalInject"
    }
}
```

```kotlin:src/main/kotlin/cn/langya/lmixin/SimpleMixin.kt
package cn.langya.lmixin

import cn.langya.lmixin.Mixin
import cn.langya.lmixin.Inject
import cn.langya.lmixin.At

@Mixin(TargetClass::class)
class SimpleMixin {
    @Inject(method = "methodToInject", at = At.HEAD)
    fun injectAtHead() {
        println("Code injected at HEAD of methodToInject")
    }

    @Inject(method = "methodToInject", at = At.TAIL)
    fun injectAtTail() {
        println("Code injected at TAIL of methodToInject")
    }

    @Inject(method = "println", at = At.BEFORE, ordinal = 0) // 在第一个 println 调用前注入
    fun injectBeforePrintln() {
        println("Code injected BEFORE println call")
    }

    @Inject(method = "println", at = At.AFTER, ordinal = 0) // 在第一个 println 调用后注入
    fun injectAfterPrintln() {
        println("Code injected AFTER println call")
    }
}
```

当 LMixin Agent 运行时，`injectAtHead` 和 `injectAtTail` 方法中的代码将被分别注入到 `TargetClass.methodToInject` 的头部和尾部`injectBeforePrintln` 和 `injectAfterPrintln` 将在 `methodToInject` 中第一个 `println` 调用前后注入代码

## 常见问题

### Q: 为什么需要使用 @Shadow 注解？

A: `@Shadow` 注解允许你在 Mixin 类中安全地引用目标类的成员，而无需直接修改目标类。这提供了更好的封装性和可维护性。

### Q: @Shadow 字段和方法会被添加到目标类中吗？

A: 不会。`@Shadow` 成员只是对目标类中现有成员的引用，它们不会被添加到目标类中。

### Q: 如何处理目标类中不存在的成员？

A: 如果引用的成员在目标类中不存在，运行时会出现错误。确保目标类中确实存在对应的成员。

### Q: 可以在 @Shadow 方法中调用其他 @Shadow 方法吗？

A: 可以。`@Shadow` 方法可以相互调用，就像普通方法一样。

## 总结

LMixin 框架提供了强大的字节码操作能力，通过 `@Mixin`、`@Shadow`、`@Overwrite` 和 `@Inject` 注解，你可以轻松地修改和增强 Java 类，而无需直接修改原始代码。

记住：
- 总是为 `@Shadow` 字段和方法提供默认值
- 使用私有访问修饰符来避免命名冲突
- 明确指定目标成员名
- 在文档中说明用途
- 注意类型匹配
- 合理使用 remap 参数

## Java API 支持

LMixin 提供了完整的 Java API 支持，让 Java 开发者也能轻松使用 LMixin 的功能。

### 初始化

```java
import cn.langya.lmixin.LMMixinJavaAPI;
import java.lang.instrument.Instrumentation;

public class MyJavaApplication {
    public static void premain(String agentArgs, Instrumentation inst) {
        // 初始化 LMixin
        LMMixinJavaAPI.init(inst, "mappings.srg");
    }
}
```

### 获取字节码

```java
import cn.langya.lmixin.LMMixinJavaAPI;
import java.util.Map;

public class BytecodeExample {
    public void getProcessedClasses() {
        // 获取所有处理过的类的字节码映射
        Map<String, byte[]> bytesMap = LMMixinJavaAPI.getBytes();
        
        // 获取指定类的字节码
        byte[] targetClassBytes = LMMixinJavaAPI.getClassBytes("cn/langya/lmixin/TargetClass");
        
        // 获取所有处理过的类名列表
        List<String> classNames = LMMixinJavaAPI.getProcessedClassNames();
        
        // 检查指定类是否已被处理
        boolean isProcessed = LMMixinJavaAPI.isClassProcessed("cn/langya/lmixin/TargetClass");
        
        // 获取处理过的类数量
        int count = LMMixinJavaAPI.getProcessedClassCount();
    }
}
```

### API 方法说明

#### `getBytes(): Map<String, ByteArray>`
获取所有处理过的类的字节码映射。
- **返回**: 类名到字节码的映射
- **类名格式**: 内部格式，如 `cn/langya/lmixin/ExampleClass`

#### `getClassBytes(className: String): ByteArray?`
获取指定类的字节码。
- **参数**: `className` - 类名（内部格式）
- **返回**: 类的字节码，如果不存在则返回 `null`

#### `getProcessedClassNames(): List<String>`
获取所有处理过的类名列表。
- **返回**: 类名列表

#### `isClassProcessed(className: String): Boolean`
检查指定类是否已被处理。
- **参数**: `className` - 类名
- **返回**: 是否已处理

#### `getProcessedClassCount(): Int`
获取处理过的类数量。
- **返回**: 类数量

### 使用场景

#### 1. 字节码分析
```java
public class BytecodeAnalyzer {
    public void analyzeProcessedClasses() {
        Map<String, byte[]> bytesMap = LMMixinJavaAPI.getBytes();
        
        bytesMap.forEach((className, bytes) -> {
            System.out.println("Class: " + className);
            System.out.println("Size: " + bytes.length + " bytes");
            
            // 验证Java类文件魔数
            if (bytes.length >= 4) {
                boolean isValidClass = bytes[0] == (byte) 0xCA && 
                                     bytes[1] == (byte) 0xFE && 
                                     bytes[2] == (byte) 0xBA && 
                                     bytes[3] == (byte) 0xBE;
                System.out.println("Valid Java class: " + isValidClass);
            }
        });
    }
}
```

#### 2. 动态类加载
```java
public class DynamicClassLoader {
    public Class<?> loadProcessedClass(String className) throws ClassNotFoundException {
        byte[] classBytes = LMMixinJavaAPI.getClassBytes(className);
        if (classBytes == null) {
            throw new ClassNotFoundException("Class not found: " + className);
        }
        
        // 使用自定义ClassLoader加载类
        return defineClass(className.replace('/', '.'), classBytes, 0, classBytes.length);
    }
}
```

#### 3. 调试和监控
```java
public class MixinDebugger {
    public void debugMixinProcessing() {
        System.out.println("Processed classes count: " + LMMixinJavaAPI.getProcessedClassCount());
        
        List<String> classNames = LMMixinJavaAPI.getProcessedClassNames();
        System.out.println("Processed classes:");
        classNames.forEach(System.out::println);
        
        // 检查特定类是否被处理
        String targetClass = "cn/langya/lmixin/TargetClass";
        if (LMMixinJavaAPI.isClassProcessed(targetClass)) {
            System.out.println(targetClass + " has been processed");
            byte[] bytes = LMMixinJavaAPI.getClassBytes(targetClass);
            System.out.println("Bytecode size: " + bytes.length + " bytes");
        }
    }
}
```

### 注意事项

1. **初始化顺序**: 必须在调用任何 API 方法之前先调用 `LMMixinJavaAPI.init()`
2. **类名格式**: 使用内部格式的类名（用 `/` 分隔，如 `cn/langya/lmixin/ExampleClass`）
3. **线程安全**: API 方法是线程安全的，可以在多线程环境中使用
4. **内存管理**: 返回的字节码数组是原始数据的副本，可以安全地修改和保存

## 更新日志

### v1.01-SNAPSHOT
- ✅ 添加了 `getBytes()` 方法支持
- ✅ 提供了完整的 Java API 接口
- ✅ 修复了 InputStream reset 问题
- ✅ 增强了错误处理和类型安全
- ✅ 添加了详细的测试用例
- ✅ 完善了文档说明