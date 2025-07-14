package com.lloir.ornaassistant.utils

object Constants {

    // Supported Apps
    const val ORNA_PACKAGE_NAME = "playorna.com.orna"
    const val DISCORD_PACKAGE_NAME = "com.discord"
    const val AVALON_PACKAGE_NAME = "com.avalon.rpg"

    // Notification IDs
    const val SERVICE_NOTIFICATION_ID = 1001
    const val WAYVESSEL_NOTIFICATION_ID = 2000

    // Screen Detection Patterns
    val DUNGEON_KEYWORDS = listOf(
        "dungeon", "valley", "battle", "dragon", "underworld", "chaos"
    )

    val ITEM_QUALITY_PREFIXES = listOf(
        "Broken", "Poor", "Superior", "Famed", "Legendary", "Ornate",
        "Masterforged", "Demonforged", "Godforged"
    )

    val ENCHANTMENT_PREFIXES = listOf(
        "burning", "embered", "fiery", "flaming", "infernal", "scalding", "warm",
        "chilling", "icy", "oceanic", "snowy", "tidal", "winter",
        "balanced", "earthly", "grounded", "natural", "organic", "rocky", "stony",
        "electric", "shocking", "sparking", "stormy", "thunderous",
        "angelic", "bright", "divine", "moral", "pure", "purifying", "revered",
        "righteous", "saintly", "sublime",
        "corrupted", "diabolic", "demonic", "gloomy", "impious", "profane",
        "unhallowed", "wicked",
        "beastly", "bestial", "chimeric", "dragonic", "wild",
        "colorless", "customary", "normalized", "origin", "reformed", "renewed", "reworked"
    )

    // Local Assessment
    const val EXPECTED_ITEM_COUNT = 2168
}
