# Upstream Sources / 上游源码

## Build Dependencies / 构建依赖

MaLiLib 和 Litematica 使用 [masa 指定的维护者](https://www.reddit.com/r/litematica/comments/1di4o3h/announcement_about_litematicas_future_and_updates/) Sakura-Ryoko 的官方更新源码：[MaLiLib](https://github.com/sakura-ryoko/malilib)、[Litematica](https://github.com/sakura-ryoko/litematica)。这些仓库的默认分支可能已面向其他 Minecraft 版本，不跟随 HEAD。

MaLiLib and Litematica come from [masa's designated maintainer](https://www.reddit.com/r/litematica/comments/1di4o3h/announcement_about_litematicas_future_and_updates/), Sakura-Ryoko. Do not follow default branch HEADs, which may target newer Minecraft versions.

URL、完整提交 SHA、期望版本和双版本 profile 只在 [`gradle.properties`](../gradle.properties) 配置；current profile 对应同一 Minecraft 版本最新的正式发布，legacy profile 对应 Creator 支持范围下限的正式发布。准备脚本核实 commit 和源文件版本。构建方式见[开发指南](development.md)，运行测试组合见[兼容说明](optional-mod-compatibility.md)。

URLs, full SHAs, expected versions, and both profiles are configured only in `gradle.properties`. The current profile pins the latest release for the Minecraft version, and the legacy profile pins the release at the lower bound of Creator's supported range. Preparation verifies both identity and source version. See the development guide for commands and compatibility notes for runtime evidence.

上游 tag 可能打在发布之后的提交上，发布也可能完全没有 tag。选择 pin 时以 Modrinth 正式发布的 JAR 为准：校验官方 SHA-512，并确认 JAR 中存在候选提交的代码、不存在后续提交的代码后，再采用该提交。2026-10-03 的 26.3 pin 即按此确认，例如 MaLiLib `0.30.1` 的 JAR 缺少其 tag 提交新增的 schema 常量，因此固定为 tag 前的发布提交。

Upstream tags may land after the released commit, and some releases have no tag. Choose pins from the official Modrinth JAR: verify its SHA-512, then confirm the JAR contains the candidate commit's code and lacks later commits' code. The 26.3 pins were checked this way on 2026-10-03; for example, the MaLiLib `0.30.1` JAR lacks a schema constant added by its tag commit, so the pin is the release commit before the tag.

依赖源码保留各自的许可证，不提交到 Creator Git，也不捆入 Creator JAR。上游修改必须单独审计并更新 pin，不对下载的源码做私有补丁，不读取原来的手工 sibling 目录或 `mavenLocal()`。

Dependency sources retain their own licenses, stay outside Creator Git, and are not bundled into Creator JARs. Audit changes and update pins deliberately; do not patch downloaded sources or resolve from former manual sibling directories or `mavenLocal()`.

## Optional References / 可选兼容参考

以下是后续源码阅读和兼容审计的上游入口，不是 Creator 的构建依赖，也不表示其最新 HEAD 已验证兼容。需要实际测试时应选择对应游戏版本并记录准确 commit/artifact，而不是使用来源不明的本地副本。

These repositories are reference/audit sources, not Creator build dependencies or claims of compatibility with latest HEADs. Select the correct game line and record exact commits/artifacts when testing instead of relying on unidentified local copies.

| Project / 项目 | Upstream / 上游 |
| --- | --- |
| Tweakeroo | [sakura-ryoko/tweakeroo](https://github.com/sakura-ryoko/tweakeroo) |
| Syncmatica | [End-Tech/syncmatica](https://github.com/End-Tech/syncmatica) |
| Lithium | [CaffeineMC/lithium](https://github.com/CaffeineMC/lithium) |
| Sodium | [CaffeineMC/sodium](https://github.com/CaffeineMC/sodium) |
| Litematica Printer | [aleksilassila/litematica-printer](https://github.com/aleksilassila/litematica-printer) |

Printer 的原项目不自动提供当前游戏版本的移植；不能把此链接当成已审计的新版本移植源。可选模组无需为了构建 Creator 全部下载/构建，原有本地参考源码本批没有被删除。

The original Printer repository is not a verified port for every newer game line. Optional mods do not all need to be downloaded/built to compile Creator; existing local reference directories are left untouched.
