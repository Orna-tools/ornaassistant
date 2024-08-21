package com.lloir.ornaassistant

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class DungeonVisit(var sessionID: Long?, val name: String, var mode: DungeonMode) {
    var orns: Long = 0
    var gold: Long = 0
    var experience: Long = 0
    var floor: Long = 0
    var godforges:Long = 0
    var completed = false

    var mStarted: LocalDateTime = LocalDateTime.now()
    var mDurationSeconds: Long = 0

    fun finish() {
        mDurationSeconds = ChronoUnit.SECONDS.between(mStarted, LocalDateTime.now())
    }

    override fun toString(): String {
        return "Dungeon visit $mStarted, sessionID $sessionID, name $name, mode $mode, duration $mDurationSeconds s, gold $gold, experience $experience, orns $orns, floor $floor"
    }

    fun coolDownHours():Long {
        var hours: Long = 0
        val split = name.split(' ')
        if (split.size > 1) {
            if (split.last() == "Dungeon") {
                hours = when (mode.mMode) {
                    DungeonMode.Modes.NORMAL -> if (mode.mbHard) 11 else 6
                    DungeonMode.Modes.BOSS -> if (mode.mbHard) 22 else 11
                    DungeonMode.Modes.ENDLESS -> if (mode.mbHard) 22 else 22
                }
            }
        }

        return hours
    }

    fun coolDownEnds(): LocalDateTime {
        return mStarted.plusHours(coolDownHours())
    }
}