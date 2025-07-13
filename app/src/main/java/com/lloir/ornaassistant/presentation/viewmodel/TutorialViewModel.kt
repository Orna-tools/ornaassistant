package com.lloir.ornaassistant.presentation.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloir.ornaassistant.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TutorialPage(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@HiltViewModel
class TutorialViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val tutorialPages = listOf(
        TutorialPage(
            title = "Welcome to Orna Assistant",
            description = "Your companion app for enhancing your Orna RPG experience with powerful tracking and analysis tools.",
            icon = Icons.Default.Home
        ),
        TutorialPage(
            title = "Dungeon Tracking",
            description = "Automatically track your dungeon runs, including floors completed, rewards earned, and time spent.",
            icon = Icons.Default.LocationOn
        ),
        TutorialPage(
            title = "Item Assessment",
            description = "Get detailed quality assessments for your items to help you decide what to keep and what to sell.",
            icon = Icons.Default.Star
        ),
        TutorialPage(
            title = "Material Tracking",
            description = "Keep track of materials you need for crafting and upgrades, with notifications when you reach your targets.",
            icon = Icons.Default.List
        ),
        TutorialPage(
            title = "Accessibility Features",
            description = "Customize the app with larger text, high contrast mode, and reduced animations for a better experience.",
            icon = Icons.Default.Settings
        )
    )

    fun markTutorialAsCompleted() {
        viewModelScope.launch {
            settingsRepository.updateHasCompletedTutorial(true)
        }
    }

    fun launchCoroutineScope(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(block = block)
    }
}