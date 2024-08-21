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
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val GITHUB_API_URL = "https://api.github.com/repos/Orna-tools/ornaassistant/releases/latest"

    fun getRemoteTimestamp(context: Context): Long? {
        return try {
            val url = URL("https://ornahub.co.uk/oa/latest/ornaassistant.apk")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"

            val lastModified = connection.getHeaderField("Last-Modified")
            val dateFormat = SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.US)
            dateFormat.parse(lastModified)?.time
        } catch (e: Exception) {
            Log.e(TAG, "Error getting remote timestamp", e)
            null
        }
    }

    fun checkForUpdates(context: Context): Boolean {
        var success = false
        Thread {
            try {
                val preferences = context.getSharedPreferences("appUpdates", Context.MODE_PRIVATE)
                val lastUpdate = preferences.getLong("lastUpdate", 0)

                val remoteTimestamp = getRemoteTimestamp(context)
                if (remoteTimestamp == null) {showErrorToast(context, "Couldn't check for updates.")
                    return@Thread
                } else if (remoteTimestamp <= lastUpdate) {
                    showInfoToast(context, "No updates available.")
                    return@Thread
                }

                val latestRelease = getLatestReleaseInfo(context)
                if (latestRelease == null) {
                    showErrorToast(context, "Couldn't fetch latest release information.")
                    return@Thread
                }

                val versionName = latestRelease.versionName
                val downloadUrl = latestRelease.downloadUrl

                val fileName = "OrnaAssistant_$versionName.apk"

                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val savePath = File(context.getExternalFilesDir(null), fileName)

                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(savePath)

                val data = ByteArray(4096)
                var count: Int
                while (inputStream.read(data).also { count = it } != -1) {
                    outputStream.write(data, 0, count)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                success = savePath.exists()

                if (success) {
                    if (!verifyApkSignature(context, savePath)) {
                        showErrorToast(context, "Update verification failed.")
                        return@Thread
                    }

                    showInfoToast(context, "Update found and downloaded.")
                    preferences.edit().putLong("lastUpdate", remoteTimestamp).apply()

                    val install = Intent(Intent.ACTION_VIEW)
                    val contentUri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.applicationContext.packageName}.provider",
                        savePath
                    )
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        install.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                    install.data = contentUri
                    context.startActivity(install)
                } else {showErrorToast(context, "Update failed to download.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates", e)
                showErrorToast(context, "An error occurred while checking for updates.")
            }
        }.start()
        return success
    }


    private fun showErrorToast(context: Context, message: String) {
        (context as Activity).runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showInfoToast(context: Context, message: String) {
        (context as Activity).runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun verifyApkSignature(context: Context, apkFile: File): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_SIGNATURES
            )
            val signatures = packageInfo?.signatures
            if (signatures != null && signatures.isNotEmpty()) {
                val expectedSignature = getApkSignature(context)
                signatures[0].toByteArray().contentEquals(expectedSignature)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying APK signature", e)
            false
        }
    }

    private fun getApkSignature(context: Context): ByteArray {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNATURES
        )
        val signatures = packageInfo.signatures
        return if (signatures != null && signatures.isNotEmpty()) {
            hash(signatures[0].toByteArray())
        } else {
            // Handle the case where signatures is null or empty
            Log.e(TAG, "No signatures found for the APK.")
            ByteArray(0) // Return an empty byte array or handle differently
        }
    }

    private fun hash(byteArray: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(byteArray)
    }

    private fun getLatestReleaseInfo(context: Context): ReleaseInfo?{
        val queue = Volley.newRequestQueue(context)
        val request = JsonObjectRequest(
            Request.Method.GET, GITHUB_API_URL, null,
            { response ->
                try {
                    val versionName = response.getString("tag_name")
                    val assets = response.getJSONArray("assets")
                    if (assets.length() > 0) {
                        val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                        val releaseInfo = ReleaseInfo(versionName, downloadUrl)
                        // Use the releaseInfo object here
                    } else {
                        Log.e(TAG, "No assets found in the release.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing release information", e)
                }
            },
            { error ->
                Log.e(TAG, "Error fetching release information", error)
            }
        )
        queue.add(request)
        return null // Return null for now, as the result is handled in the response listener
    }
}

data class ReleaseInfo(val versionName: String, val downloadUrl: String)