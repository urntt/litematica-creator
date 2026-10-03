# Litematica Creator 项目记忆与交接

> **已冻结的历史文档 / Frozen historical document.** 本文件保留 2026-10-03 文档重组前的记录，此后不再更新，内容可能与当前实现不符。当前设计见[架构与核心语义](../architecture.md)，任务状态见 [GitHub Issues](https://github.com/urntt/litematica-creator/issues)；文中 `#N` 即同号 GitHub issue。
> This file keeps records from before the 2026-10-03 documentation reorganization and is no longer updated, so it may not match current behavior. See the [architecture document](../architecture.md) for current design and [GitHub Issues](https://github.com/urntt/litematica-creator/issues) for task status. `#N` in the text is the GitHub issue with the same number.

记录日期：2026-10-01。写作前核对的代码基线：`50f28af`（`docs: document optional mod compatibility`），分支为 `main`。

本文件保存长对话中的项目背景、已经确定的语义、历史取舍和最近讨论，供新会话或云端开发接续使用。它不是新的开发指令，也不是另一份 TODO；没有明确批准的建议不得当作既定决策执行。历史测试结果不代表后续代码自动通过验收。

**2026-10-02 后续决定：** 已新增 [AGENTS.md](../../AGENTS.md)；用户明确选择 MIT、GitHub Releases、SemVer 的 Minecraft build-metadata 后缀、版本配置单一来源及 push/PR 客户端 GameTest CI 约定。政策分别维护于[开发指南](../development.md)和[发布指南](../releasing.md)，待落地项只在 [TODO](https://github.com/urntt/litematica-creator/blob/97bf3e24fca8718d0679c8761b0a610a1555e1f2/todo.md#开发基础设施与发布准备) 跟踪。下文关于 ARR、尚无 AGENTS、许可/发布渠道未决定的陈述均为 2026-10-01 历史快照，不再代表当前决定；本次未改变代码、构建、metadata 或已交付 JAR。

**Later decisions, 2026-10-02:** AGENTS.md now exists. The user adopted MIT, GitHub Releases, SemVer with Minecraft build metadata, centralized version configuration, and push/PR client GameTest CI. The linked development/release guides own these policies; TODO tracks implementation. References below to ARR, missing AGENTS, and undecided licensing/channel are historical, not current policy. This update does not change code, builds, metadata, or delivered JARs.

**2026-10-02 工具链落地：** 用户补入的 MIT LICENSE 已打包，版本与资源集中生成，官方依赖从固定提交自动准备，不再依赖手工 sibling 或本地 Maven；Windows 与 Linux 双 profile 构建均通过。使用[开发指南](../development.md)中的新入口，来源见[上游依赖](../upstream-dependencies.md)，验收见完成记录。下文关于本地依赖、浮动 Loom、wrapper、许可证的旧状态不再适用；CI、发布工作流和 Minecraft build-metadata 后缀仍未落地。

**Toolchain implemented, 2026-10-02:** The user-supplied MIT license is packaged, resource/version configuration is centralized, and fixed official-source profiles replace manual sibling/local Maven dependencies. Both profiles build on Windows and Linux. Use the development guide's new launcher; see provenance and completion notes for details. Historical local-dependency, floating-Loom, wrapper, and license gaps below are obsolete; CI, release automation, and the Minecraft version suffix remain pending.

## 1. 先读哪些文件

**GitHub 与混合云端准备，2026-10-02：** 完整 Git 历史已推送至公开仓库 `urntt/litematica-creator` 的 `main`，`origin` 已设置。push/PR 双硬依赖 Linux CI、打包客户端 GameTest 与正式 JAR 审计已通过 GitHub runner 验证，证据见完成记录。用户选择 Codex Cloud 与 Claude Code 混合开发，统一规则仍为 AGENTS，CLAUDE 只引用它；账号侧环境连接尚需分别完成。下文关于无 remote / CI 的内容是旧快照，后续以[开发指南](../development.md)、[云端指南](../cloud-development.md)和 TODO 为准；本批没有创建 release/tag 或改版本。

**GitHub and mixed-cloud preparation, 2026-10-02:** Original history is on public `urntt/litematica-creator/main`, with `origin` configured. GitHub runners passed both profiles, packaged client GameTests and JAR audits; see completion notes. The user chose mixed Codex/Claude cloud development with one AGENTS policy and a CLAUDE import. Each account still needs its environment connection. Earlier no-remote/no-CI statements are historical; use the linked guides and TODO. No release, tag, or version change occurred.

**Claude Code 云端首次会话，2026-10-02：** 云端指南矩阵在 Claude Code 会话中全部通过，证据见[完成记录](completed-tasks.md#claude-code-cloud-first-session--claude-code-云端首次验收)。JDK 25 与无头变量仅在该会话中设置，须由用户保存到环境 setup script / 变量后在新会话复核；Codex Cloud 首次会话仍未执行。随后用户决定直接在 `main` 上开发和推送，规则见 [AGENTS.md](../../AGENTS.md)。

**Claude Code first cloud session, 2026-10-02:** The cloud-guide matrix passed in a Claude Code session; see the completion notes. JDK 25 and headless variables were set only for that session, so the user must save the setup script/variables and recheck in a new session. The first Codex Cloud session has not run. The user then chose direct development and pushes on `main`; AGENTS.md owns that rule.

**Minecraft 26.3 移植，2026-10-03：** 用户要求把模组更新到 26.3 并同步升级配套设置。main 现以 26.3、Loom 1.18.2、Gradle 9.7.1、Fabric API 0.161.0+26.3 为基线，current/legacy 分别为 MaLiLib `0.30.2`/`0.30.1` 与 Litematica `0.29.1`/`0.29.0`；26.2 状态保留在 Git 历史中，未另建维护分支。26.3 改用 SDL3、拆分第一人称手部状态并重写挥手系统，无头测试需要 EGL 与 `SDL_VIDEO_FORCE_EGL=1`。细节与验证见[完成记录](completed-tasks.md#minecraft-263-port--minecraft-263-移植)，实机回归与旧数据模型分支移除仍在 TODO。

**Minecraft 26.3 port, 2026-10-03:** At the user's request, main now targets 26.3 with Loom 1.18.2, Gradle 9.7.1, and Fabric API 0.161.0+26.3; current/legacy pin MaLiLib `0.30.2`/`0.30.1` and Litematica `0.29.1`/`0.29.0`. The 26.2 state remains in Git history without a maintenance branch. 26.3 moved to SDL3, split first-person hand state, and rewrote swings; headless tests need EGL plus `SDL_VIDEO_FORCE_EGL=1`. See the completion notes; manual in-game regression and removal of the old data-model branch remain in TODO.

- [当前 TODO](https://github.com/urntt/litematica-creator/blob/97bf3e24fca8718d0679c8761b0a610a1555e1f2/todo.md)：未完成项目、编号、范围和需要决策的内容。
- [完成记录](completed-tasks.md)：实现细节、修复沿革和历史验收。
- [README](../../README.md)：双语使用入口、依赖范围、默认按键和当前边界。
- [可选模组兼容](../optional-mod-compatibility.md)：准确的启动矩阵、软兼容入口和退化方式。
- [项目概览](https://github.com/urntt/litematica-creator/blob/97bf3e24fca8718d0679c8761b0a610a1555e1f2/docs/project-overview.md)：亮点、使用场景和技术概要。
- [设计与路线图](creator-design-and-roadmap.md)：早期设计背景，部分段落已经过时。例如轻量库存 GUI、仅由原生页面导出、只清理 Creator cell 等描述不能视为当前实现。

遇到文档冲突先核对源码、最新完成记录与用户的后续决定，不要从旧路线图重新实现已被替代的方案。任务状态只在 TODO 维护，验收细节放到完成记录。

## 2. 产品定位与边界

- Fabric 客户端 Litematica 附属，当前实现目标是 Minecraft `26.2`。
- 玩家无需在真实生存背包获得对应方块，即可像创造模式一样创建或编辑投影。
- 编辑数据仍是普通 `LitematicaSchematic + SchematicPlacement`，不是独立的私有草稿格式或另一套投影渲染生态。
- 不放置或破坏真实方块，不修改真实物品栏，不发送 Creator 编辑产生的真实攻击、放置或物品栏同步包。
- 硬依赖 MaLiLib、Litematica；Tweakeroo、Syncmatica、Lithium、Sodium 的相关能力是软兼容。
- “纯客户端”不等于禁止真实玩家的正常网络同步。相机替身不得发玩家移动包，但本体的重力、惯性、击退和服务端校正仍要正常同步。
- Syncmatica 目前仅审计共同安装和 GUI 共存，不是已实现实时草稿同步。未来可选服务端 companion 属于 #27 的另行设计。

## 3. Focus、Selected、草稿与编辑对象

### 已确定的模型

- schematic 是内容真源；placement 是带原点、旋转、镜像及 subregion 变换的编辑视图。
- 对同一 schematic 的任意 placement 编辑，都会修改同一份原理图，其他 placements 同步反映结果。
- Creator Focus 独立于 Litematica Selected Placement。管理器的“当前查看项”又是第三套独立状态。
- 不维护 Creator-owned / Creator-managed draft 集合来判断能不能编辑或能不能恢复。普通已加载原理图同样可编辑。
- dirty 的依据是 Litematica metadata 的 `wasModifiedSinceSaved()`，不另建一套权威 dirty 状态。
- 新建空白是真正零 region 的内存 schematic，不用占位空气 region。

### 目标归属

- 命中投影或操作位置属于唯一可编辑 placement 时，编辑它，并在实际操作可以提交后切换 Focus。
- 位置不属于任何候选 placement 时：有有效 Focus 就向该 schematic 扩展；没有就新建并聚焦草稿。不设与原投影相接的条件或额外扩展边界。
- 重叠候选先打开 Focus Switcher，由玩家选择。不按 Selected、旧 Focus、距离或渲染顺序替玩家决定。
- 重叠选择的临时隐藏是 Creator-local 状态，不能持久改写原生 placement 的启用或渲染设置。
- 中键始终只 pick block，不新建、切换或清空 Focus。
- 新草稿是否同时成为 Litematica Selected 由 `selectNewDraftPlacement` 控制，默认关闭。
- Focus placement 被手动移除时直接清空 Focus；连续操作同一个 placement 不重复提示。

### 历史取舍

曾考虑以 Litematica Selected 作为统一编辑目标，也曾取消独立 Focus；最终因“离开现有边界后，到底新建还是扩展”无法自动推断，重新保留独立 Focus。不要再次把全部编辑逻辑建立在 Selected 上。

## 4. 结束编辑、卸载、保存与导出

这些是不同操作，不应混用“保存草稿”的旧称呼：

| 操作 | 语义 |
| --- | --- |
| 结束编辑 | 只清空 Focus；不保存、不导出、不卸载，也不改 Selected |
| 卸载当前原理图 / 丢弃 | 卸载 Focus 对应 schematic 及其全部 placements，清除相关 recovery；不删除原 `.litematic` 文件 |
| 保存 | 覆盖当前绑定文件；没有绑定时进入另存并绑定流程 |
| 另存并绑定 | 写新文件，成功后将同一个内存 schematic 及全部 placements 改绑到新文件 |
| 导出副本 | 只写文件，不改变当前绑定、dirty、Focus 或编辑状态 |

#79/#80/#89 已完整实现，不是仅做了管理界面或一半导出模式：

- 写盘与绑定两阶段提交，原子写文件成功后才改内存。
- 保持 schematic / placement 对象身份、hash、变换、启用、渲染、Focus 和 Selected。
- 目标文件已绑定给另一已加载 schematic 时拒绝操作。
- 保存期间没有后续编辑就转 clean 并清除 recovery；存在后续编辑则完成绑定但保留 dirty，按新路径继续缓存。
- 导出规范化只处理快照，不立即改内存 region 拓扑；明确重新加载时才协调全部 placements 的 subregions。
- 不自动接管 Litematica 原生保存、降级导出和保存监听器。原生写盘 Mixin 的 metadata 兼容作用与 Creator 管理器的文件绑定服务是不同入口。

### 四种导出模式

默认稀疏压缩，模式只作用于显式保存或导出，不压缩 recovery 或编辑中的数据：

1. 原样保存：保留所有原 region 和 Creator `1x1x1` cells。
2. 稀疏压缩：普通 regions 保留；只压缩带 Creator 保留前缀的 unit cells，确定性合并为完全填满、无额外 AIR 的 cuboids。边或角相接不能视为面相邻。
3. 外边界，仅投影：展平为一个最小 enclosing cuboid，未覆盖位置为 AIR，不读取真实世界。
4. 外边界，补入真实世界：未覆盖位置按明确选择的同 schematic placement 采样真实客户端世界；原投影覆盖位置，包括显式 AIR，始终优先。

世界采样不会改变 Focus / Selected，不采集真实实体，也不伪造服务器 scheduled ticks。所有模式需正确迁移 block entity NBT、原理图实体和两类 scheduled ticks，以及 region 原点变化后的坐标。

## 5. Recovery 的不可丢失语义

- 路径为 `config/litematica-creator/recovery/`，每个 schematic 独立 UUID entry。
- non-file-backed 始终有缓存资格，包括空白草稿；file-backed 只在 dirty 时缓存；clean file-backed 清除旧 entry，并沿用 Litematica 原生恢复。
- 资格不取决于是否由 Creator 创建。Rebuild 或其他入口产生的未保存修改也需捕获。
- 客户端线程制作 schematic NBT / placement JSON 快照，单线程后台 writer 压缩和写盘。
- 5 秒 idle debounce、持续变化最多 30 秒提交一次、每秒 metadata 扫描；正常生命周期退出前 flush 并等待写完。
- 标准压缩 `.litematic` 内容 + manifest v1；generation 文件与原子 manifest 提交保证至少保留上一份完整缓存。
- 先验证缓存与全部 placement 数据，再替换原生干净加载结果；损坏、缺失或未知版本缓存不自动删除。
- 下一 client tick 恢复，等待 Litematica 原生 placement 加载完成。
- 恢复 placement hash、变换、启用/渲染、Selected 和 Focus，但 Creator 模式保持关闭。成功只显示一条汇总，失败保留缓存及原生对象。
- 移除一个 placement 后不再恢复它；移除最后一个后删除 entry 并抑制本会话重建，重新添加可解除抑制。
- 玩家明确卸载或丢弃要清缓存；切维度、断线、退出世界、关闭游戏不是丢弃。恢复自身的替换操作必须防止误触主动清理。
- 已修复实例目录迁移时 placement JSON 保存旧绝对路径的问题。验证 entry / generation 后在 JSON 副本重绑当前 cache path，不修改原始缓存或源文件。
- 这里的新旧依赖缓存互通仅指相同 Minecraft 版本，不代表跨游戏版本、跨 `DataVersion` 或降级安全。

## 6. 稀疏编辑与竞态修复经验

- 已有 region 内编辑用 world/container 与状态的正反变换；边界外按需创建 `1x1x1` Creator cell，不频繁 resize 巨大 region。
- 稀疏模型避免把大片真实建筑声明成“投影应为空气”（#11）。
- 删除后只有方块、block entity NBT、实体、scheduled block ticks、fluid ticks 全空才移除 region；不依赖普通 `set()` 未维护的 `blockCounts`。
- 清理适用于普通 region 和 Creator cell；同步移除 schematic 所有映射和所有 placements 的该 region，最后一个 region 清空时也需清旧框。
- 占用检查先于新建、Focus、region、dirty、recovery 与成功动画；不可替换目标静默阻断。

#37/#61 曾多次“频率降低但没有根治”。不能把第一阶段锁方案当作最终修复：

- 保留编辑事务完整修改后再发布 placement change 的一致性边界。
- 最终还处理 schematic world chunk 卸载、空 chunk 装入、完整替换期间，渲染任务缓存了中间态引用的问题。
- `CreatorSchematicEditGuard` 组合全局编辑锁与按 chunk 分片的公平读写锁；同 chunk rebuild 串行，rebuild / render compile 互斥。
- 渲染编译取得稳定锁后重新 `rebuildWorldView()`，不继续使用入队时捕获的旧 chunk 引用。
- 旧的“抑制 placement 全量刷新并缩小结构刷新范围”方案已回退；不要无依据绕过 Litematica 原生 placement change 流程。
- 用户在测试机连续数小时高频放置/删除后报告不再复现；后续修改依然需要跨 chunk、多 placement、快速编辑与渲染回归。
- 曾有约 1.9 MB 构建被 Fabric 识别成重复 Litematica 的包装问题。每次交付都要检查 mod id、体积和内容，不得捆入 MaLiLib/Litematica 类或它们的 `fabric.mod.json`。

## 7. 虚拟物品栏、输入与相机

### 虚拟物品栏

- 独立 9 格快捷栏、27 格背包、1 副手、4 盔甲、1 丢弃栏；无虚拟合成栏，不写真实背包。
- 已有原版创造分类、搜索、拖拽、palette 与虚拟生存栏；用户对创造栏效果反馈良好，不能退回早期搜索列表 MVP。
- pick 使用相同 item/components 判断：快捷栏命中就选槽，主库存命中按 suitable-hotbar 交换，不存在则取 1 个；不搜索盔甲或副手。
- Palette Shift+单击最终采用“鼠标上拿最大一组”，不是直接送进快捷栏。早先 #40 的快捷栏方案由 #47 修正。
- 搜索栏可以输入 `e`；不能为了用物品栏键关闭界面而无条件抢走文本输入。重开保留上次标签页。
- 主手无可放置方块时可用虚拟副手；换手键交换虚拟主副手，而非真实库存。
- 翻译模式和 Creator 独立语言选择已经移除；所有文本直接跟随游戏语言，不重建独立语言系统。

### 连续操作与射线

- Accurate 放置 / backfill 经实测效果不佳，已回退并取消；只保留固定间隔放置与长按持续删除。
- 编辑距离默认 10（1–128）；空中放置默认开，固定距离默认 5（1–128）；连续放置和删除各默认 4 ticks（1–20）。
- 放置、删除、pick 和原版式投影轮廓共享 Creator camera 射线与编辑距离。
- #90 将真实实体、真实方块和投影分别按距离合并；等距优先实体，再投影，再真实方块。
- 最近是实体时静默阻断，不穿透、不创建对象、不触发空中放置；只有真正 MISS 才使用固定距离空气目标。
- #56 的“忽略最终放置格实体碰撞”不是射线穿透开关。
- Creator 模式必须消费真实 attack / use 与冲突的 Litematica Rebuild 路径，失败也不能透传真实交互；模式关闭则不得残留阻断。

### Creator Camera

- 独立、未注册到真实世界实体列表的 `CreatorCameraEntity`，不能调用其网络 `LocalPlayer.tick()` 或发出替身移动包。
- 地面模式复用原版式移动与真实/投影形状碰撞；双击空格飞行，飞行穿墙；位置和飞行状态不跨会话保存。
- 相机姿态、碰撞箱、潜行、匍匐、滑翔与装备使用独立状态，不能继承本体的鞘翅小碰撞箱或爬行状态。
- 真实玩家主动输入和转向隔离，但本体继续正常物理、姿态更新和服务端位置同步；不得再“模型在掉落、服务器仍停在原处”。
- 替身不推动本体或其他真实实体。单人暂停时相机物理冻结。
- 第一人称不画透明全身，只画虚拟手持或半透明手臂；第三人称与库存预览画半透明替身，包括皮肤第二层。
- Creator 挥手只作用于替身，不带动本体。鞘翅模型、滑翔动作和披风纹理已修复，保留原版纹理选择，不强制关闭 `showCape`。
- 自动联动进入/退出 Creator 相机默认开；地面/飞行速度各 1.0（0.1–5.0）；投影碰撞默认开；忽略放置实体占位默认关。
- Tweakeroo 接管与恢复须保留原 camera / 配置快照，临时使用 `freeCameraPlayerMovement=true`、`freeCameraPlayerInputs=false`；Creator Camera 本身不依赖 Tweakeroo。
- 世界卸载、断线、死亡/玩家实例替换和异常初始化都要恢复外部相机、输入与渲染状态；死亡退出保留 Focus / recovery。

## 8. 管理器与默认按键

管理器的 Overview、Placement、保存与导出均已实现。它在客户端世界中可独立于 Creator 模式打开，和 Litematica 主菜单、已加载原理图、placement 列表同级互通。

需要守住已修复的细节：即时搜索、查看项独立、明确 Focus/Selected、单 placement 组合行避免伪重复、所有控件 hover 说明、双语布局和长路径截断。缩略图先关闭管理器，等待真实游戏帧完成再捕获并返回；复制 metadata 不得消费源缩略图 `IntStream`。切换导出模式或采样 placement 自动刷新导出信息，刷新不写文件。

| 操作 | 当前默认按键 |
| --- | --- |
| Creator 模式 | `Y` |
| Creator Camera | `M+B` |
| Creator 物品栏 | `M+E`；Creator 开启时默认也接管原版物品栏键 |
| Creator 设置 | `M+K` |
| 结束编辑 | `M+Left Shift+S` |
| 卸载 Focus 原理图 | `M+Left Shift+D` |
| Focus Switcher | `M+F` |
| 新建空白 | `M+N` |
| 原理图管理器 | `M+J` |

管理器早期 `M+G` 与 Litematica 默认显示开关冲突，已改为 `M+J`，只迁移旧默认值，不覆盖其他自定义组合键。

## 9. 当前未完成范围

具体细则与编号以 [TODO](https://github.com/urntt/litematica-creator/blob/97bf3e24fca8718d0679c8761b0a610a1555e1f2/todo.md) 为准，目前主要分组如下：

- #81：虚拟主副手的 Litematica 工具生效；处理 input 优先级而非替换真实库存。
- #82/#83：客户端原版式交互事务层和完整 BlockItem 放置，包括门/床/花丛、依附方块、同格累加、半砖合并和含水。
- #84/#85/#86：投影实体、桶与静态流体、可持久化结果的通用物品使用。
- #22/#87/#29：状态/NBT 编辑、虚拟调试棒、投影方块与虚拟容器交互。
- #30/#28：undo/redo 与线/面/盒/批量操作。
- #42/#65/#91/#93：不透明预览、特殊投影物理、空挥手、声音与粒子。
- #41：Inventory Profiles Next / ItemScroller 的本地库存兼容或行为复刻。
- #66/#67：UI 信息梳理、详细用户文档与 Wiki。
- #27/#92：多人同步与带参数的客户端 `/creator` 命令。
- #88：默认关闭的 Focus 局部世界模拟，尚需独立技术验证。

当前放置只是普通 `BlockItem` 单状态写入，不能假装已经支持完整创造模式语义。未来事务需一次验证全部写入，不能多格只放一半或覆盖不可替换投影。依附/生存判定按已确定的编辑语义放宽，但不省略投影占用保护。

普通编辑只提交明确直接结果，不运行邻居更新、侦测器、红石、流体传播或 block entity ticks。#29/#85 的动态传播不是加一个开关即可完成，属于 #88 的隔离模拟。不要把真实 `ClientLevel` 全局伪装成 schematic world。

## 10. 开发基线与验证边界

| 项目 | 当前值 |
| --- | --- |
| mod id / 版本 | `litematica-creator` / `0.1.0-dev` |
| 包名 | `io.github.urntt.litematicacreator` |
| Minecraft / Java | `26.2` / `25` |
| Gradle wrapper | `9.6.0` |
| Loom 声明 | `1.17.+`；云端准备建议改为已验证的固定版本，尚未实施 |
| Loader | `0.19.3` |
| 默认 MaLiLib / Litematica | `0.29.4` / `0.28.5` |
| 旧兼容测试基线 | `0.29.2-sakura.4` / `0.28.2-sakura.1` |
| Fabric 硬依赖范围 | MaLiLib `>=0.29.2- <0.29.5-`；Litematica `>=0.28.2- <0.28.6-` |
| 许可 | `ARR`，没有决定改 MIT |

#94 处理 `CompoundTag` 与 MaLiLib `CompoundData` 的模型差异，Creator 快照与磁盘仍用标准 NBT；实体使用自有快照，边界集中转换。旧 `NbtUtils` 和新 `DataFileUtils` 原生写盘注入都为可选，避免单一强制 Redirect 导致延迟类加载崩溃。

#95 已审计 Tweakeroo `0.29.2-sakura.1` / `0.29.3`、Syncmatica `0.3.18` / `0.3.20`、Lithium `0.25.3+mc26.2`、Sodium `0.9.2-alpha.4+mc26.2`。新版 MaLiLib 自身拒绝旧 Tweakeroo `<0.29.3`，不能任意混配。Tweakeroo 反射契约初始化一次，运行失败后停用桥接而不刷屏；其他三者无私有 API 桥接。

历史 #95 验证记录为两组硬依赖测试/构建通过（189 项测试、59 个 suite），有效软兼容组合启动至主菜单。启动审计不是所有游戏内行为的验收。上次交付记录的 JAR 为 `litematica-creator-fabric-26.2-0.1.0-dev.20260823.210657.jar`，407154 字节；本次只写文档，没有重跑测试或生成新 JAR。

标准本地命令：

```powershell
.\gradlew.bat build --no-daemon --max-workers=1
```

兼容矩阵验证必须确认实际解析到对应依赖。只传 `-Plitematica_version=...` 不保证无条件 composite build 中的 sibling 源码也切到旧版。

## 11. 本地工作区与 GitHub 迁移讨论

### 本地路径历史

- 聊天配置工作区：`C:\Users\urntt\OneDrive\otherProject\litematica-creator`。
- 用户此前说明已搬到 `C:\Minecraft\mymods\litematica-creator`，旧路径以 Directory Junction 指向新位置，用于规避 OneDrive 锁。
- 此次会话可读取的是聊天配置路径，未找到此前所述的目标源码目录，`Get-Item` 也未给出 junction target。因此以上迁移记为用户提供的历史上下文，不作为本次已验证的路径状态；后续文件操作应重新核实当前环境。
- Git 仓库在 workspace 下的 `litematica-creator-26.2`，不是包含 sibling 源码、历史 JAR 和日志的整个父目录。不要重新初始化仓库或丢弃既有 Git 历史。
- 过去构建还遇到输出目录删除权限与 ZIP/JAR 锁，曾用工作区外的允许写入镜像完成验证。不要通过削弱沙箱或大范围改 ACL 解决。

### 迁移准备：已经讨论，尚未实施

- 用户准备迁 GitHub 并在云端继续开发；目前没有 Git remote，也没有 CI、`AGENTS.md`、`.gitattributes` 或独立 LICENSE 文件。
- `settings.gradle` 无条件 `includeBuild('../litematica-26.2')`，构建使用 `mavenLocal()`，不能认为云端只检出 Creator 就能构建。
- 前次只读请求发现配置 Maven 的 MaLiLib `0.29.4` 可获取，而 Litematica `0.28.5` 与旧兼容基线的所查坐标返回 404；这是当时指定 URL 的结果，不证明所有来源永久不可用。
- sibling 当前依赖源码当时无 Git 元数据，需要确认上游来源和固定提交，或可靠制品坐标/归档校验和，再做自动获取与构建。
- `gradlew` 当前 Git mode 为 `100644`，Linux 准备需要可执行位与 LF；完整 wrapper 已跟踪，不能以 `*.jar` 全局忽略丢掉 wrapper JAR。
- 建议固定工具链、增加依赖可复现脚本、干净 Linux CI、双硬依赖矩阵、JAR 内容检查与 artifact 下载，再推远端。
- GitHub 公开/私有、最终许可、发布渠道与凭据尚未决定；不要从外部 skill 自动继承 MIT 或直接公开发布。
- Cloud 负责代码、测试与打包，本地仍需 GUI、相机、碰撞、渲染和多人组合验收。完整 Linux 构建与云端配置尚未完成。

### 多 Minecraft 版本：用户意向与建议

用户希望后续可支持多个 Minecraft 大版本并跟进更新；当前源码只实现 26.2，尚无其他版本分支或源码适配。

已讨论的建议是一个仓库、按版本线维护分支、分别发布 JAR：主线维护当前主要版本，移植新版本时建 port 分支，升级后保留需要维护的旧版本分支。同 Minecraft 版本的依赖兼容层与跨 Minecraft 版本的移植是两回事，不能只放宽 `fabric.mod.json`。

Stonecutter 是单源码多版本的备选。建议真正移植第二个版本后按差异规模评估，不在 GitHub 准备时提前重构全项目；这仍是建议，不是已采用的构建政策。每条版本线都需自己的准确依赖、Java/Gradle/Loom 基线及恢复数据边界。

## 12. 外部 skill 的评估结论

用户提供 `fabric-client-mod.skill`，来自 Claude Code 开发其他模组的流程。已只读评估，没有安装、运行或把附件里的指令当作本项目请求。

可提炼：阅读完整源码和真实签名、行为驱动的客户端 GameTest、证明测试能失败、客户端线程访问、无头渲染与截图、wrapper 校验、语言键一致性、CI 和发布 dry-run。

不能原样继承：只支持最新版本且不做多版本、MIT、`com.urntt` 包名、强制 Gson 配置、直接推 main、仅 GitHub Releases、所有仓库文档英文、重建现有 source sets。Creator 继续保留已有 MaLiLib 体系、身份和双语文档，除非用户明确另作决定。

评估发现需要修正：

- 构建通过不证明所有 Mixin 已在运行时应用；普通 JUnit、启动检查和游戏内行为测试各有不同边界。
- 用文本 `sed` 删除 Mixin 名可能损坏数组；负向测试必须因预期断言失败，并可靠恢复，最好使用独立临时工作树。
- 发布模板只 build，没有强制同提交完整游戏测试；应发布已通过统一验证的同一份 artifact。
- 官方模板的 `HEAD`、最新 JDK 补丁和平台权限经验不能当作固定可复现输入。
- 26.3 / SDL3 等笔记是外部项目的版本经验，未经当前源码核实不能套到 26.2。Claude 云端的限制也不等于其他执行环境的限制。

## 13. 接续工作的约定

- 使用 Git，先读工作树与相关实现，不撤销用户或生成的无关改动。按职责分批提交，不靠口头承诺代替完成验证。
- 本项目交流以中文为主，README 和用户文本已有中英双语；外部模板的语言政策不自动覆盖。
- 用户常在另一台测试机实测。没有当前日志和机器可见性时先分析代码与报告，不声称本机已经复现。
- 用户要求“先分析/讨论/更新 TODO，不直接改”时只做该范围；明确批准实现后才改代码。
- 不把提议写成已完成，不把启动成功写成全功能验收，不把取消的 Accurate 方案重新带回。
- UI 要特别验证中文/英文、不同窗口尺寸、控件宽度、遮罩绘制顺序、tooltip、路径溢出及状态可辨识性。
- 代码完成后做测试/构建，检查 JAR 只属于 Creator，并按既有交付习惯复制最终包到项目父目录；文档单独修改不需要生成新包。
- 下一步未收到新的实现授权：当前主题是交接与 GitHub/云端准备，不是自动开始 #81/#82 或新 Minecraft 版本移植。
