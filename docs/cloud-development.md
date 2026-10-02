# Cloud Development / 云端开发

This developer document owns cloud onboarding, not release authorization. Build commands and version ownership remain in [development.md](development.md); source pins remain in `gradle.properties`.

本文件面向开发者，维护云端接入步骤，不授权发布。构建命令与版本约定见[开发指南](development.md)，源码 pin 仍以 `gradle.properties` 为准。

## Repository / 仓库

- Select the public repository [urntt/litematica-creator](https://github.com/urntt/litematica-creator), branch `main`. Its root is the mod project; sibling source folders and local game files are not part of the repository.
- 选择公开仓库 `urntt/litematica-creator` 与 `main`。GitHub 仓库根目录就是模组项目，不包含本地 sibling 源码或游戏数据。
- Keep original Git history and author identity. Use purpose-prefixed branches for work, review changes before merging, and do not create releases or tags without a user request.
- 保留 Git 历史与作者，开发分支使用用途前缀，合并前审查；未经用户要求不创建 release 或 tag。

## Environment / 环境

Create or edit a Codex Cloud environment, choose this repository, and ask setup to install the JDK matching `java_version`, Git, Python 3.9+, and these headless client libraries on Ubuntu:

创建或编辑 Codex Cloud 环境，选择本仓库，让环境准备过程安装与 `java_version` 一致的 JDK、Git、Python 3.9+，以及 Ubuntu 下的无头客户端依赖：

```bash
sudo apt-get update
sudo apt-get install -y xvfb xauth libgl1-mesa-dri libasound2t64
```

Configure `JAVA_HOME` and `PATH` in the saved environment, not only an installation shell. Set `CI=true`, `LIBGL_ALWAYS_SOFTWARE=true`, and `ALSOFT_DRIVERS=null` for headless runs. No Minecraft account, PAT, or other secret is needed to build or run the isolated singleplayer tests.

在保存的环境中配置 `JAVA_HOME` 与 `PATH`，不要仅在一次安装 shell 中 export。无头测试使用 `CI=true`、`LIBGL_ALWAYS_SOFTWARE=true` 与 `ALSOFT_DRIVERS=null`。构建与隔离单人测试不需要 Minecraft 账号、PAT 或其他秘密。

Allow HTTPS for the GitHub source pins and artifact/tool downloads: `github.com`, `codeload.github.com`, `release-assets.githubusercontent.com`, `raw.githubusercontent.com`, `services.gradle.org`, `downloads.gradle.org`, `maven.fabricmc.net`, `repo.maven.apache.org`, `maven.terraformersmc.com`, `maven.fallenbreath.me`, `jitpack.io`, `piston-meta.mojang.com`, `piston-data.mojang.com`, and `resources.download.minecraft.net`. Add the selected JDK distributor's official domains when installing it. Keep authentication in the environment's GitHub connection, not repository files.

允许源码、工具与制品下载所需的上述 HTTPS 域名；安装 JDK 时另允许所选发行版的官方域名。GitHub 身份使用环境的连接机制，不把凭据写入仓库。

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

## Cache and Evidence / 缓存与证据

- Reuse Gradle caches only within the same OS/user. A changed source pin requires moving its old ignored checkout aside; the launcher intentionally refuses to overwrite it. Re-run preparation after a branch/pin change rather than trusting an old environment snapshot.
- 只在同一 OS/用户内复用 Gradle 缓存。pin 改变时先移走对应旧源码缓存，脚本不会覆盖它；分支或 pin 变化后重新准备，不盲信旧环境快照。
- Client tests use `build/run/productionClientGameTest`, which is cleared before each run. Never point tests at a personal game directory. Logs/screenshots stay in build outputs, not Git.
- 客户端测试使用每次清空的 `build/run/productionClientGameTest`，不得指向个人游戏目录；日志与截图只留在构建产物中。
- These smoke tests do not replace manual GUI, rendering, input, multiplayer, or optional-mod regression. GitHub CI tests the two hard-dependency profiles; optional mod combinations remain separately documented.
- 冒烟测试不能替代 GUI、渲染、输入、多人或可选模组手工回归。GitHub CI 覆盖两组硬依赖，可选组合证据另行维护。
