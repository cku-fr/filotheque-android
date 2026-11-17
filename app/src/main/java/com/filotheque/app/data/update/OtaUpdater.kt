package com.filotheque.app.data.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
private data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String
)

data class UpdateInfo(
    val latestVersion: String,
    val apkUrl: String
)

object OtaUpdater {

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/cku-fr/filotheque-android/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    fun getCurrentVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val conn = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 8000
            readTimeout = 8000
        }
        try {
            if (conn.responseCode != 200) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val release = json.decodeFromString<GitHubRelease>(body)
            val remoteVer = release.tagName.removePrefix("v").trim()
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") } ?: return@withContext null

            if (isNewerVersion(remoteVer, currentVersion)) {
                UpdateInfo(latestVersion = remoteVer, apkUrl = apkAsset.downloadUrl)
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }

    fun isNewerVersion(remote: String, current: String): Boolean {
        val rParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val cParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    suspend fun downloadAndInstall(
        context: Context,
        apkUrl: String,
        onProgress: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        require(apkUrl.startsWith("https://")) { "URL de mise à jour non sécurisée" }

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val outFile = File(updatesDir, "filotheque-update.apk")

        val conn = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15000
            readTimeout = 30000
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Téléchargement impossible (HTTP ${conn.responseCode})")
            }
            val total = conn.contentLength.coerceAtLeast(1)
            conn.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buf = ByteArray(8192)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buf)
                        if (read == -1) break
                        output.write(buf, 0, read)
                        downloaded += read
                        onProgress(((downloaded * 100) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
        } finally {
            conn.disconnect()
        }

        withContext(Dispatchers.Main) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
