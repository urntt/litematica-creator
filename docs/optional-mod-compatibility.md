# 可选模组兼容 / Optional Mod Compatibility

本文记录 Minecraft `26.2` 下 Litematica Creator 的软兼容边界。MaLiLib 与 Litematica 的硬依赖范围见 README；这里列出的模组均不是 Creator 的运行必需项。

This document records soft-compatibility boundaries for Minecraft `26.2`. MaLiLib and Litematica remain the only mod-level hard dependencies; every mod below is optional.

## 已测试组合 / Tested Matrix

| 基线 | MaLiLib / Litematica | 可选模组 | 结果 |
| --- | --- | --- | --- |
| 无可选模组 | `0.29.4` / `0.28.5` | 无 | 启动至主菜单，所有兼容层安全退化 |
| 当前单模组 | `0.29.4` / `0.28.5` | Tweakeroo `0.29.3` | 启动成功，Free Camera 反射契约启用 |
| 当前单模组 | `0.29.4` / `0.28.5` | Syncmatica `0.3.20` | 启动成功，Litematica GUI Mixin 共存 |
| 当前单模组 | `0.29.4` / `0.28.5` | Lithium `0.25.3+mc26.2` | 启动成功，Creator 碰撞 Mixin 应用 |
| 当前单模组 | `0.29.4` / `0.28.5` | Sodium `0.9.2-alpha.4+mc26.2` | 启动成功，Creator 可选渲染注入无错误 |
| 当前常用组合 | `0.29.4` / `0.28.5` | 上述四个当前版本 | 启动成功，四项运行时审计均通过 |
| 旧版组合 | `0.29.2-sakura.4` / `0.28.2-sakura.1` | Tweakeroo `0.29.2-sakura.1` + Syncmatica `0.3.18` | 启动成功，旧数据模型与 Free Camera 契约均通过 |

MaLiLib `0.29.4` 明确声明与 Tweakeroo `<0.29.3` 冲突，因此“新版硬依赖 + 旧 Tweakeroo”会在 Fabric 解析阶段被拒绝。这是上游版本配对约束，不是 Creator 的延迟运行时故障。

Unit coverage verifies version policy, Tweakeroo contract probing, Creator Camera lifecycle, projection collision, GUI navigation policy, and renderer chunk-refresh calculations. Startup checks verify Fabric/Mixin transformation and runtime contract selection; detailed in-world behavior still receives normal release-candidate play testing.

## Tweakeroo

- **用途**：Creator Camera 接管现有 Free Camera 时，保存并临时设置 `freeCameraPlayerInputs=false`、`freeCameraPlayerMovement=true`，退出后恢复；必要时重设 Tweakeroo 的原 camera entity。
- **入口**：启动时一次性探测 `FeatureToggle.TWEAK_FREE_CAMERA`、`Configs.Generic` 的两个配置项，以及 `CameraEntity` 的原相机字段。运行期间只使用缓存后的反射对象。
- **退化**：未安装时不加载任何 Tweakeroo 类。版本超出 `>=0.29.2- <0.30.0-` 或契约不完整时，记录一次明确警告并仅禁用 Tweakeroo 桥接；Creator Camera 自身仍可使用。
- **异常边界**：运行时反射调用第一次失败后，本会话不再重复调用或刷屏；捕获配置后若只完成了部分修改，会尽力恢复原快照。

Tweakeroo `0.29.3` 在 Sodium `0.9.2-alpha.4` 存在时会由它自己的 conditional-mixin 规则关闭两个 `sodium_breaks` Mixin。Creator Camera 不依赖这两个 Tweakeroo Mixin，其桥接契约仍可启用。

## Syncmatica

- **当前范围**：只保证共同安装和 GUI 注入共存；Creator 草稿的实时同步与服务端 companion 仍属于 #27。
- **入口**：Creator 与 Syncmatica 都在 Litematica `initGui()` 尾部追加按钮，但使用独立按钮实例和不同布局位置；Creator 不调用 Syncmatica 私有 API，也不改其 placement 扩展字段。
- **退化**：未安装时 Creator 管理器和普通 placement 保存完全不变。未知版本只给出兼容审计警告，不会阻止 Creator 启动或伪装成已经支持同步。
- **未来集成边界**：应从普通 `.litematic` 与 placement 共享入口开始；实时 dirty 草稿同步必须另行定义所有权、冲突和恢复协议。

## Lithium

- **入口**：Creator 不引用 Lithium 类。投影碰撞只对 `CreatorCameraEntity` 在原版 `Entity.collide()` 头部执行独立解析，再合并真实世界、投影和实体碰撞。
- **原因**：Lithium 会替换原版碰撞收集内部实现，但仍经过 `Entity.collide()`；Creator 的入口不依赖其私有碰撞迭代器。
- **退化**：未安装时使用同一代码路径。注入声明为可选；若未来 Minecraft 或优化模组完全移除该方法，Creator 不会因找不到注入点而阻止启动，但投影碰撞需要重新审计。

## Sodium

- **入口**：Creator 不引用 Sodium 类或渲染器扩展。`LevelRenderer.repositionCamera()` 上的边缘区块刷新为 `require=0` 的原版可选注入；Sodium 接管区块渲染时继续使用它自己的 camera render state。
- **退化**：未安装时执行原版刷新；安装时不存在版本专用 Mixin。未知版本会记录审计警告，Creator 不尝试调用不稳定的 Sodium 私有 API。
- **已知上游日志**：Sodium `0.9.2-alpha.4` 会自行报告一个缺失的 `StagedVertexBuffer.GpuBufferPool` Mixin 目标；同一警告在未安装 Creator 的环境也来自 Sodium 本身，不表示 Creator 注入失败。

## 运行时诊断 / Runtime Diagnostics

Creator 初始化时会输出一行 `Optional compatibility audit`，列出四个模组的安装版本、是否为已测试版本，以及 Tweakeroo 相机桥接是否启用。

- 精确命中已测试版本：记录为 `tested`。
- 位于支持范围但未单独测试：允许通用路径，同时记录一次警告。
- 超出范围：记录明确警告；Tweakeroo 的私有反射桥接会安全禁用。
- 未安装：记录 `not installed`，且不会触发对应类加载。

The audit is diagnostic only for Syncmatica, Lithium, and Sodium because Creator has no private API bridge to disable for those mods. Tweakeroo is stricter because it is the only integration that intentionally reflects private implementation details.
