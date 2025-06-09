package com.lloir.ornaassistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.core.content.edit

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val GITHUB_API_URL = "https://api.github.com/repos/Orna-tools/ornaassistant/releases/latest"
    private const val APK_URL = "https://ornahub.co.uk/oa/latest/ornaassistant.apk"
    private const val CONNECT_TIMEOUT = 10000
    private const val READ_TIMEOUT = 30000

    suspend fun checkForUpdates(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val preferences = context.getSharedPreferences("appUpdates", Context.MODE_PRIVATE)
                val lastUpdate = preferences.getLong("lastUpdate", 0)

                val remoteTimestamp = getRemoteTimestamp()
                if (remoteTimestamp == null) {
                    showToast(context, "Couldn't check for updates.", true)
                    return@withContext
                }

                if (remoteTimestamp <= lastUpdate) {
                    showToast(context, "No updates available.", false)
                    return@withContext
                }

                val latestRelease = getLatestReleaseInfo(context) ?: run {
                    showToast(context, "Couldn't fetch latest release information.", true)
                    return@withContext
                }

                val fileName = "OrnaAssistant_${latestRelease.versionName}.apk"
                val savePath = File(context.getExternalFilesDir(null), fileName)

                // Clean up old APK files
                cleanupOldApkFiles(context, fileName)

                if (downloadApk(latestRelease.downloadUrl, savePath)) {
                    if (verifyApkSignature(context, savePath)) {
                        showToast(context, "Update found and downloaded.", false)
                        preferences.edit { putLong("lastUpdate", remoteTimestamp) }
                        installApk(context, savePath)
                    } else {
                        showToast(context, "Update verification failed.", true)
                        // Delete the potentially malicious file
                        savePath.delete()
                    }
                } else {
                    showToast(context, "Update failed to download.", true)
                    // Clean up partial download
                    if (savePath.exists()) savePath.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates", e)
                showToast(context, "An error occurred while checking for updates.", true)
            }
        }
    }

    private fun cleanupOldApkFiles(context: Context, currentFileName: String) {
        try {
            val downloadDir = context.getExternalFilesDir(null) ?: return
            downloadDir.listFiles { file ->
                file.name.startsWith("OrnaAssistant_") &&
                        file.name.endsWith(".apk") &&
                        file.name != currentFileName
            }?.forEach { file ->
                file.delete()
                Log.d(TAG, "Cleaned up old APK: ${file.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cleanup old APK files", e)
        }
    }

    private fun getRemoteTimestamp(): Long? {
        return try {
            val connection = URL(APK_URL).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "HEAD"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                setRequestProperty("User-Agent", "OrnaAssistant-UpdateChecker")
            }

            connection.connect()
            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val lastModified = connection.getHeaderField("Last-Modified")
                val dateFormat = SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.US)
                dateFormat.parse(lastModified)?.time
            } else {
                Log.w(TAG, "Failed to get remote timestamp, response code: $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting remote timestamp", e)
            null
        }
    }

    private suspend fun getLatestReleaseInfo(context: Context): ReleaseInfo? {
        return suspendCoroutine { continuation ->
            val queue = Volley.newRequestQueue(context)
            val request = JsonObjectRequest(
                Request.Method.GET, GITHUB_API_URL, null,
                { response ->
                    try {
                        val versionName = response.getString("tag_name")
                        val assets = response.getJSONArray("assets")
                        if (assets.length() > 0) {
                            val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                            continuation.resume(ReleaseInfo(versionName, downloadUrl))
                        } else {
                            Log.e(TAG, "No assets found in the release.")
                            continuation.resume(null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing release information", e)
                        continuation.resume(null)
                    }
                },
                { error ->
                    Log.e(TAG, "Error fetching release information", error)
                    continuation.resume(null)
                }
            )
            queue.add(request)
        }
    }

    private fun downloadApk(downloadUrl: String, savePath: File): Boolean {
        return try {
            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                setRequestProperty("User-Agent", "OrnaAssistant-UpdateChecker")
            }

            connection.connect()
            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val contentLength = connection.contentLength
                Log.d(TAG, "Downloading APK, size: $contentLength bytes")

                connection.inputStream.use { input ->
                    FileOutputStream(savePath).use { output ->
                        val buffer = ByteArray(8192)
                        var totalBytes = 0
                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                        }

                        Log.d(TAG, "Download completed, total bytes: $totalBytes")
                        totalBytes > 0 && savePath.exists()
                    }
                }
            } else {
                Log.e(TAG, "Download failed with response code: $responseCode")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading APK", e)
            false
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.applicationContext.packageName}.provider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                setDataAndType(contentUri, "application/vnd.android.package-archive")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install APK", e)
            showToast(context, "Failed to install update", true)
        }
    }

    private fun verifyApkSignature(context: Context, apkFile: File): Boolean {
        return try {
            // Get signatures from the APK file
            val apkSignatures = getApkSignatures(apkFile.absolutePath, context.packageManager)
            if (apkSignatures.isNullOrEmpty()) {
                Log.e(TAG, "No signatures found in downloaded APK")
                return false
            }

            // Get signatures from the installed app
            val installedSignatures = getInstalledAppSignatures(context)
            if (installedSignatures.isNullOrEmpty()) {
                Log.e(TAG, "No signatures found in installed app")
                return false
            }

            // Compare signatures
            val apkSignatureHashes = apkSignatures.map { hash(it.toByteArray()) }
            val installedSignatureHashes = installedSignatures.map { hash(it.toByteArray()) }

            val signatureMatch = apkSignatureHashes.any { apkHash ->
                installedSignatureHashes.any { installedHash ->
                    apkHash.contentEquals(installedHash)
                }
            }

            if (signatureMatch) {
                Log.d(TAG, "APK signature verification successful")
            } else {
                Log.e(TAG, "APK signature verification failed")
            }

            signatureMatch
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying APK signature", e)
            false
        }
    }

    private fun getApkSignatures(apkPath: String, packageManager: PackageManager): Array<Signature>? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = packageManager.getPackageArchiveInfo(
                    apkPath,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo?.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = packageManager.getPackageArchiveInfo(
                    apkPath,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo?.signatures
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting APK signatures", e)
            null
        }
    }

    private fun getInstalledAppSignatures(context: Context): Array<Signature>? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting installed app signatures", e)
            null
        }
    }

    private fun hash(byteArray: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(byteArray)
    }

    private fun showToast(context: Context, message: String, isError: Boolean) {
        (context as? Activity)?.runOnUiThread {
            Toast.makeText(
                context,
                message,
                if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@Serializable
data class ReleaseInfo(val versionName: String, val downloadUrl: String)