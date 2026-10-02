# Development / 开发指南

本文件面向开发者，维护工具链、构建和验证约定；用户操作见 [README](../README.md)，产品方向见[设计与路线图](creator-design-and-roadmap.md)，发布流程见[发布指南](releasing.md)。通用工程与协作约束只在 [AGENTS.md](../AGENTS.md) 维护。

This developer guide owns toolchain, build, and validation conventions. See the README for usage, the roadmap for product direction, and the release guide for publishing. General engineering and collaboration rules belong to AGENTS.md.

## Version Sources / 版本来源

- `gradle.properties` 是 mod、Minecraft、Fabric Loader、Loom、Fabric API、Mod Menu 和 Java 版本的唯一配置来源。`build.gradle`、生成后的 `fabric.mod.json`、Mixin 配置和 CI 读取这些值，不再独立硬编码；不要在普通文档中维护另一份当前版本表。
- Preserve a single version source in `gradle.properties` for the mod, Minecraft, Fabric Loader, Loom, Fabric API, Mod Menu, and Java. Build scripts, generated resources, and workflows must consume those properties instead of maintaining independent values.
- 运行兼容范围由生成的 `fabric.mod.json` 声明；编译基线、允许范围和已测试组合不是同一概念。测试记录可以保留准确版本和日期，但不能替代当前配置。
- Generated `fabric.mod.json` declares runtime compatibility. Compile baselines, allowed ranges, and tested combinations are distinct; dated test records are evidence, not another current configuration.
- Gradle 本身使用已跟踪的 wrapper；变更依赖或工具链时选择可复现版本，不能因“跟随最新模板”而静默升级或追踪浮动 HEAD。
- Use the tracked Gradle wrapper and reproducible dependency/toolchain inputs. Following the official template is not permission for silent upgrades or floating source revisions.

## Fabric Baseline / Fabric 基线

参考 [Fabric 官方模板](https://github.com/FabricMC/fabric-example-mod)及[模板生成器](https://fabricmc.net/develop/template/)，按目标 Minecraft 版本选择适用分支。当前源码采用 `net.fabricmc.fabric-loom`、Minecraft 官方命名、无 `mappings` 依赖，以及 `implementation` 而非 `modImplementation`；不使用 Yarn。可选编译依赖仍可使用 `compileOnly`，不为了模仿模板强制引入未使用的 Fabric API 或重建 source sets。

Follow the [official Fabric template](https://github.com/FabricMC/fabric-example-mod) and [generator](https://fabricmc.net/develop/template/) for the target Minecraft line. Use `net.fabricmc.fabric-loom`, official Minecraft names, no `mappings` dependency, and `implementation`, not `modImplementation`; do not use Yarn. Keep appropriate `compileOnly` dependencies and existing source sets. Template alignment does not require adding unused Fabric API dependencies.

## Local Validation / 本地验证

先安装与 `java_version` 对应的 JDK，并让 `JAVA_HOME` / `PATH` 指向它；准备 Git 与 Python 3.9+。使用仓库 wrapper，不需要系统 Gradle。本仓库不依赖 sibling 目录或本地 Maven。固定源码来源见[上游依赖](upstream-dependencies.md)。

Install the JDK selected by `java_version` and configure `JAVA_HOME` / `PATH`, plus Git and Python 3.9+. The tracked wrapper supplies Gradle. No sibling checkout or local Maven artifact is needed; see [upstream dependencies](upstream-dependencies.md).

Windows / Linux / macOS（Linux 可使用 `python3` / use `python3` on Linux）:

```bash
python scripts/build.py build --no-daemon --max-workers=1
python scripts/build.py --profile legacy clean build --no-daemon --max-workers=1
python scripts/build.py --profile current clean build --no-daemon --max-workers=1
python -m unittest discover -s scripts/tests -v
```

脚本校验官方 URL、完整 commit、源码版本和 tracked 修改，首次下载到忽略的 `.dependencies/<profile>/`。`current` 和 `legacy` 同时切换实际源码与版本，不能用单独的 `-P..._version` 冒充兼容测试。更改 pin 后须先将旧缓存移到其他位置；脚本不覆盖修改过、来源不符或已有不同 commit 的缓存。

The launcher validates official URLs, full commits, source versions, and tracked modifications. Initial downloads go into ignored `.dependencies/<profile>/`. Profiles switch both source and coordinates; a version-only override is not a compatibility test. Move the old cache aside when updating pins; modified, foreign, or differently pinned caches are never overwritten.

单独准备源码后也可直接调用 wrapper，但必须携带 init script，以覆盖上游浮动 Loom 并排除本地 Maven；Windows 把 `./gradlew` 换成 `.\gradlew.bat`。不修改上游源码或使用上游 wrapper。

After preparation, direct wrapper calls must include the init script: it pins upstream Loom and excludes local Maven. On Windows use `.\gradlew.bat` instead of `./gradlew`. Upstream source files and wrappers are not patched or used.

```bash
python scripts/build.py --prepare-only
./gradlew --init-script scripts/upstream.init.gradle build --no-daemon --max-workers=1
```

产物位于 `build/libs/`。JAR 名称和 metadata 使用同一个 `mod_version`，不再追加当前时间；archive 使用稳定文件顺序且不保留文件时间。Linux wrapper 的 LF 与 Git executable bit 已配置，wrapper 下载以官方 SHA-256 校验。不要在不同 OS / 用户身份间共享可写 Loom 缓存。

Artifacts are written to `build/libs/`. JAR names and metadata use the same `mod_version` without a wall-clock suffix; archives use stable ordering and omit source timestamps. LF/executable wrapper settings and the official distribution SHA-256 are tracked. Do not share writable Loom caches across operating systems or user identities.

单元测试验证策略和事务，启动检查验证实际 Mixin 应用，客户端 GameTest 验证游戏行为，本地实测补充 GUI、渲染、输入与模组组合。各类验证不能互相冒充，未执行的部分必须说明。

Unit tests cover policies and transactions, startup checks exercise Mixin application, client GameTests cover in-game behavior, and local testing supplements GUI/render/input/combination coverage. Report which checks actually ran.

## CI Contract / CI 约定

`.github/workflows/build.yml` 在每次 push、pull request 及手动运行时执行 Linux `current` / `legacy` 矩阵，读取同一版本配置，运行 Python 策略测试、完整 Gradle 构建/JUnit、正式 JAR 审计及打包客户端 GameTest。Actions 固定提交、默认只读仓库权限，不使用 PR 秘密。通过全部检查才上传正式 JAR，测试日志与截图始终保留。发布门禁见发布指南。

The build workflow runs the Linux `current` / `legacy` matrix on push, PR, and manual dispatch. It reads canonical versions, runs Python policies, Gradle/JUnit, production JAR checks, and packaged client GameTests. Actions use fixed commits and read-only repository permissions without PR secrets. Production JARs upload only after all checks pass; diagnostic artifacts always upload. See the release guide for publishing gates.

客户端测试位于独立 `src/gametest/` 测试模组；Fabric API 仅用于该测试环境，正式 Creator JAR 不捆入它或测试代码。`runProductionClientGameTest` 使用实际打包的 Creator 与上游 JAR，不依赖开发模式 Mixin 放宽。地面单人世界断言覆盖投影编辑、空 cell、虚拟库存、相机、focus 与卸载，并检查真实客户端/服务端方块不变；渲染等待检查 schematic world，输出截图。`build` 不自动启动图形客户端。

Client tests live in the separate `src/gametest/` test mod. Fabric API is test-only and neither it nor tests are bundled in Creator. The production test task uses packaged Creator/upstream JARs without development-mode Mixin relaxations. Singleplayer assertions cover edits, empty cells, virtual inventory, camera, focus/discard, unchanged client/server blocks, and schematic-world rebuild completion. Screenshots are retained; `build` alone does not launch a graphical client.

在有显示环境时可直接运行；Linux 无头环境设置 `CI=true` 让 Loom 使用 Xvfb，并安装[云端指南](cloud-development.md)中的系统库。

Run directly with a display, or set `CI=true` for Loom's Xvfb support on Linux after installing the libraries in the cloud guide.

```bash
python3 scripts/build.py runProductionClientGameTest --no-daemon --max-workers=1
python3 scripts/verify_artifact.py
```

`-PverifyGameTestFailure=true` 仅用于测试模组的负向门禁验证：预期断言失败并让 Gradle 非零退出，正常运行不传该参数。CI 对 current 执行此验证后再次运行正常测试；失败日志必须包含预期断言，启动崩溃不能冒充负向验证通过。

The failure-verification property intentionally fails a test-only assertion and must produce a nonzero Gradle exit. CI checks the expected assertion and then reruns normal tests; an unrelated startup crash cannot count as the negative check.

## Remaining Infrastructure / 后续基础设施

2026-10-02 已在独立 Windows 源码目录和 Ubuntu 干净 Gradle 缓存上通过双 profile 完整构建；Linux 两组均为 189 项 JUnit 测试通过。准备脚本 8 项测试在两平台均通过。具体证据和未执行的游戏验收见[完成记录](completed-tasks.md#official-sources-and-linux-toolchain--官方源码与-linux-工具链)。

On 2026-10-02 both profiles passed full builds in isolated Windows and clean-cache Ubuntu directories; Linux reports 189 passing JUnit tests per profile. All 8 preparation tests passed on both platforms. See the completion notes for evidence and unperformed game-level checks.

任务状态见 [TODO 的开发基础设施部分](../todo.md#开发基础设施与发布准备)。固定依赖、资源生成与跨平台构建入口已实现；Fabric API 属性供需要时使用，Creator 没有为了工具链迁移新增运行依赖。上游项目自己的依赖版本继续由各自固定提交管理。

See TODO for remaining work. Pinned dependencies, resource generation, and the cross-platform launcher are implemented. Creator does not add unused Fabric API runtime dependencies; upstream dependency versions remain owned by their pinned source commits.

- CI 与客户端 GameTest 已接入，实际验证证据见完成记录；云端账号接入步骤见[云端开发](cloud-development.md)。
- CI/client GameTests are implemented; see completion notes for actual evidence and the cloud guide for account onboarding.
- SemVer Minecraft 后缀与发布工作流仍为独立任务；迁移 GitHub 不等于正式发布，也不移植游戏版本。
- SemVer Minecraft suffixes and release automation remain separate tasks. GitHub migration is not a release or a game-version port.
