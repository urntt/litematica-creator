# Litematica Creator TODO

最后更新：2026-08-03

## 已完成

- [x] #1 HUD 叠加和位置问题
- [x] #2 删除投影方块后方块总数不减少
- [x] #3 翻译按钮 tooltip 错误
- [x] #4 设置界面默认快捷键
- [x] #5 组合键不生效
- [x] #6 debug 设置名称 key 直显
- [x] #7 打开 Creator 物品栏时输入快捷键字符
- [x] #8 原版创造栏式虚拟物品栏
- [x] #9 Creator HUD 使用虚拟快捷栏和副手
- [x] #10 placement 移动后投影方块编辑位置异常
- [x] #11 稀疏草稿中未编辑位置被声明为空气
- [x] #12 投影方块使用原版式选中框
- [x] #13 Tweakeroo `freeCameraPlayerInputs` 兼容处理时机
- [x] #14 面向空气放置时提示刷屏
- [x] #15 长按放置的固定间隔
- [x] #16 Creator 模式下真实破坏阻断
- [x] #17 退出 Creator 模式后的异常交互阻断
- [x] #18 未保存 schematic 的 recovery cache
- [x] #19 独立翻译模式正确应用所选语言
- [x] #23 可配置的 Creator 投影编辑距离
- [x] #24 第一/第三人称使用虚拟手持与虚拟装备渲染
- [x] #25 成功编辑投影时播放本地手部动画
- [x] #31 退出世界时关闭 Creator 模式
- [x] #32 重新进入世界后 Creator 模式与 HUD 状态不一致
- [x] #33 Creator focus 目标解析与切换
- [x] #34 切换 Creator focus 时提示当前编辑目标
- [x] #35 删除彻底为空的 subregion
- [x] #36 普通放置不覆盖已有投影方块
- [x] #38 Creator pick block 使用原版式选槽与单个物品
- [x] #39 Creator 模式下用原版物品栏键打开 Creator 物品栏
- [x] #40 修正虚拟创造栏的 palette 点击语义
- [x] #43 更新默认快捷键
- [x] #44 搜索栏无法输入原版物品栏键对应字符
- [x] #45 Creator 物品栏重开后标签页被重置
- [x] #46 虚拟主手为空时不能使用虚拟副手放置
- [x] #47 Palette Shift+单击恢复原版 carried 行为
- [x] #48 Creator 模式下换手键误操作真实主副手
- [x] Creator 与 Litematica 的完成编辑/卸载命令边界
- [x] 坐标、目标解析和稀疏 region 核心单元测试
- [x] Focus、稀疏 subregion 和生命周期设计文档同步

实现细节和历史验收记录见 [`docs/completed-tasks.md`](docs/completed-tasks.md)。

## 状态标记

- [ ] 未开始
- [~] 进行中或已部分实现
- [!] 需要设计决策

## 编辑正确性

- [~] #37 删除单个投影方块后，整个投影区块偶发暂时不可见
  - 已确认这更像 schematic world/render chunk 的失效刷新问题，而不是投影数据丢失：再次放置方块会触发重建并恢复原有内容。
  - Creator 新建的 `1x1x1` cell 删除唯一方块时必然进入 `removeRegion()`；当前实现会先对 placement 发布 post-change 并调度后台 rebuild，之后才从 schematic 的各个 region 数据映射中移除该 region。Litematica worker 因此可能读到 placement 与 schematic 暂时不一致的状态。
  - 普通 region 内删块则会调用 `rebuildAllPlacements()`，为该 schematic 的每个 placement 重建全部 touched chunks，刷新范围远大于实际变更范围。
  - Litematica 的 rebuild task 会先卸载已有 schematic chunk，再重建并替换；同区块的新任务只会移除仍在队列中的旧任务，已经运行的任务不会被取消。全量重复调度与结构变更期间的异步读取共同构成目前最可信的竞态来源。
  - 修复方向：先在客户端线程完整提交 schematic 与所有 placement 的结构变更，再统一发布 placement 更新；分别保存每个 placement 的 old/new touched chunks，只对并集调度一次刷新。普通方块状态变化只刷新该 schematic 坐标映射到各 placement 后实际受影响的区块。
  - 诊断：debug 模式记录编辑坐标、是否删除 region、placement hash、old/new touched chunks、目标区块是否已加载及 rebuild 提交序号，用提交序号确认是否存在旧任务晚于新任务完成。
  - 复现矩阵：Creator cell/普通多方块 region、同区块/跨区块、玩家与目标位于同一/相邻区块、单 placement/同 schematic 多 placement、单击/快速连续删除、`loadEntireSchematics` 开启/关闭。
  - 验收：上述组合中删除只更新实际受影响的投影内容，不会令区块内其他投影暂时消失；跨区块和多 placement 变换后也不遗留旧渲染。

## 虚拟物品栏与输入

- [!] #41 后期兼容或模仿 Inventory Profiles Next / ItemScroller
  - Creator 物品栏使用本地虚拟 container；第三方模组通常针对原版 screen、`inventoryMenu` 和服务端 slot packet 工作，不能直接允许其操作真实菜单或发送同步包。
  - 先按目标版本调查两者可用的公开接口、screen/menu 识别方式和 mixin 注入点，再决定采用显式兼容适配器还是只复刻高价值行为。
  - 候选行为包括排序、同类物品移动、滚轮搬运、拖拽搬运和快捷栏补充；所有结果必须只写虚拟物品栏。
  - 验收：兼容功能不修改真实背包、不发送 container packet，并在未安装第三方模组时保持当前行为。

## Creator Camera

- [!] #20 用 Creator 自带相机替代或超越 Tweakeroo Free Camera
  - 设计纯客户端 camera entity，真实玩家本体保持不动且不向服务器发送移动。
  - 地面模式类似生存移动，支持跳跃、重力和可选碰撞。
  - 飞行模式类似创造飞行，并可像旁观模式一样穿过方块。
  - 处理相机输入、碰撞、其他玩家渲染、退出模式、断线和世界卸载。
  - 验收：未安装 Tweakeroo 时 Creator camera 仍可用，退出后玩家和相机状态完整恢复。

- [!] #42 Creator Camera 预览模式
  - 目标是把投影按普通世界方块的模型、纹理、流体和 block entity 方式不透明渲染，并隐藏缺失/错误方块的彩色 overlay 与轮廓。
  - Litematica 现有 translucent、colliding blocks 和 overlay 开关是全局配置；直接切换会影响所有 placement 并篡改玩家设置，因此需要 Creator 范围内的临时 render profile 或 placement-scoped renderer 判断。
  - 需要先确定预览作用于 focus placement 还是全部可见 placement，以及投影与真实方块重叠时的深度、遮挡和 block entity 渲染规则。
  - 进入/退出预览、退出 Creator、切世界和异常恢复时必须完整恢复 Litematica 原有渲染状态。
  - 验收：预览中的目标投影视觉上等同正常方块，同时不改变真实世界、不影响非目标 placement，也不持久改写 Litematica 配置。

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
