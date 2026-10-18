# 🧊 液态玻璃实验室 · Liquid Glass Lab

> Android 插件化液态玻璃效果测试软件 —— 集成 GitHub 五大顶级开源液态玻璃库，
> 插件化隔离、单仓库 vcdiff 增量更新、GitHub Actions 全链路编译验证。

[![ci](https://github.com/Ultra-Guru/Liquid-Glass-Lab/actions/workflows/ci.yml/badge.svg)](https://github.com/Ultra-Guru/Liquid-Glass-Lab/actions/workflows/ci.yml)
[![guard-rails](https://github.com/Ultra-Guru/Liquid-Glass-Lab/actions/workflows/guard-rails.yml/badge.svg)](https://github.com/Ultra-Guru/Liquid-Glass-Lab/actions/workflows/guard-rails.yml)
[![release](https://github.com/Ultra-Guru/Liquid-Glass-Lab/actions/workflows/release.yml/badge.svg)](https://github.com/Ultra-Guru/Liquid-Glass-Lab/actions/workflows/release.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

## 主界面（用户规约）

```
┌──────────────────────────────┐
│ 1. Kyant0 · AndroidLiquidGlass  ← 你指定的第一位
│ 2. skydoves · Cloudy
│ 3. QmDeve · AndroidLiquidGlassView
│ 4. QWEA0 · Liquid-Glass-Android
│ 5. NadeemIqbal · liquid-glass
│ 6. 关于                         ← 倒数第二
│ 7. 设置                         ← 最后
└──────────────────────────────┘
```

点开任一库 → **安装按钮**（从 GitHub Release 下载插件 APK → sha256 校验 →
DexClassLoader 隔离加载）→ 安装完成自动展示四大对比场景：

| # | 场景 | 考察点 |
|---|------|--------|
| 1 | 深色栏 UI | 顶栏 + 底部导航玻璃化，滚动内容从玻璃后流过 |
| 2 | 按钮UI | 按钮矩阵 + 按压形变反馈 |
| 3 | 液体 | 可拖拽弹性液滴 |
| 4 | 生物拟态 | 有机形态呼吸群落 |

## 五大顶级开源库

| 插件 | 库 | Stars | 坐标 |
|------|-----|-------|------|
| kyant0 | [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) | 4.2k | `io.github.kyant0:backdrop:1.0.6` |
| cloudy | [skydoves/Cloudy](https://github.com/skydoves/Cloudy) | 1.3k | `com.github.skydoves:cloudy:1.0.0-alpha01` |
| qmdeve | [QmDeve/AndroidLiquidGlassView](https://github.com/QmDeve/AndroidLiquidGlassView) | 329 | `com.qmdeve.liquidglass:core:1.1.0` |
| qwea0 | [QWEA0/Liquid-Glass-Android](https://github.com/QWEA0/Liquid-Glass-Android) | 214 | `com.github.QWEA0:liquidglass:v2.0.11`（JitPack） |
| nadeem | [NadeemIqbal/liquid-glass](https://github.com/NadeemIqbal/liquid-glass) | — | `io.github.nadeemiqbal:liquid-glass:0.2.3` |

## 架构：插件化隔离

```
┌──────────────── 宿主 app（com.ultraguru.liquidglass）────────────────┐
│  主列表 / 设置 / 关于 / 更新中心（Compose + Material3）              │
│  插件安装器：下载 → sha256 → 签名预检 → DexClassLoader 隔离加载      │
└────────────────────────────┬───────────────────────────────────────┘
                 parent-delegation（Compose 运行时由宿主提供）
     ┌───────────────┬──────┴──────┬─────────────┬───────────────┐
 plugin-kyant0   plugin-cloudy  plugin-qmdeve  plugin-qwea0  plugin-nadeem
 （APK·内嵌库1）  （APK·内嵌库2）  （APK·内嵌库3） （APK·内嵌库4）  （APK·内嵌库5）
     └───────────────┴─────────────┴─────────────┴───────────────┘
          每插件独立 ClassLoader —— 库与库之间类空间完全隔离
```

- **plugin-api**：零依赖契约（`GlassPlugin` 接口 + `SceneIds` 四场景规约），
  同时进入宿主与所有插件 APK，parent-first 保证接口同一性。
- 插件 APK 只打包「库本体 + 演示代码」（Compose 为 compileOnly），单插件 ~1-3MB。
- 插件清单通过 `AndroidManifest.xml` 的
  `com.ultraguru.liquidglass.PLUGIN_ENTRY` meta-data 声明入口。

## 更新体系：单仓库 + 插件独立链（比双仓更复杂）

```
仓库根
├── version.json   ← App 版本清单（全量 APK + 相邻直连补丁）
├── plugins.json   ← 插件清单（每插件独立 versionCode + 补丁链 + changelog）
├── patches.json   ← App 跨版本补丁窗口（最近 10 基底，含 apkUrl 反查）
└── changelog.json ← 版本日志

App 更新：version.json →（直连补丁｜窗口补丁｜全量）→ vcdiff 合成 → 安装器
插件更新：plugins.json →（插件补丁链｜全量 APK）→ sha256 → 原子落位 → 即时生效
```

- 补丁由 CI 用 `xdelta3 -e -S lzma` 生成，App 内**纯 Kotlin VCDIFF 解码器**
  合成（移植自同组织 [Android-Guru-Agent](https://github.com/Ultra-Guru/Android-Guru-Agent)，协议互通）。
- 镜像源可自定义：清单基地址 + 资产相对路径（`path` 字段）双解析。

## CI / 工作流

| 工作流 | 触发 | 职责 |
|--------|------|------|
| [ci.yml](.github/workflows/ci.yml) | push / PR | 宿主 assembleDebug+Release、**插件矩阵**（5 插件独立 job，fail-fast=false）、清单门禁、wrapper 校验 |
| [guard-rails.yml](.github/workflows/guard-rails.yml) | 清单/插件/CI 变更 | 版本单调性、补丁命名契约、目录对账 |
| [release.yml](.github/workflows/release.yml) | tag `v*` | 签名构建 → xdelta3 双链补丁 → Release 资产 → 四清单回写 main |

**编译验证一律走 GitHub Actions**（本仓不含任何本地构建产物）。

## 工具链（高版本策略）

| 组件 | 版本 |
|------|------|
| AGP | 9.4.1 |
| Gradle | 9.8.1 |
| Kotlin | 2.4.21 |
| Compose BOM | 2026.09.00 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 33 |
| JDK | 21 |

## 从源码构建

```bash
git clone https://github.com/Ultra-Guru/Liquid-Glass-Lab
cd Liquid-Glass-Lab
./gradlew :app:assembleDebug          # 宿主
./gradlew :plugins:plugin-kyant0:assembleRelease   # 单个插件
./gradlew assembleRelease             # 全部（无 secrets 时产物未签名，功能一致）
```

## 许可与致谢

本项目 Apache-2.0。VCDIFF 解码器移植自同组织 Android-Guru-Agent（Apache-2.0）。
五个插件分别内嵌对应上游开源库并遵守其原协议（Apache-2.0 / MIT），
插件 APK 由 CI 自动构建，附 sha256 校验。
