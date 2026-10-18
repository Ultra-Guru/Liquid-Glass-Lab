#!/usr/bin/env python3
"""
regen_manifests.py —— 发版流水线的四清单再生成器。

输入（由 release.yml 注入）：
  --repo   OWNER/REPO
  --tag    v0.1.0
  --code   新 versionCode
  --version 新 versionName
  --commit-sha 本版 commit

读取（当前 checkout 的旧清单）：
  version.json / plugins.json / patches.json / changelog.json

读取（本地产物）：
  dist/*.apk           本版新 APK（含宿主 + 5 插件）
  patches-out/*.vcdiff 本版补丁（App 窗口 + 插件链）

输出（原地覆盖，供 CI 提交回 main）：
  version.json    App 版本清单（全量 APK + 相邻直连补丁）
  plugins.json    插件清单（版本 + APK + 独立补丁链）
  patches.json    App 跨版本补丁窗口（含每个基底的 apkUrl 供下一版 CI 用）
  changelog.json  版本日志（置顶新增）

命名契约（与 App 端 UpdateCenter.patchFrom 的解析严格耦合）：
  App 补丁      patch_v{from}_to_v{tag}.vcdiff
  插件补丁      plugin-{id}_v{from}_to_v{tag}.vcdiff
  App APK       liquidglass-v{version}-vc{code}.apk
  插件 APK      plugin-{id}-v{version}-vc{code}.apk
"""
import argparse
import datetime
import hashlib
import json
import pathlib
import sys

PLUGIN_IDS = ["kyant0", "cloudy", "qmdeve", "qwea0", "nadeem"]
PLUGIN_NAMES = {
    "kyant0": "Kyant0 · AndroidLiquidGlass",
    "cloudy": "skydoves · Cloudy",
    "qmdeve": "QmDeve · AndroidLiquidGlassView",
    "qwea0": "QWEA0 · Liquid-Glass-Android",
    "nadeem": "NadeemIqbal · liquid-glass",
}
WINDOW = 10  # 跨版本补丁窗口：保留最近 10 个基底


def sha256(path: pathlib.Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", required=True)
    ap.add_argument("--tag", required=True)
    ap.add_argument("--code", type=int, required=True)
    ap.add_argument("--version", required=True)
    ap.add_argument("--commit-sha", default=None)
    args = ap.parse_args()

    root = pathlib.Path(".")
    dist = root / "dist"
    patches_out = root / "patches-out"

    prev_version = json.loads((root / "version.json").read_text())
    prev_plugins = json.loads((root / "plugins.json").read_text())
    prev_patches = json.loads((root / "patches.json").read_text())
    changelog = json.loads((root / "changelog.json").read_text())

    app_name = f"liquidglass-v{args.version}-vc{args.code}.apk"
    if not (dist / app_name).exists():
        print(f"❌ 缺少宿主 APK：dist/{app_name}", file=sys.stderr)
        sys.exit(1)

    # ── version.json ──
    # 命名契约：补丁文件名 = patch_v{from}_to_{tag}.vcdiff（tag 自带 v 前缀，勿重复）
    direct_patch = patches_out / f"patch_v{prev_version.get('versionCode', 0)}_to_{args.tag}.vcdiff"
    version = {
        "schema": "liquid-glass-lab.version.v1",
        "versionName": args.version,
        "versionCode": args.code,
        "tag": args.tag,
        "publishedAt": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "download": asset(args.repo, args.tag, app_name, dist),
        "patch": (
            {
                **asset(args.repo, args.tag, direct_patch.name, patches_out),
                "fromVersionCode": prev_version.get("versionCode", 0),
            }
            if direct_patch.exists() else None
        ),
        "commitSha": args.commit_sha,
        "signingCertSha256": prev_version.get("signingCertSha256"),
        "minAndroid": 33,
    }

    # ── plugins.json ──
    prev_by_id = {p["id"]: p for p in prev_plugins.get("plugins", [])}
    plugins = {"schema": "liquid-glass-lab.plugins.v1", "generatedAt": version["publishedAt"], "tag": args.tag, "plugins": []}
    for pid in PLUGIN_IDS:
        apk_name = f"plugin-{pid}-v{args.version}-vc{args.code}.apk"
        if not (dist / apk_name).exists():
            print(f"❌ 缺少插件 APK：dist/{apk_name}", file=sys.stderr)
            sys.exit(1)
        prev_rel = prev_by_id.get(pid, {})
        prev_code = prev_rel.get("versionCode", 0)
        patch_name = f"plugin-{pid}_v{prev_code}_to_{args.tag}.vcdiff"
        has_patch = prev_code > 0 and (patches_out / patch_name).exists()
        rel = {
            "id": pid,
            "name": PLUGIN_NAMES[pid],
            "versionName": args.version,
            "versionCode": args.code,
            "minHostVersionCode": 1,
            "apk": asset(args.repo, args.tag, apk_name, dist),
            "patches": (
                [
                    {
                        **asset(args.repo, args.tag, patch_name, patches_out),
                        "fromVersionCode": prev_code,
                    }
                ]
                if has_patch else []
            ),
            "changelog": [
                f"v{args.version}：随 {args.tag} 发布（宿主/插件版本对齐）",
            ],
        }
        plugins["plugins"].append(rel)

    # ── patches.json（App 窗口，含 apkUrl 供下一版 CI 反查基底 APK）──
    # 相邻基底（上一版全量 APK）
    bases = [{
        "from": prev_version.get("versionCode", 0),
        "apkUrl": (prev_version.get("download") or {}).get("url", ""),
    }]
    # 历史基底 carry-forward（去重）
    for b in prev_patches.get("bases", []):
        frm = b.get("from")
        if frm is None or frm == prev_version.get("versionCode"):
            continue
        bases.append({"from": frm, "apkUrl": b.get("apkUrl", "")})
    # 截断窗口
    bases = bases[:WINDOW]
    # 补全每个基底的补丁资产（存在才算数）
    final_bases = []
    for b in bases:
        patch_name = f"patch_v{b['from']}_to_{args.tag}.vcdiff"
        if (patches_out / patch_name).exists():
            entry = {
                **asset(args.repo, args.tag, patch_name, patches_out),
                "from": b["from"],
                "apkUrl": b["apkUrl"],
            }
            final_bases.append(entry)
        else:
            print(f"↷ 基底 v{b['from']} 补丁缺失，窗口剔除")
    patches_index = {
        "schema": "liquid-glass-lab.patches.v1",
        "tag": args.tag,
        "versionCode": args.code,
        "bases": final_bases,
    }

    # ── changelog.json ──
    changelog.insert(0, {
        "versionCode": args.code,
        "versionName": args.version,
        "tag": args.tag,
        "date": version["publishedAt"][:10],
        "notes": [
            f"宿主与五插件对齐 v{args.version}（versionCode {args.code}）",
            f"App 补丁窗口：{len(final_bases)} 个基底；插件独立补丁链已更新",
        ],
    })

    for name, data in [
        ("version.json", version),
        ("plugins.json", plugins),
        ("patches.json", patches_index),
        ("changelog.json", changelog),
    ]:
        (root / name).write_text(
            json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
        print(f"✅ {name} 已生成")

    print(json.dumps({
        "app": {"versionCode": args.code, "apk": app_name},
        "plugins": {p["id"]: p["versionCode"] for p in plugins["plugins"]},
        "appPatchBases": len(final_bases),
    }, ensure_ascii=False, indent=2))


def asset(repo: str, tag: str, name: str, dist_dir: pathlib.Path) -> dict:
    p = dist_dir / name
    return {
        "url": f"https://github.com/{repo}/releases/download/{tag}/{name}",
        "path": f"releases/download/{tag}/{name}",
        "sizeBytes": p.stat().st_size if p.exists() else 0,
        "sha256": sha256(p) if p.exists() else "",
    }


if __name__ == "__main__":
    main()
