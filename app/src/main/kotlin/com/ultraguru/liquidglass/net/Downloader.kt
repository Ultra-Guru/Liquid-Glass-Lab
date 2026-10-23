package com.ultraguru.liquidglass.net

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 轻量下载器：OkHttp 单客户端复用，流式落盘 + 进度回调。
 *
 * v3 网络加固（针对 GitHub 直连不稳定的网络环境）：
 * - 所有请求带 User-Agent（部分 CDN / 加速代理会拒绝无 UA 请求 → 此前「秒失败」的原因之一）。
 * - [fetchText] / [download] 均支持「加速通道」：直连失败（网络异常 / HTTP>=400）时
 *   自动依序尝试公共加速镜像与 jsDelivr CDN（仅 raw 清单）。
 * - 连接超时收紧到 12s：死通道快速跳到下一个，总等待时间可控。
 * - 失败信息按通道逐条列出（HTTP 码 / 异常摘要），诊断页与错误 UI 可直接读因。
 * - 完整性由调用侧 sha256 校验兜底，加速通道不构成信任边界。
 * - [onChannel] 回调汇报实际命中的通道（诊断可见）。
 */
object Downloader {

    private const val TAG = "Downloader"

    private const val USER_AGENT =
        "LiquidGlassLab/0.1 (Android; +https://github.com/Ultra-Guru/Liquid-Glass-Lab)"

    /** GitHub 资产公共加速镜像（前缀拼接式：proxy + 完整 URL）。2026-10 实测可用性排序。 */
    private val GH_PROXY_PREFIXES = listOf(
        "https://gh-proxy.com/",
        "https://ghfast.top/",
        "https://gh.llkk.cc/",
        "https://github.moeyy.xyz/",
        "https://ghproxy.net/"
    )

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private fun Request.Builder.withUa() = header("User-Agent", USER_AGENT)

    /** 依原始 URL 构建候选通道（直连优先；仅 GitHub 域名追加加速通道）。 */
    fun candidateUrls(url: String, boost: Boolean): List<String> {
        val list = mutableListOf(url)
        if (!boost) return list
        val isGithubAsset = url.startsWith("https://github.com/") ||
            url.startsWith("https://raw.githubusercontent.com/") ||
            url.startsWith("https://objects.githubusercontent.com/") ||
            url.startsWith("https://release-assets.githubusercontent.com/")
        if (!isGithubAsset) return list
        GH_PROXY_PREFIXES.forEach { list.add(it + url) }
        // raw 清单 → jsDelivr CDN（git 内容镜像）
        RawRegex.find(url)?.let { m ->
            val (user, repo, branch, file) = m.destructured
            list.add("https://cdn.jsdelivr.net/gh/$user/$repo@$branch/$file")
        }
        return list.distinct()
    }

    private val RawRegex =
        Regex("""^https://raw\.githubusercontent\.com/([^/]+)/([^/]+)/([^/]+)/(.+)$""")

    /** 通道别名（诊断显示用）。 */
    private fun channelOf(url: String): String = when {
        url.startsWith("https://gh-proxy.com/") -> "加速①gh-proxy"
        url.startsWith("https://ghfast.top/") -> "加速②ghfast"
        url.startsWith("https://gh.llkk.cc/") -> "加速③llkk"
        url.startsWith("https://github.moeyy.xyz/") -> "加速④moeyy"
        url.startsWith("https://ghproxy.net/") -> "加速⑤ghproxy"
        url.startsWith("https://cdn.jsdelivr.net/") -> "CDN·jsDelivr"
        else -> "直连"
    }

    /** 下载文本（清单 json；失败时按候选通道重试，错误信息含每通道原因）。 */
    fun fetchText(url: String, boost: Boolean = true, onChannel: (String) -> Unit = {}): String {
        val candidates = candidateUrls(url, boost)
        val failures = mutableListOf<String>()
        for (u in candidates) {
            try {
                val text = fetchTextOnce(u)
                onChannel(channelOf(u))
                return text
            } catch (e: IOException) {
                failures.add("${channelOf(u)}: ${e.message ?: "IO异常"}")
            } catch (e: Exception) {
                failures.add("${channelOf(u)}: ${e.javaClass.simpleName} ${e.message ?: ""}")
            }
        }
        throw IOException(
            "清单获取失败（已尝试 ${candidates.size} 条通道）→ ${failures.joinToString("；")}",
            null
        )
    }

    /** 直连拉取文本（不经任何加速通道）—— 镜像测速专用。 */
    fun fetchTextDirect(url: String): String = fetchTextOnce(url)

    private fun fetchTextOnce(url: String): String {
        client.newCall(Request.Builder().url(url).withUa().build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            return resp.body.string()
        }
    }

    /**
     * 流式下载到目标文件（整段重下，不做断点）。
     * [onProgress] 参数为 0..100；[onChannel] 汇报命中通道。
     */
    fun download(
        url: String,
        dest: File,
        onProgress: (Int) -> Unit = {},
        boost: Boolean = true,
        onChannel: (String) -> Unit = {}
    ): File {
        val candidates = candidateUrls(url, boost)
        val failures = mutableListOf<String>()
        for (u in candidates) {
            try {
                downloadOnce(u, dest, onProgress)
                onChannel(channelOf(u))
                return dest
            } catch (e: IOException) {
                failures.add("${channelOf(u)}: ${e.message ?: "IO异常"}")
                android.util.Log.w(TAG, "通道失败 ${channelOf(u)}：${e.message}")
            } catch (e: Exception) {
                failures.add("${channelOf(u)}: ${e.javaClass.simpleName} ${e.message ?: ""}")
                android.util.Log.w(TAG, "通道失败 ${channelOf(u)}：${e.message}")
            }
        }
        dest.delete()
        throw IOException(
            "下载失败（已尝试 ${candidates.size} 条通道）→ ${failures.joinToString("；")}",
            null
        )
    }

    private fun downloadOnce(url: String, dest: File, onProgress: (Int) -> Unit) {
        dest.parentFile?.mkdirs()
        client.newCall(Request.Builder().url(url).withUa().build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val body = resp.body
            val total = if (body.contentLength() > 0) body.contentLength() else -1L
            var read = 0L
            var lastPct = -1
            body.byteStream().use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(1 shl 15)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0) {
                            val pct = ((read * 100) / total).toInt().coerceIn(0, 100)
                            if (pct != lastPct) {
                                lastPct = pct
                                onProgress(pct)
                            }
                        }
                    }
                }
            }
            if (total > 0 && read != total) {
                throw IOException("下载不完整（$read/$total 字节，连接中断）")
            }
        }
    }
}
