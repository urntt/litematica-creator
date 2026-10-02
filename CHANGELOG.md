# Changelog / 变更记录

Notable user-visible changes are recorded here in English and Chinese, following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project's [version and release policy](docs/releasing.md).

本文件按 Keep a Changelog 与项目版本/发布约定记录中英双语的用户可见变化。既有开发历史见[完成记录](docs/completed-tasks.md)；尚未核实的发布历史不补造版本节。

## [Unreleased]

### Changed / 变更

- Prepare both dependency profiles from pinned official upstream commits instead of manual sibling sources or local Maven. / 两组硬依赖改从固定官方提交准备，不再依赖手工 sibling 源码或本地 Maven。
- Pin Loom and centralize Java, game, loader, and compatibility resource values; enable Linux wrapper execution and verify the Gradle distribution checksum. / 固定 Loom，集中 Java、游戏、Loader 与兼容范围资源生成，配置 Linux wrapper 与 Gradle 下载校验。
- Use stable archive ordering and a JAR version matching mod metadata without a build-time timestamp. / 使用稳定打包顺序，JAR 版本与 metadata 一致，不再追加构建时间戳。
- Package the user-supplied MIT license and declare MIT in mod metadata. / 打包用户提供的 MIT 许可证并更新模组许可声明。
