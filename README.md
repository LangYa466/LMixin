# LMixin

LMixin 是基于 ASM 的 Kotlin 库 通过写 "Mixin" 类轻松修改或增强 Java 字节码 无需改动原始代码 比Mixin更轻量

## 功能
支持 映射支持 | 字节码注入 | 方法覆盖 | Java Agent 集成

### 示例

假如你有一个目标类 `TargetClass` 和一个 Mixin 类 `SimpleMixin`:

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

当 LMixin Agent 运行时 `TargetClass` 的 `originalMethod` 会被 `SimpleMixin` 的方法覆盖 然后返回 "Overwritten"