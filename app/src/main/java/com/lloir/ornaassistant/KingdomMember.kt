package com.lloir.ornaassistant

import java.time.LocalDateTime

data class KingdomGauntletFloor(
    val number:Int,
    val mobName: String,
    val loss: Boolean,
    val win: Boolean
) {
    override fun toString(): String {
        var ret = "$number"
        if (loss) {
            ret += " L"
        }
        if (win) {
            ret += " W"
        }
        return ret
    }
}

data class KingdomMember(val character: String, var floors: MutableMap<Int, KingdomGauntletFloor>) {
    var immunity: Boolean = false
    var endTimeLeftSeconds: Long = 0
    var endTime: LocalDateTime = LocalDateTime.now()
    var discordName = ""
    var seenCount = 0
    var timezone: Int = 1000
    val numFloors
        get() = floors.filterValues { floor -> !floor.loss && !floor.win }.size
    val zerk
        get() = floors.filterValues { floor -> !floor.loss && !floor.win }
            .any { it.value.mobName.lowercase().contains("(berserk)") }

    override fun toString(): String {
        return "$character: ${floors.values}"
    }
}