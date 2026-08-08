# Litematica Creator 已完成工作记录

最后更新：2026-08-08

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
- #35 已将清理规则扩展到任意经 Creator 删除后彻底为空的 subregion。
- Creator 的完成编辑/卸载命令已与 Litematica 原生导出和 selected-placement 行为分离。
- 单元测试覆盖坐标旋转/镜像往返、候选身份合并、目标决策、放置占用策略、通用 region 空状态和 focus 通知决策。
- `docs/creator-design-and-roadmap.md` 已同步当前 focus、稀疏 subregion 和生命周期模型。
- 本项完成的是稳定、语义正确的稀疏编辑表示；导出时把相邻 Creator cells 压缩为无空洞 cuboids 的后续工作单独由 #80 跟踪。

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

## Focus 切换提示（#34）

- Focus 设置、重叠选择、清空和生命周期恢复统一经过按 placement 对象身份比较的状态变更入口。
- 只有 placement 对象真正变化时才更新 `CreatorFocus`、通知 recovery 并显示消息；连续操作仍归属同一 focus 时保持静默。
- 普通切换显示 placement 与 schematic 名称，主动清空显示单独提示。
- 新建草稿合并为一条“已新建并聚焦”消息；结束编辑和丢弃只保留各自的结果消息。
- 世界切换清理和 recovery 恢复 focus 使用显式静默入口，因此不会与 #18 的恢复汇总重复。
- 从 Litematica 移除当前 focus placement 时会清空 focus；重叠选择后的临时可见性抑制行为保持不变。

## 空 Subregion 清理（#35）

- Creator 删除方块时先写入空气并只更新一次 schematic 总方块数；只有这次删除可能使 region 变空时才执行扫描。
- 容器扫描逐格检查真实 block state 并在首个非空气方块处短路，不使用 Litematica 普通 `set()` 不会维护的 `blockCounts`。
- 只有 block states、block entities/NBT、entities、scheduled block ticks 和 scheduled fluid ticks 全部为空时才删除 region；任一附属数据存在都会保留。
- Region 会从 schematic 的全部数据映射及同一 schematic 的所有 placements 中移除。
- Litematica placement pre/post change 流程负责旧、新 touched chunks、渲染缓存和 Creator placement index；最后一个 region 被删除时也会清空旧 enclosing box。
- 几何 metadata、region count、volume、enclosing size、修改时间和 recovery dirty 调度会同步更新。
- Creator 模式会阻止 Litematica Rebuild 抢先处理同一次左右键；即使其他编辑入口已经先把目标写成空气，Creator 删除仍会检查并清理彻底为空的 region，且不会重复扣减方块数。

## 投影放置占用预检（#36）

- Litematica 通用射线已经使用方块的实际 `VoxelShape`；Creator 保持“命中方块相邻格”的落点语义，并在最终目标格增加独立占用检查。
- 目标解析现在先产生无副作用 resolution；从最终候选 placement 的 region container 直接读取并变换世界朝向状态，避免组合 schematic world 或其他 placement 干扰。
- 使用虚拟物品、camera 朝向和 schematic world 构造目标 `BlockPlaceContext`，按原版 `BlockState.canBeReplaced()` 语义允许空气或可替换投影。
- 放置状态无效或目标不可替换时，不会切换 focus、创建草稿/subregion、标记 dirty 或调度 recovery；占用阻断保持静默。
- 只有预检成功后才提交 focus、新草稿和 schematic 写入；重叠 placement 仍先打开 Focus Switcher，不执行本次放置。

## 投影编辑原子提交（#37 第一阶段）

- Creator 的结构编辑会先保存各 placement 的旧 touched chunks，再完整修改 schematic container、附属数据、subregion placement 和 metadata；所有对象一致后才逐个发布 placement change。
- 发布每个 placement change 前，通过最小 accessor 把对应的旧区块快照交给 Litematica pre/post 流程；新增或删除 subregion 使用 old/new touched chunks 并集完成卸载与重建。
- 普通方块编辑不再调用全量 `rebuildAllPlacements()`；同一 container 坐标会按每个 placement 和 subregion 的 origin、rotation、mirror 映射到世界坐标，只刷新实际受影响的区块。
- 全局公平读写锁把 `PlacementManagerTaskRebuild.run()` 包装为读事务，把 Creator schematic 编辑包装为写事务。编辑会等待旧状态读取结束，后续重建只能看到完整提交后的状态；异常路径均保证释放锁。
- `debugLogging` 会记录事务编号、锁等待时间、编辑坐标、region 删除状态、placement hash、old/new touched chunks 和最终刷新区块。
- 单元测试覆盖并行读、读写互斥、异常释放、全部 placement/subregion 旋转镜像组合的坐标往返、普通编辑最小区块刷新及结构编辑 old/new 区块合并去重。
- 该阶段曾显著降低 #37 的出现频率，但后续实测确认没有根治；最终修复由下节的 schematic world 与渲染快照同步完成。

## Schematic world 与渲染快照同步（#37、#61）

- 源码审计确认 Litematica 后台 `PlacementManagerTaskRebuild` 会依次卸载旧 chunk、装入空 chunk、构造并替换完整 chunk；渲染任务则可能在中间窗口创建 `ChunkCacheSchematic`，把空 chunk 引用保留到稍后的网格编译。
- Creator 为 schematic game chunk 使用公平的分片读写锁：同一区块的 placement rebuild 使用写锁并彼此串行，渲染编译使用读锁；不同区块仍可并行。
- 渲染任务取得稳定读锁后，会在真正编译前重新执行 `rebuildWorldView()`，避免继续使用任务入队时捕获的旧或中间态 chunk 引用；编译和 GPU 上传完成后才释放读锁。
- 旧的“抑制 placement 全量刷新并缩小结构刷新范围”方案已回退，避免改变 Litematica 原生 placement change 语义。
- 并发单元测试覆盖同区块 rebuild/rebuild、rebuild/render 双向互斥、不同区块并行和异常释放；Fabric 26.2 开发客户端已验证全部新增 Mixin 能正常应用。
- 2026-08-07，测试机经过连续数小时高频放置与删除测试后未再复现整块或整体投影消失，#37、#61 正式关闭。

## 虚拟创造物品栏（#8）

- 虚拟物品栏配置升级为 v2，使用 `ItemStack.CODEC` 保存数量和完整 data components；旧 item-id 配置会迁移为对应物品的最大堆叠数。
- GUI 使用独立的客户端 `AbstractContainerScreen`、menu 和 container，复用 26.2 原版创造栏的尺寸、背景、tab、滚动条、搜索框、槽位布局和 tooltip，但不会替换 `player.containerMenu` 或调用网络 game mode。
- 原版和已注册创造分类均由当前 feature flags 重建，OP 分类固定按有权限状态生成；搜索页合并创造栏变体与所有已启用注册物品，不再局限于 `BlockItem`。
- 搜索支持本地化名称、注册 ID、tooltip 与 `#tag`；原版保存快捷栏只读加载，Creator 不过滤或覆写其中物品。
- Inventory 页提供虚拟 9 格快捷栏、27 格主栏、副手、四个受装备位限制的盔甲槽和丢弃槽，不提供合成栏。
- 左右键拿取/放入/拆分/交换、拖拽、数字键、shift-click、clone 和本地丢弃均只修改虚拟状态；中键 clone 使用最大堆叠数，投影放置不消耗数量。
- 每次 GUI 事务只保存一次，配置先写临时文件再原子替换；损坏栏位单独跳过，丢弃槽不持久化。

## 虚拟物品栏小修复（#38、#39、#40、#43）

- Creator pick block 按 `ItemStack.isSameItemSameComponents()` 搜索虚拟快捷栏和 27 格主物品栏：快捷栏命中只切槽，主物品栏命中按原版 suitable-hotbar 规则交换，完全不存在时才新建数量为 1 的 stack。
- Suitable-hotbar 从当前槽开始循环，依次选择空槽、未附魔槽和当前槽；覆盖前会优先把原 stack 移到空虚拟主物品栏槽。整个 pick 使用单个虚拟库存 transaction，不修改真实背包或发送 pick item 包。
- Palette 普通点击复制条目的显示数量；同种 carried 左键增加 1、右键减少 1，不同种 carried 左键清空、右键减少 1。所有计算都基于副本，不修改创造分类共享 stack。
- Palette Shift+单击按原版规则把最大堆叠复制到 carried，不直接修改虚拟快捷栏；同种 carried 左键补到最大，其他左右键分支继续使用原版清空或减一规则。
- `openCreatorInventoryWithInventoryKey` 默认开启。Creator 模式会在原版处理前消费当前物品栏键并打开虚拟物品栏；关闭设置后保留真实物品栏行为，独立 Creator 物品栏热键不受影响。
- Creator 物品栏在非搜索标签页使用当前原版物品栏键关闭；搜索框聚焦时该键作为普通搜索字符输入，`Escape` 仍可关闭界面。
- 新默认键位为 `Y`、`M,E`、`M,K`、`M,LEFT_SHIFT,S`、`M,LEFT_SHIFT,D`、`M,F` 和 `M,N`；只影响全新配置和重置默认值，不迁移或覆盖已有玩家绑定。
- 单元测试覆盖 pick 选槽、components 匹配、全满覆盖、palette 左右键数量边界、Shift 快移和配置默认值。

## 虚拟物品栏回归修复（#44、#45、#46、#47、#48）

- #44 删除了 Creator GUI 对物品栏键的提前消费；输入交由 26.2 原版创造栏式搜索分支处理，因此默认 `E` 可进入搜索文本，非搜索标签页仍由基础界面关闭。
- #45 与原版一致，将当前创造标签页保存在静态 GUI 状态中；关闭并重新打开时保留标签页，注册表重建令标签失效时才回退到默认标签。
- #46 投影放置按原版顺序解析虚拟双手：虚拟主手是 `BlockItem` 时优先使用，否则回退到虚拟副手；选定的手同时用于 `BlockPlaceContext`、replaceable 检查和本地挥手动画。
- #47 移除了 Shift+单击写入第一个空快捷栏的自定义行为，改为原版 carried 最大堆叠逻辑，且继续基于 stack 副本计算，不修改 palette 共享条目。
- #48 Creator 模式且没有 GUI 时会在原版处理前消费当前换手键，原子交换虚拟当前快捷栏槽与虚拟副手；真实背包不会变化，也不会进入原版换手发包路径。
- 换手只响应首次按下，键盘重复事件不会来回交换；包含同一按键的已激活 Creator 热键优先，因此默认 `M+F` 只打开 Focus Switcher。
- 单元测试覆盖虚拟主手优先、副手回退、双手均不可放置、虚拟换手、重复按键与组合热键冲突，以及 palette Shift 点击的最大堆叠、同种补满和其他移除分支。

## 虚拟快捷栏与玩家渲染（#9、#24）

- Creator 模式下，原版 HUD 快捷栏读取虚拟九格、虚拟 selected slot 和虚拟副手，物品名称提示也跟随虚拟选中物品；真实 selected slot 和真实背包保持不变。
- 第一人称 `ItemInHandRenderer` 在 tick 和双手选择阶段读取稳定的虚拟主副手快照，因此保留原版换物品过渡动画且不会因配置副本每 tick 抖动。
- 第三人称只替换本地玩家 `AvatarRenderState` 中的主副手、左右主手映射、手臂姿势和四件盔甲；其他玩家始终使用真实同步状态。
- 虚拟盔甲仅进入渲染快照，不修改真实装备、属性、护甲值、耐久、HUD 护甲条或服务器状态；退出 Creator 模式后所有读取自动恢复真实物品。

## Creator 目标轮廓与编辑距离（#12、#23）

- `creatorEditRange` 为统一的 Creator 目标距离配置，默认 10 格、允许 1–128 格；放置、删除、pick block 和投影选中框均通过同一 `CreatorTargeting` 使用 Creator camera 与 Litematica 通用射线。
- Minecraft 26.2 的 `LevelExtractor.extractBlockOutline()` 完成原版轮廓提取后，Creator 只在最近命中为有效、启用且已索引的投影方块时替换 `BlockOutlineRenderState`，因此后方真实方块不会再显示穿透轮廓。
- 投影轮廓使用 schematic world 中状态的实际 `VoxelShape`、模型半透明标志、高对比度选项以及 debug collision/occlusion/interaction shapes；台阶、栅栏等非完整方块保持原版形状。
- Creator 关闭、GUI 打开、真实方块更近、placement 隐藏或禁用、投影状态为空时均保留原版轮廓行为；重叠 placement 的交互选择逻辑仍由 Focus Switcher 负责。
- 单元测试覆盖编辑距离默认值与边界，以及 Creator 世界视图和有效投影目标的轮廓启用条件。

## 游戏语言跟随（#19）

- 独立翻译模式和 Creator 语言选择已移除；Creator 不再注册 MaLiLib 翻译覆盖 manager，所有文本直接由 Minecraft 从 `assets/litematica-creator/lang/` 按当前游戏语言解析。
- 旧配置中的 `translationLanguage` 和 `translationMode` 会被忽略，并在下一次保存 Creator 配置时自然移除。

## 本地编辑动作反馈（#25）

- 只有 `CreatorSchematicEditor.setBlockState()` 确认实际修改成功后才挥手；放置使用本次解析到的虚拟主手或副手，删除使用主手。
- 被占用、无目标、重叠待选择、重复状态和其他失败路径不播放动画；中键 pick block 按原版行为不挥手。
- 两参数非广播 swing 只更新本地动画，不调用 `LocalPlayer.swing(hand)` 的发包路径；投影交互动画留待 #29。
- 单元测试覆盖配置迁移与完整 ItemStack 往返、搜索合并和文本匹配、左右主手及盔甲位映射，以及成功/失败编辑的反馈决策。

## 长按持续拆除（#21）

- 本批最终只保留按住攻击键持续拆除投影方块；游戏内效果不符合预期的 Accurate 连续放置、目标历史、backfill 和模式配置均已回退。
- 投影放置继续使用 #15 的固定间隔模型；#49 后续只把该间隔开放为配置，不重新引入 Accurate 行为。
- Creator 的删除输入由 client tick 手势控制器执行；原始键鼠事件只锁存 fresh press 和释放状态，Minecraft 的攻击路径在 Creator 模式下只负责阻断原版行为。
- Fresh press 会立即删除当前命中的投影；长按时按 `continuousBreakIntervalTicks` 重新射线，配置默认 4、范围 1–20，并只处理相较上次采样发生变化的目标。
- 删除当前目标后，下一次采样可以继续命中并删除后方投影；同一未变化目标不会被反复处理，无目标保持静默。
- 重叠 placement 会打开 Focus Switcher、终止当前拆除手势并要求松键后重新开始。
- 打开 GUI、退出 Creator、切换世界或失去玩家/世界会清空手势；同一 tick 内快速按下后立即释放仍会保留一次删除，键盘重复 press 不会制造额外操作。
- 每次实际删除继续更新 focus、metadata、recovery 和最小区块刷新，并播放本地主手动画；真实攻击路径仍被阻断且不发送相关服务端包。
- 单元测试覆盖 1/4/20 tick 配置、立即删除、目标推进、相同目标抑制、无目标后重新命中、快速点按、键盘重复、释放停止和中断后重新武装。

## 固定间隔持续放置（#49）

- 新增 `continuousPlaceIntervalTicks`，默认 4、范围 1–20，并在设置页中紧邻且位于 `continuousBreakIntervalTicks` 上方。
- 配置只替换 #15 原有的硬编码 4 tick 长按间隔；fresh press 仍立即执行，后续重复仍按固定 tick 调度。
- 不包含 Accurate 模式、目标历史、鼠标/相机移动判定或 backfill，也不会恢复已取消的 #21 方案。
- 切换 GUI、世界或 Creator 模式时继续清理输入；真实放置路径和相关服务端数据包仍被阻断。

## 固定距离空中放置（#26）

- 统一 Creator 射线未命中真实或投影方块时，会按 camera 眼睛位置和视线方向生成固定距离空中目标；真实/投影命中始终优先使用原有相邻面放置规则。
- `airPlacementDistance` 默认 5、范围 1–128，实际距离会被 `creatorEditRange` 钳制，避免空中目标超出统一编辑范围。
- 目标格取归一化视线在固定距离处所在的方块；合成命中面使用视线主轴的反方向，并强制 `BlockPlaceContext` 在目标格本身计算状态，因此朝向、柱体轴向、台阶/半砖点击面等仍基于 camera。
- 空中目标继续走既有 placement 范围、Creator focus、重叠选择、占用预检、稀疏 subregion、metadata、recovery 和最小区块刷新流程。
- 没有 focus 且目标不属于任何 placement 时创建新草稿；已有 focus 时可直接扩展。首块出现后，持续放置会优先命中新投影并按相邻面继续延伸。
- 空中放置只修改客户端 schematic，保持本地挥手反馈，不放置真实方块也不发送放置包。
- 单元测试覆盖水平/垂直视线、非单位视线归一化、命中面方向、目标格中心和空中距离受编辑范围约束；平面锁定、网格锁定等高级辅助不属于本阶段。

## Creator Camera（#20）

- Creator 模式开启时会从当前 camera entity 的位置和朝向创建未注册到世界实体列表的纯客户端 `CreatorCameraEntity`；从真实玩家接管时默认地面模式，从已有独立相机接管时默认飞行。
- 地面模式复用 `LocalPlayer` 的原版输入与移动物理，包括重力、跳跃、潜行、疾跑、台阶、液体和梯子；双击空格切换飞行，飞行时启用 `noPhysics` 并可穿过方块。地面与飞行速度倍率均可在 `0.1–5.0` 间即时调整，默认 `1.0`。
- 相机不执行会发送移动状态的 `LocalPlayer.tick()`，并屏蔽 abilities、骑乘和鞘翅等服务端发包入口。真实本地玩家继续作为网络受控玩家同步本体自身的位置、落地和碰撞状态；服务端位置校正仍只写回真实玩家。
- 地面碰撞为真实世界碰撞与当前可见投影 `VoxelShape` 的并集；解析遵循 Litematica placement/subregion 启用及渲染状态、合成顺序、空气覆盖、客户端区块可用性和 `loadEntireSchematics`。投影只贡献形状，不模拟投影液体、梯子、摩擦或弹跳。
- Creator 相机跨区块时补充刷新新进入视野边缘的客户端区块；渲染器注入为可选，允许 Sodium 等渲染器替换原版路径。
- 进入时保存原 camera entity、`smartCull` 及 Tweakeroo Free Camera 配置；Tweakeroo 存在时临时协调为 `freeCameraPlayerMovement=true`、`freeCameraPlayerInputs=false`，Creator 退出后精确恢复原值与原相机。
- 退出 Creator、世界卸载、断线、客户端关闭或初始化失败时按相反顺序恢复状态；本地玩家死亡/重生导致实例替换时自动关闭 Creator，但保留 focus 和 recovery cache。相机位置和飞行状态不跨 Creator 会话持久化。
- Creator HUD 显示地面/飞行状态。单元测试覆盖速度边界、会话恢复、双击飞行、玩家隔离、Tweakeroo 快照、投影可见性合成和跨区块刷新；开发客户端已启动到主菜单验证所有相机 Mixin 可正常应用。

## Creator Camera 后续修复（#50–#56）

- #50 真实本体只隔离主动输入和鼠标转向，不再清零速度或取消 `move()`；本体继续执行原版玩家 tick，保留进入相机前的惯性、重力、碰撞和服务端位置校正。26.2 默认会跳过非当前 camera 的 `LocalPlayer`，因此 Creator 在实体提取末尾补入真实本体 render state。
- #51 未注册的 Creator 相机实体也会被显式提取为玩家 render state；它使用原版“实体隐身但对当前玩家可见”的半透明 RenderType，同时强制完整玩家模型并移除名字、阴影、火焰和 outline。第一人称和第三人称均补入该替身，真实本体保持正常不透明渲染。
- 虚拟手持与装备跟随半透明相机替身；相机活动时真实本体恢复真实装备渲染，相机关闭但 Creator 模式仍开启时继续沿用此前的本地玩家虚拟装备行为。按实体 ID 去重可避免 Tweakeroo 同时补入本体时重复渲染。
- #52 新增默认 `M+B` 的独立相机热键，以及默认开启的“进入 Creator 模式时开启相机”和“退出 Creator 模式时关闭相机”设置。手动关闭相机后不会被 client tick 自动重开；关闭退出联动后，相机可在 Creator 模式结束后继续活动。世界卸载、断线和客户端关闭仍无条件清理相机会话。
- #53 相机只继承来源位置和朝向，始终以站立姿态、独立碰撞箱、零初速度和非落地状态创建，不再继承本体鞘翅、游泳、爬行、潜行或疾跑状态。
- #54 通过最小 `LocalPlayer` accessor 每 tick 同步原版 `autoJump` 选项，关闭自动跳跃时 Creator 相机不再自行开启该能力。
- #55 `creatorCameraProjectionCollision` 默认开启；关闭后地面模式只与真实世界碰撞，飞行模式继续忽略全部碰撞。
- #56 `ignoreCreatorCameraEntityPlacementCollision` 默认关闭。默认 placement preflight 使用最终投影状态在 schematic world 中计算原版碰撞形状，以真实客户端世界检查本体和其他阻挡放置的实体，并额外检查未注册的相机替身；开启后跳过这层实体占位检查。
- 单元测试覆盖相机模式联动、默认配置、双 render state 去重决策和实体放置碰撞决策；开发客户端已完成 Fabric/Mixin 初始化及资源加载验证。

## Creator Camera 紧急崩溃修复（#57）

- Minecraft 26.2 的 `Entity.getId()` 会在实体尚未分配 ID 时抛出 `IllegalStateException`。Creator 相机刻意不注册到 `ClientLevel` 实体列表，因此 #51 新增的替身 render-state 提取在进入 Creator 模式后的首个渲染帧触发了该检查。
- `CreatorCameraEntity` 现在于构造时显式取得固定的非零负数客户端 ID。该 ID 只满足渲染状态提取和去重，不把相机加入世界实体索引，也不改变服务端同步、碰撞或生命周期语义。
- 回归测试锁定客户端相机 ID 必须非零且位于服务端正常分配范围之外；完整测试、构建及开发客户端 Fabric/Mixin 初始化均已通过。

## Creator Camera 替身渲染修复（#58–#60）

- #58 世界中的半透明 Creator 相机替身只在第三人称提交；第一人称不再渲染相机位置上的完整身体，继续使用原版 `ItemInHandRenderer`。手持物品保持正常渲染，裸露的第一人称手臂使用与世界替身一致的半透明 tint。
- 第一人称手持渲染的玩家参数在相机活动时替换为 `CreatorCameraEntity`，因此视角、主手侧和挥手进度均来自当前受控相机，而虚拟主副手物品仍沿用 Creator loadout。
- #59 成功放置或删除投影后的本地 `swing()` 改为优先作用于 Creator 相机；相机自己的轻量 tick 显式推进原版挥手时间。真实本体不再同步播放 Creator 编辑动画，相机替身和第一人称手持仍正常播放。
- #60 相机实体动态代理真实本地玩家的主手侧与 `PlayerModelPart` 可见性，因此头部、夹克、左右袖和裤腿第二层会随真实玩家设置完整渲染，不再使用新建 `LocalPlayer` 的默认关闭状态。
- 单元测试覆盖第一人称世界替身抑制、第三人称去重和反馈目标选择；完整构建及开发客户端 Mixin 类加载验证通过，最终视觉效果留待游戏内验收。

## Creator Camera 物理兼容修复（#62–#64）

- #62 `CreatorCameraEntity` 不再执行 `LivingEntity.pushEntities()`。相机替身仍可按正常移动逻辑检测方块碰撞，但不会主动给真实本体或其他世界实体施加推力；它本身未注册到世界实体列表，真实实体也不会反向把它作为可推动目标。
- #63 相机的轻量 tick 会在 `aiStep()` 后显式执行原版 `updatePlayerPose()`，补回跳过 `Player.tick()` 后缺失的姿态刷新。地面模式潜行会切换为独立的蹲姿和 1.5 格碰撞箱，飞行模式继续按原版创造模式把潜行键用于下降而保持站姿。
- #64 移除了依赖 `CollisionGetter.getBlockCollisions()` 返回值追加形状的旧 Mixin。Lithium 的移动优化直接扫描区块方块并绕过该接口，因此旧路径无法参与相机移动碰撞。
- 新的 `EntityCollisionMixin` 只在实体为 Creator Camera 且本次扫掠范围确实包含投影碰撞形状时，于 `Entity.collide()` 入口接管求解；其他实体、飞行模式、关闭投影碰撞或附近没有投影时继续走原版或 Lithium 快路径。
- 相机专用求解器保持原版的实体、世界边界、真实方块、投影形状合并顺序，并复用轴向裁剪、落地判定和跨台阶候选高度规则；实现不链接 Lithium 私有类，也不要求安装 Lithium。
- 单元测试覆盖无形状快路径、投影形状轴向裁剪以及跨台阶候选高度去重排序；Fabric 开发客户端已在同时加载 Lithium `0.25.3+mc26.2` 时完成 Mixin、资源和主菜单初始化，实际投影碰撞手感留待游戏内验收。

## Creator Camera 运动与替身状态修复（#68–#74）

- #68 覆写 Creator Camera 的原版潜行防坠落计算，用真实世界碰撞与当前可见投影 `VoxelShape` 的并集判断脚下支撑；站在投影上潜行时不再把全部水平位移削为零，走到投影边缘时仍保留原版式防坠落。姿态空间检查也使用相同投影支撑语义。
- #69 Creator 虚拟物品栏的玩家预览在相机活动时改为提取 `CreatorCameraEntity`，因此预览跟随替身姿态、虚拟手持和虚拟装备；相机关闭时继续显示真实玩家。
- #70 相机保留 `tryToStartFallFlying()` 的网络发包阻断，改由本地 fresh-jump 状态机直接启动虚拟鞘翅滑翔；落地、移除可滑翔装备或切入创造飞行时停止。该流程不发 `START_FALL_FLYING` 包，也不消耗真实或虚拟物品耐久。
- #71 手工相机 tick 补齐 `ClientAvatarState.tick()` 和原版私有 `updateSwimAmount()`；匍匐/游泳模型会随独立碰撞箱平滑转为水平姿态，替身披风与鞘翅动画也取得连续的运动状态。
- #72 `CreatorCameraController` 在单人游戏真正暂停时不推进相机物理；多人游戏打开菜单但世界未暂停时仍按原版继续运动。
- #73、#74 真实玩家继续使用空输入隔离主动移动和转向，但恢复 `isControlledCamera=true`。原版 `LocalPlayer.tick()` 因而会把本体自己的重力、惯性、击退、推动、流体和挤压结果发送给服务端，怪物追踪、虚空伤害、落地及鞘翅停止均以实际本体位置处理；未注册相机仍不执行网络玩家 tick，绝不会发送相机坐标。
- 单元测试覆盖投影支撑下的潜行位移、边缘回退、虚拟鞘翅启动/停止条件和既有相机生命周期；本批完成后共有 125 项测试通过。

## Creator Camera 放置、预览与姿态修复（#70、#75–#78）

- #75 新增默认开启的 `enableAirPlacement`。关闭后，无论是否启用 Creator Camera，射线未命中真实方块或可见投影时都不会再生成固定距离目标；命中方块后的相邻放置不受影响，原有距离配置继续只控制启用时的固定距离。
- #76 Creator 物品栏仍复用原版 `InventoryScreen` 玩家预览，但在其私有 render-state 提取返回处仅对 `CreatorCameraEntity` 应用与世界替身相同的隐身半透明状态。真实玩家物品栏预览及其他实体预览不受影响。
- #70、#77 在 `AvatarRenderer` 完成原版状态提取和虚拟装备替换后，显式从相机替身同步当前 pose、滑翔标志、滑翔时长、游泳混合值和鞘翅动画角度；本地滑翔物理和不发包边界保持不变。最终纹理选择见后续完成记录。
- #78 投影碰撞候选不再以整个 `VoxelShape` 的外接盒判断相交，而是对相机查询盒与实际体素形状执行布尔求交。倒放楼梯空缺的半格、其他凹形或多盒形状现在与真实方块使用一致的姿态空间语义，同时保留 Lithium 兼容碰撞入口。
- 回归测试覆盖空中放置默认值、倒放楼梯实际形状的占用与空缺区域、滑翔姿态兜底和虚拟鞘翅披风纹理决策；本批完成后共有 129 项测试通过。开发客户端在未安装和安装 Lithium `0.25.3` 两种情况下均通过 Fabric/Mixin 初始化，物品栏预览注入点成功应用。

> 2026-08-07 复测更正：上述 #70、#77 的 render-state 补丁只改变了鞘翅纹理选择，没有补回 Creator 轻量 tick 跳过的原版滑翔动画生命周期，因此不能视为完成。后续修复改为每 tick 推进 `fallFlyTicks` 和 `ElytraAnimationState`；最终状态以 `todo.md` 和新的游戏内验收为准。

> 2026-08-08 复测：鞘翅模型、替身模型和滑翔动作已正常；此前为避免固定矩形披风而关闭 `showCape` 的处理同时阻止了 `WingsLayer` 使用玩家披风纹理，因此 #70 仍保留该项纹理修复。

## Creator 鞘翅纹理与导出 metadata（#70、#77、#79）

- #70、#77 保留原版从真实玩家提取的 `AvatarRenderState.showCape`，不再因虚拟胸甲为鞘翅而强制关闭。原版 `WingsLayer` 因而继续按“专用 Elytra 纹理、玩家披风纹理、默认鞘翅纹理”的顺序选择纹理；`CapeLayer` 会在胸甲为 wings 时自行跳过矩形披风。
- #79 新增纯 NBT 的 `CreatorSchematicExportNormalizer`，并在 `LitematicaSchematic.writeToFile(Path, String, boolean, boolean)` 最终压缩写盘处统一接入 v7 与 downgrade v6 两条分支。
- 只有 metadata name 严格匹配 `creator-draft-yyyyMMdd-HHmmss` 的导出副本会被规范化：内部名称改为实际写盘文件名的 stem，有效创建时间保持不变，旧草稿缺失或非正创建时间则从占位名称按系统时区恢复，无效日期回退到本次导出时间。
- 普通 schematic 的 Save As、`timeModified`、author、description、counts 和 regions 均不改变；当前内存 schematic、placement、dirty 状态及 recovery cache 也不经过该导出入口。这个入口后续由 #80 扩展 region 压缩。
- 单元测试覆盖扩展名、Unicode 名称、创建时间恢复与回退、普通 schematic no-op、无关 NBT 保持及输入 NBT 不被原地修改；完整 Gradle 测试套件通过。

## Recovery cache 实例迁移修复（#18）

- Placement JSON 由 Litematica 序列化时会冗余保存 recovery `.litematic` 的绝对路径。复制或重命名 Minecraft 实例后，该字段仍指向旧实例目录，旧恢复逻辑因而会把完整有效的 cache 误判为路径无效。
- 恢复时继续严格验证 manifest 的 entry ID、generation 和 `cache_file`，但不再要求 placement JSON 内的旧绝对路径等于当前路径。通过验证后，会在内存副本中将其重新绑定到当前 recovery 文件，再交给 `SchematicPlacement.fromJson()`；磁盘上的 manifest、cache 和原始 schematic 均不修改。
- 结构验证仍要求 placement JSON 具有非空的 `schematic` 字段和数组形式的 `placements`，损坏数据继续按失败路径保留。回归测试覆盖实例目录迁移、外部旧路径重绑定、源 JSON 不变和畸形字段拒绝；本批完成后共有 132 项测试通过。
