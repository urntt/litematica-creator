# Litematica Creator

[中文](#中文) | [English](#english)

Litematica Creator is a Fabric client-side Litematica addon for building schematic drafts as naturally as placing blocks in Creative mode, without changing the real world.

Litematica Creator 是一个 Fabric 客户端 Litematica 附属模组，用于像在创意模式下放置方块一样自然地建造原理图草稿，而无需改变原版世界。

> Development build: the current mod and game versions are configured in [gradle.properties](gradle.properties). Back up important schematics before editing them.
>
> 开发版本：当前模组及游戏版本见 [gradle.properties](gradle.properties)。编辑重要原理图前请先备份。

## 中文

### 简介

Litematica Creator 让玩家在获取对应原材料前从零搭建或继续编辑 Litematica 原理图。

投影编辑均在客户端完成，不修改真实世界或背包，不发送对应的攻击、放置或库存操作包；真实玩家本体的正常物理与服务端同步仍然保留。

编辑结果仍是普通 Litematica 原理图，可以继续使用 Litematica 的渲染、材料列表、Verifier、保存和后续建造流程。

### 主要功能

- **直接编辑投影**：右键放置、左键删除、中键拾取投影或真实方块。
- **原版放置语义**：放置沿用原版 `BlockItem` 规则：门、床、高花一次放好两格，半砖可合并，蜡烛与海泡菜可在同一格叠加，栅栏、墙等会与相邻投影连接。
- **从零创建草稿**：在没有可编辑 placement 时自动创建草稿，也可主动新建空白原理图。
- **稀疏动态扩展**：在原理图边界外编辑时按需创建 `1x1x1` subregion，不用频繁扩张一个巨大 region，也不会声明大片隐式空气。
- **虚拟创造物品栏**：具有独立的虚拟快捷栏、背包、副手、盔甲和丢弃栏，支持创造分类、中英双语搜索和原版式 pick block；数据仅保存在客户端。
- **虚拟调试棒**：Creator 模式下在虚拟主手或副手持调试棒，即可按原版方式选择并切换投影方块的属性，不需要真实玩家的权限。
- **Litematica 工具**：Creator 模式下把 Litematica 工具物品（默认木棍）放在虚拟主手或副手即可使用选区、placement 和模式切换等工具操作。
- **Creator Focus**：编辑目标独立于 Litematica selected placement；同一 schematic 的任意 placement 都可以作为编辑入口，修改会反映到它的全部 placements。
- **Creator Camera**：纯客户端地面/飞行相机，支持双击空格飞行、穿墙、速度调节和可选投影碰撞；真实玩家本体保持独立，继续受重力、惯性和服务端校正影响。
  - 同时兼容 Tweakeroo 的 Free Camera。
- **安全恢复**：未落盘或修改后未保存的 schematic 会写入 recovery cache，可在重新进入同一世界后恢复 placement、selected placement 和 Creator focus。
- **原理图管理器**：集中管理 schematic metadata、缩略图、placements、Creator Focus、Litematica Selected Placement、文件绑定和重新加载，并可与 Litematica 原生页面直接切换。
- **可控保存与导出**：区分保存、另存并绑定和导出副本，支持原样、稀疏压缩、仅投影外边界和补入真实世界外边界四种 region 模式。
- **旋转与镜像感知**：编辑坐标和方块状态会根据 placement 与 subregion 的旋转、镜像正确换算。

### 运行要求

| 组件 | 要求 |
| --- | --- |
| Minecraft | 以 JAR 的声明为准；源码声明见 [fabric.mod.json](src/main/resources/fabric.mod.json) |
| Java / Fabric Loader | 工具链与版本来源见[开发指南](docs/development.md) |
| MaLiLib | `>=0.30.1` 且 `<0.30.3` |
| Litematica | `>=0.29.0` 且 `<0.29.2` |

可选模组兼容矩阵：

| 模组 | 已测试版本 | Creator 行为 |
| --- | --- | --- |
| Tweakeroo | `0.30.0`、`0.30.1` | 协调 Free Camera 状态；反射契约不匹配时只禁用该桥接 |
| Syncmatica | `0.3.20` | 可与 Creator/Litematica 管理页面共存；不提供实时同步 |
| Lithium | `0.26.2+mc26.3` | Creator Camera 投影碰撞使用独立的原版 `Entity.collide()` 入口 |
| Sodium | `0.9.2+mc26.3` | 不引用 Sodium 私有 API；原版渲染刷新注入保持可选 |

Tweakeroo `0.30.1` 自身要求 MaLiLib `>=0.30.2`，搭配 MaLiLib `0.30.1` 时请使用 Tweakeroo `0.30.0`。完整入口、退化路径和启动矩阵见[可选模组兼容说明](docs/optional-mod-compatibility.md)。

同一个 Creator JAR 已针对 Minecraft 26.3 下 `MaLiLib 0.30.1 + Litematica 0.29.0` 和 `MaLiLib 0.30.2 + Litematica 0.29.1` 两组硬依赖完成构建与测试。未经审计的更高版本会由 Fabric 拒绝加载，而不是在编辑时延迟崩溃。

### 安装

1. 安装该 JAR 支持的 Minecraft、Fabric Loader、MaLiLib 和 Litematica。
2. 将 Litematica Creator JAR 放入客户端的 `mods` 文件夹。
3. 启动游戏，并在 MaLiLib 配置界面确认 `Litematica Creator` 已加载。

这是纯客户端模组，服务器不需要安装。

### 快速上手

1. 按 `Y` 开启 Creator 模式。默认会同时开启 Creator Camera。
2. 按原版物品栏键（默认为 `E`）或 `M + E` 打开 Creator 物品栏，选择要放置的方块。
3. 右键放置投影方块，左键删除，中键拾取；数字键和滚轮切换虚拟快捷栏。
4. 在空气中右键时，投影会按配置的固定距离放置；编辑距离和连续放置/删除间隔可以在设置中调整。
5. 对已有投影操作会切换 Creator focus；发生重叠或需要主动切换时，使用 `M + F` 打开 Focus Switcher。
6. 按 `M + J` 打开 Creator 原理图管理器，检查 metadata 和导出信息，然后选择保存、另存并绑定或导出副本；也可以从管理器切换到 Litematica 原生页面。

默认快捷键：

| 操作 | 默认按键 |
| --- | --- |
| 切换 Creator 模式 | `Y` |
| 切换 Creator Camera | `M + B` |
| 打开 Creator 物品栏 | `M + E` |
| 打开 Creator 设置 | `M + K` |
| 结束编辑并清空 focus | `M + Left Shift + S` |
| 卸载 focus 对应的原理图及其 placements | `M + Left Shift + D` |
| 打开 Focus Switcher | `M + F` |
| 打开 Creator 原理图管理器 | `M + J` |
| 新建空白原理图 | `M + N` |

“结束编辑”不会保存、导出或卸载原理图；“卸载当前原理图”也不会删除磁盘上的 `.litematic` 文件。“保存”覆盖当前绑定文件，“另存并绑定”让当前内存对象继续编辑新文件，“导出副本”则完全不改变当前绑定和编辑状态。

### 恢复缓存

Recovery cache 位于 `config/litematica-creator/recovery/`，用于恢复：

- 尚未关联文件的 schematic。
- 已关联文件、但修改后尚未保存的 schematic。

正常退出、断线、切换世界和异常终止后均可使用最近一次完整缓存。恢复后 Creator 模式保持关闭，但会恢复退出前的 focus。Recovery cache 不是正式版本管理或手动保存的替代品。

### 当前边界

- 放置只支持方块物品，物品自带的方块实体数据（命名、旗帜图案、告示牌文字等）暂不写入；放置不连锁更新，只让直接相邻的投影方块调整一次形状。
- 尚无通用 BlockState/NBT 编辑器，也不支持与投影容器、告示牌等交互。
- 不提供撤销/重做和实时多人同步；尚无多方块批量操作。多人协作计划通过 Syncmatica 共享已保存的原理图。
- Creator Camera 的普通方块式投影预览模式尚未实现。
- 项目依赖 Litematica 内部实现，目标版本变化时可能需要适配。

计划中的功能与已知问题见 [GitHub Issues](https://github.com/urntt/litematica-creator/issues)。

### 构建

构建命令与工具链见[开发指南](docs/development.md)，云端接入见[云端开发](docs/cloud-development.md)，版本号与 GitHub Releases 流程见[发布指南](docs/releasing.md)。依赖从固定官方提交自动获取，不再需要 sibling 目录；push/PR CI 覆盖两组硬依赖构建、单元测试、产物审计及客户端 GameTest。

### 文档

面向用户：

- [可选模组兼容说明](docs/optional-mod-compatibility.md)
- [变更记录](CHANGELOG.md)

面向开发者：

- [架构与核心语义](docs/architecture.md)
- [开发指南](docs/development.md)
- [云端开发](docs/cloud-development.md)
- [版本与发布](docs/releasing.md)
- [工程与协作约定](AGENTS.md)
- [任务与问题（GitHub Issues）](https://github.com/urntt/litematica-creator/issues)
- [历史文档](docs/history/README.md)

## English

### Overview

Litematica Creator makes it possible for players to draft a new Litematica schematic or continue editing an existing one in-world, even before actually having the materials.

Projection edits stay client-side: they do not change the real world or inventory, or send corresponding attack, placement, or inventory-operation packets. Normal physics and server synchronization for the real player continue.

The result remains a normal Litematica schematic and can use Litematica's rendering, material list, Verifier, save, and later construction workflows.

### Features

- **Direct projection editing**: right-click to place, left-click to remove, and middle-click to pick projected or real blocks.
- **Vanilla placement rules**: placement follows vanilla `BlockItem` rules. Doors, beds, and tall plants place both cells at once, slabs merge, candles and sea pickles stack in one cell, and fences, walls, and similar blocks connect to adjacent projections.
- **Draft from scratch**: automatically create a draft when no placement can be edited, or explicitly create an empty schematic.
- **Sparse expansion**: create `1x1x1` subregions only where edits occur outside existing bounds, without repeatedly resizing one huge region or declaring large volumes of implicit air.
- **Virtual Creative inventory**: separate virtual hotbar, inventory, offhand, armor, and trash slots with Creative tabs, search, and vanilla-style pick block. All data stays client-side.
- **Virtual debug stick**: in Creator mode, hold a debug stick in the virtual main hand or offhand to select and cycle projection block properties the vanilla way, without the real player's permissions.
- **Litematica tool**: in Creator mode, hold Litematica's tool item (a stick by default) in the virtual main hand or offhand to use selection, placement, and mode-switching tool actions.
- **Creator Focus**: the edit target is independent from Litematica's selected placement. Any placement of the same schematic can be used as an edit entry point, and every placement reflects the same schematic changes.
- **Creator Camera**: a client-only ground/flight camera with double-tap flight, noclip while flying, speed controls, and optional projection collision. The real player remains behind and continues normal gravity, momentum, and server corrections.
  - Also compatible with Free Camera from Tweakeroo.
- **Recovery cache**: schematics without a backing file, and modified file-backed schematics, can be restored with their placements, Litematica selection, and Creator focus.
- **Schematic manager**: manage metadata, thumbnails, placements, Creator Focus, Litematica Selected Placement, file binding, and reloads from one screen, with direct navigation to Litematica's native screens.
- **Controlled save and export**: distinguish Save, Save As and Bind, and Export Copy, with raw, sparse compact, projection bounds, and world-filled bounds region modes.
- **Transform-aware editing**: placement and subregion rotation and mirroring are respected for coordinates and block states.

### Requirements

| Component | Requirement |
| --- | --- |
| Minecraft | Follow the JAR metadata; source declarations are in [fabric.mod.json](src/main/resources/fabric.mod.json) |
| Java / Fabric Loader | See the [development guide](docs/development.md) for toolchain and version sources |
| MaLiLib | `>=0.30.1` and `<0.30.3` |
| Litematica | `>=0.29.0` and `<0.29.2` |

Optional compatibility matrix:

| Mod | Tested versions | Creator behavior |
| --- | --- | --- |
| Tweakeroo | `0.30.0`, `0.30.1` | Coordinates Free Camera state; disables only this bridge when its reflection contract does not match |
| Syncmatica | `0.3.20` | Coexists with the Creator/Litematica management screens; real-time synchronization is not provided |
| Lithium | `0.26.2+mc26.3` | Creator Camera projection collision uses an independent vanilla `Entity.collide()` entry point |
| Sodium | `0.9.2+mc26.3` | Uses no private Sodium API; the vanilla renderer refresh injection remains optional |

Tweakeroo `0.30.1` itself requires MaLiLib `>=0.30.2`; use Tweakeroo `0.30.0` with MaLiLib `0.30.1`. See [optional mod compatibility](docs/optional-mod-compatibility.md) for entry points, fallbacks, and the startup matrix.

The same Creator JAR is built and tested on Minecraft 26.3 against both `MaLiLib 0.30.1 + Litematica 0.29.0` and `MaLiLib 0.30.2 + Litematica 0.29.1`. Fabric rejects unaudited newer versions at load time instead of allowing a delayed editing crash.

### Installation

1. Install the Minecraft version supported by the JAR, Fabric Loader, MaLiLib, and Litematica.
2. Put the Litematica Creator JAR in the client's `mods` directory.
3. Start the game and confirm that `Litematica Creator` appears in the MaLiLib configuration screen.

This is a client-only mod; the server does not need it.

### Quick Start

1. Press `Y` to enable Creator mode. Creator Camera is enabled with it by default.
2. Press the vanilla inventory key or `M + E` to open the Creator inventory and choose a block.
3. Right-click to place a projected block, left-click to remove one, and middle-click to pick one. Number keys and the mouse wheel select the virtual hotbar slot.
4. Right-clicking air uses the configured fixed placement distance. Edit range and continuous placement/removal intervals are configurable.
5. Editing an existing projection changes Creator focus. Use `M + F` when projections overlap or when you want to select a focus explicitly.
6. Press `M + J` to open the Creator schematic manager, review metadata and export details, then Save, Save As and Bind, or Export Copy. The manager also links directly to Litematica's native screens.

Default keybinds:

| Action | Default key |
| --- | --- |
| Toggle Creator mode | `Y` |
| Toggle Creator Camera | `M + B` |
| Open Creator inventory | `M + E` |
| Open Creator settings | `M + K` |
| Finish editing and clear focus | `M + Left Shift + S` |
| Unload the focused schematic and its placements | `M + Left Shift + D` |
| Open Focus Switcher | `M + F` |
| Open Creator schematic manager | `M + J` |
| Create an empty schematic | `M + N` |

"Finish editing" does not save, export, or unload anything. "Unload current schematic" never deletes the original `.litematic` file. Save overwrites the bound file, Save As and Bind keeps editing the same in-memory object under a new file, and Export Copy leaves the current binding and edit state untouched.

### Recovery Cache

Recovery data is stored in `config/litematica-creator/recovery/` for:

- Schematics without a backing file.
- File-backed schematics modified since their last save.

The latest complete cache can be recovered after a normal exit, disconnect, world switch, or abnormal process termination. Creator mode remains disabled after recovery, while the previous focus is restored. Recovery is not a replacement for normal saves or version control.

### Current Limitations

- Only block items can be placed, and block entity data carried by the item (names, banner patterns, sign text, and so on) is not written yet. Placement does not chain updates; only directly adjacent projection blocks adjust their shape once.
- There is no general BlockState/NBT editor or virtual interaction for containers, signs, and similar projected blocks.
- Undo/redo and real-time multiplayer synchronization are not provided, and bulk placement is not available yet. Multiplayer sharing is planned through Syncmatica for saved schematics.
- Normal opaque block rendering for Creator Camera preview mode is not implemented yet.
- The addon relies on Litematica internals and may require adaptation for other target versions.

Planned features and known issues are tracked in [GitHub Issues](https://github.com/urntt/litematica-creator/issues).

### Building

See the [development guide](docs/development.md) for builds/toolchains, [cloud development](docs/cloud-development.md) for onboarding, and the [release guide](docs/releasing.md) for versioning and GitHub Releases. Pinned official sources replace sibling dependencies. Push/PR CI covers both hard-dependency profiles with builds, unit tests, artifact audits, and client GameTests.

### Documentation

For users:

- [Optional mod compatibility](docs/optional-mod-compatibility.md)
- [Changelog](CHANGELOG.md)

For developers:

- [Architecture and core semantics](docs/architecture.md)
- [Development guide](docs/development.md)
- [Cloud development](docs/cloud-development.md)
- [Version and release policy](docs/releasing.md)
- [Engineering and collaboration rules](AGENTS.md)
- [Tasks and known issues (GitHub Issues)](https://github.com/urntt/litematica-creator/issues)
- [Historical documents, mostly Chinese](docs/history/README.md)

## License

Licensed under [MIT](LICENSE). Third-party dependencies retain their own licenses; see the [release guide](docs/releasing.md#license--许可) for historical build status.

项目采用 [MIT](LICENSE)。第三方依赖保留各自许可；历史开发包状态见[发布指南](docs/releasing.md#license--许可)。
