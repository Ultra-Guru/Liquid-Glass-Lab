package com.ultraguru.liquidglass.net

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 轻量下载器：OkHttp 单客户端复用，流式落盘 + 进度回调。
 */
object Downloader {

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /** 下载文本（清单 json）。 */
    fun fetchText(url: String): String {
        client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} @ $url")
            return resp.body.string()
        }
    }

    /**
     * 流式下载到目标文件（自动断点：不续传，整段重下）。
     * [onProgress] 参数为 0..100。
     */
    fun download(url: String, dest: File, onProgress: (Int) -> Unit = {}): File {
        dest.parentFile?.mkdirs()
        client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} @ $url")
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
            return dest
        }
    }
}
