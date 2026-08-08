# Litematica Creator TODO

最后更新：2026-08-08

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
- [x] #11 稀疏草稿编辑模型与 Litematica 集成基础
- [x] #12 投影方块使用原版式选中框
- [x] #13 Tweakeroo `freeCameraPlayerInputs` 兼容处理时机
- [x] #14 面向空气放置时提示刷屏
- [x] #15 长按放置的固定间隔
- [x] #16 Creator 模式下真实破坏阻断
- [x] #17 退出 Creator 模式后的异常交互阻断
- [x] #18 未保存 schematic 的 recovery cache
- [x] #19 Creator 文本直接跟随游戏语言
- [x] #20 Creator Camera
- [x] #21 按住攻击键持续拆除投影方块
- [x] #23 可配置的 Creator 投影编辑距离
- [x] #24 第一/第三人称使用虚拟手持与虚拟装备渲染
- [x] #25 成功编辑投影时播放本地手部动画
- [x] #26 固定距离在空中放置投影方块
- [x] #31 退出世界时关闭 Creator 模式
- [x] #32 重新进入世界后 Creator 模式与 HUD 状态不一致
- [x] #33 Creator focus 目标解析与切换
- [x] #34 切换 Creator focus 时提示当前编辑目标
- [x] #35 删除彻底为空的 subregion
- [x] #36 普通放置不覆盖已有投影方块
- [x] #37 删除投影方块后的投影区块重建竞态
- [x] #38 Creator pick block 使用原版式选槽与单个物品
- [x] #39 Creator 模式下用原版物品栏键打开 Creator 物品栏
- [x] #40 修正虚拟创造栏的 palette 点击语义
- [x] #43 更新默认快捷键
- [x] #44 搜索栏无法输入原版物品栏键对应字符
- [x] #45 Creator 物品栏重开后标签页被重置
- [x] #46 虚拟主手为空时不能使用虚拟副手放置
- [x] #47 Palette Shift+单击恢复原版 carried 行为
- [x] #48 Creator 模式下换手键误操作真实主副手
- [x] #49 可配置的持续放置投影间隔
- [x] #50 Creator Camera 下渲染并保留真实本体物理
- [x] #51 第一/第三人称渲染半透明 Creator 相机替身
- [x] #52 独立 Creator Camera 热键与 Creator 模式自动联动设置
- [x] #53 Creator 相机姿态和碰撞箱不继承真实本体状态
- [x] #54 Creator 相机遵循原版自动跳跃设置
- [x] #55 可配置的 Creator 相机投影碰撞
- [x] #56 可配置的 Creator 相机实体放置碰撞忽略
- [x] #57 进入 Creator 模式时未注册相机实体缺少 ID 导致崩溃
- [x] #58 第一人称 Creator 相机只渲染手持物品或半透明手臂
- [x] #59 Creator 编辑挥手只作用于相机替身
- [x] #60 Creator 相机替身渲染玩家皮肤第二层
- [x] #61 放置投影方块后的投影区块重建竞态
- [x] #62 Creator 相机替身不再推动真实实体
- [x] #63 Creator 相机独立更新潜行等姿态碰撞箱
- [x] #64 Creator 相机投影碰撞兼容 Lithium
- [x] #68 潜行时可在投影方块支撑面上移动
- [x] #69 Creator 物品栏玩家预览使用相机替身
- [x] #71 Creator 相机匍匐模型姿态同步
- [x] #72 单人游戏暂停时冻结 Creator 相机
- [x] #73 Creator Camera 下本体继续更新姿态与运动状态
- [x] #74 Creator Camera 下本体运动继续同步至服务端
- [x] #75 可配置是否允许在空中放置投影方块
- [x] #76 Creator 物品栏替身预览使用半透明渲染
- [x] #78 投影楼梯空缺部分不再误触发匍匐姿态
实现细节和历史验收记录见 [`docs/completed-tasks.md`](docs/completed-tasks.md)。

## 已取消

- #21 Accurate 连续放置与 backfill：游戏内效果不符合预期，已回退；放置保留 #15 的固定间隔模型，并由 #49 配置间隔。

## 状态标记

- [ ] 未开始
- [~] 进行中或已部分实现
- [!] 需要设计决策

## 原理图导出与数据模型

- [ ] #79 Creator 草稿导出 metadata 规范化
  - Litematica 的“保存到文件”区分文件名与 schematic metadata name；Creator 临时草稿首次导出时需要消除 `creator-draft-*` 占位名称。
  - 仅对仍使用 Creator 临时名称的草稿，把导出文件内的 metadata name 设为最终文件名 stem；已有正式名称的普通 schematic 保持原样。
  - 保留有效的 `timeCreated`；旧草稿的创建时间小于等于 0 时，优先从 `creator-draft-yyyyMMdd-HHmmss` 恢复，无法解析时再回退到导出时间。
  - `timeModified` 继续表示最后编辑时间，author 和普通 file-backed schematic 的重命名语义不变。
  - 验收：Creator 新旧草稿导出后名称与创建时间正确；普通 Litematica schematic 的“重命名原理图/重命名文件”边界不受影响。

- [ ] #80 导出时压缩稀疏 Creator cell subregions
  - 当前 `1x1x1` Creator cell 是编辑态和 recovery cache 的稀疏表示；导出普通 `.litematic` 时生成独立规范化快照，不实时改写当前 schematic 或 placements。
  - 只压缩带 Creator 保留前缀的 cell regions，原有普通 Litematica regions 保持名称、边界和内容不变。
  - 将面相邻 cells 确定性地合并/分割为无空洞、完全填满的 cuboid regions；不得用包含未编辑位置的包围盒，避免重新引入 #11 的显式空气问题。
  - L 形、中空或其他非长方体结构保留为多个 cuboids；相距较远及仅边/角接触的 cells 不合并。
  - 搬移并重定位 BlockState、block entity NBT、entities、scheduled block ticks 和 scheduled fluid ticks，随后重新计算 region count、total volume、total blocks 和 enclosing size。
  - Recovery cache 继续保存未经压缩的稀疏编辑状态；压缩仅作用于玩家通过 Litematica 明确导出的文件。
  - 验收：相邻实心结构导出后 region 数显著减少且语义不变；不规则结构不产生额外显式空气，保存重载后方块、附属数据和 metadata 保持正确。

## 虚拟物品栏与输入

- [!] #41 后期兼容或模仿 Inventory Profiles Next / ItemScroller
  - Creator 物品栏使用本地虚拟 container；第三方模组通常针对原版 screen、`inventoryMenu` 和服务端 slot packet 工作，不能直接允许其操作真实菜单或发送同步包。
  - 先按目标版本调查两者可用的公开接口、screen/menu 识别方式和 mixin 注入点，再决定采用显式兼容适配器还是只复刻高价值行为。
  - 候选行为包括排序、同类物品移动、滚轮搬运、拖拽搬运和快捷栏补充；所有结果必须只写虚拟物品栏。
  - 验收：兼容功能不修改真实背包、不发送 container packet，并在未安装第三方模组时保持当前行为。

## Creator Camera

- [~] #70、#77 Creator 相机虚拟鞘翅模型与滑翔姿态同步
  - 已确认此前只替换了虚拟胸甲和纹理相关 render state，却没有补回跳过 `LivingEntity.tick()` 后缺失的 `fallFlyTicks` 与 `ElytraAnimationState.tick()`。
  - `fallFlyTicks` 未推进会让 `AvatarRenderState.fallFlyingScale()` 始终为 0，因此替身保持直立；翼动画未推进会让左右翼角始终为 0，因此两片翼重叠成使用默认鞘翅纹理的竖直矩形。
  - 当前实现已按原版生命周期推进滑翔计时和翼动画；游戏内已确认鞘翅模型、替身模型和滑翔动作正常。
  - 剩余问题：虚拟鞘翅错误地抑制了原版 `showCape`，导致 `WingsLayer` 无法选择玩家披风纹理；应恢复原版披风显示状态，由原版 `CapeLayer` 在胸甲为 wings 时自行跳过矩形披风渲染。
  - 验收：启用玩家披风时虚拟鞘翅使用玩家披风纹理，关闭披风显示时使用默认鞘翅纹理，且不会额外渲染固定矩形披风。

- [!] #42 Creator Camera 预览模式
  - 目标是把投影按普通世界方块的模型、纹理、流体和 block entity 方式不透明渲染，并隐藏缺失/错误方块的彩色 overlay 与轮廓。
  - Litematica 现有 translucent、colliding blocks 和 overlay 开关是全局配置；直接切换会影响所有 placement 并篡改玩家设置，因此需要 Creator 范围内的临时 render profile 或 placement-scoped renderer 判断。
  - 需要先确定预览作用于 focus placement 还是全部可见 placement，以及投影与真实方块重叠时的深度、遮挡和 block entity 渲染规则。
  - 进入/退出预览、退出 Creator、切世界和异常恢复时必须完整恢复 Litematica 原有渲染状态。
  - 验收：预览中的目标投影视觉上等同正常方块，同时不改变真实世界、不影响非目标 placement，也不持久改写 Litematica 配置。

- [!] #65 投影方块特殊物理效果
  - 当前投影只向 Creator Camera 提供 `VoxelShape`，相机的方块内效果、摩擦、攀爬和流体检查仍读取真实 `ClientLevel`，因此不会感知投影蜘蛛网、梯子、粘液块、蜂蜜块、灵魂沙、细雪、冰或流体。
  - 分阶段实现：先处理摩擦、地面速度和弹跳，再处理方块内减速/攀爬/细雪，最后单独设计投影流体高度、推动、游泳和呼吸语义。
  - 不把 schematic world 全局伪装成真实世界；适配器只能作用于 `CreatorCameraEntity`，并继续遵循投影可见性、placement 合成和碰撞开关。
  - 验收：常见特殊方块在地面模式表现接近原版，飞行模式仍可穿透且不受效果影响，真实玩家和其他实体的物理不改变。

## UI 与信息

- [!] #66 UI 不清晰、状态信息不足
  - 重新梳理 Creator HUD、设置分组、物品栏入口、focus/draft/recovery 状态以及 Camera 的地面/飞行/碰撞状态，不用单纯堆叠更多常驻文本。
  - 先通过实际工作流确定需要常驻、按需显示和仅在错误时提示的信息，再统一视觉层级和交互入口。

## 文档

- [ ] #67 补齐用户文档与 Wiki
  - 以仓库内文档作为唯一源稿，覆盖安装与依赖、快捷键、草稿/focus/recovery、Creator 物品栏、Camera、兼容性、故障排查和开发架构。
  - README 保持项目入口与快速开始，详细章节后续可同步发布到 Wiki，避免两套内容独立维护后漂移。

## 放置控制

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
