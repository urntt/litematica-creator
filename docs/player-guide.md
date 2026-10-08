# 玩家指南 / Player Guide

[中文](#中文) | [English](#english)

本指南按任务介绍 Litematica Creator 的玩法。安装、运行要求和完整快捷键表见 [README](../README.md)，各项行为的精确定义见[架构文档](architecture.md)。

This guide walks through Litematica Creator task by task. See the [README](../README.md) for installation, requirements, and the full keybind table, and the [architecture document](architecture.md) for precise behavior.

## 中文

### 核心概念

- **Creator 模式**：按 `Y` 开关。开启后，使用、攻击和中键用来编辑投影：只修改内存中的原理图，不改变真实世界和背包，也不向服务器发送放置、攻击或库存操作。关闭后按键恢复原版行为。
- **原理图与 placement**：原理图是内容本身；placement 是它在世界中的一次摆放，带有位置、旋转和镜像。同一原理图可以有多个 placement，通过其中任意一个编辑，所有 placement 都会一起变化。
- **Creator Focus**：当前正在编辑的原理图。对某个投影操作时，Focus 自动切换到它；在所有投影之外操作时，扩展当前 Focus 的原理图，没有 Focus 时新建草稿。Focus 与 Litematica 的 Selected Placement 互不影响。
- **草稿与 cell**：草稿就是还没有保存成文件的原理图。在原理图已有区域之外放置时，Creator 只为那一格添加一个 `1x1x1` 的小区域（cell），不会扩大整个区域，也不会把周围的真实建筑当成投影里的空气。保存时如何整理这些 cell，见下文“导出区域模式”。
- **未保存与恢复缓存**：修改后没有保存的原理图（包括所有草稿）会自动写入恢复缓存，退出、断线或崩溃后重新进入同一世界即可恢复。恢复缓存不能代替保存。

### 教程一：从零搭建并保存

1. 按 `Y` 开启 Creator 模式，Creator Camera 默认一起开启。此时你操作的是一个替身，真实玩家留在原地。双击空格飞行，飞行时可以穿过方块。
2. 按物品栏键（默认 `E`）或 `M + E` 打开 Creator 物品栏，从创造分类中挑选方块，或直接搜索（中英文名称都可以），把它放进虚拟快捷栏。
3. 右键放置，左键删除，中键把投影或真实方块拾取到虚拟快捷栏；长按可以连续放置或删除。对着空气右键时，方块放在视线前方的固定距离处。
4. 第一次放置时，如果没有可以编辑的投影，Creator 会自动新建草稿并设为 Focus。也可以按 `M + N` 在当前位置新建一个空白原理图。
5. 按 `M + J` 打开 Creator 原理图管理器：
   - 在“概览”页填写内部名称、作者和描述后点击“应用”，并可点击“从当前画面捕获”生成缩略图。
   - 在“保存与导出”页选择区域模式，填写输出目录和文件名，点击“另存并绑定”。之后原理图就绑定到这个文件，再修改时点击“保存”即可覆盖它。
6. 搭建完成后，按 `M + Left Shift + S` 结束编辑（只清空 Focus，不保存也不卸载），或按 `Y` 退出 Creator 模式。

### 教程二：编辑已有原理图

1. 像平常一样用 Litematica 加载原理图并放置投影。
2. 开启 Creator 模式，直接在投影上放置或删除方块。第一次有效修改时，Focus 会切换到这个原理图，并显示提示。
3. 在投影范围之外继续放置，会扩展当前 Focus 的原理图，不要求与原投影相连。
4. 多个投影在同一位置重叠时，Creator 会打开 Focus Switcher，让你选择要编辑哪一个；也可以随时按 `M + F` 主动切换或清空 Focus。
5. 旋转或镜像过的 placement 同样可以编辑，方块朝向会自动换算。
6. 保存方式：
   - **保存**：覆盖当前绑定的文件。
   - **另存并绑定**：写成新文件，之后继续编辑新文件，原文件保持不变。
   - **导出副本**：只写出一个副本，当前绑定和未保存状态都不变，适合做备份或分享某个阶段的版本。

### 教程三：照着草稿在生存中建造

1. 保存原理图后，按 `Y` 退出 Creator 模式。
2. 之后完全按 Litematica 自己的流程建造：材料列表、Verifier、Easy Place 等都可以照常使用，参见 [Litematica](https://github.com/sakura-ryoko/litematica) 的说明。Creator 不参与真实放置。

### 功能说明

#### Creator Camera

- 有地面和飞行两种状态：地面状态有碰撞，默认也会与可见的投影方块碰撞；双击空格切换飞行，飞行时可以穿过方块。速度倍率与投影碰撞在设置中调整。
- 按 `M + B` 可以单独开关相机。设置中可以决定开关 Creator 模式时是否同时开关相机。
- 真实玩家留在原地，仍然受重力、怪物和服务器影响。在生存世界里请先把本体放在安全的地方；真实玩家死亡时，Creator 模式和相机会自动关闭。
- 装有 Tweakeroo 时，开启 Creator Camera 会接管正在使用的 Free Camera，关闭后恢复原状。Creator Camera 本身不需要 Tweakeroo。

#### Creator 物品栏

- 包括虚拟快捷栏、背包、副手、盔甲槽和丢弃栏，与真实背包完全分开，只保存在本机。
- 支持创造分类和中英文搜索；在分类物品中 Shift + 单击可以直接拿起一整组。
- 中键拾取时，快捷栏里已有的物品直接选中，背包里有的换到快捷栏，都没有时放入一个。
- 主手没有可放置的方块时会使用副手；换手键交换的是虚拟主副手。

#### 放置规则

- 沿用原版方块物品的规则：门和床一次放两格，半砖可以合并，朝向随点击面和视角变化。
- 火把、灯笼、按钮等不需要投影支撑，海草与海带不需要水。
- 放置后只让直接相邻的投影方块调整一次形状（例如栅栏连接），不会连锁更新，也没有红石或流体模拟。
- 目标格被实体（包括你的替身）占住时不能放置；需要时可以在设置中开启“Creator 相机忽略实体放置碰撞”。
- 物品自带的方块实体数据（名称、旗帜图案、潜影盒内容等）会一并保存进投影。

#### 调试棒与 Litematica 工具

- **调试棒**：放在虚拟主手或副手，左键选择投影方块的属性，右键切换属性值，潜行时反向，与原版一致，不需要真实玩家的权限。
- **Litematica 工具**（默认是木棍）：放在虚拟主手或副手即可使用选区、placement 和模式切换等工具操作。真实手里的工具在 Creator 模式下不再生效。

#### 用原版界面编辑投影方块

- 在投影的告示牌、悬挂告示牌、命令方块、箱子、陷阱箱、木桶、潜影盒、漏斗、发射器和投掷器上右键，会打开对应的原版界面。
- 容器界面下方显示的是 Creator 虚拟物品栏，可以把物品放进投影容器里，不会动到真实背包。
- 告示牌和容器在关闭界面时保存；命令方块在点击“完成”时保存，按 Esc 取消。
- 手里拿着物品时潜行右键，会改为对着这个方块放置；空手潜行仍然打开界面。

#### 结束编辑与卸载

- **结束编辑**（`M + Left Shift + S`）：只清空 Focus，原理图仍保持加载，未保存的修改也还在。
- **卸载**（`M + Left Shift + D`）：立即卸载 Focus 的原理图及其全部 placement，并清除它的恢复缓存；未保存的修改会丢失，热键不会再次确认。磁盘上的 `.litematic` 文件不会被删除。管理器中的“卸载”按钮需要点击两次确认。

#### 导出区域模式

在管理器的“保存与导出”页或设置中选择，两处是同一项设置。区域模式只影响保存和导出的文件，不影响正在编辑的原理图和恢复缓存。

| 模式 | 适用情况 |
| --- | --- |
| 稀疏压缩（默认） | 大多数情况。保留原有区域，把 Creator 添加的相邻 cell 合并成长方体，不额外添加空气。 |
| 原样保存 | 需要原封不动保留编辑时的区域结构，包括每个 `1x1x1` cell。 |
| 按外边界：仅投影 | 需要只有一个区域的文件。投影没有覆盖的位置会写成空气，照着建造时这些位置会被当成需要清空。 |
| 按外边界：补入真实世界 | 在已有建筑上扩建，并希望把周围的真实方块一起存进文件。需要选择用于采样的 placement，相关区块必须已加载。 |

#### 恢复缓存

- 位置：`config/litematica-creator/recovery/`。
- 重新进入同一世界后自动恢复，并显示恢复了多少原理图和 placement。恢复后 Focus 会还原，但 Creator 模式保持关闭，需要再按 `Y`。
- 只有主动卸载原理图，或在 Litematica 中移除它的全部 placement，才会清除它的缓存；切换维度、断线、退出世界和关闭游戏都不会清除。

### 常见问题

**右键没有放置方块？**

- 确认 Creator 模式已开启（按 `Y`）。
- 目标格被实体占住，包括你的替身自己时，会静默阻断；换个角度或位置再试。
- 目标格已有不可替换的投影方块。
- 超出了编辑距离，可在设置中调整。
- 手里拿的不是方块物品时，会提示“请先在 Creator 虚拟快捷栏中选择一个方块”。非方块物品（例如桶、刷怪蛋）暂不支持放置。

**游戏启动时 Fabric 拒绝加载？**

MaLiLib 或 Litematica 的版本不在支持范围内。请按 [README 的运行要求](../README.md#运行要求)安装匹配的版本。

**草稿不见了？**

重新进入原来的世界，恢复缓存会自动加载未保存的原理图。主动卸载过、或全部 placement 都被移除的原理图不会再恢复。

**在服务器上使用安全吗？**

投影编辑不向服务器发送放置、攻击或库存操作，真实世界不会改变；真实玩家本体仍照常与服务器同步。命令方块界面的命令补全会像聊天栏一样向服务器请求补全建议。服务器是否允许使用客户端模组，以服务器规则为准。

**能撤销吗？**

目前没有撤销和重做。建议经常保存，或在大改之前用“导出副本”留一份备份。

**能多人一起编辑吗？**

没有实时同步。保存后的原理图可以通过 Syncmatica 等方式分享。

**和其他模组一起用会冲突吗？**

已测试的组合与已知限制见[可选模组兼容说明](optional-mod-compatibility.md)。Creator 模式开启时，Litematica Easy Place 和 Tweakeroo 的放置功能不会改变 Creator 的放置结果。

### 更多资料

- [README](../README.md)：安装、运行要求、快捷键和当前边界。
- [可选模组兼容说明](optional-mod-compatibility.md)
- [变更记录](../CHANGELOG.md)
- [GitHub Issues](https://github.com/urntt/litematica-creator/issues)：反馈问题和查看计划中的功能。

## English

### Core Concepts

- **Creator mode**: toggle it with `Y`. While it is on, use, attack, and middle-click edit projections: only the schematic in memory changes. The real world and inventory stay as they are, and no placement, attack, or inventory packets go to the server. Turning it off gives the keys their vanilla behavior back.
- **Schematics and placements**: a schematic is the content itself; a placement is one copy of it in the world, with a position, rotation, and mirror. One schematic can have several placements, and editing through any of them changes all of them.
- **Creator Focus**: the schematic you are editing. Editing a projection moves Focus to it. Editing outside every projection extends the focused schematic, or creates a new draft when nothing is focused. Focus and Litematica's Selected Placement are independent.
- **Drafts and cells**: a draft is a schematic that has not been saved to a file yet. When you place outside a schematic's existing regions, Creator adds a `1x1x1` region (a cell) for that block only. It does not grow the whole region, and it never treats the real buildings around it as projected air. How cells are arranged on save is covered under "Export region modes" below.
- **Unsaved changes and recovery**: schematics with unsaved changes, including every draft, are written to the recovery cache automatically. After quitting, disconnecting, or a crash, rejoin the same world to get them back. Recovery does not replace saving.

### Tutorial 1: Build from Scratch and Save

1. Press `Y` to enable Creator mode; Creator Camera turns on with it by default. You now control a stand-in while the real player stays where it is. Double-tap Space to fly, and fly through blocks.
2. Press the inventory key (`E` by default) or `M + E` to open the Creator inventory. Pick a block from the Creative tabs or search for it by its Chinese or English name, and put it in the virtual hotbar.
3. Right-click to place, left-click to remove, and middle-click to pick a projected or real block into the virtual hotbar. Hold the button to place or remove continuously. Right-clicking air places the block at a fixed distance in front of you.
4. If nothing editable is there on your first placement, Creator creates a draft and focuses it. You can also press `M + N` to create an empty schematic where you are.
5. Press `M + J` to open the Creator schematic manager:
   - On the "Overview" tab, fill in the internal name, author, and description, click "Apply", and click "Capture current view" for a thumbnail.
   - On the "Save & Export" tab, choose a region mode, enter the output directory and file name, and click "Save As & Bind". The schematic is now bound to that file; after further edits, click "Save" to overwrite it.
6. When you are done, press `M + Left Shift + S` to finish editing (this only clears Focus; it neither saves nor unloads), or press `Y` to leave Creator mode.

### Tutorial 2: Edit an Existing Schematic

1. Load the schematic and place it with Litematica as usual.
2. Turn on Creator mode and place or remove blocks on the projection. Focus moves to that schematic on the first successful edit, with a message.
3. Placing beyond the projection keeps extending the focused schematic; the new blocks do not need to touch it.
4. Where several projections overlap, Creator opens the Focus Switcher so you can choose which one to edit. Press `M + F` at any time to switch or clear Focus yourself.
5. Rotated or mirrored placements can be edited too; block orientation is converted for you.
6. Ways to save:
   - **Save** overwrites the bound file.
   - **Save As & Bind** writes a new file and keeps editing that one; the old file stays as it was.
   - **Export Copy** only writes a copy. The binding and unsaved state stay unchanged, which suits backups or sharing a snapshot.

### Tutorial 3: Build the Draft in Survival

1. Save the schematic, then press `Y` to leave Creator mode.
2. From here on, build with Litematica's own workflow: material list, Verifier, Easy Place, and so on, as described by [Litematica](https://github.com/sakura-ryoko/litematica). Creator takes no part in real placement.

### Features

#### Creator Camera

- It has a ground state and a flying state. On the ground it collides with blocks, and by default with visible projected blocks too. Double-tap Space to fly; flying passes through blocks. Speed multipliers and projection collision are in the settings.
- Press `M + B` to toggle the camera on its own. The settings decide whether toggling Creator mode also toggles the camera.
- The real player stays where it is and is still subject to gravity, mobs, and the server. In survival worlds, park your real player somewhere safe first; if it dies, Creator mode and the camera turn off.
- With Tweakeroo installed, Creator Camera takes over an active Free Camera and restores it when turned off. Creator Camera does not need Tweakeroo.

#### Creator Inventory

- It has a virtual hotbar, inventory, offhand, armor slots, and a trash slot. It is completely separate from the real inventory and is saved only on your computer.
- It supports Creative tabs and search in Chinese and English. Shift-click an item in a tab to pick up a full stack.
- Middle-click selects the item if it is already in the hotbar, swaps it in from the inventory, and otherwise adds one.
- With no placeable block in the main hand, the offhand is used; the swap-hands key swaps the virtual hands.

#### Placement Rules

- Vanilla block item rules apply: doors and beds place both cells, slabs merge, and orientation follows the clicked face and your view.
- Torches, lanterns, buttons, and similar blocks need no projected support, and seagrass and kelp need no water.
- After a placement, only directly adjacent projected blocks update their shape once (fences connect, for example). Nothing chains, and there is no redstone or fluid simulation.
- A cell occupied by an entity, including your own stand-in, cannot be placed into. Turn on "Ignore Creator Camera Entity Placement Collision" in the settings if you need to.
- Block entity data on the item, such as names, banner patterns, and shulker box contents, is saved into the projection too.

#### Debug Stick and Litematica Tool

- **Debug stick**: hold it in the virtual main hand or offhand. Left-click selects a projected block's property, right-click cycles its value, and sneaking reverses either, just like vanilla, without the real player's permissions.
- **Litematica tool** (a stick by default): hold it in the virtual main hand or offhand to use selection, placement, and mode-switching tool actions. A tool in your real hand no longer counts in Creator mode.

#### Edit Projected Blocks in Vanilla Screens

- Right-clicking a projected sign, hanging sign, command block, chest, trapped chest, barrel, shulker box, hopper, dispenser, or dropper opens its vanilla screen.
- The bottom of a container screen shows the Creator virtual inventory, so you can put items into the projected container without touching your real inventory.
- Signs and containers save when the screen closes; a command block saves on "Done" and cancels on Esc.
- Sneaking with an item in hand places against the block instead; sneaking empty-handed still opens the screen.

#### Finish Editing and Unload

- **Finish editing** (`M + Left Shift + S`) only clears Focus. The schematic stays loaded, and unsaved changes are kept.
- **Unload** (`M + Left Shift + D`) immediately unloads the focused schematic with all its placements and clears its recovery cache. Unsaved changes are lost, and the hotkey does not ask again. The `.litematic` file on disk is never deleted. The manager's "Unload" button needs a second click to confirm.

#### Export Region Modes

Choose the mode on the manager's "Save & Export" tab or in the settings; both change the same setting. The mode only affects saved and exported files, never the schematic being edited or the recovery cache.

| Mode | When to use it |
| --- | --- |
| Sparse Compact (default) | Most of the time. Keeps the original regions and merges adjacent Creator cells into cuboids without adding air. |
| Keep Regions Unchanged | You need the editing-time region layout exactly, including every `1x1x1` cell. |
| Bounding Box: Projection Only | You need a file with a single region. Positions the projection does not cover become air, so building from it treats them as blocks to clear. |
| Bounding Box: Include World | You are extending an existing building and want the surrounding real blocks saved too. Choose the placement to sample through; the chunks involved must be loaded. |

#### Recovery Cache

- Location: `config/litematica-creator/recovery/`.
- Rejoining the same world restores it automatically, with a message saying how many schematics and placements came back. Focus is restored, but Creator mode stays off until you press `Y`.
- Only unloading a schematic yourself, or removing all of its placements in Litematica, clears its cache. Changing dimensions, disconnecting, leaving the world, and closing the game never do.

### FAQ

**Right-click does not place anything?**

- Check that Creator mode is on (press `Y`).
- A cell occupied by an entity, including your stand-in, blocks placement silently; try another angle or position.
- The target cell already holds a projected block that cannot be replaced.
- The target is beyond the edit range, which you can change in the settings.
- Holding something that is not a block shows "Select a block in the virtual Creator hotbar first". Non-block items, such as buckets or spawn eggs, cannot be placed yet.

**Fabric refuses to load the game?**

Your MaLiLib or Litematica version is outside the supported range. Install matching versions as listed in the [README requirements](../README.md#requirements).

**My draft is gone?**

Rejoin the world you were in; the recovery cache loads unsaved schematics automatically. Schematics you unloaded, or whose placements were all removed, do not come back.

**Is it safe to use on a server?**

Projection edits send no placement, attack, or inventory packets, and the real world does not change; the real player still synchronizes with the server as usual. Command suggestions in the command block screen ask the server just like the chat command line. Whether client mods are allowed is up to each server's rules.

**Can I undo?**

There is no undo or redo yet. Save often, or use "Export Copy" before big changes.

**Can several players edit together?**

There is no real-time synchronization. Saved schematics can be shared through Syncmatica or other means.

**Does it conflict with other mods?**

See [optional mod compatibility](optional-mod-compatibility.md) for tested combinations and known limits. While Creator mode is on, Litematica Easy Place and Tweakeroo's placement features do not change Creator's placement results.

### More Resources

- [README](../README.md): installation, requirements, keybinds, and current limitations.
- [Optional mod compatibility](optional-mod-compatibility.md)
- [Changelog](../CHANGELOG.md)
- [GitHub Issues](https://github.com/urntt/litematica-creator/issues): report problems and see planned features.
