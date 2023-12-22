package com.rockethat.ornaassistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    fun checkForUpdates(context: Context): Boolean {
        var success = false
        Thread {
            try {
                // Specify the URL of your APK
                val url = URL("https://ornahub.co.uk/oa/latest/ornaassistant.apk")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.doOutput = true

                // Specify the path where you want to store the APK
                val savePath = File(context.getExternalFilesDir(null), "update.apk")

                // Download and save the APK
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

                success = savePath.exists() // Check if file is successfully saved

                if (success) {
                    // Install the APK
                    val install = Intent(Intent.ACTION_VIEW)
                    val contentUri: Uri = FileProvider.getUriForFile(
                        context,
                        "${context.applicationContext.packageName}.provider",
                        savePath)
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    install.data = contentUri
                    context.startActivity(install)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
        return success
    }
}