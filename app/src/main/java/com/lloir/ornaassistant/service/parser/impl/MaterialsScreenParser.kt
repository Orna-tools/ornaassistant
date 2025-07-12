package com.lloir.ornaassistant.service.parser.impl

import android.util.Log
import com.lloir.ornaassistant.domain.model.Material
import com.lloir.ornaassistant.domain.model.ParsedScreen
import com.lloir.ornaassistant.domain.model.ScreenData
import com.lloir.ornaassistant.domain.model.ScreenType
import com.lloir.ornaassistant.domain.repository.NotificationRepository
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import com.lloir.ornaassistant.domain.usecase.CheckMaterialTargetsUseCase
import com.lloir.ornaassistant.domain.usecase.GetOrCreateMaterialUseCase
import com.lloir.ornaassistant.service.parser.BaseScreenParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for the materials inventory screen.
 *
 * This class is responsible for parsing the materials inventory screen
 * and extracting material names and quantities.
 */
@Singleton
class MaterialsScreenParser @Inject constructor(
    private val getOrCreateMaterialUseCase: GetOrCreateMaterialUseCase,
    private val checkMaterialTargetsUseCase: CheckMaterialTargetsUseCase,
    private val settingsRepository: SettingsRepository,
    private val notificationRepository: NotificationRepository
) : BaseScreenParser() {

    private val parserScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val TAG = "MaterialsScreenParser" // Keep local TAG for logging

    // State for tracking current materials
    private val _currentMaterials = MutableStateFlow<List<Material>>(emptyList())
    val currentMaterials: StateFlow<List<Material>> = _currentMaterials.asStateFlow()

    // Flag to track if we're currently in the materials inventory
    private val isInMaterialsInventory = AtomicBoolean(false)

    // List of all valid material names
    private val validMaterialNames = setOf(
        "adamantine", "ancient stone", "avalon ore", "baldur", "balorite", "bone",
        "broken statue", "cavestone", "cursed ortanite", "darkstone", "dayleaf",
        "demonic ore", "draconite", "dragonite", "earthstone", "elstone", "eyestone",
        "firestone", "fogstone", "greater soul", "hardened steel", "hide", "iron",
        "leather", "lesser balorite", "lightningstone", "lightstone", "lyonite",
        "mandrake", "mythril", "nightshade", "nightstone", "orichalcum", "ortanite",
        "perfect baldur", "perfect runestone", "platinum", "pure darkstone",
        "pure draconite", "pure lightstone", "pure waterstone", "realm ore",
        "red draconite", "runestone", "sandstone", "scalestone", "silk", "silphium",
        "silver", "skystone", "solarite", "soul", "steel", "stone", "titanium",
        "twilightstone", "undead bone", "vampiric ore", "waterstone", "witchstone",
        "wolf's blood", "wood"
    )

    /**
     * Check if this parser can handle the given screen data
     */
    override fun canParse(data: List<ScreenData>): Boolean {
        // Check if we're in the materials section of inventory
        return findTextElements(data) { it.equals("Materials", ignoreCase = true) }.isNotEmpty()
    }

    /**
     * Perform the actual parsing
     */
    override suspend fun doParse(parsedScreen: ParsedScreen) {
        if (parsedScreen.screenType != ScreenType.INVENTORY) {
            // If we're not in the inventory screen, reset state
            if (isInMaterialsInventory.get()) {
                isInMaterialsInventory.set(false)
                debugLog("Exited materials inventory")
            }
            return
        }

        // We're in the materials section
        isInMaterialsInventory.set(true)
        debugLog("In materials inventory")

        // Extract materials from the screen
        val materials = extractMaterials(parsedScreen.data)

        // Process each material
        parserScope.launch {
            val processedMaterials = materials.map { (name, quantity) ->
                getOrCreateMaterialUseCase(name, quantity)
            }

            // Update current materials
            _currentMaterials.value = processedMaterials

            debugLog("Processed ${processedMaterials.size} materials")

            // Check if material tracking is enabled
            val settings = settingsRepository.getSettings()
            if (settings.enableMaterialTracking) {
                // Check if any tracked materials have reached their targets
                checkReachedTargets()
            }
        }
    }

    /**
     * Get the name of this parser for logging
     */
    override fun getParserName(): String = "Materials"

    /**
     * Extract materials from screen data.
     *
     * @param screenData The screen data to extract materials from
     * @return A map of material names to quantities
     */
    private suspend fun extractMaterials(screenData: List<ScreenData>): Map<String, Int> {
        val materials = mutableMapOf<String, Int>()

        // Find material entries
        // Material entries typically have the format: "Material Name: 123"
        for (i in screenData.indices) {
            val data = screenData[i]
            val text = data.text.trim()

            // Check if this is a material name
            val materialName = findMaterialName(text)
            if (materialName != null) {
                // Look for quantity in the same text or next item
                val quantity = extractQuantity(text) ?: run {
                    // Try to find quantity in the next item
                    if (i + 1 < screenData.size) {
                        extractQuantity(screenData[i + 1].text)
                    } else {
                        null
                    }
                } ?: 0

                materials[materialName] = quantity
                debugLog("Found material: $materialName, quantity: $quantity")
            }
        }

        return materials
    }

    /**
     * Find a valid material name in the given text.
     *
     * @param text The text to search for a material name
     * @return The material name if found, null otherwise
     */
    private fun findMaterialName(text: String): String? {
        // Clean and normalize the text using helper method from BaseScreenParser
        val cleanedText = cleanText(text).lowercase()

        // First, check if the text contains a valid material name
        for (materialName in validMaterialNames) {
            if (cleanedText.contains(materialName)) {
                return materialName
            }
        }

        // If we couldn't find a direct match, try to extract the material name
        // Material entries might have format like "Wood: 123" or just "Wood"
        val colonIndex = cleanedText.indexOf(':')
        if (colonIndex > 0) {
            val potentialName = cleanedText.substring(0, colonIndex).trim()
            if (validMaterialNames.contains(potentialName)) {
                return potentialName
            }
        }

        return null
    }

    /**
     * Extract quantity from text.
     *
     * @param text The text to extract quantity from
     * @return The quantity if found, null otherwise
     */
    private fun extractQuantity(text: String): Int? {
        // Use helper method from BaseScreenParser
        return extractNumber(text)
    }

    /**
     * Clear current materials state.
     */
    fun clearCurrentMaterials() {
        _currentMaterials.value = emptyList()
        isInMaterialsInventory.set(false)
    }

    /**
     * Check if any tracked materials have reached their targets and show notifications.
     */
    private suspend fun checkReachedTargets() {
        try {
            // Get materials that have reached their targets
            val reachedTargets = checkMaterialTargetsUseCase()

            // Show a notification for each material that has reached its target
            for (material in reachedTargets) {
                val message = "You've reached your target of ${material.targetQuantity} for ${material.name}!"
                notificationRepository.showOverlayNotification(message)
                debugLog("Material target reached: ${material.name} (${material.currentQuantity}/${material.targetQuantity})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking material targets", e)
        }
    }
}
