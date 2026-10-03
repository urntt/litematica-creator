# 可选模组兼容 / Optional Mod Compatibility

本文记录 Minecraft `26.3` 下 Litematica Creator 的软兼容边界。MaLiLib 与 Litematica 的硬依赖范围见 README；这里列出的模组均不是 Creator 的运行必需项。

This document records soft-compatibility boundaries for Minecraft `26.3`. MaLiLib and Litematica remain the only mod-level hard dependencies; every mod below is optional.

## 已测试组合 / Tested Matrix

2026-10-03 在无头 Linux（Xvfb + Mesa EGL）上以打包后的生产 JAR 运行完整客户端 GameTest：每组都先加载全部 27 个 Creator Mixin 目标，再完成投影编辑、虚拟库存、相机、focus、卸载与真实世界隔离断言。可选模组 JAR 均取自 Modrinth 正式发布并校验 SHA-512，通过 `-PgameTestExtraMods` 加入同一测试。

On 2026-10-03 each row ran the full packaged client GameTest headlessly on Linux (Xvfb + Mesa EGL): all 27 Creator Mixin targets loaded, then the projection edit, virtual inventory, camera, focus, discard, and real-world isolation assertions passed. Optional JARs came from official Modrinth releases with verified SHA-512 hashes and were added through `-PgameTestExtraMods`.

| 基线 | MaLiLib / Litematica | 可选模组 | 结果 |
| --- | --- | --- | --- |
| 无可选模组 | `0.30.2` / `0.29.1` | 无 | GameTest 通过，所有兼容层安全退化 |
| 无可选模组 | `0.30.1` / `0.29.0` | 无 | GameTest 通过 |
| 当前单模组 | `0.30.2` / `0.29.1` | Tweakeroo `0.30.1` | GameTest 通过，Free Camera 反射桥接启用 |
| 当前单模组 | `0.30.2` / `0.29.1` | Syncmatica `0.3.20` | GameTest 通过，与内置服务器完成 Syncmatica 握手 |
| 当前单模组 | `0.30.2` / `0.29.1` | Lithium `0.26.2+mc26.3` | GameTest 通过，Creator 碰撞 Mixin 应用 |
| 当前单模组 | `0.30.2` / `0.29.1` | Sodium `0.9.2+mc26.3` | GameTest 通过，Creator 可选渲染注入无错误 |
| 当前常用组合 | `0.30.2` / `0.29.1` | 上述四个版本 | GameTest 通过，四项运行时审计均为 tested |
| 下限组合 | `0.30.1` / `0.29.0` | Tweakeroo `0.30.0` + Syncmatica `0.3.20` | GameTest 通过，Free Camera 反射桥接启用 |

Tweakeroo `0.30.1` 自身要求 MaLiLib `>=0.30.2`，因此“下限硬依赖 + Tweakeroo 0.30.1”会在 Fabric 解析阶段被拒绝；这是上游版本配对约束，不是 Creator 的延迟运行时故障。Tweakeroo `0.30.0` 与 `0.30.1` 的反射契约经源码核对一致。

Tweakeroo `0.30.1` requires MaLiLib `>=0.30.2`, so Fabric rejects it with the lower-bound hard dependencies; this is an upstream pairing rule, not a delayed Creator failure. Source review found the same reflection contract in Tweakeroo `0.30.0` and `0.30.1`.

### 历史 26.2 矩阵 / Historical 26.2 Matrix

以下为 2026-10-02 前在 Minecraft `26.2` 上的启动记录，不代表当前支持范围。

Startup records for Minecraft `26.2` before 2026-10-02; they do not describe the current supported range.

| 基线 | MaLiLib / Litematica | 可选模组 | 结果 |
| --- | --- | --- | --- |
| 无可选模组 | `0.29.4` / `0.28.5` | 无 | 启动至主菜单，所有兼容层安全退化 |
| 当前单模组 | `0.29.4` / `0.28.5` | Tweakeroo `0.29.3` | 启动成功，Free Camera 反射契约启用 |
| 当前单模组 | `0.29.4` / `0.28.5` | Syncmatica `0.3.20` | 启动成功，Litematica GUI Mixin 共存 |
| 当前单模组 | `0.29.4` / `0.28.5` | Lithium `0.25.3+mc26.2` | 启动成功，Creator 碰撞 Mixin 应用 |
| 当前单模组 | `0.29.4` / `0.28.5` | Sodium `0.9.2-alpha.4+mc26.2` | 启动成功，Creator 可选渲染注入无错误 |
| 当前常用组合 | `0.29.4` / `0.28.5` | 上述四个当前版本 | 启动成功，四项运行时审计均通过 |
| 旧版组合 | `0.29.2-sakura.4` / `0.28.2-sakura.1` | Tweakeroo `0.29.2-sakura.1` + Syncmatica `0.3.18` | 启动成功，旧数据模型与 Free Camera 契约均通过 |

Unit coverage verifies version policy, Tweakeroo contract probing, Creator Camera lifecycle, projection collision, GUI navigation policy, and renderer chunk-refresh calculations. Startup checks verify Fabric/Mixin transformation and runtime contract selection; detailed in-world behavior still receives normal release-candidate play testing.

## Tweakeroo

- **用途**：Creator Camera 接管现有 Free Camera 时，保存并临时设置 `freeCameraPlayerInputs=false`、`freeCameraPlayerMovement=true`，退出后恢复；必要时重设 Tweakeroo 的原 camera entity。
- **入口**：启动时一次性探测 `FeatureToggle.TWEAK_FREE_CAMERA`、`Configs.Generic` 的两个配置项，以及 `CameraEntity` 的原相机字段。运行期间只使用缓存后的反射对象。
- **退化**：未安装时不加载任何 Tweakeroo 类。版本超出 `>=0.30.0- <0.31.0-` 或契约不完整时，记录一次明确警告并仅禁用 Tweakeroo 桥接；Creator Camera 自身仍可使用。
- **异常边界**：运行时反射调用第一次失败后，本会话不再重复调用或刷屏；捕获配置后若只完成了部分修改，会尽力恢复原快照。

26.2 时 Tweakeroo `0.29.3` 在 Sodium `0.9.2-alpha.4` 存在时会由它自己的 conditional-mixin 规则关闭两个 `sodium_breaks` Mixin；26.3 的 Tweakeroo `0.30.1` + Sodium `0.9.2+mc26.3` 组合中桥接契约同样启用。Creator Camera 不依赖这些 Tweakeroo Mixin。

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
- **已知上游日志**：26.2 的 Sodium `0.9.2-alpha.4` 会自行报告一个缺失的 `StagedVertexBuffer.GpuBufferPool` Mixin 目标；26.3 的 `0.9.2+mc26.3` 未再出现该警告。软件渲染环境下 Sodium 会提示找不到图形适配器并启用 `NO_ERROR_CONTEXT_UNSUPPORTED` 规避，这同样来自 Sodium 自身。

## 运行时诊断 / Runtime Diagnostics

Creator 初始化时会输出一行 `Optional compatibility audit`，列出四个模组的安装版本、是否为已测试版本，以及 Tweakeroo 相机桥接是否启用。

- 精确命中已测试版本：记录为 `tested`。
- 位于支持范围但未单独测试：允许通用路径，同时记录一次警告。
- 超出范围：记录明确警告；Tweakeroo 的私有反射桥接会安全禁用。
- 未安装：记录 `not installed`，且不会触发对应类加载。

The audit is diagnostic only for Syncmatica, Lithium, and Sodium because Creator has no private API bridge to disable for those mods. Tweakeroo is stricter because it is the only integration that intentionally reflects private implementation details.
