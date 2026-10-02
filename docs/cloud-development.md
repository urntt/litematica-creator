# Cloud Development / 云端开发

This developer document owns cloud onboarding, not release authorization. Build commands and version ownership remain in [development.md](development.md); source pins remain in `gradle.properties`.

本文件面向开发者，维护云端接入步骤，不授权发布。构建命令与版本约定见[开发指南](development.md)，源码 pin 仍以 `gradle.properties` 为准。

## Repository / 仓库

- Select the public repository [urntt/litematica-creator](https://github.com/urntt/litematica-creator), branch `main`. Its root is the mod project; sibling source folders and local game files are not part of the repository.
- 选择公开仓库 `urntt/litematica-creator` 与 `main`。GitHub 仓库根目录就是模组项目，不包含本地 sibling 源码或游戏数据。
- Keep original Git history and author identity. Develop and push directly on `main` as [AGENTS.md](../AGENTS.md) describes, and do not create releases or tags without a user request.
- 保留 Git 历史与作者，按 AGENTS 约定直接在 `main` 上开发和推送；未经用户要求不创建 release 或 tag。

## Environment / 环境

Use the same repository, instruction file, source pins, and validation commands in Codex Cloud and Claude Code cloud. Configure each account's GitHub connection/environment separately; a connection in one product does not configure the other.

Codex Cloud 与 Claude Code 云端共用同一仓库、规则、源码 pin 和验证命令。两个账号的 GitHub 连接与环境需分别配置，不能认为一边连好另一边就能使用。

- **Codex Cloud:** create/edit an environment, select this repository, have setup prepare and test it, then review and publish the environment. [Official environment guide](https://learn.chatgpt.com/docs/environments/cloud-environments).
- **Codex Cloud：** 创建/编辑环境、选择本仓库，让准备过程安装并验收；审查后发布环境。官方入口见上述指南。
- **Claude Code cloud:** connect this repository through its own GitHub onboarding, configure a cloud environment with network access, variables and a setup script, then select it for sessions. [Official cloud guide](https://code.claude.com/docs/en/claude-code-on-the-web).
- **Claude Code 云端：** 通过其 GitHub 接入流程连接仓库，配置网络、变量与 setup script，并在会话中选择该环境。官方入口见上述指南。

`AGENTS.md` owns project rules. `CLAUDE.md` contains only an `@AGENTS.md` import for sessions that do not load AGENTS natively; it does not create another policy copy. Check loaded instructions in the first session. [Claude instruction imports](https://code.claude.com/docs/en/memory#import-additional-files).

项目规则只维护于 `AGENTS.md`。`CLAUDE.md` 仅用 `@AGENTS.md` 引用它，兼容未原生加载 AGENTS 的会话，不复制规则；首次会话检查规则已加载。引用语法见上述官方文档。

Install the JDK matching `java_version`, Git, Python 3.9+, and these headless client libraries on Ubuntu:

两边均安装与 `java_version` 一致的 JDK、Git、Python 3.9+，以及 Ubuntu 下的无头客户端依赖：

```bash
sudo apt-get update
sudo apt-get install -y xvfb xauth libgl1-mesa-dri libasound2t64
```

Configure `JAVA_HOME` and `PATH` in the saved environment, not only an installation shell. Set `CI=true`, `LIBGL_ALWAYS_SOFTWARE=true`, and `ALSOFT_DRIVERS=null` for headless runs. No Minecraft account, PAT, or other secret is needed to build or run the isolated singleplayer tests.

在保存的环境中配置 `JAVA_HOME` 与 `PATH`，不要仅在一次安装 shell 中 export。无头测试使用 `CI=true`、`LIBGL_ALWAYS_SOFTWARE=true` 与 `ALSOFT_DRIVERS=null`。构建与隔离单人测试不需要 Minecraft 账号、PAT 或其他秘密。

The Claude Code cloud Ubuntu 24.04 image checked on 2026-10-02 preinstalls JDK 21 and exports its `JAVA_HOME` both as a container variable and from `/etc/profile.d/java.sh`. Ubuntu's `openjdk-25-jdk` package from `noble-updates` built the same JAR bytes as the Temurin CI build. Put the following in the environment's setup script, keeping the JDK major equal to `java_version`, and also set `JAVA_HOME` plus the three headless variables in the environment's variables:

2026-10-02 检查的 Claude Code 云端 Ubuntu 24.04 镜像预装 JDK 21，并同时通过容器变量与 `/etc/profile.d/java.sh` 导出其 `JAVA_HOME`。Ubuntu `noble-updates` 的 `openjdk-25-jdk` 构建出的 JAR 与 Temurin CI 产物字节一致。将下列命令放入环境 setup script（JDK 主版本须与 `java_version` 一致），并在环境变量中同时设置 `JAVA_HOME` 和上述三个无头变量：

```bash
sudo apt-get update
sudo apt-get install -y openjdk-25-jdk xvfb xauth libgl1-mesa-dri libasound2t64
printf '%s\n' 'export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64' 'export PATH=${JAVA_HOME}/bin:${PATH}' | sudo tee /etc/profile.d/java.sh > /dev/null
```

```text
JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
CI=true
LIBGL_ALWAYS_SOFTWARE=true
ALSOFT_DRIVERS=null
```

Allow HTTPS for the GitHub source pins and artifact/tool downloads: `github.com`, `codeload.github.com`, `release-assets.githubusercontent.com`, `raw.githubusercontent.com`, `services.gradle.org`, `downloads.gradle.org`, `plugins.gradle.org`, `maven.fabricmc.net`, `repo.maven.apache.org`, `maven.terraformersmc.com`, `maven.fallenbreath.me`, `jitpack.io`, `piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net`, and `resources.download.minecraft.net`. Add the selected JDK distributor's official domains, or the Ubuntu package mirrors for the apt package, when installing it. Keep authentication in the environment's GitHub connection, not repository files.

允许源码、工具与制品下载所需的上述 HTTPS 域名；安装 JDK 时另允许所选发行版的官方域名，或 apt 安装所需的 Ubuntu 软件源。GitHub 身份使用环境的连接机制，不把凭据写入仓库。

`plugins.gradle.org` serves plugin resolution and redirects to Maven Central; Loom downloads Minecraft libraries from `libraries.minecraft.net`. The first Claude Code session ran with unrestricted network access, so this list comes from build configuration and logs rather than a strict-allowlist run. Upstream builds also declare `masa.dy.fi` and `api.modrinth.com`; add them only if a strict policy reports them blocked.

`plugins.gradle.org` 用于插件解析并重定向到 Maven Central；Loom 从 `libraries.minecraft.net` 下载 Minecraft 库。Claude Code 首次会话使用不受限网络，此列表来自构建配置与日志，尚未在严格白名单下实测。上游构建还声明了 `masa.dy.fi` 与 `api.modrinth.com`，仅在严格策略报告被拦截时再添加。

## Preparation and Checks / 准备与验收

Run from the repository root. Each profile downloads only verified official sources into ignored `.dependencies/`; there is no local Maven or sibling dependency.

在仓库根目录执行；每组依赖只向忽略的 `.dependencies/` 下载已校验的官方源码，不依赖本地 Maven 或 sibling。

```bash
python3 -m unittest discover -s scripts/tests -v
python3 scripts/build.py --profile current build --no-daemon --max-workers=1
python3 scripts/verify_artifact.py
python3 scripts/build.py --profile current runProductionClientGameTest --no-daemon --max-workers=1
python3 scripts/build.py --profile legacy clean build runProductionClientGameTest --no-daemon --max-workers=1
python3 scripts/verify_artifact.py
```

Review the setup report and publish the prepared environment only after these checks pass. A cloud environment must still be created/published in the account UI; a successful local Linux run or GitHub Actions run is not evidence that this account step happened. [Official cloud environment guidance](https://learn.chatgpt.com/docs/environments/cloud-environments).

检查准备报告，验证通过后再发布环境。账号界面中的环境创建/发布仍需完成；本地 Linux 或 GitHub Actions 通过不等于此账号步骤已完成。具体入口见上述官方指南。

Run a first-session check in each provider: load the shared rules, confirm the repository, `main` and JDK, then run the matrix above. Give concurrent agents separate clones, never the same working directory, and have each integrate the latest `origin/main` before pushing. Keep CI green, preserve author identity, and update TODO/completion notes together.

每个平台首个会话都检查共享规则、仓库、`main` 与 JDK，并执行上述矩阵。并行代理使用各自的克隆，不同时写同一工作目录，推送前各自整合最新 `origin/main`。保持 CI 绿色，保留作者，并同步维护 TODO 与完成记录。

## Cache and Evidence / 缓存与证据

- Reuse Gradle caches only within the same OS/user. A changed source pin requires moving its old ignored checkout aside; the launcher intentionally refuses to overwrite it. Re-run preparation after a branch/pin change rather than trusting an old environment snapshot.
- 只在同一 OS/用户内复用 Gradle 缓存。pin 改变时先移走对应旧源码缓存，脚本不会覆盖它；分支或 pin 变化后重新准备，不盲信旧环境快照。
- On a cold cache, Maven Central may answer shared cloud egress with HTTP 429. Re-run the same command: Gradle keeps completed downloads. Retry only dependency-download 429 failures, never compilation, test or GameTest failures.
- 冷缓存时 Maven Central 可能对共享云端出口返回 HTTP 429。重新运行同一命令即可，Gradle 会保留已完成的下载；只重试依赖下载的 429，不重试编译、测试或 GameTest 失败。
- Offline GameTest logs contain expected authlib 401, Realms and Xvfb cursor errors. Judge a run by Gradle's exit code, the `Creator client GameTest passed` log line and the screenshot.
- 离线 GameTest 日志中的 authlib 401、Realms 与 Xvfb 光标错误属预期；以 Gradle 退出码、`Creator client GameTest passed` 日志行和截图判断结果。
- Client tests use `build/run/productionClientGameTest`, which is cleared before each run. Never point tests at a personal game directory. Logs/screenshots stay in build outputs, not Git.
- 客户端测试使用每次清空的 `build/run/productionClientGameTest`，不得指向个人游戏目录；日志与截图只留在构建产物中。
- These smoke tests do not replace manual GUI, rendering, input, multiplayer, or optional-mod regression. GitHub CI tests the two hard-dependency profiles; optional mod combinations remain separately documented.
- 冒烟测试不能替代 GUI、渲染、输入、多人或可选模组手工回归。GitHub CI 覆盖两组硬依赖，可选组合证据另行维护。
