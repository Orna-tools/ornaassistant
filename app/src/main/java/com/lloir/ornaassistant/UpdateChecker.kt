package com.lloir.ornaassistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val GITHUB_API_URL = "https://api.github.com/repos/Orna-tools/ornaassistant/releases/latest"
    private const val APK_URL = "https://ornahub.co.uk/oa/latest/ornaassistant.apk"

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

                if (downloadApk(latestRelease.downloadUrl, savePath)) {
                    if (!verifyApkSignature(context, savePath)) {
                        showToast(context, "Update verification failed.", true)
                        return@withContext
                    }

                    showToast(context, "Update found and downloaded.", false)
                    preferences.edit().putLong("lastUpdate", remoteTimestamp).apply()
                    installApk(context, savePath)
                } else {
                    showToast(context, "Update failed to download.", true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates", e)
                showToast(context, "An error occurred while checking for updates.", true)
            }
        }
    }

    private fun getRemoteTimestamp(): Long? {
        return try {
            val connection = URL(APK_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            val lastModified = connection.getHeaderField("Last-Modified")
            val dateFormat = SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.US)
            dateFormat.parse(lastModified)?.time
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
            connection.requestMethod = "GET"
            connection.inputStream.use { input ->
                FileOutputStream(savePath).use { output ->
                    input.copyTo(output)
                }
            }
            savePath.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading APK", e)
            false
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.applicationContext.packageName}.provider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            setDataAndType(contentUri, "application/vnd.android.package-archive")
        }
        context.startActivity(intent)
    }

    private fun verifyApkSignature(context: Context, apkFile: File): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_SIGNATURES
            )

            val signatures = packageInfo?.signatures
            if (!signatures.isNullOrEmpty()) {
                val expectedSignature = getApkSignature(context)
                hash(signatures[0].toByteArray()).contentEquals(expectedSignature)
            } else {
                Log.e(TAG, "No signatures found in APK.")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying APK signature", e)
            false
        }
    }


    private fun getApkSignature(context: Context): ByteArray {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )

            val signatures = packageInfo?.signatures
            if (!signatures.isNullOrEmpty()) {
                hash(signatures[0].toByteArray())
            } else {
                Log.e(TAG, "No signatures found for installed APK.")
                ByteArray(0) // Return an empty byte array instead of crashing
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving APK signature", e)
            ByteArray(0)
        }
    }


    private fun hash(byteArray: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(byteArray)
    }

    private fun showToast(context: Context, message: String, isError: Boolean) {
        (context as Activity).runOnUiThread {
            Toast.makeText(context, message, if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }
}

@Serializable
data class ReleaseInfo(val versionName: String, val downloadUrl: String)
