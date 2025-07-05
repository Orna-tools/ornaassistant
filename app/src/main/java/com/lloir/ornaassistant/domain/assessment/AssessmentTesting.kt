package com.lloir.ornaassistant.domain.assessment

import android.content.Context
import android.util.Log
import com.lloir.ornaassistant.domain.model.AssessmentResult

/**
 * Testing utility for local item assessment system
 */
class AssessmentTesting(private val context: Context) {

    companion object {
        private const val TAG = "AssessmentTesting"
    }

    /**
     * Test the local assessment system with known items
     */
    fun runAssessmentTests() {
        Log.i(TAG, "🧪 Starting local assessment tests...")

        // Initialize database
        EnhancedItemDatabase.initialize(context)
        val stats = EnhancedItemDatabase.getStats()
        Log.i(TAG, "📊 Database loaded: ${stats.totalItems}/2168 items, ${stats.bossItems} boss items")

        // Test cases with known items from baseitem.txt
        val testCases = listOf(
            TestCase(
                name = "Blue Dagger",
                level = 1,
                attributes = mapOf("Att" to 26, "Mag" to 5),
                expectedBoss = false,
                description = "Basic weapon test"
            ),
            TestCase(
                name = "Adamantine Staff", 
                level = 10,
                attributes = mapOf("Att" to 114, "Mag" to 407), // Example from API docs
                expectedBoss = false,
                description = "High-tier weapon with known quality"
            ),
            TestCase(
                name = "Arisen Ankh",
                level = 10, 
                attributes = mapOf("Att" to 150, "Mag" to 150, "Def" to 50, "Res" to 50),
                expectedBoss = true,
                description = "Boss item with 12.5% growth"
            ),
            TestCase(
                name = "Ornate Legendary Dragon Sword", // Quality + enchant + item
                level = 7,
                attributes = mapOf("Att" to 180),
                expectedBoss = false,
                description = "Complex name parsing test"
            )
        )

        val localAssessment = LocalItemAssessment()

        testCases.forEach { testCase ->
            Log.i(TAG, "\n🔍 Testing: ${testCase.description}")
            Log.i(TAG, "Item: ${testCase.name} (level ${testCase.level})")
            Log.i(TAG, "Attributes: ${testCase.attributes}")

            try {
                val result = localAssessment.assessItemLocally(
                    testCase.name,
                    testCase.level, 
                    testCase.attributes
                )

                Log.i(TAG, "✅ Quality: ${String.format("%.3f", result.quality)} (${(result.quality * 100).toInt()}%)")
                Log.i(TAG, "📊 Stats: ${result.stats}")
                Log.i(TAG, "🔨 Materials: 10★=${result.materials.getOrNull(0)}, MF=${result.materials.getOrNull(1)}, DF=${result.materials.getOrNull(2)}")

                // Validate results
                validateTestResult(testCase, result)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Test failed for ${testCase.name}", e)
            }
        }

        // Test database lookup functionality
        testDatabaseLookup()

        Log.i(TAG, "🏁 Assessment testing complete!")
    }

    private fun validateTestResult(testCase: TestCase, result: AssessmentResult) {
        // Basic validation
        if (result.quality <= 0) {
            Log.w(TAG, "⚠️ Quality is zero or negative")
        }

        if (result.stats.isEmpty()) {
            Log.w(TAG, "⚠️ No stats calculated")
        }

        if (result.materials.size < 3) {
            Log.w(TAG, "⚠️ Materials calculation incomplete")
        }

        // Check if stats make sense (10★ < MF < DF < GF)
        for ((statName, values) in result.stats) {
            if (values.size >= 4) {
                val tenStar = values[0].toIntOrNull() ?: 0
                val mf = values[1].toIntOrNull() ?: 0
                val df = values[2].toIntOrNull() ?: 0

                if (tenStar > 0 && mf > 0 && df > 0) {
                    if (tenStar >= mf || mf >= df) {
                        Log.w(TAG, "⚠️ Stat progression looks incorrect for $statName: 10★=$tenStar, MF=$mf, DF=$df")
                    }
                }
            }
        }
    }

    private fun testDatabaseLookup() {
        Log.i(TAG, "\n🔍 Testing database lookup...")

        val testLookups = listOf(
            "Blue Dagger",
            "blue dagger",
            "BLUE DAGGER", 
            "Ornate Blue Dagger",
            "Burning Blue Dagger",
            "Adamantine Staff",
            "NotARealItem123"
        )

        testLookups.forEach { itemName ->
            val found = EnhancedItemDatabase.findItemByPartialName(itemName)
            if (found != null) {
                Log.i(TAG, "✅ Found '$itemName' → '${found.name}' (boss=${found.isBossItem}, tier=${found.tier})")
            } else {
                Log.i(TAG, "❌ Not found: '$itemName'")
            }
        }
    }

    data class TestCase(
        val name: String,
        val level: Int,
        val attributes: Map<String, Int>,
        val expectedBoss: Boolean,
        val description: String
    )
}

/**
 * Extension function to run tests from any activity/fragment
 */
fun Context.testLocalAssessment() {
    val tester = AssessmentTesting(this)
    tester.runAssessmentTests()
}
