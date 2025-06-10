package com.lloir.ornaassistant.service.parser.impl

import android.util.Log
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.ScreenData
import com.lloir.ornaassistant.domain.usecase.AssessItemUseCase
import com.lloir.ornaassistant.service.parser.ScreenParser
import com.lloir.ornaassistant.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemScreenParser @Inject constructor(
    private val assessItemUseCase: AssessItemUseCase
) : ScreenParser {

    private val parserScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val TAG = "ItemScreenParser"
    }

    override suspend fun parseScreen(parsedScreen: ParsedScreen) {
        try {
            val itemName = extractItemName(parsedScreen.data)
            val level = extractLevel(parsedScreen.data)
            val attributes = extractAttributes(parsedScreen.data)

            if (itemName != null && level != null && attributes.isNotEmpty()) {
                // Assess item asynchronously
                parserScope.launch {
                    try {
                        assessItemUseCase(itemName, level, attributes)
                        Log.d(TAG, "Successfully assessed item: $itemName")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to assess item: $itemName", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing item screen", e)
        }
    }

    private fun extractItemName(screenData: List<ScreenData>): String? {
        var name = screenData.firstOrNull()?.text

        if (name?.contains("You are") == true) {
            name = screenData.getOrNull(1)?.text
        }

        // Remove quality prefixes
        Constants.ITEM_QUALITY_PREFIXES.forEach { prefix ->
            if (name?.startsWith(prefix) == true) {
                name = name.replace(prefix, "").trim()
            }
        }

        // Remove enchantment prefixes
        Constants.ENCHANTMENT_PREFIXES.forEach { prefix ->
            if (name?.startsWith(prefix.replaceFirstChar { it.uppercase() }) == true) {
                name = name.replace("${prefix.replaceFirstChar { it.uppercase() }} ", "")
            }
        }

        return name
    }

    private fun extractLevel(screenData: List<ScreenData>): Int? {
        return screenData.find { it.text.startsWith("Level ") }
            ?.text
            ?.replace("Level ", "")
            ?.toIntOrNull()
    }

    private fun extractAttributes(screenData: List<ScreenData>): Map<String, Int> {
        val attributes = mutableMapOf<String, Int>()
        val acceptedAttributes = listOf("Att", "Mag", "Def", "Res", "Dex", "Crit", "Mana", "Ward", "HP")
        var isAdornmentSection = false

        screenData.forEach { item ->
            if (item.text.contains("ADORNMENTS")) {
                isAdornmentSection = true
                return@forEach
            }

            val cleanText = item.text
                .replace("−", "-")
                .replace(" ", "")
                .replace(",", "")
                .replace(".", "")

            val match = Regex("([A-Za-z\\s]+):\\s*(-?[0-9]+)").find(cleanText)
            if (match != null && match.groups.size == 3) {
                val attName = match.groups[1]?.value?.trim()
                val attVal = match.groups[2]?.value?.toIntOrNull()

                if (attName != null && attVal != null && acceptedAttributes.contains(attName)) {
                    if (isAdornmentSection) {
                        // Subtract adornment values from base stats
                        val currentValue = attributes[attName] ?: 0
                        attributes[attName] = currentValue - attVal
                    } else {
                        attributes[attName] = attVal
                    }
                }
            }
        }

        return attributes
    }
}
