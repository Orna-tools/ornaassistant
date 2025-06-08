// AssessUtils.kt
package com.lloir.ornaassistant.assess

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

//――――――――――――――――――――――――――――――――――
// Data types
//――――――――――――――――――――――――――――――――――
data class AssessResult(
    val quality: Double,
    val angLevel: Int,
    val stats: Map<String, StatSeries>,
    val levels: Int,
    val exact: Boolean = true,
    val range: Pair<Double, Double>? = null
)

data class StatSeries(
    val base: Int,
    val values: List<Int>
)

//――――――――――――――――――――――――――――――――――
// Constants
//――――――――――――――――――――――――――――――――――
private val celestialWeaponSlots = listOf(
    1,1,1,1,2,2,2,2,2,3,3,3,3,3,4,4,4,4,4,5
)
private val anguishSkipSet = setOf("ward","foresight")

//――――――――――――――――――――――――――――――――――
// Helpers
//――――――――――――――――――――――――――――――――――
private fun delta(base: Int, isBossScaling: Boolean): Int {
    val posDiv = if (isBossScaling) 8 else 10
    val negDiv = if (isBossScaling) -600 else -75
    val divisor = if (base > 0) posDiv else negDiv
    return ceil(base.toDouble() / divisor).toInt()
}

private fun qualityDelta(level: Int): Double =
    if (level > 10) (level - 10) / 100.0 else 0.0

private fun getUpgradedStat(
    base: Int,
    level: Int,
    quality: Double,
    isBossScaling: Boolean,
    isCelestial: Boolean = false,
    angLevel: Int = 0
): Int {
    val statΔ = delta(base, isBossScaling)
    val qΔ = if (isCelestial) 0.0 else qualityDelta(level)
    val raw = ceil((base + if (level == 1) 0 else level * statΔ) * (quality + qΔ)).toInt()
    return if (angLevel > 0) raw + floor(0.03 * angLevel * raw).toInt() else raw
}

private fun getUpgradedStatArray(
    base: Int,
    quality: Double,
    isBossScaling: Boolean,
    levels: Int,
    key: String?,
    angLevel: Int
): List<Int> {
    val statΔ = delta(base, isBossScaling)
    val qΔf: (Int) -> Double = { lvl -> if (levels > 13) 0.0 else qualityDelta(lvl) }

    if (key == "crit") return List(levels) { base }
    if (key == "dexterity") {
        return (1..levels).map { lvl ->
            ceil((base + if (lvl == 1) 0 else lvl * statΔ).toDouble()).toInt()
        }
    }

    val applyAng = !anguishSkipSet.contains(key) && angLevel > 0

    return (1..levels).map { lvl ->
        val baseVal = base + if (lvl == 1) 0 else lvl * statΔ
        val v = ceil(baseVal * (quality + qΔf(lvl))).toInt()
        if (applyAng) {
            // <-- here we ensure both operands of '+' are Double
            floor(v * (1.0 + 0.03 * angLevel)).toInt()
        } else {
            v
        }
    }
}

private fun getItemQuality(
    input: Int,
    base: Int,
    level: Int,
    isBossScaling: Boolean
): Double {
    val baseStat = getUpgradedStat(base, level, 1.0, isBossScaling)
    val rawQ = input.toDouble() / baseStat * (1 + qualityDelta(level)) - qualityDelta(level)
    return (round(rawQ * 100)) / 100.0
}

private fun getAdditionalSlots(quality: Double): Int {
    val q = round(quality * 100).toInt()
    return when {
        q > 200  -> -1
        q >= 170 -> 2
        q > 100  -> 1
        q > 70   -> 0
        else     -> -1
    }
}

//――――――――――――――――――――――――――――――――――
// Public API
//――――――――――――――――――――――――――――――――――
fun assess(
    attrs: Map<String, Int>,
    level: Int,
    bossScaling: Int,
    isCelestial: Boolean,
    isUpgradable: Boolean,
    isOffHand: Boolean,
    qualityCalc: Boolean
): AssessResult {
    if (bossScaling == 0 && isUpgradable) {
        return AssessResult(0.0, 0, emptyMap(), 0)
    }

    val q0    = if (qualityCalc) (attrs["quality"] ?: 100) / 100.0 else 1.0
    val angL  = attrs["angLevel"] ?: 0
    val isBoss = bossScaling > 0

    val baseStats = attrs.filterKeys { it != "adornment_slots" }
    val levels    = when {
        isCelestial   -> 20
        !isUpgradable -> 1
        else          -> 13
    }

    val quality = if (qualityCalc) q0 else {
        val (maxKey, maxVal) = baseStats.maxByOrNull { kotlin.math.abs(it.value) }!!
        getItemQuality(maxVal, baseStats[maxKey]!!, level, isBoss)
    }

    val statMap = baseStats.mapValues { (k, b) ->
        StatSeries(b, getUpgradedStatArray(b, quality, isBoss, levels, k, angL))
    }.toMutableMap()

    if (isCelestial) {
        statMap["adornment_slots"] = StatSeries(
            if (isOffHand) 1 else 2,
            if (isOffHand) celestialWeaponSlots.map { it + 1 } else celestialWeaponSlots
        )
    } else if (isUpgradable) {
        val baseSlot = attrs["adornment_slots"] ?: 0
        val vals = List(levels) { baseSlot + getAdditionalSlots(quality).coerceAtLeast(0) }
        statMap["adornment_slots"] = StatSeries(baseSlot, vals)
    }

    return AssessResult(quality, angL, statMap, levels, exact = true)
}
