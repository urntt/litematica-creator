# Litematica Creator TODO

最后更新：2026-08-01

## 已完成

- [x] #1 HUD 叠加和位置问题
- [x] #2 删除投影方块后方块总数不减少
- [x] #3 翻译按钮 tooltip 错误
- [x] #4 设置界面默认快捷键
- [x] #5 组合键不生效
- [x] #6 debug 设置名称 key 直显
- [x] #7 打开 Creator 物品栏时输入快捷键字符
- [x] #10 placement 移动后投影方块编辑位置异常
- [x] #11 稀疏草稿中未编辑位置被声明为空气
- [x] #13 Tweakeroo `freeCameraPlayerInputs` 兼容处理时机
- [x] #14 面向空气放置时提示刷屏
- [x] #15 长按放置的固定间隔
- [x] #16 Creator 模式下真实破坏阻断
- [x] #17 退出 Creator 模式后的异常交互阻断
- [x] #31 退出世界时关闭 Creator 模式
- [x] #32 重新进入世界后 Creator 模式与 HUD 状态不一致
- [x] #33 Creator focus 目标解析与切换
- [x] Creator 与 Litematica 的完成编辑/卸载命令边界
- [x] 坐标、目标解析和稀疏 region 核心单元测试
- [x] Focus、稀疏 subregion 和生命周期设计文档同步

实现细节和历史验收记录见 [`docs/completed-tasks.md`](docs/completed-tasks.md)。

## 状态标记

- [ ] 未开始
- [~] 进行中或已部分实现
- [!] 需要设计决策

## 状态与生命周期

- [~] #18 未保存 schematic 的 recovery cache
  - 继续以 Litematica metadata 作为 dirty 状态的唯一依据，不维护 Creator-owned 或 Creator-managed 身份。
  - 生命周期卸载前，缓存所有 non-file-backed schematic 和所有已修改的 file-backed schematic。
  - 不缓存未修改的 file-backed schematic，由 Litematica 原生 per-dimension placement 持久化负责恢复。
  - 在 Creator 专用恢复目录中保存临时 `.litematic` 和 sidecar manifest。
  - Manifest 记录世界/服务器、dimension、schematic 标识、原始文件路径、缓存路径、placement transforms、dirty 时间戳，以及可选的退出前 Creator focus。
  - 进入世界时先让 Litematica 恢复原生 file-backed placements，再恢复匹配当前世界的 cache entries。
  - 区分玩家主动移除 placement/卸载 schematic 与退出世界、断线、关闭游戏等生命周期卸载。
  - 玩家主动卸载或 Creator 丢弃时清除对应缓存；生命周期清理时写入并保留缓存。
  - `SchematicPlacementEventHandler` 只作为 placement、focus 和 manifest 更新的观察器；它不是可取消事务 API，也不能替代 dirty metadata。
  - 验收：non-file-backed 和已修改的 file-backed schematics 在退出世界或异常中断后可以恢复，而玩家明确卸载/丢弃的内容不会被恢复。

- [ ] #34 切换 Creator focus 时提示当前编辑目标
  - 在 focus 状态变更入口统一发送提示，使自动目标归属、Focus Switcher、快捷键和新建草稿行为一致。
  - Focus 真正发生变化时显示 placement 名和 schematic 名；清空 focus 时显示单独提示。
  - 操作仍解析到当前 focus 时不重复提示。
  - 验收：每次真实 focus 切换只出现一条简短提示，能够明确识别新的编辑目标，普通连续操作不会刷屏。

## 编辑正确性

- [ ] #35 删除 subregion 中最后一个投影方块后自动删除该 subregion
  - 将现有仅针对 `1x1x1` Creator cell 的自动清理扩展到任何经 Creator 编辑后变空的 subregion。
  - 从 schematic 及该 schematic 的所有 placements 中移除 region，并刷新几何 metadata、方块数、touched chunks 和 placement index。
  - 判定 subregion 为空之前，正确处理残留 block entities、entities 和 scheduled ticks。
  - 验收：删除一个 subregion 中的全部投影方块后，该 subregion 自动消失；保存重载和同 schematic 的所有 placements 仍保持一致。

- [ ] #36 普通放置不得覆盖目标位置已有的投影方块
  - Creator 当前会直接把计算出的状态写入目标位置，没有先检查该位置已有的 schematic state。
  - 铁砧等非完整轮廓方块可能让射线穿过空隙命中后方方块，从而把已被铁砧占据的位置算作放置目标。
  - Litematica Rebuild 的基础 `placeSchematicBlock()` 路径也缺少目标空气检查，但方向放置和填充空气路径会检查空气。将其视为共享的上游行为缺口，但在 Creator 内独立修复。
  - 普通放置仅在目标投影状态为空气，或按原版放置语义可替换时允许写入；显式替换属于另一种编辑操作。
  - 占用检查必须发生在切换 focus、创建草稿/subregion 和标记 metadata dirty 之前。
  - 验收：透过铁砧及其他非完整轮廓投影方块的空隙观察时，放置操作不会静默顶掉已有投影方块。

- [ ] #12 面向投影方块时选中框穿透到真实方块
  - Creator 模式下使用 Creator/Litematica trace 结果渲染目标框。
  - 投影方块是有效目标时抑制原版真实方块选中框。
  - 验收：面向 Creator 投影方块时，选中框只显示在该投影方块上。

- [ ] #23 投影方块触及/交互距离不可调
  - 将 `CreatorEditService.EDIT_RANGE` 改为配置项。
  - 建议默认 10 格，允许范围 1-128 格。
  - 放置、删除、pick block、交互和后续 Creator camera 复用同一配置。
  - 验收：修改配置后，所有 Creator 编辑操作的有效距离同步变化。

## 本地化

- [ ] #19 独立翻译模式不切换语言
  - `translationMode=INDEPENDENT` 时，将 `translationLanguage` 直接应用到 Creator i18n manager。
  - 保存并重新加载配置后保留独立语言。
  - 验收：不改变 Minecraft 或 MaLiLib 语言时，Creator 设置文本可以独立切换语言。

## 虚拟物品栏与显示

- [ ] #8 虚拟物品栏不像原版创造模式物品栏
  - 替换当前按钮列表 GUI。
  - 提供类似原版创造物品栏的 tab、搜索、物品网格和虚拟生存物品栏页。
  - 第一阶段只提供 `BlockItem`，后续再扩展交互类物品。
  - 验收：外观和操作接近原版创造物品栏，而不是按钮列表。

- [ ] #9 Creator 模式没有用虚拟快捷栏替换屏幕快捷栏
  - Creator 模式下用九格虚拟快捷栏覆盖真实快捷栏渲染。
  - 数字键和滚轮改变虚拟选中槽。
  - 不修改真实背包和真实 selected slot。
  - 验收：HUD 快捷栏显示虚拟内容和虚拟选中格。

- [ ] #24 手持物品仍显示真实手持物品
  - Creator 模式下第一人称和第三人称手持渲染使用当前虚拟物品。
  - 不向服务器发送换手或背包同步。
  - 验收：渲染的手持物品与虚拟快捷栏当前格一致。

- [ ] #25 放置/破坏投影方块没有手部动画
  - Creator 编辑成功后触发本地 hand swing。
  - 不发送真实交互包。
  - 验收：放置、删除、pick 和交互均提供适当的本地手部反馈。

## Creator Camera

- [!] #20 用 Creator 自带相机替代或超越 Tweakeroo Free Camera
  - 设计纯客户端 camera entity，真实玩家本体保持不动且不向服务器发送移动。
  - 地面模式类似生存移动，支持跳跃、重力和可选碰撞。
  - 飞行模式类似创造飞行，并可像旁观模式一样穿过方块。
  - 处理相机输入、碰撞、其他玩家渲染、退出模式、断线和世界卸载。
  - 验收：未安装 Tweakeroo 时 Creator camera 仍可用，退出后玩家和相机状态完整恢复。

## 放置控制

- [ ] #21 连续放置与长按持续破坏控制
  - 保留固定 tick 间隔模式。
  - 参考 AccurateBlockPlacement-Reborn 的 fresh press、目标变化、鼠标移动和 backfill 行为增加 accurate repeat 模式。
  - 按住攻击键时，以受控间隔持续 trace 并删除投影方块，不向服务器发送攻击包。
  - 避免反复处理同一个已变为空气的目标；实现 #30 后再将连续手势组织为合适的历史事务。
  - 增加 placement repeat mode、fixed interval ticks 和连续破坏间隔配置。
  - 验收：长按放置速度可控，准星移动到新目标时能够可预测地补放；长按攻击可以持续删除新命中的投影方块。

- [ ] #26 在空中放置投影方块
  - 第一阶段支持固定距离放置。
  - 后续支持平面锁定、网格锁定和从最近投影面延伸。
  - 验收：面向空气时可在配置目标处创建投影方块，且不会产生提示刷屏。

- [ ] #28 多方块放置
  - 支持线、面、盒和拖拽填充。
  - 建立在 undo/redo 和稀疏草稿模型之上。
  - 验收：一次操作可以创建多个投影方块，并作为一个 undo transaction。

## 方块状态、NBT 与交互

- [ ] #22 投影方块状态/NBT 编辑
  - 支持朝向、半砖、楼梯、含水等常见 BlockState 属性循环切换。
  - 为容器、告示牌、命令方块等 block entity 提供专用 NBT 编辑。
  - 将 block entity NBT 持久化到 schematic tile-entity 数据。
  - 验收：常见 BlockState 属性和 NBT 保存重载后保持一致。

- [ ] #29 与投影方块交互
  - 区分右键交互和相邻放置。
  - 对可交互投影方块打开虚拟 UI 或状态/NBT 编辑器。
  - 不向服务器发送交互。
  - 验收：右键投影容器、告示牌等方块时进入 Creator 编辑流程。

## 撤销、重做与历史

- [ ] #30 撤销/重做
  - 为每次编辑记录 before/after 状态。
  - 在合适场景下将多方块操作或连续手势视为一个 transaction。
  - 完成编辑后可以保留 history，明确丢弃 schematic 时清空。
  - 验收：放置、删除、批量编辑、状态修改和 NBT 修改均可撤销/重做。

## 多人同步

- [!] #27 多人游戏同步
  - Phase 1：集成 Syncmatica，共享普通 `.litematic` schematic 和 placement。
  - Phase 2：如果 Syncmatica 无法支持实时 Creator 草稿，则设计可选服务端 companion。
  - 按需同步草稿编辑、placement transform、undo/redo 冲突，以及 Creator camera 中其他玩家的位置。
  - 验收：多个客户端能实时或近实时看到同一个 Creator 草稿。
