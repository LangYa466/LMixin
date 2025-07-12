# LMixin - 轻量级 Mixin 框架

LMixin 是一个基于 ASM 的 Kotlin 库，让你可以像写普通类一样写 Mixin，轻松修改或增强 Java 字节码，无需动原始代码。

使用方法：`implementation 'com.github.LangYa466:LMixin:版本号'`，详见 [JitPack 页面](https://jitpack.io/#LangYa466/LMixin)。

## 主要特性
- 灵活的名称映射
- 字节码注入（支持方法头/尾/前/后）
- 方法覆盖
- 字段/方法 Shadow 引用
- Java Agent 集成

## 快速上手

### 1. 定义 Mixin
```kotlin
@Mixin(TargetClass::class)
class MyMixin {
    @Shadow
    private val targetField: String = ""
    @Inject(method = "someMethod", at = At.HEAD)
    fun injectCode() { /* ... */ }
}
```

### 2. 初始化 Agent
```kotlin
LMMixinAgent.init(inst)
```

### 3. 通过 API 获取结果
```kotlin
val api = LMMixinAPI.getInstance()
val allBytes = api.getBytes()
val classBytes = api.getClassBytes("com/example/TargetClass")
```

## 实例化说明

所有核心类都已实例化（不再是 object 单例），你可以随时 new，也可以用 getInstance()。

## 常用用法

```kotlin
// 获取所有处理过的类名
val names = api.getProcessedClassNames()
// 检查类是否被处理
val ok = api.isClassProcessed("com/example/TargetClass")
// 获取处理过的类数量
val count = api.getProcessedClassCount()
```

## 进阶用法

- 支持自定义 SRG 映射文件
- 支持目录/JAR 批量处理
- 支持多种注入点（At.HEAD、At.TAIL、At.BEFORE、At.AFTER）

## 最佳实践
- Shadow 字段/方法建议加默认值，类型要和目标类一致
- @Shadow("xxx") 用于名称不一致时
- @Shadow(remap = false) 用于无需映射时
- 注入点建议用 At 枚举，代码更直观

## 兼容性

静态方法依然可用，老代码不用改。实例方法更灵活，方便测试。

## 单元测试示例
```kotlin
@Test
fun testMappingParser() {
    val parser = MappingParser("test_mappings.srg")
    assertEquals(0, parser.getClassMapSize())
    assertFalse(parser.hasClassMapping("TestClass"))
}
```

## 参考
- 详细注解和 API 见源码
- 更多例子见 test

---

有问题欢迎提 issue