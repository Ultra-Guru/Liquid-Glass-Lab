# 更新管线（Update Pipeline）

本仓库为「液态玻璃实验室」App 与五个插件提供双更新链路，全部资产由 GitHub Release 分发。

## 三个清单（App「更新中心」与静默检查的数据源）

| 文件 | 内容 | 消费方 |
| --- | --- | --- |
| `version.json` | App 最新版：全量 APK + 相邻直连补丁（含 `fromVersionCode`） | App 自身增量更新 |
| `plugins.json` | 每插件：版本 + APK + 独立补丁链（`fromVersionCode`）+ changelog | 插件安装 / 更新 |
| `patches.json` | App 跨版本补丁窗口（最近 10 个基底，含各基底 `apkUrl`） | 旧版本一步直达 + 下一版 CI 反查基底 |
| `changelog.json` | 版本日志（`release-notes/{tag}.md` 优先） | 更新中心「本版更新要点」/ 关于页 |

清单由 `release.yml` 在发版时经 `.github/scripts/regen_manifests.py` 生成并回写 main。

## App 更新链（App 内执行，不经命令行）

```
version.json（远端 versionCode > 本机）
  ├─ ① 相邻直连补丁（patch.fromVersionCode == 本机）
  │     下载补丁 → 以运行中 APK 为源，App 内 VCDIFF(xdelta3-LZMA) 合成新 APK
  │     → sha256 校验 → 系统安装器覆盖安装
  ├─ ② 窗口补丁（patches.json 中任一基底 == 本机，同上流程）
  └─ ③ 全量 APK 直下 → sha256 校验 → 安装器
```

## 插件更新链（不经系统安装器，即时生效）

```
plugins.json（远端 versionCode > 本机或未安装）
  ├─ 补丁路径：下载补丁 → 本机插件 APK 为源合成 → 校验
  └─ 全量路径：下载 APK → 校验
  → 落位 files/plugins/ → DexClassLoader 加载（ComposeView 嵌入宿主）
```

## 镜像语义

- **清单**（json）与**资产**（APK / vcdiff）分离：
  - 预设源（官方 raw / jsDelivr / Fastly / Gcore）只镜像 **git 内容（清单）**；
  - 资产始终从 `github.com/{repo}/releases/download/...` 端点下载（叠加网络加速通道代理）。
- **自定义镜像** = 全镜像（清单与资产同源拼接），适合自建反代。
- 设置页提供预设单选 + 并发测速 + 一键应用最快镜像。

## 命名契约（勿改）

```
App APK      liquidglass-v{version}-vc{code}.apk
App 补丁     patch_v{from}_to_v{tag}.vcdiff
插件 APK     plugin-{id}-v{version}-vc{code}.apk
插件补丁     plugin-{id}_v{from}_to_v{tag}.vcdiff
```

补丁基底解析：清单显式 `fromVersionCode` 优先，否则按文件名 `_v{N}_to_` 解析。

## 发版流程

1. main 分支合入变更 → `ci.yml`（宿主 + 5 插件矩阵构建）+ `guard-rails.yml` 通过
2. 打 tag `vX.Y.Z` → `release.yml`：
   - 构建全部 APK（secrets keystore 签名，versionCode = 上一版 + 1）
   - 与历史基底各生成 xdelta3 补丁（App 窗口 + 插件链）
   - 上传 Release + `SHA256SUMS`
   - `regen_manifests.py` 回写四清单到 main（changelog 优先取 `release-notes/{tag}.md`）
3. 老用户在 App「更新中心」拉取增量；新用户下载全量 APK
