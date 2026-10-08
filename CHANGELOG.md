# Changelog / 变更记录

Notable user-visible changes are recorded here in English and Chinese, following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project's [version and release policy](docs/releasing.md).

本文件按 Keep a Changelog 与项目版本/发布约定记录中英双语的用户可见变化。既有开发历史见[完成记录](docs/history/completed-tasks.md)；尚未核实的发布历史不补造版本节。

## [Unreleased]

### Added / 新增

- Creator placement now follows vanilla block item rules: doors, beds, and tall plants place both cells, slabs merge, candles and sea pickles stack in one cell, facing follows the clicked face and view, and adjacent projected fences, walls, and similar blocks connect. The whole placement is checked and committed together, so no half structure is left behind. / Creator 放置改为沿用原版方块物品规则：门、床、高花放置两格，半砖合并，蜡烛与海泡菜同格叠加，朝向随点击面与视角，相邻投影中的栅栏、墙等会连接；整次放置一起校验、一起提交，不会只留下半个结构。
- A debug stick in the Creator virtual main hand or offhand now edits projection block states with vanilla semantics: attack selects a property, use cycles its value, and sneaking reverses either; the selection is saved on the virtual stick. / Creator 虚拟主手或副手中的调试棒可按原版语义编辑投影方块状态：攻击选择属性、使用切换属性值、潜行反向，所选属性保存在虚拟调试棒上。
- Under Creator Camera, an attack that removes no projection now swings the camera stand-in's hand once, like a vanilla attack click; nothing is sent and the real player does not animate. / Creator Camera 下攻击未删除投影时，替身会像原版攻击一样挥一次手；不发送任何数据包，真实玩家也不播放动作。
- In Creator mode, Litematica's tool item works from the Creator virtual main hand or offhand: the Tool HUD, corner and selection clicks, mode switching, and modifier scrolling use it, and a tool click no longer also edits the projection. A tool in the real hand no longer counts while Creator mode is on. / Creator 模式下 Litematica 工具物品可从虚拟主手或副手生效：Tool HUD、角点与选择点击、模式切换及带修饰键的滚轮均识别虚拟工具，工具点击不再同时编辑投影；Creator 模式开启时真实手中的工具不再生效。
- Add a GitHub release workflow that publishes only a JAR validated at the release commit against both dependency profiles, with a default dry-run mode for manual runs. / 新增 GitHub Release 工作流：只发布在发布提交上经两组依赖验证的同一 JAR，手动运行默认 dry run。
- Add push/PR Linux CI for both hard-dependency profiles, packaged client GameTests, and production JAR audits. / 新增两组硬依赖的 push/PR Linux CI、打包客户端 GameTest 与正式 JAR 审计。
- Publish repository/support links in mod metadata and document cloud development setup. / 模组 metadata 增加仓库/问题入口，并提供云端开发准备文档。

### Fixed / 修复

- Placing onto a real block now decides where the block goes from that real block, as vanilla does: using a block on real short grass replaces the grass cell instead of placing above it, and the placement state is computed for the cell actually written. / 在真实方块上放置时，改为像原版一样依据该真实方块决定落点：对真实矮草放置会替换草所在格而不是放到其上方，放置状态也按实际写入的格子计算。
- In Creator mode, the middle click no longer also runs Litematica's schematic pick block, which could change the real inventory; it now only picks into the Creator inventory. / Creator 模式下中键不再同时触发 Litematica 的原理图 pick block（它可能改动真实背包），只向 Creator 物品栏拾取。
- Removing or replacing a projected block now also clears its block entity data and scheduled ticks, so saved schematics no longer keep stale data such as old chest contents, and emptied Creator cells are removed. / 删除或替换投影方块时会一并清除其方块实体数据与计划刻，保存的原理图不再残留旧箱子内容等过期数据，清空的 Creator cell 也会被正确移除。

### Changed / 变更

- The mod version now carries the target Minecraft version as SemVer build metadata (`0.1.0-dev+26.3`), and JARs are named `litematica-creator-fabric-<version>.jar`. / 模组版本以 SemVer build metadata 标注目标 Minecraft 版本（`0.1.0-dev+26.3`），JAR 命名为 `litematica-creator-fabric-<version>.jar`。
- Target Minecraft 26.3 with Fabric Loader 0.19.5. Creator now requires MaLiLib `>=0.30.1 <0.30.3` and Litematica `>=0.29.0 <0.29.2`; 26.2 is no longer supported by this line. / 目标版本改为 Minecraft 26.3 与 Fabric Loader 0.19.5；Creator 现需要 MaLiLib `>=0.30.1 <0.30.3` 与 Litematica `>=0.29.0 <0.29.2`，此版本线不再支持 26.2。
- Update the optional-mod audit to the 26.3 releases of Tweakeroo, Syncmatica, Lithium, and Sodium; older releases are reported as outside the audited range and the Tweakeroo camera bridge stays disabled for them. / 可选模组审计更新为 Tweakeroo、Syncmatica、Lithium 与 Sodium 的 26.3 版本；旧版本会提示超出审计范围，Tweakeroo 相机桥接对其保持禁用。
- Creator placement and deletion swings now use the held item's 26.3 interaction and attack animations. / Creator 放置与删除的挥手动作改用手持物品在 26.3 中的交互与攻击动画。
- Prepare both dependency profiles from pinned official upstream commits instead of manual sibling sources or local Maven. / 两组硬依赖改从固定官方提交准备，不再依赖手工 sibling 源码或本地 Maven。
- Pin Loom and centralize Java, game, loader, and compatibility resource values; enable Linux wrapper execution and verify the Gradle distribution checksum. / 固定 Loom，集中 Java、游戏、Loader 与兼容范围资源生成，配置 Linux wrapper 与 Gradle 下载校验。
- Use stable archive ordering and a JAR version matching mod metadata without a build-time timestamp. / 使用稳定打包顺序，JAR 版本与 metadata 一致，不再追加构建时间戳。
- Package the user-supplied MIT license and declare MIT in mod metadata. / 打包用户提供的 MIT 许可证并更新模组许可声明。
