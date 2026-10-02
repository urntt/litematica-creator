# Releases / 版本与发布

本文件维护版本、changelog、GitHub 发布和许可落地约定；构建与 CI 基础见[开发指南](development.md)。**只有用户明确要求时才能执行发布。** 记录发布流程不授权现在推送、创建 tag 或公开项目。

This guide owns versioning, changelog, GitHub release, and licensing conventions. Build/CI foundations belong to the [development guide](development.md). **Release only when explicitly requested by the user.** This policy does not authorize a push, tag, or publication now.

## Versioning / 版本号

模组遵循 [Semantic Versioning](https://semver.org/)，从 `0.1.0` 起步，不继承 Litematica 或其他前身项目的发布序号。`mod_version` 保存完整版本；`+<Minecraft version>` 是标识目标 Minecraft 的 build metadata，例如 `0.1.0+26.2`，开发版本可用 `0.1.0-dev+26.2`。示例不是当前配置值。

Use [Semantic Versioning](https://semver.org/), starting at `0.1.0` independently of Litematica or predecessor releases. Store the complete version in `mod_version`; `+<Minecraft version>` names the target game as build metadata. Examples such as `0.1.0+26.2` or `0.1.0-dev+26.2` are illustrative, not current configuration.

Minecraft 后缀不影响 SemVer 优先级，也不声明跨 Minecraft 版本兼容。tag、changelog 标题、JAR metadata 和 release 必须使用完全一致的版本字符串；不得发布后覆盖同版本内容。当前 JAR 名称与 metadata 已统一使用 `mod_version`，不再动态追加时间戳；Minecraft build-metadata 后缀尚待版本迁移任务，不把历史开发包当作正式 release。

Build metadata does not change SemVer precedence or imply cross-Minecraft compatibility. Tags, changelog headings, JAR metadata, and releases must agree on the full version; published versions are immutable. JAR naming now matches `mod_version` without timestamps; the Minecraft build-metadata suffix is still pending.

## Changelog / 变更记录

[CHANGELOG.md](../CHANGELOG.md) 按 [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) 维护。每个用户可见变化与实现同次写入 `Unreleased`，使用中英双语；按需使用 `Added`、`Changed`、`Deprecated`、`Removed`、`Fixed`、`Security` 分类，不列空分类，不把 Git 日志或未来 TODO 当成已完成变化。

Maintain [CHANGELOG.md](../CHANGELOG.md) using [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Add every user-visible change to `Unreleased` in the same change, in English and Chinese. Use the standard change categories when needed, omit empty categories, and do not substitute commit logs or future tasks for completed changes.

## Release Workflow Contract / 发布工作流约定

计划的 `.github/workflows/release.yml` 构建并验证 mod，然后发布 GitHub Release，附上 JAR，并以对应 changelog 版本节作为 release notes。GitHub Releases 是选定的发布渠道，但工作流当前尚不存在。

The planned `.github/workflows/release.yml` builds and validates the mod, attaches the JAR to a GitHub Release, and uses the matching changelog section as release notes. GitHub Releases is the chosen channel; the workflow does not exist yet.

- 推送 `v<version>` tag 时运行，去掉 `v` 后须严格等于该提交的项目版本；例如 `v1.0.0+26.3` 仅为格式示例。
- Run on pushed `v<version>` tags; removing `v` must exactly match the project version at that commit. `v1.0.0+26.3` is only a format example.
- 支持分支上的 `workflow_dispatch`，捕获启动时的最新提交 SHA，验证后在同一 SHA 创建 tag；默认发布入口为 `main`，其他维护线须明确指定，不在构建后重新取可能变化的分支头。
- Support manual dispatch on a branch, capture its latest SHA at dispatch, validate it, and create the tag at that same SHA. Default to `main`; other maintenance lines require explicit selection. Do not resolve a moving branch head again after the build.
- tag 与版本不符、缺少对应 changelog 节、release 已存在或完整验证失败时，必须失败，不能覆盖已有发布。仅创建 tag 不算发布成功。
- Fail on version/tag mismatch, missing changelog section, an existing release, or failed validation. Never overwrite a release; a tag alone is not successful publication.
- 发布经过同提交构建、单元测试、客户端 GameTest 和 JAR 内容检查的同一 artifact；保留失败日志，不把可执行工作流文件当成已通过 CI 的证明。
- Publish the same artifact validated by the build, unit tests, client GameTests, and JAR content checks at that commit. Retain failure logs; a workflow file is not evidence of a passing CI run.

## Authorized Release Procedure / 获准后的操作流程

1. 用户要求发布后，确认版本、目标分支、远端与权限，将完整 `mod_version` 写入 `gradle.properties`。
2. 将 `Unreleased` 内容移入 `[<version>] - <YYYY-MM-DD>`，上方保留新的空 `Unreleased`；日期使用用户时区的实际发布日期。
3. 完成规定验证，提交并推送获准分支；默认在 `main` 手动启动 release workflow，随后确认 tag、release notes 与附件确实发布成功。
4. 不能推送 tag 的会话使用 GitHub Actions 的手动运行或 API，而非绕过权限。用户描述的 Claude Code 云端限制仅适用于该环境，不能推广到所有工具；无可用凭据或 API 时报告阻碍，不泄露 token。

1. After authorization, confirm the version, branch, remote, and permissions; set the complete `mod_version` in `gradle.properties`.
2. Move `Unreleased` changes into `[<version>] - <YYYY-MM-DD>` beneath a new empty `Unreleased`. Use the actual release date in the user's timezone.
3. Validate, commit, and push the authorized branch. Normally dispatch the release workflow on `main`, then verify the tag, release notes, and attached JAR.
4. If the session cannot push tags, use manual GitHub Actions dispatch or its API. The user's Claude Code cloud restriction is environment-specific, not universal. Report missing credentials/API access instead of bypassing permissions or exposing tokens.

[GitHub 的手动 workflow 文档](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow)说明 UI、CLI 和 API 入口；在工作流、远端与授权准备完成前，本流程不可视为可直接执行。

[GitHub's manual workflow documentation](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow) describes UI, CLI, and API dispatch. This procedure is not operational until the workflow, remote, and authorization are ready.

## License / 许可

项目使用用户提供的 [MIT LICENSE](../LICENSE)，`fabric.mod.json` 声明 MIT，构建将正文打入 Creator JAR。第三方依赖和参考材料保留原许可；依赖实现不捆入 Creator JAR。旧 ARR 开发包没有被重打包，也不视为自动发布的新 MIT 制品。

The project uses the user-supplied [MIT LICENSE](../LICENSE); mod metadata declares MIT and the build packages its text in the Creator JAR. Third-party dependencies/references retain their licenses and dependency implementations are not bundled. Historical ARR development JARs are not rebuilt or treated as new published MIT artifacts.
