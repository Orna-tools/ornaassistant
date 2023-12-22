import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object UpdateChecker {

    fun getRemoteTimestamp(): Long? {
        val url = URL("https://ornahub.co.uk/oa/latest/ornaassistant.apk")
        val connection = url.openConnection() as HttpURLConnection
        connection.doOutput = true
        connection.requestMethod = "HEAD"

        val lastModified = connection.getHeaderField("Last-Modified")
        val dateFormat = SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.US)

        return dateFormat.parse(lastModified)?.time
    }

    fun checkForUpdates(context: Context): Boolean {
        var success = false
        Thread {
            try {
                // Fetch saved update timestamp from SharedPreferences
                val preferences = context.getSharedPreferences("appUpdates", Context.MODE_PRIVATE)
                val lastUpdate = preferences.getLong("lastUpdate", 0)

                val remoteTimestamp = getRemoteTimestamp()
                if (remoteTimestamp == null) {
                    // Couldn't fetch remote timestamp
                    (context as Activity).runOnUiThread {
                        Toast.makeText(context, "Couldn't check for updates.", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                } else if (remoteTimestamp <= lastUpdate) {
                    // No updates available
                    (context as Activity).runOnUiThread {
                        Toast.makeText(context, "No updates available.", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

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
                    // Inform the user via Toast that an update was found
                    (context as Activity).runOnUiThread {
                        Toast.makeText(context, "Update found and downloaded.", Toast.LENGTH_SHORT).show()
                    }

                    // Update saved timestamp after successfully updating the APK
                    preferences.edit().putLong("lastUpdate", remoteTimestamp!!).apply()

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
                else {
                    // Inform the user via Toast that update download failed
                    (context as Activity).runOnUiThread {
                        Toast.makeText(context, "Update failed to download.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()

                // Inform the user via Toast that an error occurred while checking for updates
                (context as Activity).runOnUiThread {
                    Toast.makeText(context, "An error occurred while checking for updates.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
        return success
    }
}