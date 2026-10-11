package com.brycewg.asrkb.ui

import android.content.Context
import com.brycewg.asrkb.R

internal data class DownloadSourceOption(
    val label: String,
    val url: String,
    val fallbackUrls: List<String> = listOf(url)
)

internal object DownloadSourceConfig {
    private data class Mirror(val labelRes: Int, val prefix: String)

    private val mirrors = listOf(
        Mirror(R.string.download_source_mirror_1, "https://ghproxy.net/"),
        Mirror(R.string.download_source_mirror_2, "https://hub.gitmirror.com/"),
        Mirror(R.string.download_source_mirror_3, "https://fastgit.cc/")
    )

    fun buildOptions(
        context: Context,
        officialUrl: String,
        officialLabelRes: Int = R.string.download_source_github_official
    ): List<DownloadSourceOption> = buildOptions(context, listOf(officialUrl), officialLabelRes)

    fun buildOptions(
        context: Context,
        officialUrls: List<String>,
        officialLabelRes: Int = R.string.download_source_github_official
    ): List<DownloadSourceOption> {
        val primaryOfficialUrl = officialUrls.firstOrNull().orEmpty()
        val options = ArrayList<DownloadSourceOption>(mirrors.size + 1)
        options.add(
            DownloadSourceOption(
                context.getString(officialLabelRes),
                primaryOfficialUrl,
                officialUrls
            )
        )
        mirrors.forEach { mirror ->
            val mirroredUrls = officialUrls.map { applyMirrorPrefix(it, mirror.prefix) }
            options.add(
                DownloadSourceOption(
                    context.getString(mirror.labelRes),
                    mirroredUrls.firstOrNull().orEmpty(),
                    mirroredUrls
                )
            )
        }
        return options
    }

    private fun applyMirrorPrefix(originalUrl: String, mirrorPrefix: String): String = if (originalUrl.startsWith("https://github.com/")) {
        mirrorPrefix + originalUrl
    } else {
        originalUrl
    }
}
