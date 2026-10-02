# Litematica Creator TODO

最后更新：2026-10-02

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
- [x] #70 Creator 相机虚拟鞘翅与披风纹理遵循原版选择
- [x] #71 Creator 相机匍匐模型姿态同步
- [x] #72 单人游戏暂停时冻结 Creator 相机
- [x] #73 Creator Camera 下本体继续更新姿态与运动状态
- [x] #74 Creator Camera 下本体运动继续同步至服务端
- [x] #75 可配置是否允许在空中放置投影方块
- [x] #76 Creator 物品栏替身预览使用半透明渲染
- [x] #77 Creator 相机替身鞘翅滑翔模型与动作同步
- [x] #78 投影楼梯空缺部分不再误触发匍匐姿态
- [x] #79 保存后绑定当前内存 schematic 与全部 placements
- [x] #80 四种可配置的草稿导出 region 规范化方式
- [x] #89 Creator 原理图管理器与 Litematica 页面互通
- [x] #90 实体命中时禁止误触发空中放置
- [x] #94 MaLiLib `0.29.2/0.29.4` 与 Litematica `0.28.2/0.28.5` 双版本兼容
- [x] #95 Tweakeroo、Syncmatica、Lithium、Sodium 可选组合兼容审计
实现细节和历史验收记录见 [`docs/completed-tasks.md`](docs/completed-tasks.md)。

## 已取消

- #21 Accurate 连续放置与 backfill：游戏内效果不符合预期，已回退；放置保留 #15 的固定间隔模型，并由 #49 配置间隔。

## 状态标记

- [ ] 未开始
- [~] 进行中或已部分实现
- [!] 需要设计决策

## 开发基础设施与发布准备

以下为已选定政策的待实现部分，本次仅整理文档。详情见[开发指南](docs/development.md)与[发布指南](docs/releasing.md)。

These adopted policies still need implementation; this change only organizes documentation. See the development and release guides for the contracts.

- [ ] 集中版本配置与资源生成 / Centralize version configuration and resource generation.
- [ ] 固定云端依赖来源，接入 push/PR 构建及客户端 GameTest / Pin cloud dependencies and add build/client GameTest CI.
- [ ] 落实 SemVer 命名与 GitHub Release 工作流 / Implement version naming and the GitHub release workflow.
- [ ] 补齐 MIT LICENSE、metadata 与 JAR 许可证 / Add the MIT license, matching metadata, and JAR license packaging.

## Litematica 工具兼容

- [ ] #81 Creator 虚拟主副手中的 Litematica 工具物品生效
  - Creator 模式下，用虚拟主手和副手匹配 Litematica 的 `toolItem` 与 `toolItemComponents` 配置；不临时替换或修改真实玩家物品栏。
  - Tool HUD、选区点设置、placement 选择/移动、操作模式切换、中键和带 modifier 的滚轮操作都应识别虚拟工具。
  - 虚拟工具操作与 Creator 放置、删除、pick block 发生按键冲突时，工具语义优先且同一次输入只能执行一条路径；普通无 modifier 滚轮仍切换虚拟快捷栏。
  - 验收：默认木棍及自定义带 components 的工具在虚拟任一只手中行为与真实手持一致，真实主副手及服务端状态不变。

## 虚拟物品栏与输入

- [!] #41 后期兼容或模仿 Inventory Profiles Next / ItemScroller
  - Creator 物品栏使用本地虚拟 container；第三方模组通常针对原版 screen、`inventoryMenu` 和服务端 slot packet 工作，不能直接允许其操作真实菜单或发送同步包。
  - 先按目标版本调查两者可用的公开接口、screen/menu 识别方式和 mixin 注入点，再决定采用显式兼容适配器还是只复刻高价值行为。
  - 候选行为包括排序、同类物品移动、滚轮搬运、拖拽搬运和快捷栏补充；所有结果必须只写虚拟物品栏。
  - 验收：兼容功能不修改真实背包、不发送 container packet，并在未安装第三方模组时保持当前行为。

## Creator Camera

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

- [ ] #91 Creator Camera 下允许空挥手
  - Creator Camera 活跃且攻击射线没有可删除或可交互目标时，允许相机替身在本地播放一次原版式主手挥动；真实本体不播放动画。
  - 空挥手不执行投影编辑、不触碰真实世界、不切换 Focus，也不向服务器发送攻击或交互包。
  - GUI 打开、按键重复和长按持续删除不得额外制造异常挥手；具体节奏遵循当前 Creator 输入手势边界。

- [!] #93 Creator Camera 的声音与粒子反馈
  - 提供可独立开关的相机替身声音、投影方块声音和投影粒子效果；默认值及更细的分类在实现前结合实际编辑体验确定。
  - 覆盖替身脚步/动作等本地声音、投影放置/删除/交互声音，以及破坏粒子和其他可映射的方块粒子；均以相机或投影世界坐标播放。
  - 反馈只存在于客户端，不创建真实世界事件、不传播给服务端或其他玩家；隐藏/禁用 placement 和失败的编辑不得播放成功反馈。
  - 与 #65 的投影物理、#91 的空挥手和未来投影交互共用统一反馈入口，避免同一次事务重复播放。

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

## 原版式放置与物品使用

- [!] #82 客户端原版式交互事务层
  - 当前实现仅调用 `Block#getStateForPlacement()` 并向一个预先确定的格子写入单个 BlockState，无法承载原版 `Item#useOn`、`BlockItem#place` 和方块交互产生的多位置、副数据或实体变更。
  - 建立纯客户端事务世界视图：读取目标 placement 和事务内先前写入；真实世界只提供射线命中位置/面等操作上下文，不作为投影占用或方块生存条件。捕获方块、block entity NBT、实体和 scheduled ticks 变更，但不修改真实世界、不发送包、不消耗或损伤虚拟物品。
  - 最终写入位置必须来自原版 context，包括替换命中格、写相邻格和一次写多个格；事务全部验证成功后再原子提交到 schematic，失败时不创建草稿/subregion、不切 focus、不标 dirty。
  - 与重叠 placement、旋转/镜像、多 placements、recovery、最小 chunk rebuild 以及未来 #30 undo transaction 保持一致。

- [ ] #83 完整 BlockItem 放置语义
  - 多格放置：门、床、大型花丛及其他一次放置多个 BlockState 的方块必须完整写入；事务中任一目标格已有不可替换的非空气投影时整次放置失败，不能只放一半或静默顶掉已有设计。
  - 依附与生存条件：梯子、藤蔓、发光地衣、幽匿脉络、按钮、灯笼、滴水石锥、硫磺尖锥、竹子、海草、海带、拉杆、钟等忽略真实/投影支撑与生存判定，依据点击面、相机朝向和物品类型生成可保存的目标状态。
  - 原地合并与累加：半砖合成双半砖；蜡烛、花簇、海龟蛋及其他可在同一格重复放置的方块更新命中格，而不是把结果错误写到相邻格。
  - 原地合并属于对命中状态的明确变换，可以替换该格；除此之外所有写入都先按目标 placement 的投影状态统一做 replaceable/占用检查，真实世界方块不阻止投影写入。
  - 保留水含状态、朝向、连接状态和 placement context 产生的其他属性；以代表性方块矩阵测试，而不是维护按方块硬编码的例外列表。

- [ ] #84 投影实体放置
  - 支持盔甲架、展示框、画、刷怪蛋等通过物品生成实体的 Creator 操作，将实体类型、位置、朝向、变体和必要 NBT 写入 schematic entity 数据。
  - 画等具有尺寸和附着面的实体要执行原版式空间校验；实体创建失败时整个事务回滚。
  - 后续补齐投影实体的命中、删除、pick 和 NBT 编辑；全程不在真实客户端世界注册实体或向服务器发包。

- [!] #85 桶与投影流体
  - 支持水桶、岩浆桶及其他标准流体容器放置静态投影流体源，并持久化对应 BlockState/FluidState。
  - 对支持含水的投影方块优先修改 `WATERLOGGED`/对应含流体状态，而不是用流体方块覆盖原方块；空桶操作需要对称地清除含水状态。
  - 需要确定空桶清除投影流体源与细雪时的虚拟物品结果，以及不模拟流动情况下是否支持 pickup；该决策不能影响真实流体。
  - #85 基础范围只提交静态结果，不传播流体、不执行邻居更新；focus 中的流动、scheduled fluid ticks 和方块更新统一交给 #88 的可选模拟运行时，Creator Camera 流体物理由 #65 跟踪。

- [!] #86 通用物品使用与持久化结果
  - 在 #82 上接入会产生可持久化 schematic 结果的非 BlockItem 使用，例如末影之眼写入末地传送门框架，以及后续蜡化/除蜡、点燃、施肥等明确可映射为 BlockState/NBT 的行为。
  - 区分“对投影方块使用物品”“空手使用投影方块”和“只影响玩家或真实实体、无法写入 schematic”的物品；后者不能假装成功或向服务器发送操作。
  - 与 #22 的状态/NBT 编辑和 #29 的虚拟方块交互共用同一事务层，逐类增加能力测试，不按一长串物品 ID 写特判。

## 投影运行时模拟

- [!] #88 Focus 投影的隔离局部世界模拟
  - 普通 Creator 编辑只提交玩家明确产生的状态变更，不自动运行流体、红石、侦测器、邻居更新、scheduled ticks、random ticks 或 block entity tick。
  - 若要支持流体扩散、开门触发侦测器、红石信号传播等行为，需要为 focused schematic 建立独立、有限边界且不接入真实 `ClientLevel` 的模拟运行时；这不是给 #29/#85 增加一个布尔开关即可安全完成的功能。
  - 需要先确定模拟边界、tick 速率、暂停/单步、流体/红石/方块实体等子系统开关、未显式草稿位置的 AIR 语义，以及模拟结果如何分组进入 recovery 和未来 #30 undo history。
  - 运行时应在 schematic 本地坐标中维护权威状态，再通过 focus placement 映射交互和渲染；多个 placements 继续共享同一 schematic 结果。
  - 默认关闭；关闭或退出时不得影响真实世界、服务端、真实实体或玩家物品栏。是否能可靠复用原版服务端逻辑需先做独立技术验证。

## 方块状态、NBT 与交互

- [ ] #22 投影方块状态/NBT 编辑
  - 支持朝向、半砖、楼梯、含水等常见 BlockState 属性循环切换。
  - 为容器、告示牌、命令方块等 block entity 提供专用 NBT 编辑。
  - 将 block entity NBT 持久化到 schematic tile-entity 数据。
  - 验收：常见 BlockState 属性和 NBT 保存重载后保持一致。

- [ ] #87 Creator 虚拟调试棒编辑投影状态
  - Creator 虚拟主手或副手持有原版调试棒时，对投影方块复用原版的属性选择、属性值循环和潜行反向循环语义，并绕过真实玩家的权限/创造模式限制。
  - 调试棒输入优先于 Creator 删除、放置和 pick block；一次按键只能执行一次调试棒操作，不触碰被投影覆盖的真实方块。
  - 当前选中的属性继续保存在虚拟调试棒 ItemStack 的 components 中，并随 Creator 虚拟物品栏配置持久化。
  - 状态变更通过 Creator 事务、focus、metadata、recovery 和最小 chunk rebuild 提交；重叠 placement 仍先进入 Focus Switcher。
  - 验收：虚拟任一只手中的调试棒可以稳定修改常见 BlockState，重开物品栏和重启客户端后保留所选属性，真实世界与服务器不变化。

- [ ] #29 与投影方块交互
  - 区分空手/持物右键交互和相邻放置；门、活板门、拉杆、按钮等原版交互应修改对应投影状态。
  - 对可交互投影方块打开虚拟 UI 或状态/NBT 编辑器。
  - 不向服务器发送交互。
  - 箱子等容器使用虚拟 block entity inventory，告示牌等进入专用编辑；末影之眼等持物交互由 #86 处理。
  - 基础 #29 只提交直接结果及同一多方块结构的必要一致性变更，例如同步门的上下半部；不会触发邻近侦测器、红石传播或其他世界更新。动态传播统一属于 #88。
  - 验收：右键常见状态方块、容器和告示牌时进入正确 Creator 编辑流程，结果保存重载后保持一致。

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

## 客户端命令

- [!] #92 Creator 客户端侧命令
  - 注册带参数补全和错误提示的纯客户端 `/creator` 命令树；命令必须在发送聊天包前被客户端消费，不能要求服务端安装模组或获得权限。
  - 默认编辑目标为 Creator Focus，并允许通过明确参数选择其他 schematic/placement；没有有效目标时拒绝会修改投影的命令，不隐式新建或切换 Focus。
  - 初始命令范围：`/creator tp` 传送相机替身，`fill` 填充投影方块，`setblock` 放置投影方块，`effect` 为替身或投影实体设置药水效果，`clear` 清空虚拟生存物品栏，`give` 向虚拟物品栏添加物品，`summon` 创建投影实体。
  - 需要先统一世界坐标与 schematic 本地坐标、相对坐标、placement 旋转/镜像、方块状态/NBT、物品 components、实体选择和命令返回值语义。
  - 修改投影的命令复用 #82 事务层、占用检查、metadata、recovery、最小区块刷新及未来 #30 undo；物品栏命令只修改 Creator 虚拟库存，相机命令只移动替身。
  - 验收：命令补全和参数错误清晰；所有成功操作均可保存/恢复且不修改真实世界、真实玩家背包或服务端状态。
