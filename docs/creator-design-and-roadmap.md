# litematica-creator 设计与开发计划

## 1. 模组定位

`litematica-creator` 是一个 Litematica 客户端附属，目标是在生存模式下提供“像创造模式一样直接搭建投影”的客户端编辑体验。

它只创建和编辑 `.litematic` 投影草稿，不放置真实方块，不绕过服务器生存限制，也不向服务器发送真实放置包。草稿可以后续保存为普通 `.litematic`，也可以在未来通过 Syncmatica 做共享或同步。

依赖关系：

- 硬依赖：MaLiLib、Litematica。
- 软兼容：Tweakeroo Free Camera、Syncmatica。
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
- 保存：把当前草稿保存为普通 `.litematic`。
- 退出 Creator 模式：草稿仍可保留为未保存状态，并随时可以保存或丢弃。

目标选择：

- 命中已有 Creator 投影：在相邻面放置新投影。
- 命中真实世界方块：在真实方块相邻位置生成投影。
- 命中空气：后续支持固定距离、网格、平面锁定，或从最近投影面延伸。

### 3.4 动态扩容

不采用单个巨大 region 频繁 resize 的模型。

第一版采用 tile/subregion 模型，例如：

- `16x16x16`
- `16x64x16`
- chunk column

玩家在新区域放置方块时，自动创建对应 subregion。扩容后需要：

- 创建新的 schematic subregion/container。
- 更新 schematic metadata。
- 更新或重建 placement 的 subregion placement 信息。
- 让 placement manager 重新计算 touched chunks。
- 触发相关 schematic chunks rebuild。

### 3.5 Free Camera 行为

未安装 Tweakeroo：

- 从玩家当前视角编辑。

安装并开启 Tweakeroo Free Camera：

- 从 freecam 相机视角编辑。
- Creator 模式消费左右键，避免真实攻击或真实放置。
- Creator 默认要求 `freeCameraPlayerInputs=false`，否则 Tweakeroo 会把点击射线切回真实玩家身体。
- 放置朝向应基于 camera entity，而不是玩家身体朝向。

Tweakeroo 作为软兼容处理：没有 Tweakeroo 时 Creator 不应崩溃，也不应要求硬依赖。

### 3.6 Syncmatica 行为

Syncmatica 集成不进入第一版核心范围。

后续可能支持：

- 将 Creator 草稿发布到 Syncmatica。
- 从 Syncmatica 共享草稿继续编辑。
- 多人协作编辑同一草稿。

## 4. 核心技术设计

### 4.1 Draft 数据模型

新增 `CreatorDraft` 作为草稿生命周期的核心对象。

职责：

- 持有当前 `LitematicaSchematic`。
- 持有当前 `SchematicPlacement`。
- 管理 tile/subregion。
- 管理未保存状态。
- 提供保存、丢弃、恢复、查询和修改 API。

建议内部模型：

- 世界坐标按 tile 坐标映射到 region name。
- 每个 tile 对应一个 Litematica subregion。
- 每个 subregion 内部坐标为固定尺寸局部坐标。
- 所有写入最终落到 `LitematicaBlockStateContainer#set(...)`。

### 4.2 Tile/Subregion 模型

推荐第一版采用 `16x64x16` 或 chunk column 作为默认 tile。

优点：

- X/Z 与 chunk 对齐，便于 rebuild 和命中计算。
- 避免每次越界都复制巨大 container。
- 动态扩容只需要新增 subregion。
- 保存后仍是普通 multi-region `.litematic`。

需要注意：

- `SchematicPlacement` 内部有 subregion placement map，需要在新增 subregion 后同步刷新。
- Litematica 的 `SchematicPlacementManager` 对 touched chunks 有缓存，新 subregion 创建后必须更新这些缓存。
- 如果现有 API 不够，需要 mixin/access widener 暴露最小必要方法。

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
9. 找到或创建目标 tile/subregion。
10. 写入 container。
11. 更新 metadata。
12. 标记相关 chunk rebuild。

左键删除流程：

1. 判断 Creator 模式是否开启。
2. 消费本次 attack input。
3. 从 camera entity ray trace 到 Creator 投影。
4. 找到对应 tile/subregion 和局部坐标。
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

### 4.5 Free Camera 兼容层

新增 `CreatorCameraCompat`。

职责：

- 不硬依赖 Tweakeroo 类。
- 检测 `tweakeroo` 是否加载。
- 读取当前 camera entity。
- 判断是否处于 Free Camera 编辑场景。
- 在 Creator 模式下避免依赖 `mc.hitResult`，统一使用 camera entity 自己 ray trace。

行为建议：

- 不直接强行改 Tweakeroo 配置，第一版可以在状态不兼容时提示玩家关闭 `freeCameraPlayerInputs`。
- 后续可增加“进入 Creator 时临时关闭，退出时恢复”的选项。

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
- 实现 `CreatorDraft`。
- 创建空 `LitematicaSchematic`。
- 创建初始 tile/subregion。
- 创建 `SchematicPlacement` 并加入 Litematica placement manager。
- 实现保存/丢弃/退出时保留未保存状态。

验收：

- 开启 Creator 模式后 placement manager 中出现草稿。
- 关闭 Creator 模式后草稿仍可保留。
- 可保存为普通 `.litematic`。

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

### 阶段 4：动态扩容

目标：玩家向新区域放置时自动创建 tile/subregion。

任务：

- 实现 world pos 到 tile key 的映射。
- 实现按需创建 subregion。
- 实现 region name 规则。
- 实现 schematic metadata 更新。
- 实现 placement subregion 刷新。
- 实现 placement manager touched chunks 更新。
- 处理跨 tile 相邻放置。

验收：

- 在远离已有投影的位置放置会自动扩容。
- 新 tile 能正确渲染。
- 新 tile 保存到 `.litematic`。
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

### 阶段 6：Tweakeroo Free Camera 兼容

目标：安装并开启 Tweakeroo Free Camera 时，从 freecam 位置编辑投影。

任务：

- 实现 `CreatorCameraCompat`。
- 检测 Tweakeroo 是否加载。
- 始终从 `mc.getCameraEntity()` 或 MaLiLib camera entity 取编辑视角。
- 避免使用可能被 Tweakeroo 改写到玩家身体的 `mc.hitResult`。
- 检测 `freeCameraPlayerInputs` 不兼容状态并提示。
- 放置朝向基于 camera entity。

验收：

- Free Camera 下右键从相机位置放置投影。
- 真实玩家身体不攻击、不放置、不移动。
- 方块朝向符合相机视角。

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

### 6.5 Free Camera 软兼容

Tweakeroo 没有专门为 Creator 暴露稳定 public API。直接硬 import 会增加版本耦合。

策略：

- 默认软检测。
- 尽量只依赖 Minecraft 的 `mc.getCameraEntity()`。
- 不依赖 Tweakeroo 私有类来完成核心功能。
- 需要读取具体配置时再考虑反射或可选编译依赖。

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
- tile/subregion 动态扩容。
- 保存普通 `.litematic`。
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
8. 实现动态 tile。
9. 实现保存。
10. 再做 GUI 和 Free Camera 细节。

这个顺序能尽早验证最关键风险：Litematica 的 schematic/placement 数据能否被外部 addon 稳定修改并实时渲染。

## 9. 当前 MVP 实现状态

截至当前工作树，已实现并通过本地编译闭环的内容：

- Fabric `26.2` 客户端模组骨架、MaLiLib/Litematica 硬依赖、Tweakeroo/Syncmatica `suggests`、Mod Menu 入口、MaLiLib 配置页、热键与中英文 i18n。
- Creator 模式生命周期、从第一个目标方块创建 `LitematicaSchematic + SchematicPlacement` 草稿，并加入 Litematica placement manager。
- `16x16x16` tile/subregion 动态扩容；新 tile 写入 schematic private maps，刷新 schematic metadata、placement subregion count、placement subregion values，并标记 touched chunk rebuild。
- 客户端虚拟栏数据模型：9 格虚拟快捷栏、27 格虚拟背包、1 格副手、4 格盔甲、1 格丢弃栏；支持 selected slot、本地 JSON 持久化、数字键/滚轮切换。
- 简化版虚拟物品栏 GUI：左侧显示 42 个虚拟槽位并支持选中/清空目标槽位，右侧按 `BlockItem` 列表搜索和分页选择，点击后写入目标虚拟槽位，不触碰真实背包。
- 基础 HUD 状态显示：Creator 模式开启时显示虚拟槽位、虚拟方块、草稿名、保存状态、tile 数和投影方块数。
- Creator 编辑闭环：右键用虚拟 `BlockItem` 创建投影方块，左键删除当前 Creator 投影方块，中键 pick 真实方块或 Creator 投影方块。
- 保存入口：将当前草稿保存到 Litematica schematics 目录中的普通 `.litematic` 文件，保存后清除 dirty 状态。
- Free Camera 基本兼容路径：编辑 ray trace 始终从 MaLiLib/Minecraft camera entity 获取；未安装 Tweakeroo 时自然回退玩家视角。
- 放置状态计算会在构造 `BlockPlaceContext` 时临时使用 camera entity 的 yaw/pitch，再立即恢复真实 player 旋转，让 Free Camera 下的朝向跟随相机。
- Tweakeroo Free Camera 配置提示：通过反射软检测 `TWEAK_FREE_CAMERA` 与 `FREE_CAMERA_PLAYER_INPUTS`，在不兼容组合下节流 warning，不引入硬依赖。
- 左右键拦截：在 Creator 模式下通过 Mixin 消费 `Minecraft.startUseItem` 与 `Minecraft.startAttack`，避免真实服务器交互透传。

当前已知边界：

- 虚拟创造物品栏 GUI 仍是轻量搜索列表和槽位按钮，尚未完全还原原版创造模式分类页、物品图标网格和拖拽交互。
- 空气命中暂未实现固定距离、网格、平面锁定或从最近投影面延伸。
- 第一版只覆盖普通 `BlockItem` 的单方块放置语义；门、床、高草、复杂 block entity、undo/redo 需要后续专项完善。
- Tweakeroo 的 `freeCameraPlayerInputs=false` 已做运行时提示，但还没有自动临时切换和退出恢复。
- Syncmatica 集成仍在后续范围。

## 10. 开发与验证记录

已完成/当前准备提交的 git 里程碑：

- `a8408df init: init git repo and create planning doc file`
- `da765ec build: scaffold fabric 26.2 mod`
- `3fc8718 feat: add creator draft placement core`
- `feat: add creator editing virtual inventory and dynamic tiles`
- `feat: improve creator virtual inventory gui`
- `feat: warn about incompatible tweakeroo free camera inputs`
- `feat: add creator status hud`
- `fix: use camera rotation for creator placement state`

本地构建方式：

- 当前环境无全局 `gradle`，使用工作树内忽略提交的 `.gradle-local/gradle-9.6.0/bin/gradle.bat`。
- 构建命令：`.\.gradle-local\gradle-9.6.0\bin\gradle.bat build --no-daemon --console=plain --stacktrace`
- `.gradle-local/` 与 `gradle-*.zip` 已在 `.gitignore` 中忽略，避免把本地工具缓存提交进仓库。
