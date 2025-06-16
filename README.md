# LMixin

LMixin 是基于 ASM 的 Kotlin 库 通过写 "Mixin" 类轻松修改或增强 Java 字节码 无需改动原始代码 比Mixin更轻量

## 功能
LMixin 支持以下核心功能：
- **映射支持**: 灵活的名称映射能力
- **字节码注入**: 在指定位置精确插入代码
- **方法覆盖**: 完全替换目标类中的方法实现
- **Java Agent 集成**: 无缝集成到 JVM 启动过程

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