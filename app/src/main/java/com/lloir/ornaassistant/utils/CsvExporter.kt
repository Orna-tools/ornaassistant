package com.lloir.ornaassistant.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.lloir.ornaassistant.domain.model.DungeonVisit
import com.lloir.ornaassistant.domain.model.ItemAssessment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun exportDungeonVisits(visits: List<DungeonVisit>): Uri? {
        try {
            val fileName = "dungeon_visits_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)

            FileWriter(file).use { writer ->
                // Write header
                writer.append("Date,Dungeon,Mode,Hard,Floor,Duration(s),Orns,Gold,Experience,Godforges,Completed\n")

                // Write data
                visits.forEach { visit ->
                    writer.append("${visit.startTime.format(dateFormatter)},")
                    writer.append("${visit.name},")
                    writer.append("${visit.mode.type},")
                    writer.append("${visit.mode.isHard},")
                    writer.append("${visit.floor},")
                    writer.append("${visit.durationSeconds},")
                    writer.append("${visit.orns},")
                    writer.append("${visit.gold},")
                    writer.append("${visit.experience},")
                    writer.append("${visit.godforges},")
                    writer.append("${visit.completed}\n")
                }
            }

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e("CsvExporter", "Error exporting dungeon visits", e)
            return null
        }
    }

    fun exportAssessments(assessments: List<ItemAssessment>): Uri? {
        try {
            val fileName = "item_assessments_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)

            FileWriter(file).use { writer ->
                // Write header
                writer.append("Date,Item,Level,Quality,Att,Mag,Def,Res,Dex,HP,Mana,Ward,Type\n")

                // Write data
                assessments.forEach { assessment ->
                    writer.append("${assessment.timestamp.format(dateFormatter)},")
                    writer.append("${assessment.itemName},")
                    writer.append("${assessment.level},")
                    writer.append("${String.format("%.2f", assessment.quality)},")
                    writer.append("${assessment.attributes["Att"] ?: 0},")
                    writer.append("${assessment.attributes["Mag"] ?: 0},")
                    writer.append("${assessment.attributes["Def"] ?: 0},")
                    writer.append("${assessment.attributes["Res"] ?: 0},")
                    writer.append("${assessment.attributes["Dex"] ?: 0},")
                    writer.append("${assessment.attributes["HP"] ?: 0},")
                    writer.append("${assessment.attributes["Mana"] ?: 0},")
                    writer.append("${assessment.attributes["Ward"] ?: 0},")
                    writer.append("${getItemType(assessment.itemName)}\n")
                }
            }

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e("CsvExporter", "Error exporting assessments", e)
            return null
        }
    }

    fun shareFile(uri: Uri, title: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, title)
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }

    private fun getItemType(itemName: String): String {
        return when {
            itemName.contains("Ornate", ignoreCase = true) -> "Ornate"
            itemName.contains("Godforged", ignoreCase = true) -> "Godforged"
            itemName.contains("Demonforged", ignoreCase = true) -> "Demonforged"
            itemName.contains("Masterforged", ignoreCase = true) -> "Masterforged"
            else -> "Normal"
        }
    }
}
