# Architecture / 架构与核心语义

本文件是 Litematica Creator 当前产品定位、边界与核心语义的唯一出处。使用方法见 [README](../README.md)，构建与验证见[开发指南](development.md)，可选模组测试矩阵见[兼容说明](optional-mod-compatibility.md)。任务状态、验收条件与完成证据在 [GitHub Issues](https://github.com/urntt/litematica-creator/issues) 维护；[历史文档](history/README.md)只作背景，其中的旧方案不代表当前实现。配置默认值以 `CreatorConfigDefaults` 为准，本文引用的数值与代码冲突时以代码为准。

This file is the single source for Litematica Creator's current scope, boundaries, and core semantics. See the README for usage, the development guide for builds, and the compatibility notes for the optional-mod matrix. GitHub Issues track tasks, acceptance criteria, and completion evidence. The [history](history/README.md) folder is background only; its older designs are not current behavior. Configuration defaults live in `CreatorConfigDefaults`; the code wins over any value quoted here.

## Scope and Boundaries / 定位与边界

- Fabric 客户端 Litematica 附属，目标 Minecraft 版本由 `gradle.properties` 决定。玩家无需在真实背包取得方块，即可像创造模式一样在世界中创建或编辑投影。
- 编辑数据是普通 `LitematicaSchematic` 与 `SchematicPlacement`，不引入私有草稿格式或另一套投影渲染；结果可继续使用 Litematica 的保存、渲染、材料列表、Verifier 与建造流程。
- Creator 编辑不放置或破坏真实方块、不修改真实物品栏，也不发送由此产生的攻击、放置或库存操作包。
- “纯客户端”不等于压制真实玩家的同步：相机替身不发移动包，但本体的重力、惯性、击退与服务端校正照常进行。
- 硬依赖仅 MaLiLib 与 Litematica；Tweakeroo、Syncmatica、Lithium、Sodium 为软兼容。Syncmatica 目前只保证共存；[#27](https://github.com/urntt/litematica-creator/issues/27) 只计划通过 Syncmatica 共享已保存的原理图与 placement，不做实时草稿同步或服务端组件。
- 普通编辑只提交玩家明确产生的直接结果：放置写入原版放置本身产生的全部格子，并让直接相邻的投影方块各做一次原版形状更新；不连锁邻居更新，也不运行侦测器、红石、流体传播或 block entity tick。放置用的事务世界只读取目标投影、记录写入，不是持续运行的世界模拟（[#88](https://github.com/urntt/litematica-creator/issues/88) 已关闭），也不能把真实 `ClientLevel` 伪装成 schematic world。

- A Fabric client-side Litematica addon whose target Minecraft version comes from `gradle.properties`. Players create or edit projections in-world, Creative-style, without owning the real blocks.
- Edits operate on ordinary `LitematicaSchematic` and `SchematicPlacement` objects, with no private draft format or separate projection renderer, so Litematica's save, render, material list, Verifier, and build workflows keep working.
- Creator edits never place or break real blocks, never touch the real inventory, and never send the resulting attack, placement, or inventory-operation packets.
- "Client-only" does not suppress the real player's synchronization. The camera stand-in sends no movement packets, while the real body keeps gravity, momentum, knockback, and server corrections.
- MaLiLib and Litematica are the only hard dependencies. Tweakeroo, Syncmatica, Lithium, and Sodium are soft-compatible. Syncmatica is only guaranteed to coexist; [#27](https://github.com/urntt/litematica-creator/issues/27) only plans to share saved schematics and placements through Syncmatica, with no real-time draft sync or server component.
- Ordinary edits commit only the direct result the player asked for: a placement writes every cell vanilla placement itself produces, and each directly adjacent projection block runs one vanilla shape update. Neighbor updates never chain, and no observers, redstone, fluid spread, or block entity ticks run. The transaction world used for placement only reads the target projection and records writes; it is not a running world simulation ([#88](https://github.com/urntt/litematica-creator/issues/88) was closed), and the real `ClientLevel` is never disguised as the schematic world.

典型场景：在生存服务器现场设计建筑后按材料列表施工；从空白快速草拟红石、装饰或结构布局；从任意旋转或镜像的 placement 进入编辑，同步修正同一 schematic 的全部实例；用 Creator Camera 检查高处、地下或封闭空间；在管理器中整理多个 schematics 与 placements 后保存、改绑或只导出副本；断线或异常退出后恢复尚未保存的设计。

Typical uses: design a build on a survival server and then gather materials from the material list; sketch redstone, decoration, or structure layouts from scratch; edit through any rotated or mirrored placement so every instance of the schematic updates; inspect high, underground, or enclosed spaces with Creator Camera; organize schematics and placements in the manager before saving, rebinding, or exporting a copy; and restore unsaved designs after a disconnect or crash.

## Edit Model / 编辑模型

### State Ownership / 状态所有权

- schematic 是内容真源；placement 是带原点、旋转、镜像与 subregion 变换的视图。通过任一 placement 编辑都修改同一 schematic，其他 placements 同步反映。
- Creator Focus、Litematica Selected Placement 与管理器“当前查看项”是三套相互独立的状态，不擅自联动。
- 不维护 Creator 自有草稿集合来决定能否编辑或恢复；任何已加载原理图都可编辑。
- dirty 以 Litematica metadata 的 `wasModifiedSinceSaved()` 为准，不另建权威 dirty 状态。
- 新建空白是零 region 的内存 schematic，不用占位空气 region。

- The schematic is the source of truth; a placement is a view with origin, rotation, mirror, and subregion transforms. Editing through any placement changes the shared schematic, and every other placement reflects it.
- Creator Focus, Litematica's Selected Placement, and the manager's viewed entry are three independent states that never follow each other implicitly.
- There is no Creator-owned draft set that gates editing or recovery; any loaded schematic is editable.
- Dirty state is Litematica metadata's `wasModifiedSinceSaved()`; Creator keeps no competing dirty flag.
- A new blank schematic is an in-memory schematic with zero regions, not a placeholder air region.

### Target Resolution / 目标归属

- 命中投影或操作位置只属于一个可编辑 placement 时编辑它，并在操作确定可提交后切换 Focus。放置的操作位置是原版的实际落点，例如点中真实矮草时是草所在格，而不是其上方。
- 位置不属于任何候选 placement 时：有有效 Focus 就向该 schematic 扩展，没有就新建并聚焦草稿；不要求与原投影相接，也不设扩展边界。
- 重叠候选先打开 Focus Switcher 由玩家选择，不按 Selected、旧 Focus、距离或渲染顺序代为决定；选择期间的临时隐藏是 Creator 本地状态，不改写 placement 的启用或渲染设置。
- 中键只 pick block，不新建、切换或清空 Focus。
- 新草稿是否同时成为 Litematica Selected 由 `selectNewDraftPlacement` 控制，默认关闭。
- Focus 所在 placement 被移除时直接清空 Focus；连续操作同一 placement 不重复提示。

- When the hit projection or target position belongs to exactly one editable placement, edit it and move Focus only once the operation is known to commit. For placement, the target position is where vanilla actually puts the block; clicking real short grass, for example, targets the grass cell rather than the cell above it.
- When the position belongs to no candidate placement, extend the focused schematic if Focus is valid; otherwise create and focus a new draft. Extensions need not touch the existing projection and have no extra bound.
- Overlapping candidates open the Focus Switcher for the player to choose. Creator never decides by Selected, previous Focus, distance, or render order. Temporary hiding during the choice is Creator-local and never rewrites a placement's enabled or render settings.
- Middle-click only picks a block; it never creates, switches, or clears Focus.
- `selectNewDraftPlacement` controls whether a new draft also becomes Litematica's Selected Placement; it defaults to off.
- Removing the focused placement clears Focus. Repeated edits of the same placement do not repeat the notice.

设计取舍：曾尝试以 Litematica Selected 作为唯一编辑目标、取消独立 Focus，但离开现有边界时“新建还是扩展”无法自动推断，因此保留独立 Focus。不要把编辑逻辑重新建立在 Selected 上。

Design note: making Litematica's Selected Placement the only edit target was tried and dropped, because leaving existing bounds cannot tell "extend" from "create new". Focus therefore stays independent; do not rebuild editing on Selected.

### Sparse Editing / 稀疏编辑

- 已有 region 内的编辑使用 world/container 坐标与方块状态的正反变换；边界外按需创建名称带 `__litematica_creator_cell_` 前缀的 `1x1x1` Creator cell，不反复扩张巨大 region，也不把大片真实建筑声明成“投影应为空气”。
- 某格的 block entity NBT 与 scheduled block/fluid ticks 属于该格方块：同一方块只改状态时保留，换成其他方块（含删除为空气）时一并清除（[#105](https://github.com/urntt/litematica-creator/issues/105)）。
- 删除后只有方块、block entity NBT、实体、scheduled block ticks 与 fluid ticks 全空时才移除 region，不依赖普通 `set()` 未维护的 `blockCounts`。清理同时适用于普通 region 和 Creator cell，并同步移除 schematic 映射与所有 placements 中的该 region，最后一个 region 清空时也清除旧框。
- 放置在事务世界 `CreatorPlacementWorld`（继承 Litematica `WorldSchematic`、不渲染、不进入 Litematica 的世界管理）中执行原版 `BlockItem.place`。它读取目标 placement 的投影；点中真实方块时，只额外看到被点中的那一格，供原版判断是替换（如矮草）还是放到相邻格（如石头）。写入只被记录，不触及任何区块。门、床、高花写入两格，半砖合并，蜡烛与海泡菜同格叠加，朝向随点击面与视角，都沿用原版（[#82](https://github.com/urntt/litematica-creator/issues/82)）。
- 投影是规划而不是成品：事务世界中方块的存活检查（`canSurvive`）与多面方块的附着检查一律视为满足，火把、灯笼、梯子、按钮、门、蜡烛、发光地衣、藤蔓等按点击面与视角放置，不需要投影提供支撑（[#83](https://github.com/urntt/litematica-creator/issues/83)）。真实世界与 Litematica 的原理图世界不受影响。滴水石锥、硫磺尖锥与竹子在放置规则中直接检查支撑或下方方块，海草与海带要求目标格有水；这些方块目前仍需投影提供相应条件，后续处理在 #83 中跟踪。
- 放置后，与写入格直接相邻的每个投影方块运行一次原版形状更新（如栅栏、墙、楼梯连接）。只保留改变了且没有变成空气的结果，不连锁，也不因放置删除投影。
- 全部写入一起校验：除原版已检查的落点外，写入格原有投影须为空气或可替换；每个非空气写入都要通过实体占位检查。全部通过后才新建草稿或切换 Focus，并在同一编辑事务中提交；任何一格失败都不写入，不留下半个结构。占用检查先于建 region、标 dirty、recovery 与成功反馈；目标不可替换时静默阻断，原版找不到可放置状态时提示。
- 物品自带的方块实体数据（命名、旗帜图案、告示牌文字等）暂不写入，由 [#22](https://github.com/urntt/litematica-creator/issues/22) 跟踪；非方块物品的放置见 [#84](https://github.com/urntt/litematica-creator/issues/84) 与 [#86](https://github.com/urntt/litematica-creator/issues/86)。

- Inside existing regions, edits use forward and inverse transforms for world/container coordinates and block states. Outside them, Creator adds `1x1x1` cells named with the `__litematica_creator_cell_` prefix instead of resizing a huge region or declaring large volumes of real buildings as "projected air".
- A cell's block entity NBT and scheduled block and fluid ticks belong to the block in that cell. A state change of the same block keeps them; a different block, including removal to air, clears them ([#105](https://github.com/urntt/litematica-creator/issues/105)).
- After a removal, a region is deleted only when its blocks, block entity NBT, entities, scheduled block ticks, and fluid ticks are all empty; `blockCounts`, which plain `set()` does not maintain, is not trusted. Cleanup covers ordinary regions and Creator cells, removes the region from the schematic maps and every placement, and clears the stale box when the last region empties.
- Placement runs vanilla `BlockItem.place` in the transaction world `CreatorPlacementWorld`, which extends Litematica's `WorldSchematic` but is never rendered or managed by Litematica. It reads the target placement's projection; when a real block was clicked, it also sees that one cell, so vanilla can decide whether to replace it (short grass) or place next to it (stone). Writes are only recorded and touch no chunk. Doors, beds, and tall plants write both cells, slabs merge, candles and sea pickles stack in their cell, and facing follows the clicked face and view, all as in vanilla ([#82](https://github.com/urntt/litematica-creator/issues/82)).
- A projection is a plan, not a finished build: in the transaction world, block survival (`canSurvive`) and multiface attachment checks always pass, so torches, lanterns, ladders, buttons, doors, candles, glow lichen, vines, and the like follow the clicked face and view without projected support ([#83](https://github.com/urntt/litematica-creator/issues/83)). The real world and Litematica's schematic world are unaffected. Pointed dripstone, sulfur spikes, and bamboo check their support or the block below directly in their placement rules, and seagrass and kelp require water in the target cell; these still need the projection to provide those conditions, which #83 continues to track.
- After a placement, every projection block directly adjacent to a written cell runs one vanilla shape update, so fences, walls, and stairs connect. Only results that change the block without turning it into air are kept; nothing chains, and placement never removes a projection block.
- All writes are validated together. Apart from the cell vanilla already checked, every written cell must hold air or a replaceable projection, and every non-air write must pass the entity-occupancy check. Only then is a draft created or Focus switched, and every cell is committed in one edit transaction; if any cell fails, nothing is written and no half structure remains. The occupancy check runs before creating a region, marking dirty, scheduling recovery, or showing success. Non-replaceable targets block silently; a warning appears when vanilla finds no placeable state.
- Block entity data carried by the item (names, banner patterns, sign text, and so on) is not written yet; [#22](https://github.com/urntt/litematica-creator/issues/22) tracks it. Placing non-block items is covered by [#84](https://github.com/urntt/litematica-creator/issues/84) and [#86](https://github.com/urntt/litematica-creator/issues/86).

### Edit and Render Consistency / 编辑与渲染一致性

- 编辑事务完整修改 schematic 后才发布 placement change，Litematica 只重建实际受影响的 schematic chunks。
- `CreatorSchematicEditGuard` 组合一个公平的全局编辑锁和按 chunk 分片的公平读写锁：同一 chunk 的 rebuild 串行，rebuild 与渲染编译互斥，不同 chunk 仍可并行。
- 渲染编译取得稳定锁后重新执行 `rebuildWorldView()`，不使用入队时捕获、可能已被卸载或替换的旧 chunk 引用。
- 不绕过 Litematica 原生 placement change 流程；曾经“抑制全量刷新、缩小刷新范围”的方案已回退。[#37](https://github.com/urntt/litematica-creator/issues/37)/#61 曾多次只是降低频率而未根治，相关修改须做跨 chunk、多 placement、快速编辑与渲染并发回归。

- An edit transaction finishes changing the schematic before it publishes the placement change, and Litematica rebuilds only the schematic chunks actually affected.
- `CreatorSchematicEditGuard` combines a fair global edit lock with fair, chunk-striped read/write locks. Rebuilds of the same chunk run serially, rebuilds exclude render compilation, and different chunks still proceed in parallel.
- Render compilation calls `rebuildWorldView()` again after acquiring the stable lock instead of using chunk references captured at enqueue time, which may have been unloaded or replaced.
- Do not bypass Litematica's native placement-change flow; the earlier "suppress full refresh and narrow the update" approach was reverted. [#37](https://github.com/urntt/litematica-creator/issues/37) and [#61](https://github.com/urntt/litematica-creator/issues/61) were "fixed" several times before the root cause was found, so changes here need cross-chunk, multi-placement, rapid-edit, and concurrent-render regression.

## File Operations / 文件操作

| 操作 / Operation | 语义 | Semantics |
| --- | --- | --- |
| 结束编辑 / Finish editing | 只清空 Focus；不保存、不导出、不卸载，也不改 Selected | Clears Focus only; no save, export, unload, or Selected change |
| 卸载 / Unload | 卸载 Focus 对应 schematic 及其全部 placements，清除相关 recovery；不删除原 `.litematic` | Unloads the focused schematic with all its placements and clears its recovery; never deletes the original `.litematic` |
| 保存 / Save | 覆盖当前绑定文件；没有绑定时进入另存并绑定 | Overwrites the bound file, or falls back to Save As and Bind |
| 另存并绑定 / Save As and Bind | 写新文件，成功后把同一内存 schematic 及全部 placements 改绑到新文件 | Writes a new file, then rebinds the same in-memory schematic and all placements to it |
| 导出副本 / Export Copy | 只写文件，不改变绑定、dirty、Focus 或编辑状态 | Writes a file only; binding, dirty state, Focus, and editing state stay unchanged |

- 写盘与绑定两阶段提交：原子写文件成功后才改内存。保持 schematic 与 placement 的对象身份、hash、变换、启用与渲染状态、Focus 和 Selected。
- 目标文件已绑定给另一已加载 schematic 时拒绝操作。
- 写盘期间没有新编辑则转为 clean 并清除 recovery；期间有新编辑则完成绑定但保留 dirty，并按新路径继续缓存。
- 导出规范化只作用于快照，不立即改变内存中的 region 拓扑；用户明确重新加载时才原地协调全部 placements 的 subregions。
- Creator 不接管 Litematica 原生保存、降级导出和保存监听器。原生写盘 Mixin 只做 metadata 兼容，与管理器的文件绑定服务是不同入口。

- Writing and binding commit in two phases: in-memory state changes only after the atomic file write succeeds. Schematic and placement identity, hash, transforms, enabled/render state, Focus, and Selected are preserved.
- The operation is refused when the target file is already bound to another loaded schematic.
- If no edit happens during the write, the schematic becomes clean and its recovery entry is cleared. If an edit happens, binding still completes but the schematic stays dirty and keeps caching under the new path.
- Export normalization works on a snapshot and does not change in-memory region topology. Only an explicit reload reconciles the subregions of every placement in place.
- Creator does not take over Litematica's native save, downgrade export, or save listeners. The native write Mixin only keeps metadata compatible; it is a separate entry point from the manager's binding service.

### Export Region Modes / 导出 region 模式

模式只作用于显式保存或导出，不压缩 recovery 或编辑中的数据；默认稀疏压缩。

Modes apply only to explicit saves and exports, never to recovery or live editing data. Sparse compact is the default.

1. **原样 / Raw**：保留全部原 region 与 Creator `1x1x1` cells。Keeps every original region and Creator cell.
2. **稀疏压缩 / Sparse compact**：普通 region 保留；只把带 Creator 前缀的 unit cell 确定性合并为完全填满、不含额外 AIR 的长方体，边或角相接不算面相邻。Keeps ordinary regions and deterministically merges only Creator unit cells into fully filled cuboids with no added air; edge or corner contact is not face adjacency.
3. **外边界，仅投影 / Projection bounds**：展平为一个最小包围长方体，未覆盖位置为 AIR，不读取真实世界。Flattens to one minimal enclosing cuboid with air in uncovered cells and no world reads.
4. **外边界，补入真实世界 / World-filled bounds**：未覆盖位置按明确选择的同 schematic placement 采样真实客户端世界；投影已覆盖的位置（包括显式 AIR）始终优先。Fills uncovered cells by sampling the real client world through an explicitly chosen placement of the same schematic; projected cells, including explicit air, always win.

世界采样在客户端线程分批进行，不改变 Focus 或 Selected，不采集真实实体，也不伪造服务端 scheduled ticks。所有模式都正确迁移 block entity NBT、原理图实体、两类 scheduled ticks，以及 region 原点变化后的坐标。

World sampling runs in batches on the client thread. It changes neither Focus nor Selected, captures no real entities, and fabricates no server scheduled ticks. Every mode carries block entity NBT, schematic entities, both scheduled-tick kinds, and coordinates across region-origin changes.

## Recovery / 恢复缓存

- 缓存位于 `config/litematica-creator/recovery/`，每个 schematic 一个 UUID entry；内容为标准压缩 `.litematic` 与 manifest（格式版本 1）。generation 文件加原子 manifest 提交，保证至少保留上一份完整缓存。
- 没有绑定文件的 schematic（包括空白草稿）始终可缓存；有文件的只在 dirty 时缓存，clean 时清除旧 entry 并沿用 Litematica 原生恢复。资格与是否由 Creator 创建无关，Rebuild 等其他入口产生的未保存修改同样捕获。
- 客户端线程制作 schematic NBT 与 placement JSON 快照，单线程后台 writer 压缩写盘。5 秒无变化后提交，持续变化时最多 30 秒提交一次，每秒扫描 metadata；正常生命周期退出前 flush 并等待写完。
- 恢复在下一 client tick、Litematica 原生 placement 加载完成后进行。先验证缓存与全部 placement 数据再替换原生干净加载结果；损坏、缺失或未知版本的缓存不自动删除。
- 恢复 placement hash、变换、启用与渲染状态、Selected 和 Focus，但 Creator 模式保持关闭；成功只显示一条汇总，失败保留缓存与原生对象。
- 移除一个 placement 后不再恢复它；移除最后一个时删除 entry 并在本会话抑制重建，重新添加可解除抑制。
- 只有玩家明确卸载或丢弃才清缓存；切维度、断线、退出世界、关闭游戏都不是丢弃。恢复过程自身的替换必须防止误触发主动清理。
- 实例目录迁移后，验证 entry 与 generation，再在 placement JSON 副本中把绝对路径重绑到当前缓存路径；不修改原始缓存或源文件。
- 不同依赖版本间的缓存互通只限同一 Minecraft 版本，不保证跨游戏版本、跨 `DataVersion` 或降级安全。

- Entries live in `config/litematica-creator/recovery/`, one UUID entry per schematic, holding a standard compressed `.litematic` plus a manifest (format version 1). Generation files and an atomic manifest commit keep at least the previous complete cache.
- Schematics without a backing file, blank drafts included, are always eligible. File-backed ones are cached only while dirty; once clean their entry is cleared and Litematica's own restore applies. Eligibility does not depend on Creator having created the schematic, so unsaved changes from Rebuild or other entry points are captured too.
- The client thread snapshots schematic NBT and placement JSON; a single background writer compresses and writes them. Writes happen after 5 idle seconds, at most every 30 seconds under continuous change, with a metadata scan every second. Normal lifecycle exits flush and wait for the write.
- Restore runs on the next client tick after Litematica's native placements load. The cache and all placement data are validated before replacing the clean native load; corrupt, missing, or unknown-version caches are never deleted automatically.
- Restore brings back placement hash, transforms, enabled/render state, Selected, and Focus, while Creator mode stays off. Success shows one summary; failure keeps both the cache and the native objects.
- A removed placement is not restored. Removing the last one deletes the entry and suppresses re-creation for the session until a placement is added again.
- Only an explicit unload or discard clears the cache. Changing dimension, disconnecting, leaving the world, or closing the game is not a discard, and restore's own replacement must not trigger an active cleanup.
- After an instance directory moves, restore validates the entry and generation, then rebinds absolute paths in a copy of the placement JSON to the current cache path without touching the original cache or source files.
- Cache compatibility across dependency versions holds only within the same Minecraft version, not across game versions, `DataVersion` changes, or downgrades.

## Input and Targeting / 输入与射线

- Creator 模式消费真实 attack/use 以及与之冲突的 Litematica Rebuild 路径，编辑失败也不透传为真实交互；模式关闭后不残留任何拦截。
- 放置、删除、pick 与原版式投影轮廓共享 Creator Camera 射线和编辑距离（默认 10，范围 1–128）。
- 真实实体、真实方块与投影分别求最近命中后合并；等距时实体优先，其次投影，最后真实方块。最近的是实体时静默阻断：不穿透、不建对象、不触发空中放置。只有真正 MISS 才用固定距离的空气目标（空中放置默认开启，距离默认 5，范围 1–128）。
- “忽略放置格实体碰撞”只放宽最终放置格的实体占位检查，不是射线穿透开关（默认关闭）。
- Creator 模式下 Litematica 的原理图 pick block 不生效：它从真实玩家射线并写入真实背包，中键只执行 Creator 的虚拟 pick（[#106](https://github.com/urntt/litematica-creator/issues/106)）。
- 长按按固定间隔连续放置或删除（各默认 4 ticks，范围 1–20）。Accurate 连续放置与 backfill 经实测效果不佳已取消，不再恢复。

- Creator mode consumes real attack/use and the conflicting Litematica Rebuild paths. A failed edit never falls through to a real interaction, and nothing stays blocked once the mode is off.
- Placement, removal, pick, and the vanilla-style projection outline share the Creator Camera ray and edit range (default 10, range 1–128).
- Real entities, real blocks, and projections are traced separately and merged by distance; ties prefer entities, then projections, then real blocks. A nearest entity blocks silently with no pass-through, no new object, and no air placement. Only a true MISS uses the fixed-distance air target (air placement on by default, distance 5, range 1–128).
- "Ignore entity collision at the placement cell" only relaxes the entity-occupancy check for the final cell; it does not let the ray pass through entities (off by default).
- Litematica's schematic pick block is disabled in Creator mode, because it traces from the real player and fills the real inventory; the middle click runs only Creator's virtual pick ([#106](https://github.com/urntt/litematica-creator/issues/106)).
- Holding the key repeats placement or removal at fixed intervals (4 ticks each by default, range 1–20). Accurate continuous placement and backfill were tried, worked poorly in practice, and were removed for good.

## Virtual Inventory / 虚拟物品栏

- 独立的 9 格快捷栏、27 格背包、副手、4 格盔甲与丢弃栏，无合成栏；只保存在客户端，不写真实背包。
- 原版式创造分类、搜索、拖拽、palette 与虚拟生存栏；Creator 开启时默认接管原版物品栏键，重开保留上次标签页。搜索框可正常输入 `e`，不能为了用物品栏键关闭界面而抢走文本输入。
- pick 按 item 与 components 匹配：快捷栏命中则选中该槽，主库存命中则与合适的快捷栏槽交换，不存在则放入 1 个；不搜索盔甲或副手。Palette 中 Shift+单击在鼠标上拿起一整组。
- 主手无可放置方块时可用虚拟副手；换手键交换虚拟主副手而非真实库存。
- Creator 模式下 Litematica 的工具物品（`toolItem` 与 `toolItemComponents`）只按虚拟主副手判断，真实手持的工具不再生效。Tool HUD、角点、选择、模式切换和带修饰键的滚轮都识别虚拟工具；工具热键作用的那次按下不再触发 Creator 放置、删除或 pick，不带修饰键的滚轮仍切换虚拟快捷栏（[#81](https://github.com/urntt/litematica-creator/issues/81)）。
- 虚拟主手或副手持原版调试棒时（主手优先），攻击投影方块按原版语义选择属性，使用则循环所选属性的值，潜行反向；不检查真实玩家的权限。调试棒优先于删除和放置，每次按下只执行一次，长按不重复；真实方块与空气不受影响，中键 pick 不变。所选属性写入虚拟调试棒的 `debug_stick_state` 组件并随物品栏持久化；值的修改走普通编辑路径，重叠时先打开 Focus Switcher，同一方块改状态时保留其 NBT（[#87](https://github.com/urntt/litematica-creator/issues/87)）。
- 所有文本跟随游戏语言，不设独立语言选择或翻译模式。

- A separate 9-slot hotbar, 27-slot inventory, offhand, 4 armor slots, and a trash slot, with no crafting grid. Everything stays client-side and never writes to the real inventory.
- Vanilla-style Creative tabs, search, dragging, palette, and a virtual survival tab. While Creator is on it takes over the vanilla inventory key by default, and reopening keeps the last tab. The search box accepts `e`; the inventory key must not steal text input to close the screen.
- Pick matches item and components: a hotbar hit selects that slot, a main-inventory hit swaps into a suitable hotbar slot, otherwise one item is added. Armor and offhand are not searched. Shift-clicking the palette picks up a full stack on the cursor.
- When the main hand holds nothing placeable, the virtual offhand is used. The swap-hands key swaps the virtual hands, not the real inventory.
- In Creator mode, Litematica's tool item (`toolItem` and `toolItemComponents`) is matched against the virtual hands only, so a tool in the real hand no longer counts. The Tool HUD, corners, selection, mode switching, and modifier scrolling follow the virtual tool. A press a tool hotkey acts on no longer also places, removes, or picks, and scrolling without a modifier still switches the virtual hotbar ([#81](https://github.com/urntt/litematica-creator/issues/81)).
- With a vanilla debug stick in the virtual main hand or offhand (main hand first), attacking a projection block selects a property and using it cycles that property's value, both reversed while sneaking, without checking the real player's permissions. The stick takes priority over removal and placement, acts once per press, and does not repeat while held; real blocks and air are unaffected, and middle-click pick is unchanged. The selected property lives in the virtual stick's `debug_stick_state` component and persists with the inventory. Value changes use the normal edit path: overlaps open the Focus Switcher first, and a state change of the same block keeps its NBT ([#87](https://github.com/urntt/litematica-creator/issues/87)).
- All text follows the game language; there is no separate language selector or translation mode.

## Creator Camera / Creator 相机

- 独立的 `CreatorCameraEntity`，不注册进真实世界实体列表，不调用联网的 `LocalPlayer.tick()`，不发替身移动包。
- 地面模式复用原版式移动与真实/投影形状碰撞（投影碰撞默认开启）；双击空格飞行，飞行时穿墙。位置与飞行状态不跨会话保存。地面与飞行速度倍率默认 1.0，范围 0.1–5.0。
- 姿态、碰撞箱、潜行、匍匐、滑翔与装备使用替身自己的状态，不继承本体的鞘翅小碰撞箱或爬行状态。
- 真实玩家的主动输入与转向被隔离，但本体继续原版物理、姿态更新与服务端位置同步。替身不推动本体或其他真实实体；单人暂停时相机物理冻结。
- 攻击没有删除投影时（打空、真实方块或实体），替身在本地挥一次主手；只在新按下时挥手，长按不重复，相机关闭时不挥手，也不带动真实玩家（[#91](https://github.com/urntt/litematica-creator/issues/91)）。
- 第一人称只画虚拟手持物或半透明手臂，不画透明全身；第三人称与物品栏预览画半透明替身，包括皮肤第二层。Creator 挥手只作用于替身；鞘翅、滑翔动作与披风沿用原版纹理选择，不强制关闭 `showCape`。
- 开启或关闭 Creator 模式时默认同步开关相机。Tweakeroo Free Camera 接管与恢复时保存原相机与配置快照，临时设置 `freeCameraPlayerMovement=true`、`freeCameraPlayerInputs=false`；Creator Camera 本身不依赖 Tweakeroo。
- 世界卸载、断线、死亡或玩家实例替换以及异常初始化时恢复外部相机、输入与渲染状态；因死亡退出时保留 Focus 与 recovery。
- 投影目前只向相机提供碰撞形状；摩擦、攀爬、流体等特殊方块效果仍读取真实世界，由 [#65](https://github.com/urntt/litematica-creator/issues/65) 跟踪。

- A standalone `CreatorCameraEntity` that is never registered in the real world's entity list, never runs the networked `LocalPlayer.tick()`, and sends no stand-in movement packets.
- Ground mode reuses vanilla-style movement with real and projected shape collision (projection collision on by default). Double-tap jump to fly; flight passes through walls. Position and flight state are not saved across sessions. Ground and flight speed multipliers default to 1.0, range 0.1–5.0.
- Pose, hitbox, sneaking, crawling, gliding, and equipment are the stand-in's own state; it never inherits the real body's elytra hitbox or crawl state.
- The real player's active input and turning are isolated, while the body keeps vanilla physics, pose updates, and server position sync. The stand-in pushes neither the real body nor other real entities, and its physics freezes while singleplayer is paused.
- An attack that removes no projection (a miss, a real block, or an entity) swings the stand-in's main hand locally. Only a fresh press swings, holding does not repeat, and nothing swings while the camera is off, so the real player never animates for it ([#91](https://github.com/urntt/litematica-creator/issues/91)).
- First person draws only virtual held items or translucent arms, never a transparent full body. Third person and the inventory preview draw a translucent stand-in, including the outer skin layer. Creator swings animate only the stand-in. Elytra, gliding, and cape use vanilla texture selection without forcing `showCape` off.
- By default the camera turns on and off with Creator mode. Taking over and restoring Tweakeroo Free Camera keeps a snapshot of the previous camera and configuration and temporarily sets `freeCameraPlayerMovement=true` and `freeCameraPlayerInputs=false`; Creator Camera itself does not depend on Tweakeroo.
- World unload, disconnect, death or player replacement, and failed initialization restore the external camera, input, and render state. Leaving through death keeps Focus and recovery.
- Projections currently supply only collision shapes to the camera. Friction, climbing, fluids, and other special block effects still read the real world; [#65](https://github.com/urntt/litematica-creator/issues/65) tracks them.

## Schematic Manager / 原理图管理器

管理器在客户端世界中可独立于 Creator 模式打开，与 Litematica 主菜单、已加载原理图和 placement 列表互通，提供 Overview、Placement、保存与导出页面。需保持：即时搜索；查看项独立于 Focus 与 Selected 并明确标示二者；单 placement 的组合行不显示成重复项；所有控件有 hover 说明；双语布局与长路径截断。缩略图先关闭管理器、等待真实游戏帧完成后捕获再返回；复制 metadata 不得消费源缩略图的 `IntStream`。切换导出模式或采样 placement 时自动刷新导出信息，刷新不写文件。

默认按键见 [README](../README.md)。管理器早期的 `M+G` 与 Litematica 默认显示开关冲突，已改为 `M+J`，只迁移旧默认值，不覆盖用户自定义组合。

The manager opens in a client world independently of Creator mode and links both ways with Litematica's main menu, loaded schematics, and placement list. It provides Overview, Placement, Save, and Export pages. Keep these properties: instant search; a viewed entry independent of Focus and Selected, with both clearly marked; single-placement combined rows that do not look like duplicates; hover help on every control; bilingual layouts and truncated long paths. Thumbnails close the manager, wait for a real game frame, capture, and return; copying metadata must not consume the source thumbnail's `IntStream`. Changing the export mode or sampling placement refreshes the export details without writing a file.

Default keys are listed in the README. The manager's early `M+G` default clashed with Litematica's display toggle and became `M+J`; only the old default migrates, never a custom binding.

## Implementation Pipeline / 实现链路

1. 输入层在 Creator 模式下拦截使用与攻击，阻止真实世界交互。
2. `CreatorTargeting` 从 Creator Camera 统一执行真实世界与投影 `VoxelShape` 射线。
3. `CreatorFocus` 决定边界外操作扩展哪个 schematic；placement 负责坐标、旋转与镜像。
4. `CreatorPlacementSimulation` 在 `CreatorPlacementWorld` 中运行原版放置与一次相邻形状更新，给出待提交的全部格子。
5. `CreatorSchematicEditor` 写入已有 region 或按需创建 Creator cell，并同步全部 placements 与 metadata。
6. Litematica placement manager 重建受影响的 schematic chunks，`CreatorSchematicEditGuard` 保证重建与渲染编译在稳定快照上进行。
7. `CreatorRecoveryManager` 在客户端线程生成不可变快照，后台原子提交 generation 与 manifest。
8. `CreatorSchematicExportService` 在不可变快照上执行四种 region 规范化，真实世界补入按选定 placement 反向映射采样。
9. `CreatorSchematicBindingService` 只在原子写文件成功后更新 schematic 与全部 placements 的文件身份，重新加载时原地协调 region 拓扑。
10. 可选模组兼容注册表在启动时记录版本并验证契约（见下节）。

1. The input layer intercepts use and attack in Creator mode so nothing reaches the real world.
2. `CreatorTargeting` traces real-world and projected `VoxelShape`s from Creator Camera.
3. `CreatorFocus` decides which schematic an out-of-bounds edit extends; placements own coordinates, rotation, and mirroring.
4. `CreatorPlacementSimulation` runs vanilla placement and one adjacent shape update in `CreatorPlacementWorld`, yielding every cell to commit.
5. `CreatorSchematicEditor` writes into existing regions or creates Creator cells, then updates every placement and the metadata.
6. Litematica's placement manager rebuilds the affected schematic chunks, and `CreatorSchematicEditGuard` keeps rebuilds and render compilation on stable snapshots.
7. `CreatorRecoveryManager` takes immutable snapshots on the client thread and commits generations and manifests atomically in the background.
8. `CreatorSchematicExportService` applies the four region modes to immutable snapshots, mapping world-filled sampling back through the chosen placement.
9. `CreatorSchematicBindingService` changes the file identity of the schematic and its placements only after the atomic write succeeds, and reconciles region topology in place on reload.
10. The optional-mod registry records versions and validates contracts at startup (next section).

## Compatibility Boundaries / 兼容边界

- Creator 内部快照、recovery 与导出统一使用原版 `CompoundTag`；Litematica 运行时的 `CompoundData` 只在 `CreatorLitematicaDataAdapter` 集中转换，实体使用 Creator 自有快照。
- 原生写盘注入只重定向 Litematica 调用的 `DataFileUtils` 写盘入口，且为可选注入（`require = 0`）：入口失配时只跳过 Creator 的 metadata 规范化，不影响 Litematica 自身保存，也不会在延迟类加载时崩溃。
- 硬依赖范围由生成的 `fabric.mod.json` 声明，未审计的新版本由 Fabric 拒绝加载。同一 Minecraft 版本内的依赖兼容层与跨 Minecraft 版本的移植是两回事，不能只放宽版本声明。
- Litematica 的工具判定只在 `EntityUtils.hasToolItemInHand` 读取手中物品处改为虚拟主副手，比较逻辑仍由 Litematica 负责；Creator 模式下 `shouldPickBlock` 返回 false。
- 放置调用原版 `BlockItem.place`，因此其他模组对 `BlockItem` 的钩子也会在事务世界中运行。Litematica Easy Place 与 Tweakeroo 客户端放置协议只解析编码进点击坐标的协议值，Creator 的普通点击不受影响。放宽支撑的 `canSurvive` 与 `MultifaceBlock.canAttachTo` 注入只在关卡是 `CreatorPlacementWorld` 时生效。
- 可选模组：只有 Tweakeroo 使用私有反射桥接，契约在启动时初始化一次，失配或运行失败只停用该桥接且不刷屏；Syncmatica、Lithium、Sodium 不链接私有 API。测试组合与退化方式见[兼容说明](optional-mod-compatibility.md)。
- `main` 跟随一个目标 Minecraft 版本，旧版本状态保留在 Git 历史中。是否以及如何维护多条版本线（分支或单源码多版本工具）尚未决定。

- Creator's internal snapshots, recovery, and exports use vanilla `CompoundTag`. Litematica's runtime `CompoundData` is converted only in `CreatorLitematicaDataAdapter`, and entities use Creator's own snapshots.
- The native write injection redirects only the `DataFileUtils` call Litematica uses to write files, and it is optional (`require = 0`). If that entry point ever stops matching, only Creator's metadata normalization is skipped; Litematica still saves, and deferred class loading cannot crash.
- The generated `fabric.mod.json` declares hard-dependency ranges, and Fabric refuses unaudited newer versions. A dependency compatibility layer within one Minecraft version is different from a port to another Minecraft version; never just widen the version declaration.
- For Litematica's tool checks, Creator only swaps the stack `EntityUtils.hasToolItemInHand` reads for the virtual hand; Litematica still does the matching. `shouldPickBlock` returns false in Creator mode.
- Placement calls vanilla `BlockItem.place`, so other mods' `BlockItem` hooks run in the transaction world too. Litematica Easy Place and Tweakeroo's client placement protocol only decode values encoded in the click position, which Creator's ordinary clicks never carry. The support-relaxing injections into `canSurvive` and `MultifaceBlock.canAttachTo` act only when the level is a `CreatorPlacementWorld`.
- Optional mods: only Tweakeroo uses a private reflection bridge. Its contract initializes once at startup, and a mismatch or runtime failure disables just that bridge without log spam. Syncmatica, Lithium, and Sodium link no private API. See the compatibility notes for tested combinations and fallbacks.
- `main` follows one target Minecraft version, and older states remain in Git history. Whether and how to maintain several version lines (branches or a multi-version single-source tool) is undecided.
