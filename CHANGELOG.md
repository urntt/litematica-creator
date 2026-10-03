# Changelog / 变更记录

Notable user-visible changes are recorded here in English and Chinese, following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project's [version and release policy](docs/releasing.md).

本文件按 Keep a Changelog 与项目版本/发布约定记录中英双语的用户可见变化。既有开发历史见[完成记录](docs/completed-tasks.md)；尚未核实的发布历史不补造版本节。

## [Unreleased]

### Added / 新增

- Add push/PR Linux CI for both hard-dependency profiles, packaged client GameTests, and production JAR audits. / 新增两组硬依赖的 push/PR Linux CI、打包客户端 GameTest 与正式 JAR 审计。
- Publish repository/support links in mod metadata and document cloud development setup. / 模组 metadata 增加仓库/问题入口，并提供云端开发准备文档。

### Changed / 变更

- Target Minecraft 26.3 with Fabric Loader 0.19.5. Creator now requires MaLiLib `>=0.30.1 <0.30.3` and Litematica `>=0.29.0 <0.29.2`; 26.2 is no longer supported by this line. / 目标版本改为 Minecraft 26.3 与 Fabric Loader 0.19.5；Creator 现需要 MaLiLib `>=0.30.1 <0.30.3` 与 Litematica `>=0.29.0 <0.29.2`，此版本线不再支持 26.2。
- Update the optional-mod audit to the 26.3 releases of Tweakeroo, Syncmatica, Lithium, and Sodium; older releases are reported as outside the audited range and the Tweakeroo camera bridge stays disabled for them. / 可选模组审计更新为 Tweakeroo、Syncmatica、Lithium 与 Sodium 的 26.3 版本；旧版本会提示超出审计范围，Tweakeroo 相机桥接对其保持禁用。
- Creator placement and deletion swings now use the held item's 26.3 interaction and attack animations. / Creator 放置与删除的挥手动作改用手持物品在 26.3 中的交互与攻击动画。
- Prepare both dependency profiles from pinned official upstream commits instead of manual sibling sources or local Maven. / 两组硬依赖改从固定官方提交准备，不再依赖手工 sibling 源码或本地 Maven。
- Pin Loom and centralize Java, game, loader, and compatibility resource values; enable Linux wrapper execution and verify the Gradle distribution checksum. / 固定 Loom，集中 Java、游戏、Loader 与兼容范围资源生成，配置 Linux wrapper 与 Gradle 下载校验。
- Use stable archive ordering and a JAR version matching mod metadata without a build-time timestamp. / 使用稳定打包顺序，JAR 版本与 metadata 一致，不再追加构建时间戳。
- Package the user-supplied MIT license and declare MIT in mod metadata. / 打包用户提供的 MIT 许可证并更新模组许可声明。
