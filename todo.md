# Litematica Creator TODO

Last updated: 2026-06-28

## Status Legend

- [ ] Not started
- [~] In progress / partially designed
- [x] Done
- [!] Needs design decision

## Current Baseline

第一批游戏内测试已确认修复：

- [x] #1 HUD 叠加/位置问题
- [x] #2 删除投影方块后方块总数不减少
- [x] #3 翻译按钮 tooltip 错误
- [x] #4 设置界面默认快捷键
- [x] #5 组合键不生效
- [x] #6 debug 设置名称 key 直显
- [x] #7 打开 Creator 物品栏时输入首字母
- [x] #13 freeCameraPlayerInputs 提示/处理时机
- [x] #14 面向空气放置时刷屏
- [x] #15 长按放置间隔
- [x] #16 Creator 模式下真实破坏阻断
- [x] #17 退出 Creator 模式后的异常交互阻断

## Next Batch: State, Lifecycle, Coordinates

这些项优先做。它们决定后续虚拟背包、相机、多方块放置和撤销/重做是否会返工。

- [ ] #31 退出世界时关闭 Creator 模式
  - 在离开世界/断开连接时关闭 Creator 模式。
  - 清理输入拦截、放置 cooldown、相机兼容状态。
  - 明确当前草稿状态：保留内存草稿、提示未保存，或主动解除引用。
  - Acceptance: Creator 模式下退出世界再回到主菜单，配置状态不再保持开启。

- [ ] #32 重新进入世界后 Creator 模式状态与 HUD 不一致
  - 与 #31 一起处理。
  - Acceptance: 重新进入任意世界后 Creator 模式默认关闭；HUD 不会消失但模式仍开启。

- [!] #18 Creator 编辑焦点、Litematica selected placement、卸载与未保存状态
  - 采用“窗口/镜像/聚焦”模型：Creator 实际编辑对象是 `LitematicaSchematic`，placement 只是操作镜头/镜像。
  - `Litematica selected placement` 是 Litematica 原生全局选择，用于原生 GUI、热键、材料列表、verifier、rebuild、原生卸载等流程；它可以被 Litematica 自己的界面和快捷键随时改变。
  - `Creator focus` 是 Creator 自己的编辑上下文，不等同于 Litematica selected placement。
  - 内部状态从单一 `currentDraft` 改为 `currentEditTarget = focusedSchematic + activePlacement + draft state + dirty/recovery state`。
  - `activePlacement` 表示当前通过哪个镜像观察和操作 `focusedSchematic`；对旋转/镜像 placement 操作时，坐标、朝向、方块状态都要反变换后写回 schematic。
  - 同一个 schematic 有多个 placement 时，任意 placement 都是同一份原理图的镜像；对哪个镜像操作都修改同一个 schematic，其他镜像同步显示结果。
  - Creator focus -> Litematica selected placement 可以单向同步：Creator 主动聚焦某个 placement 后，可把它设为 Litematica selected placement，方便用户继续使用 Litematica 原生 UI。
  - Litematica selected placement -> Creator focus 默认不自动同步；它只作为主动聚焦/多候选选择时的候选项，避免原生 UI 操作意外改变 Creator 正在编辑的对象。
  - Creator 模式下可以聚焦并编辑任意 loaded placement；一旦 schematic 被 Creator 聚焦接管，它就是当前 Creator-managed draft，无论它最初是 Creator 新建还是 Litematica 已加载的 file-backed schematic。
  - “完成编辑/失焦”表示只清除 Creator focus，后续对真实方块/空气的新编辑应新建草稿；它不导出文件、不卸载原理图、不删除 placement。
  - “丢弃草稿/关闭当前草稿”表示卸载当前 focused schematic 及其所有 placement，并删除对应 recovery cache；如果它来自已有 `.litematic` 文件，只影响当前加载/编辑会话，不删除原文件。
  - Litematica 的 `unloadCurrentSchematic` 是原生命令：基于 Litematica 当前 selected placement 卸载其背后的 schematic，并移除该 schematic 的所有 placement；Creator 丢弃草稿应显式作用于 Creator focus，而不是依赖当前 selected placement。
  - Litematica 原生界面继续负责导出 `.litematic`、管理已加载原理图、创建/删除/移动 placement。
  - 未保存/dirty 行为：
    - dirty edit target 需要 Creator recovery cache，而不是只依赖 Litematica placement 持久化。
    - recovery cache 存在 Creator 专用缓存目录，包含临时 `.litematic` 和 sidecar manifest。
    - manifest 记录世界/服务器/dimension、schematic 名、Creator-managed draft 标记、原始文件路径、缓存文件路径、focused/active placement transform、dirty 时间戳等。
    - 编辑后 debounce 自动写缓存；退出世界、断线、关闭 Creator 模式前强制写缓存。
    - 恢复入口/时机未定：至少提供手动恢复 UI；是否在进入匹配世界时自动提示恢复/删除需要继续讨论；无论哪种方式，都不要静默混入普通 loaded placements。
  - 操作聚焦规则：
    - 没有 edit focus，右键真实方块/空气：新建一个 Creator-managed 草稿并聚焦它。
    - 有 edit focus，右键真实方块/空气：继续在当前 focused schematic 里扩展新区域。
    - 对某个投影方块右键/左键/中键：聚焦该投影背后的 schematic，并以被命中的 placement 作为 `activePlacement`。
    - 对同一个 schematic 的另一个 placement 操作：切换 `activePlacement`，但仍编辑同一份 schematic。
    - 对另一个 schematic 的 placement 操作：Creator focus 切到那个 schematic，并将它纳入当前 Creator-managed draft/edit target。
    - HUD 明确显示当前 editing schematic、active placement、dirty/recovery 状态。
  - 多 placement 重叠时的目标选择规则：
    - 当前 `activePlacement` 覆盖命中位置时优先 active placement。
    - 否则 Litematica 当前 selected placement 覆盖命中位置时作为优先候选，但仍需要满足 Creator 的聚焦规则。
    - 否则只有一个候选 placement 时聚焦它。
    - 否则提示多个投影重叠，要求玩家先选择 placement，不猜测编辑目标。
  - 注册 Litematica `SchematicPlacementEventHandler` listener，但只把它当作事件观察器使用：
    - 监听 placement added/removed/updated/selected/transform/subregion/serialization 相关事件，维护 Creator focus 和候选状态。
    - 如果 active placement 被外部移除，不自动切到其他 placement；保留 focused schematic 的 dirty/recovery 状态，清除 active placement，并要求玩家显式选择新的 active placement 或结束编辑。
    - 如果 focused schematic 被外部卸载导致 placements 全部移除，清除 focus，并保留/标记 recovery cache 状态。
    - Litematica selected placement 变化只更新候选，不默认抢占 Creator focus。
    - 该 event handler 不是 cancellable transaction API，不能用来阻止原生 unload/reload，也不能替代 schematic dirty 追踪。
  - Acceptance: Litematica 原生选择和 Creator 编辑焦点边界清晰；点哪个投影就聚焦并编辑哪个投影背后的 schematic；完成编辑只失焦；丢弃只关闭 Creator 草稿；未保存编辑在退出/中断后可恢复；多个镜像 placement 显示同一份修改结果。

- [!] #33 非 focus placement 范围内操作与主动切换 Creator focus
  - 默认不因为目标坐标落在某个非 focus placement 的 bounding box/空气区域内而自动切换 focus。
  - 命中投影方块时，可以自动聚焦该投影方块所属的 placement/schematic，并执行 Creator 操作；这里命中的是实际非空气投影块，不是透明体积。
  - 命中真实方块或空气，且当前已有 Creator focus 时，继续编辑当前 focused schematic。
  - 命中真实方块或空气，且当前没有 Creator focus 时，新建 Creator-managed 草稿并聚焦。
  - 想编辑某个非 focus placement 的空气位置时，必须先主动切换 Creator focus 到它。
  - Litematica selected placement 是主动切换 focus 时的重要候选，但不自动等同于 Creator focus。
  - 新增 `Focus Looked-at Placement` 快捷操作：
    - 看向投影方块、placement box 或 placement origin 时聚焦对应 placement。
    - 唯一命中时直接聚焦。
    - 多个 placement 重叠时打开 Focus Switcher。
    - 看向真实方块/空气时，如果 Litematica selected placement 覆盖目标点，可以把 selected placement 作为候选并聚焦。
  - 新增 `Focus Switcher` 界面，类似 F3+F4/Alt+Tab，但使用 Creator 自己的快捷键，避免与系统 Alt+Tab 冲突。
  - Focus Switcher 候选项包含：当前 Creator focus、准星下候选 placements、Litematica selected placement、最近编辑过的 schematics/placements、全部 loaded placements。
  - 选择某个 placement 后设置 `focusedSchematic = placement.getSchematic()`，`activePlacement = placement`，并可同步设置 Litematica selected placement；被聚焦的 schematic 按当前 Creator-managed draft 处理。
  - 主动聚焦时的优先级：
    - 当前 `activePlacement` 覆盖命中位置时优先 active placement。
    - 否则 Litematica selected placement 覆盖命中位置时优先作为候选。
    - 否则只有一个候选 placement 时直接聚焦。
    - 否则打开 Focus Switcher，不猜测编辑目标。
  - 可选后续配置：`autoFocusPlacementVolumes`，允许高级用户在真实方块/空气目标落入非 focus placement 体积时自动切换 focus；默认关闭。
  - Acceptance: 非 focus placement 的透明区域不会抢焦点；Litematica selected placement 不会意外改写 Creator focus；玩家可以通过快捷键或 Focus Switcher 明确切换当前编辑目标；被聚焦的 schematic 统一按 Creator-managed draft 处理。

- [ ] #19 翻译设置独立模式不生效
  - `translationMode=INDEPENDENT` 时，`translationLanguage` 改变后直接切换 Creator i18n manager。
  - 保存配置后保持独立语言。
  - Acceptance: 原版/MaLiLib 语言不变时，Creator 设置页文本能随独立语言切换。

- [ ] #23 投影方块触及/交互距离不可调
  - 把 `CreatorEditService.EDIT_RANGE` 改为配置项。
  - 建议默认 10，允许 1-128。
  - 后续 Creator camera 可复用同一配置。
  - Acceptance: 修改配置后，放置、删除、pick block 的有效距离同步变化。

- [ ] #10 placement/选区移动后投影方块放置位置异常
  - 不再直接使用世界坐标作为 schematic container 坐标。
  - 复用或等价实现 Litematica rebuild 模式的 world -> schematic 反变换逻辑。
  - 动态 subregion 创建也必须基于 placement 变换后的 Creator 局部坐标。
  - Acceptance: 移动 Creator placement origin 后，在投影上继续放置/删除仍命中正确位置。

- [ ] #12 面向投影方块时黑线边框穿透到真实方块
  - Creator 模式下使用 Creator/Litematica trace 结果渲染自定义目标框。
  - 必要时取消 vanilla block outline，避免真实世界目标框显示在投影后方。
  - Acceptance: 面向 Creator 投影方块时，选中框只出现在投影方块上。

## Draft Data Model

- [!] #11 包含真实方块的区域放置投影后，已有方块被标红并显示应为空气
  - Root cause: 当前 tile/subregion 把未编辑格子也声明为空气。
  - Recommended direction: Creator 内部维护稀疏草稿，只把玩家明确编辑过的位置视为草稿内容。
  - First implementation option: `1x1x1 subregion per edited block`，语义正确但大量方块时 subregion 数量较多。
  - Later optimization: 将相邻投影方块 pack 成紧凑 cuboid subregion，减少 subregion 数量和保存体积。
  - Avoid: 只屏蔽 overlay/红色渲染，因为保存后的 `.litematic` 仍会携带错误空气语义。
  - Acceptance: 在真实方块旁或真实方块区域内创建少量投影时，未编辑位置不会被 verifier/render 视为应为空气。

## Virtual Inventory And Presentation

- [ ] #8 虚拟物品栏长得不像真正的创造模式物品栏
  - 替换当前按钮列表 GUI。
  - 提供类似原版创造模式物品栏的 tab、搜索、物品格、虚拟生存栏区域。
  - 只允许选择 `BlockItem`，后续再扩展交互类物品。
  - Acceptance: 外观和操作接近原版创造物品栏，而不是列表按钮。

- [ ] #9 Creator 模式打开时快捷栏没有替换为虚拟快捷栏
  - Creator 模式下渲染虚拟热栏覆盖真实热栏。
  - 数字键/滚轮改变虚拟选中槽。
  - 不修改真实背包和真实 selected slot。
  - Acceptance: HUD 热栏显示 Creator 虚拟热栏内容和选中格。

- [ ] #24 手持物品仍显示真实手持物品
  - Creator 模式下第一人称/第三人称手持渲染使用虚拟当前物品。
  - 不向服务器发送换手或背包同步。
  - Acceptance: 屏幕中手持物品与虚拟热栏当前格一致。

- [ ] #25 放置/破坏投影方块没有手部动画
  - Creator 编辑成功时触发本地 hand swing。
  - 不发送真实交互包。
  - Acceptance: 放置、删除、pick/交互时有本地手部动画。

## Creator Camera

- [!] #20 自由视角不好操控，考虑弃用 Tweakeroo Free Camera
  - 设计 Creator 自带客户端 camera mode。
  - 真实玩家本体不动，不向服务器发送移动。
  - 地面模式类似生存：移动、跳跃、重力/碰撞可选。
  - 飞行模式类似创造：自由飞行。
  - 飞行穿墙类似旁观：不受方块碰撞限制。
  - 需要处理：camera entity、输入、碰撞、渲染其他玩家、退出世界清理。
  - Acceptance: 未安装 Tweakeroo 时也能用 Creator camera；退出 Creator 模式后玩家状态恢复正常。

## Placement Controls

- [ ] #21 放置间隔不好控制，添加 AccurateBlockPlacement-like 模式
  - 保留固定 tick 间隔模式。
  - 新增 accurate placement repeat 模式：鼠标移动、目标变化、fresh press/backfill 行为参考 AccurateBlockPlacement-Reborn。
  - 配置项：placement repeat mode、fixed interval ticks。
  - Acceptance: 长按右键时不会过快刷放，准星移动到新目标时能准确补放。

- [ ] #26 无法在空中放置投影方块
  - 支持固定距离放置。
  - 支持平面锁定、网格锁定、从最近投影面延伸。
  - 第一版建议先做 fixed distance。
  - Acceptance: 面向空气时可在配置距离处创建投影方块，且不刷 warning。

- [ ] #28 不支持多方块放置
  - 支持线、面、盒、拖拽填充。
  - 依赖 undo/redo 和稀疏草稿模型。
  - Acceptance: 一次操作可创建多个投影方块，并作为单个 undo step。

## Block State, NBT, Interaction

- [ ] #22 无法改变投影方块状态或 NBT
  - BlockState cycling：朝向、半砖、楼梯、水含量等属性。
  - NBT 编辑：箱子、告示牌、命令方块等需要单独 UI。
  - 保存时写入 schematic tile entities。
  - Acceptance: 至少支持常见 `BlockState` 属性编辑和保存/重载。

- [ ] #29 无法与投影方块交互
  - 右键交互与放置需要区分。
  - 对可交互 block 打开虚拟 UI 或编辑状态/NBT。
  - 不发送服务器交互包。
  - Acceptance: 右键投影容器/告示牌等不会真实交互，而是进入 Creator 编辑流程。

## Undo, Redo, History

- [ ] #30 不支持撤销/重做
  - 每次编辑记录 before/after。
  - 多方块操作作为单个 transaction。
  - 保存后可保留 history，但丢弃草稿时清空。
  - Acceptance: 放置、删除、批量放置、状态修改均可撤销/重做。

## Multiplayer Sync

- [!] #27 没有多人游戏同步
  - Phase 1: Syncmatica 集成，支持分享普通 `.litematic` 和 placement。
  - Phase 2: 如果 Syncmatica 不能实时同步 Creator 草稿，设计可选 server-side companion。
  - 需要同步：草稿 block edits、placement transform、undo/redo conflict、Creator camera 中其他玩家位置显示。
  - Acceptance: 多个客户端能看到同一 Creator 草稿的实时或近实时变化。

## Future Cleanup

- [ ] 调整 Creator 完成编辑/关闭当前编辑命令与 Litematica 原生导出/卸载的边界。
- [ ] 为 Creator draft 添加明确身份标记，避免误操作非 Creator schematic。
- [ ] 为核心编辑服务添加小型单元测试或 headless 测试，覆盖坐标变换和 metadata 计数。
- [ ] 更新 `docs/creator-design-and-roadmap.md`，同步已确定的数据模型和 camera 方案。
