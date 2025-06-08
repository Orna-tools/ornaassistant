package com.lloir.ornaassistant.api

import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.lloir.ornaassistant.VolleySingleton
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OrnaApiClient(private val volleySingleton: VolleySingleton) {

    companion object {
        private const val TAG = "OrnaApiClient"
        private const val BASE_URL = "https://orna.guide/api/v1"
        private const val ASSESS_ENDPOINT = "$BASE_URL/assess"
        private const val ITEM_ENDPOINT = "$BASE_URL/item"
    }

    data class ApiAssessRequest(
        val name: String? = null,
        val id: Int? = null,
        val level: Int,
        val hp: Int? = null,
        val mana: Int? = null,
        val attack: Int? = null,
        val magic: Int? = null,
        val defense: Int? = null,
        val resistance: Int? = null,
        val dexterity: Int? = null
    )

    data class ApiAssessResponse(
        val quality: String,
        val stats: Map<String, ApiStatInfo>,
        val tier: Int,
        val type: String,
        val name: String,
        val materials: List<ApiMaterial>?
    )

    data class ApiStatInfo(
        val base: Int,
        val values: List<Int>
    )

    data class ApiMaterial(
        val name: String,
        val id: Int
    )

    suspend fun assessItem(request: ApiAssessRequest): ApiAssessResponse? = suspendCoroutine { continuation ->
        val jsonRequest = JSONObject().apply {
            request.name?.let { put("name", it) }
            request.id?.let { put("id", it) }
            put("level", request.level)
            request.hp?.let { put("hp", it) }
            request.mana?.let { put("mana", it) }
            request.attack?.let { put("attack", it) }
            request.magic?.let { put("magic", it) }
            request.defense?.let { put("defense", it) }
            request.resistance?.let { put("resistance", it) }
            request.dexterity?.let { put("dexterity", it) }
        }

        Log.d(TAG, "Assessing item with request: $jsonRequest")

        val volleyRequest = JsonObjectRequest(
            Request.Method.POST,
            ASSESS_ENDPOINT,
            jsonRequest,
            { response ->
                try {
                    val assessResponse = parseAssessResponse(response)
                    Log.d(TAG, "Assessment successful: quality=${assessResponse.quality}")
                    continuation.resume(assessResponse)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse assess response", e)
                    continuation.resume(null)
                }
            },
            { error ->
                Log.e(TAG, "API assess request failed", error)
                val errorMsg = error.networkResponse?.let {
                    String(it.data, Charsets.UTF_8)
                } ?: error.message
                Log.e(TAG, "Error details: $errorMsg")
                continuation.resume(null)
            }
        )

        volleySingleton.addToRequestQueue(volleyRequest)
    }

    suspend fun findItemByName(itemName: String): Int? = suspendCoroutine { continuation ->
        val jsonRequest = JSONObject().apply {
            put("name", itemName)
        }

        Log.d(TAG, "Finding item: $itemName")

        val volleyRequest = JsonObjectRequest(
            Request.Method.POST,
            ITEM_ENDPOINT,
            jsonRequest,
            { response ->
                try {
                    val itemId = response.getInt("id")
                    Log.d(TAG, "Found item ID: $itemId for $itemName")
                    continuation.resume(itemId)
                } catch (e: Exception) {
                    Log.w(TAG, "Item not found or parse error for: $itemName", e)
                    continuation.resume(null)
                }
            },
            { error ->
                Log.w(TAG, "Failed to find item: $itemName", error)
                continuation.resume(null)
            }
        )

        volleySingleton.addToRequestQueue(volleyRequest)
    }

    private fun parseAssessResponse(response: JSONObject): ApiAssessResponse {
        val quality = response.getString("quality")
        val tier = response.getInt("tier")
        val type = response.getString("type")
        val name = response.getString("name")

        val statsJson = response.getJSONObject("stats")
        val stats = mutableMapOf<String, ApiStatInfo>()

        val statKeys = listOf("attack", "magic", "defense", "resistance", "dexterity", "hp", "mana")
        for (key in statKeys) {
            if (statsJson.has(key)) {
                val statObj = statsJson.getJSONObject(key)
                val base = statObj.getInt("base")
                val valuesArray = statObj.getJSONArray("values")
                val values = mutableListOf<Int>()
                for (i in 0 until valuesArray.length()) {
                    values.add(valuesArray.getInt(i))
                }
                stats[key] = ApiStatInfo(base, values)
            }
        }

        val materials = if (response.has("materials")) {
            val materialsArray = response.getJSONArray("materials")
            val materialsList = mutableListOf<ApiMaterial>()
            for (i in 0 until materialsArray.length()) {
                val materialObj = materialsArray.getJSONObject(i)
                materialsList.add(
                    ApiMaterial(
                        materialObj.getString("name"),
                        materialObj.getInt("id")
                    )
                )
            }
            materialsList
        } else null

        return ApiAssessResponse(quality, stats, tier, type, name, materials)
    }
}