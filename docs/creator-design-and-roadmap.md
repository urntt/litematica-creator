# litematica-creator 设计与开发计划

## 1. 模组定位

`litematica-creator` 是一个 Litematica 客户端附属，目标是在生存模式下提供“像创造模式一样直接搭建投影”的客户端编辑体验。

它只创建和编辑 `.litematic` 投影草稿，不放置真实方块，不绕过服务器生存限制，也不向服务器发送真实放置包。草稿可以后续保存为普通 `.litematic`，也可以在未来通过 Syncmatica 做共享或同步。

依赖关系：

- 硬依赖：MaLiLib、Litematica。
- 软兼容：Tweakeroo 外部相机状态协调、Syncmatica。
- 第一版目标版本：Minecraft/Fabric `26.2`。

## 2. 26.2 开发基线

本项目第一版按工作区内 masa 系列 `26.2` 项目的实际构建方式开发，而不是按旧 Fabric/Yarn 教程开发。

已确认的本地基线：

- Minecraft：`26.2`
- Fabric Loader：`0.19.3`
- Loom：`net.fabricmc.fabric-loom` `1.17.+`
- Java：25
- MaLiLib：`0.29.2-sakura.4`
- Litematica：`0.28.2-sakura.1`
- Tweakeroo：`0.29.2-sakura.1`
- Access Widener namespace：`official`

重要约束：

- 不使用旧式 `mappings yarn(...)` 工作流。
- Mixin target、Access Widener、Minecraft 类名均按 `official` namespace。
- 优先复用 MaLiLib 的配置、热键、GUI、渲染辅助和输入管理能力。
- 优先复用 Litematica 的 schematic 数据结构、placement manager、投影世界和渲染管线。

## 3. 预期行为

### 3.1 Creator 模式

进入 Creator 模式后，玩家可以通过直接放置“投影方块”的方式从零创建一个投影草稿。

草稿作为普通 `LitematicaSchematic + SchematicPlacement` 加入 Litematica 的 placement manager，因此以下能力应尽量沿用 Litematica 生态：

- 投影渲染
- 保存 `.litematic`
- 后续材料列表
- verifier
- printer / easy-place 相关流程

Creator 模式下的左右键由 `litematica-creator` 消费，不能落到真实世界交互上。

### 3.2 虚拟物品栏

玩家拥有一套独立的客户端虚拟物品栏，和真实快捷栏/背包完全分离。

虚拟创造模式物品栏：

- 可打开类似真正创造模式物品栏的 GUI。
- 可按分类拿取各种物品。
- 可搜索选择任意 `BlockItem`。
- 只用于选择投影编辑用物品，不写入真实玩家背包。

虚拟生存模式物品栏分类：

- 9 格虚拟快捷栏。
- 27 格虚拟物品栏。
- 1 格虚拟副手。
- 4 格虚拟盔甲栏。
- 1 格虚拟丢弃栏。
- 不做虚拟合成栏。

输入行为：

- 数字键切换虚拟快捷栏位。
- 滚轮切换虚拟快捷栏位。
- 中键 pick block 可把真实方块或投影方块放入虚拟栏。
- 虚拟栏配置保存在客户端本地。

### 3.3 编辑操作

基础操作：

- 右键：用虚拟栏当前方块创建或放置投影方块。
- 左键：删除目标投影方块。
- 中键：拾取目标真实方块或投影方块到虚拟栏。
- 完成编辑：清空 Creator focus；不导出文件、不卸载 schematic，也不修改 Litematica selected placement。
- 卸载当前原理图：卸载 focus schematic 及其全部 placements，但永不删除原 `.litematic` 文件。
- 退出 Creator 模式：loaded schematic/placements 由 Litematica 继续管理；Creator focus 可保留，但临时重叠抑制会恢复。

目标选择：

- 命中已有 Creator 投影：在相邻面放置新投影。
- 命中真实世界方块：在真实方块相邻位置生成投影。
- 命中空气：后续支持固定距离、网格、平面锁定，或从最近投影面延伸。

### 3.4 动态扩容

不采用单个巨大 region 频繁 resize，也不再采用会声明大量隐式空气的固定 tile。

当前实现使用稀疏 subregion 模型：边界外每个明确编辑的位置创建一个 `1x1x1` Creator cell；普通 Litematica region 内的编辑继续写入原 container。

玩家在新区域放置方块时，自动创建对应 subregion。扩容后需要：

- 创建新的 schematic subregion/container。
- 更新 schematic metadata。
- 更新或重建 placement 的 subregion placement 信息。
- 让 placement manager 重新计算 touched chunks。
- 触发相关 schematic chunks rebuild。

### 3.5 Creator Camera 行为

- 开启 Creator 时从当前视角创建纯客户端相机，真实玩家本体保持不动且不发送相机移动或旋转。
- 地面模式使用原版式移动、跳跃和真实世界/可见投影形状碰撞；双击空格进入可穿墙的飞行模式。
- 编辑射线和放置朝向始终基于 Creator camera entity，而不是玩家身体。
- Creator 模式消费编辑输入，避免真实攻击或真实放置。
- Tweakeroo 只作为软兼容：接管已有 Free Camera 的位置并临时协调其输入配置，退出 Creator 后恢复，不作为核心相机依赖。

### 3.6 Syncmatica 行为

Syncmatica 集成不进入第一版核心范围。

后续可能支持：

- 将 Creator 草稿发布到 Syncmatica。
- 从 Syncmatica 共享草稿继续编辑。
- 多人协作编辑同一草稿。

## 4. 核心技术设计

### 4.1 Creator Focus 数据模型

Creator 不再维护 `CreatorDraft` 或 Creator-owned schematic 集合。唯一会话状态是独立于 Litematica selected placement 的 `CreatorFocus`，它包装当前编辑使用的 `SchematicPlacement`；实际内容始终直接写入该 placement 背后的普通 `LitematicaSchematic`。

- placement 是坐标、旋转和镜像视图；schematic 是数据真源。
- 同一 schematic 的任意 placement 都可作为编辑入口，修改会反映到全部镜像。
- dirty 唯一来源是 `schematic.getMetadata().wasModifiedSinceSaved()`。
- finish 只清空 focus；unload 按 focus schematic 工作；导出文件仍由 Litematica 原生界面负责。
- 新建空白对象没有占位 region，placement origin 使用目标位置或显式 New Blank 时的 camera block position。

### 4.2 稀疏 Subregion 模型

- 现有 enabled subregion 范围内：使用 Litematica 的 world→container 逆变换和 block state 逆变换写入原 region。
- 范围外：把世界位置按编辑 placement 的 mirror/rotation 逆变换为 schematic-relative 位置，并创建 `1x1x1` Creator cell。
- Creator cell 使用保留前缀；删除其最后一个非空气方块时移除整个 region。普通 region 中删除方块只写入显式空气。
- region 增删逐 placement 执行 pre-change→map change→post-change，同步同一 schematic 的全部 placements，保留已有 placement/subregion transforms。
- metadata 由 Litematica 对象直接维护；结构变化重算 region count、volume 和 enclosing size，内容变化增量维护 total blocks，并标记全部相关 placements rebuild。

### 4.3 投影方块写入流程

右键放置流程：

1. 判断 Creator 模式是否开启。
2. 消费本次 use input，阻止真实世界交互。
3. 获取编辑视角 entity：
   - Tweakeroo Free Camera active：使用 `mc.getCameraEntity()`。
   - 普通状态：使用玩家或 MaLiLib camera entity helper。
4. 从 camera entity 做 ray trace。
5. 按命中类型计算目标世界坐标。
6. 从 `CreatorInventory` 读取当前虚拟 `ItemStack`。
7. 用 camera entity 的 yaw/pitch 和 schematic world 构造 placement context。
8. 从 `BlockItem` 计算目标 `BlockState`。
9. 找到目标 region，或在边界外创建稀疏 Creator cell。
10. 写入 container。
11. 更新 metadata。
12. 标记相关 chunk rebuild。

左键删除流程：

1. 判断 Creator 模式是否开启。
2. 消费本次 attack input。
3. 从 camera entity ray trace 到 Creator 投影。
4. 找到对应 region/container 和局部坐标。
5. 设置为空气。
6. 更新 metadata 和 rebuild。

中键 pick block 流程：

1. 优先 ray trace Creator 投影。
2. 未命中则 ray trace 真实世界方块。
3. 将对应 block 的 `ItemStack` 放入当前虚拟快捷栏位。
4. 不写入真实玩家 inventory。

### 4.4 虚拟物品栏设计

新增 `CreatorInventory`。

职责：

- 存储虚拟快捷栏、虚拟背包、副手、盔甲和丢弃栏。
- 提供当前 selected slot。
- 支持序列化到客户端本地配置文件。
- 给 GUI、HUD 和放置逻辑提供统一读取 API。

新增 `GuiCreatorInventory`。

第一版最低目标：

- 显示虚拟生存模式物品栏。
- 显示虚拟创造分类和搜索入口。
- 支持拖拽/点击放入虚拟槽位。
- 支持清空槽位。

可以分阶段完成：

- 第一阶段先支持搜索 `BlockItem` 和 9 格虚拟快捷栏。
- 第二阶段补齐 27 格背包、副手、盔甲、丢弃栏和分类页。

### 4.5 Creator Camera 与外部相机兼容层

- `CreatorCameraEntity` 复用客户端玩家移动物理，但不注册到世界实体列表，也不执行玩家网络 tick。
- `CreatorCameraController` 负责接管和恢复 camera entity、玩家输入、`smartCull`、世界生命周期及跨区块渲染刷新。
- `CreatorCameraCompat` 通过反射软检测 Tweakeroo，快照并临时协调 Free Camera 的 player movement/input 配置，不引入硬依赖。
- Creator 编辑统一从当前 Creator camera 自行 ray trace，不依赖可能被其他模组改写的 `mc.hitResult`。

### 4.6 输入拦截

Creator 模式需要比真实交互更早消费左右键。

候选接入点：

- MaLiLib 输入处理和热键。
- mixin 到 Minecraft/interaction manager 的 use/attack 路径。
- 参考 Litematica `REBUILD` 与 `EasyPlace` 的输入拦截。

原则：

- Creator 模式开启且命中有效编辑行为时，必须阻止真实服务器交互。
- 放置失败时也应谨慎处理，避免误放真实方块。
- 不依赖真实主手物品。

## 5. 开发计划

### 阶段 0：项目骨架

目标：建立可编译、可加载的 26.2 client mod。

任务：

- 创建 Gradle 项目结构。
- 配置 `fabric.mod.json`。
- 配置 mixins 和 access widener。
- 依赖 MaLiLib 和 Litematica。
- 可选 compileOnly Tweakeroo/Syncmatica。
- 注册基础 logger、初始化入口、配置分类和一个开关热键。

验收：

- `gradle build` 通过。
- 游戏内能加载 `litematica-creator`。
- MaLiLib 配置页能看到 Creator 模式开关。

### 阶段 1：Creator 模式和空草稿

目标：能从零创建一个空草稿并作为 Litematica placement 显示。

任务：

- 实现 `CreatorManager`。
- 实现独立 `CreatorFocus`。
- 创建真正零 region 的空 `LitematicaSchematic`。
- 创建 placement，但不自动覆盖 Litematica selected placement。
- 创建 `SchematicPlacement` 并加入 Litematica placement manager。
- 实现完成编辑和按 focus 卸载语义；未保存 recovery 单列后续任务。

验收：

- 开启 Creator 模式后 placement manager 中出现草稿。
- 关闭 Creator 模式后草稿仍可保留。
- 可通过 Litematica 原生界面导出为普通 `.litematic`。

### 阶段 2：基础投影编辑

目标：右键放置投影、左键删除投影。

任务：

- 实现 Creator 输入拦截。
- 实现 camera entity ray trace。
- 实现命中已有 Creator 投影后相邻面放置。
- 实现命中真实方块后相邻位置生成投影。
- 实现投影删除。
- 实现 metadata 更新和 chunk rebuild。

验收：

- 生存模式下不消耗真实物品。
- 不向服务器发送真实放置包。
- 右键可创建投影方块。
- 左键可删除投影方块。
- 保存后重新加载 `.litematic` 内容正确。

### 阶段 3：虚拟快捷栏 MVP

目标：脱离真实背包，用虚拟栏选择投影方块。

任务：

- 实现 `CreatorInventory`。
- 实现 9 格虚拟快捷栏。
- 实现数字键/滚轮切换。
- 实现 HUD 显示。
- 实现中键 pick block 到虚拟快捷栏。
- 实现客户端本地持久化。

验收：

- 当前虚拟 slot 决定放置方块。
- 真实背包不被修改。
- 重启客户端后虚拟快捷栏保留。

### 阶段 4：动态扩容（已完成稀疏第一版）

目标：玩家向新区域放置时自动创建稀疏 subregion。

任务：

- 实现 placement-aware world pos 到 schematic relative/container 坐标的逆变换。
- 实现按需创建 1×1 Creator cell subregion。
- 实现 region name 规则。
- 实现 schematic metadata 更新。
- 实现 placement subregion 刷新。
- 实现 placement manager touched chunks 更新。
- 处理不相接位置和跨 chunk 放置。

验收：

- 在远离已有投影的位置放置会自动扩容。
- 新 Creator cell 能正确渲染。
- 新 Creator cell 可由 Litematica 导出到 `.litematic`。
- 重新加载后 placement 区域正确。

### 阶段 5：虚拟创造物品栏 GUI

目标：提供接近创造模式的客户端选物 GUI。

任务：

- 实现虚拟生存模式物品栏页面。
- 实现 `BlockItem` 搜索。
- 实现基础分类。
- 支持点击/拖拽到虚拟栏。
- 支持虚拟副手、盔甲、丢弃栏展示。

验收：

- 玩家无需真实获得方块即可从 GUI 选择投影用方块。
- GUI 操作不影响真实 inventory。
- 搜索和分类能覆盖常见 `BlockItem`。

### 阶段 6：Creator Camera

目标：提供不依赖 Tweakeroo 的纯客户端编辑相机，并兼容已有外部相机状态。

任务：

- 实现客户端相机实体、地面移动、双击飞行和速度配置。
- 隔离真实玩家输入、移动和转向，阻断相机玩家发包入口。
- 合并真实世界与可见投影的碰撞形状。
- 统一从 Creator camera 做编辑射线和放置朝向计算。
- 快照、协调并恢复 Tweakeroo Free Camera 与渲染器状态。

验收：

- 地面和飞行模式移动正确，可站在非完整投影形状上并在飞行时穿墙。
- 真实玩家身体不攻击、不放置、不移动、不随相机转向。
- 退出、断线、切世界、死亡和外部相机组合下完整恢复状态。

### 阶段 7：完善放置语义

目标：提升和创造模式放置的一致性。

任务：

- 支持 stairs/slabs/facing/waterlogged 等普通状态。
- 处理 door/bed/tall plant 等多方块放置。
- 处理 neighbor dependent 方块。
- 初步处理 block entity 默认 NBT。
- 增加撤销/重做。

验收：

- 常见建筑方块朝向正确。
- 多方块方块行为可预测。
- 错误放置可撤销。

### 阶段 8：Syncmatica 集成

目标：支持共享 Creator 草稿。

任务：

- 调研 Syncmatica 当前 26.2 API 和 mixin 点。
- 将已保存或未保存草稿发布到 Syncmatica。
- 从 Syncmatica 拉取草稿继续编辑。
- 设计多人编辑冲突策略。

验收：

- 单人可把 Creator 草稿发布为共享 schematic。
- 其他客户端可看到或加载该草稿。

## 6. 主要风险

### 6.1 Litematica 内部 API 不稳定

Litematica 没有为 addon 暴露完整稳定 API。新增 subregion、刷新 placement manager touched chunks、修改 private map 等可能需要 mixin/access widener。

策略：

- 优先使用 public API。
- 只暴露最小必要访问。
- 用封装层隔离 Litematica 内部细节。

### 6.2 虚拟物品栏与放置上下文

Minecraft 的 `BlockItem#getStateForPlacement` 往往依赖 player、hand、world、hit result 和 held stack。Creator 的虚拟物品栏不在真实 player inventory 内，因此需要谨慎构造上下文。

策略：

- 第一版优先支持普通 `BlockItem`。
- 必要时 mixin `getItemInHand`，仅在 Creator placement context 内返回虚拟 stack。
- 避免污染真实 inventory。

### 6.3 输入误透传

Creator 模式下如果右键没有被正确消费，玩家可能真实放置方块或触发服务器交互。

策略：

- Creator 模式优先拦截 use/attack。
- 放置失败时默认也不透传真实 use。
- 提供明确状态 HUD，避免玩家误以为在普通模式。

### 6.4 动态 subregion 刷新

新增 subregion 后，如果 placement manager 的 touched chunks 缓存没有更新，新区域不会被 rebuild 和渲染。

策略：

- 把 subregion 创建、placement 刷新和 chunk rebuild 做成一个原子操作。
- 增加调试命令或日志输出当前 touched chunks。
- 保存/重载作为验证手段。

### 6.5 外部相机软兼容

Tweakeroo 没有专门为 Creator 暴露稳定 public API。直接硬 import 会增加版本耦合。

策略：

- Creator Camera 核心不依赖 Tweakeroo 类或功能。
- 默认通过反射软检测 Tweakeroo 配置，缺失或字段变化时安全跳过。
- 会话开始时保存外部 camera entity 和配置值，会话期间维持兼容值，退出时精确恢复。
- Tweakeroo 在 Creator 会话内切换时重新协调，Creator Camera 始终保持当前视角优先级。

## 7. 第一版范围

第一版必须包含：

- 26.2 client mod 骨架。
- Creator 模式开关。
- 从零创建草稿。
- 草稿作为 Litematica placement 渲染。
- 右键放置投影方块。
- 左键删除投影方块。
- 9 格虚拟快捷栏。
- 中键 pick block。
- 稀疏 subregion 动态扩容。
- 使用 Litematica 原生界面导出普通 `.litematic`。
- Tweakeroo Free Camera 下基本可用。

第一版暂不包含：

- Syncmatica 集成。
- 完整创造模式 GUI 还原。
- 多人协同编辑。
- 完整 block entity 编辑。
- 所有多方块放置语义。
- 撤销/重做。

## 8. 建议实现顺序

推荐先实现最小闭环：

1. 项目骨架。
2. Creator 模式开关。
3. 创建空草稿 placement。
4. 用一个硬编码 block state 写入草稿。
5. 触发 Litematica 渲染 rebuild。
6. 接入右键目标选择。
7. 接入 9 格虚拟快捷栏。
8. 实现稀疏 subregion 动态扩展。
9. 对接 Litematica 原生导出/卸载边界。
10. 再做 GUI 和 Free Camera 细节。

这个顺序能尽早验证最关键风险：Litematica 的 schematic/placement 数据能否被外部 addon 稳定修改并实时渲染。

## 9. 当前 MVP 实现状态（2026-08-03）

截至当前工作树，已实现并通过本地编译闭环的内容：

- Fabric `26.2` 客户端模组骨架、MaLiLib/Litematica 硬依赖、Tweakeroo/Syncmatica `suggests`、Mod Menu 入口、MaLiLib 配置页、热键与中英文 i18n。
- Creator 世界生命周期：退出/加入世界时 mode off，清理 focus、输入 cooldown、临时 placement 抑制和 Tweakeroo 兼容状态，并在 Litematica 加载后重建 placement index。
- 独立 `CreatorFocus`，不覆盖 Litematica selected placement；支持编辑任意普通 Litematica schematic/placement。
- 真正零 region 的空白 schematic；`M,N` 新建，`selectNewDraftPlacement` 默认关闭。
- chunk 空间索引和目标解析：唯一范围自动归属、范围外扩展 focus/无 focus 新建、重叠时打开 `M,F` Focus Switcher。
- 重叠候选的 Creator-local 临时抑制，不持久化或改写原生 enabled/render 状态。
- placement/subregion 变换感知编辑；1×1 稀疏 Creator cell 动态扩展；新增/移除 region 同步同一 schematic 的全部 placements。
- `hideSubregionBoxesInCreatorMode=true` 默认隐藏密集 placement subregion box，不修改 Litematica 配置。
- 客户端虚拟栏数据模型：9 格虚拟快捷栏、27 格虚拟背包、1 格副手、4 格盔甲、1 格丢弃栏；支持 selected slot、本地 JSON 持久化、数字键/滚轮切换。
- 简化版虚拟物品栏 GUI：左侧显示 42 个虚拟槽位并支持选中/清空目标槽位，右侧按 `BlockItem` 列表搜索和分页选择，点击后写入目标虚拟槽位，不触碰真实背包。
- 基础 HUD 状态显示：虚拟槽位/方块、focus placement、schematic、dirty、region 数和方块数。
- Creator 编辑闭环：右键用虚拟 `BlockItem` 创建投影方块，左键删除当前 Creator 投影方块，中键 pick 真实方块或 Creator 投影方块。
- 完成编辑入口只清空 focus；卸载入口按 focus schematic 卸载全部 placements；文件导出继续使用 Litematica 原生界面。
- Creator 自带纯客户端相机：地面模式复用原版玩家移动物理和真实/投影碰撞，双击空格切换可穿墙飞行，速度倍率可配置，HUD 显示当前状态。
- 真实玩家输入、移动、转向和 controlled-camera 状态在会话中隔离；相机不执行网络玩家 tick，编辑射线及放置朝向全部来自 Creator camera。
- Tweakeroo Free Camera 配置通过反射软检测并按会话快照恢复；跨区块渲染边缘刷新使用可选注入，不将 Tweakeroo 或 Sodium 变为硬依赖。
- JUnit 回归测试覆盖旋转/镜像坐标往返、候选身份合并、放置占用策略、通用 region 空状态、focus 通知决策，以及 recovery eligibility、manifest、调度、原子 generation 和主动删除抑制。
- 左右键拦截：在 Creator 模式下通过 Mixin 消费 `Minecraft.startUseItem` 与 `Minecraft.startAttack`，避免真实服务器交互透传。
- Recovery cache：non-file-backed 和已修改的 file-backed schematics 使用标准 `.litematic` generation 与 manifest v1 自动缓存；世界恢复时保留 placement 变换、selected placement 和 Creator focus，但不自动开启 Creator 模式。
- Recovery 写入采用客户端 snapshot、单线程后台压缩、5 秒 idle/30 秒最大延迟和生命周期同步 flush；主动移除、卸载与 Creator 丢弃不会在下次进入世界时复活。

当前已知边界：

- 虚拟创造物品栏 GUI 仍是轻量搜索列表和槽位按钮，尚未完全还原原版创造模式分类页、物品图标网格和拖拽交互。
- 空气命中暂未实现固定距离、网格、平面锁定或从最近投影面延伸。
- 第一版只覆盖普通 `BlockItem` 的单方块放置语义；门、床、高草、复杂 block entity、undo/redo 需要后续专项完善。
- Syncmatica 集成仍在后续范围。

## 10. 开发与验证记录

本轮 Creator 编辑内核 git 里程碑：

- `a8c0078 build: avoid local malilib composite jar lock`
- `d4aea51 feat: add creator focus and placement target resolution`
- `9a3d14e feat: support transformed sparse schematic editing`
- `fe2cd4f feat: add focus switcher and lifecycle cleanup`
- `5912a5a feat: add schematic recovery storage`
- `22ef7e0 feat: restore unsaved schematics across sessions`

本地构建方式：

- 标准验证命令：`.\gradlew.bat build --no-daemon --max-workers=1`
- 当前 wrapper 使用 Gradle `9.6.0`。OneDrive 对 Gradle ZIP/JAR 输出存在文件锁时，应把所有 composite project 的 `layout.buildDirectory` 重定向到非 OneDrive 临时目录；本轮完整 build 与测试已用该方式通过。
- 若 wrapper 分发下载不可用，可临时使用工作树内忽略提交的 `.gradle-local/gradle-9.6.0/bin/gradle.bat` 作为本地 fallback。
- `.gradle-local/` 与 `gradle-*.zip` 已在 `.gitignore` 中忽略，避免把本地工具缓存提交进仓库。
