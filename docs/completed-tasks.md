# Litematica Creator 已完成工作记录

最后更新：2026-08-02

本文档保存已完成工作的实现细节和历史验收记录。当前和部分完成的工作统一维护在 [`../todo.md`](../todo.md)。

## 第一批游戏内修复

第一批游戏内测试确认了以下修复：

- #1 HUD 不再与其他 HUD 叠加，并可按现有 HUD 机制定位。
- #2 删除投影方块后 schematic 方块总数会正确减少。
- #3 翻译按钮使用 Creator tooltip，不再显示 MaLiLib tooltip。
- #4 Creator 设置界面具有默认打开快捷键。
- #5 多键组合快捷键可以正常触发。
- #6 Debug 设置显示翻译后的名称，不再直接显示 key。
- #7 打开虚拟物品栏的按键不会再输入到搜索框。
- #13 在正确时机处理不兼容的 Tweakeroo `freeCameraPlayerInputs` 状态。
- #14 面向空气时的无目标提示已节流。
- #15 长按放置具有固定重复间隔。
- #16 Creator 模式会阻断真实方块破坏。
- #17 退出 Creator 模式后不再继续拦截真实交互。

## Placement 坐标变换（#10）

- 不再把世界坐标直接当作 schematic container 坐标写入。
- 已有 region 使用 Litematica 的 world-to-container 逆变换。
- 稀疏 region 使用 placement origin、mirror 和 rotation 的逆变换。
- BlockState 写入前也会执行逆变换。
- 移动或变换 placement 后，继续放置和删除不会再编辑错误的 schematic 位置。

## 稀疏草稿语义（#11）

- 原 tile 模型会把未编辑位置错误声明为显式空气。
- Creator 现在用保留名称的 `1x1x1` cell region 表示已有 region 外的编辑，因此只有玩家明确编辑的位置属于 schematic 内容。
- 新增和移除 region 会同步到同一 schematic 的所有 placements，同时保留 placement 及已有 subregion transforms。
- `hideSubregionBoxesInCreatorMode` 默认开启，只在 Creator 模式抑制密集 subregion box，不改写 Litematica 全局渲染配置。
- 删除保留 Creator cell 中的方块时，会移除该 cell region、更新 metadata 并重建相关 placements。
- 当前已完成边界仅覆盖保留 Creator cells；任意空 subregion 的通用清理由 #35 继续跟踪。

## 世界生命周期（#31、#32）

- World pre-unload 会关闭 Creator 模式，并清理 focus、输入 cooldown、临时 placement 可见性抑制和 Tweakeroo 兼容状态。
- 生命周期卸载不会被解释为玩家主动丢弃。
- Litematica 为新世界加载 placements 后，Creator 模式会保持关闭，并重建 Creator placement index。
- 重新进入世界后不再出现 Creator 模式仍开启但 HUD 缺失的状态。

## Creator Focus 与编辑语义（#18）

- Creator focus 独立于 Litematica selected placement，表示当前编辑画布。
- Focus 包含 placement 及其 schematic：placement 是带变换的编辑视图，schematic 是实际共享数据。
- 编辑同一 schematic 的任意 placement 都会修改同一份数据，同时正确处理 placement/subregion 正反变换。
- Creator 不维护独立 dirty flag，直接使用 `schematic.getMetadata().wasModifiedSinceSaved()`；每次编辑同步更新方块总数、修改时间、modified-since-saved 状态和相关 chunks。
- `New Blank Draft` 会创建真正没有 region 的内存 schematic 和 placement，并将其设为 focus。
- `selectNewDraftPlacement` 控制新草稿是否同时成为 Litematica selected placement，默认关闭。
- 完成编辑只清空 Creator focus，不修改 Litematica selection、不导出文件、不卸载 schematic，也不删除 placements。
- Creator 卸载/丢弃针对 focused schematic 及其所有 placements，但不会删除已有 `.litematic` 原文件。
- 文件导出及原生已加载 schematic/placement 管理继续由 Litematica 负责。

## 未保存 Schematic Recovery（#18）

- Recovery 只依据 Litematica 状态判断资格：non-file-backed schematic 始终缓存；file-backed schematic 仅在 `wasModifiedSinceSaved()` 为真时缓存；干净的 file-backed schematic 继续由 Litematica 原生 per-dimension 数据恢复。
- 每个 schematic 在 `config/litematica-creator/recovery/` 使用独立 UUID entry。Manifest v1 保存世界/服务器、dimension、原始路径和类型、dirty 状态、完整 placement JSON、Litematica selected placement hash 与 Creator focus hash。
- Schematic 内容使用标准压缩 `.litematic` NBT。写入采用 generation 文件、临时 manifest 和原子移动；新 manifest 提交成功后才删除上一代，因此中断提交至少保留上一份完整缓存。
- Creator 编辑会直接标记待写；placement 新增、更新、移除、selected/focus 变化会同步更新待写状态；每秒扫描 Litematica metadata 以捕获 Rebuild 或其他入口的修改。
- 停止变化 5 秒后异步写入，持续编辑最多延迟 30 秒；切换世界、断线和正常关闭前在客户端线程生成 snapshot，并等待单线程后台 writer 完成压缩与 I/O。
- 进入世界后的下一 client tick 才执行恢复，确保 Litematica 已先加载原生 placements。Cache 和全部 placement JSON 验证成功后才替换对应原生干净对象，原 `.litematic` 文件不会被改写。
- Placement 使用 Litematica `toJson()/fromJson()` 往返，保留 hash、origin、rotation、mirror、enabled/render、subregion 状态、颜色及扩展事件字段；空 subregion placement 列表显式保存为 `[]`。
- 恢复会按 hash 还原 Litematica selected placement 和 Creator focus，但 Creator 模式保持关闭。成功只显示一条汇总提示；失败保留 cache 和原生 placements，并记录详细日志和一次警告。
- 玩家移除单个 placement 后，下一份 manifest 不再包含它；移除最后一个 placement 会同步删除 entry 并抑制本会话自动重建，重新添加 placement 后解除抑制。
- 玩家卸载 schematic 或执行 Creator 丢弃会同步取消旧 generation 并删除对应 cache；退出世界、切换 dimension、断线和关闭游戏不视为主动丢弃。
- 单元测试覆盖 eligibility、manifest 往返、世界/维度匹配、5 秒/30 秒调度、generation 中断提交、主动删除和基于对象身份的会话抑制。

## Focus 目标解析（#33）

- 使用 chunk-based placement index 解析编辑位置所属的 placement。
- Creator 目标归属只取决于操作位置、enabled placement 范围和 Creator focus；Litematica selected placement 不参与优先级判断。
- 操作位置位于所有 placement 范围外时，已有 focus 可扩展到任意距离；没有 focus 时则创建并聚焦新空白草稿。
- 操作位置只属于一个 enabled placement 时，Creator 聚焦并编辑该 placement。
- 操作位置属于重叠 placements 时，Creator 打开 Focus Switcher，不根据 focus、selected placement、渲染顺序或 touched-list 顺序自动选择。
- 重叠选择仅在 Creator 渲染和目标解析状态中临时抑制未选候选，不持久化或改写原生 placement enabled/render 配置。
- Focus Switcher 可用 `M,F` 主动打开；`M,N` 新建空白草稿。
- 中键始终只执行 pick block，不创建、切换或清空 Creator focus。
- Creator focus 变化不会写回 Litematica selected placement。

## 配套工作

- Creator 的完成编辑/卸载命令已与 Litematica 原生导出和 selected-placement 行为分离。
- 单元测试覆盖坐标旋转/镜像往返、候选身份合并、目标决策和稀疏 Creator-cell 移除策略。
- `docs/creator-design-and-roadmap.md` 已同步当前 focus、稀疏 subregion 和生命周期模型。
