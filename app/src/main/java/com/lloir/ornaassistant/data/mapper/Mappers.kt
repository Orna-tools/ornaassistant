package com.lloir.ornaassistant.data.mapper

import com.lloir.ornaassistant.data.database.entities.*
import com.lloir.ornaassistant.domain.model.*
import com.google.gson.Gson
import java.time.LocalDateTime

/**
 * Mapper functions for converting between entity and domain models
 * This helps maintain a clean separation between the data and domain layers
 */

// DungeonVisit mappers
fun DungeonVisitEntity.toDomain(): DungeonVisit {
    return DungeonVisit(
        id = id,
        sessionId = sessionId,
        name = name,
        mode = mode.toDomain(),
        startTime = startTime,
        durationSeconds = durationSeconds,
        battleOrns = battleOrns,
        battleGold = battleGold,
        battleExperience = battleExperience,
        floorOrns = floorOrns,
        floorGold = floorGold,
        floorExperience = floorExperience,
        orns = orns,
        gold = gold,
        experience = experience,
        floor = floor,
        godforges = godforges,
        completed = completed,
        floorRewards = floorRewards.map { it.toDomain() }
    )
}

fun DungeonVisit.toEntity(): DungeonVisitEntity {
    return DungeonVisitEntity(
        id = id,
        sessionId = sessionId,
        name = name,
        mode = mode.toEntity(),
        startTime = startTime,
        durationSeconds = durationSeconds,
        battleOrns = battleOrns,
        battleGold = battleGold,
        battleExperience = battleExperience,
        floorOrns = floorOrns,
        floorGold = floorGold,
        floorExperience = floorExperience,
        orns = orns,
        gold = gold,
        experience = experience,
        floor = floor,
        godforges = godforges,
        completed = completed,
        floorRewards = floorRewards.map { it.toEntity() }
    )
}

// DungeonMode mappers
fun com.lloir.ornaassistant.data.database.entities.DungeonMode.toDomain(): com.lloir.ornaassistant.domain.model.DungeonMode {
    return com.lloir.ornaassistant.domain.model.DungeonMode(
        type = when (type) {
            com.lloir.ornaassistant.data.database.entities.DungeonMode.Type.NORMAL -> 
                com.lloir.ornaassistant.domain.model.DungeonMode.Type.NORMAL
            com.lloir.ornaassistant.data.database.entities.DungeonMode.Type.BOSS -> 
                com.lloir.ornaassistant.domain.model.DungeonMode.Type.BOSS
            com.lloir.ornaassistant.data.database.entities.DungeonMode.Type.ENDLESS -> 
                com.lloir.ornaassistant.domain.model.DungeonMode.Type.ENDLESS
        },
        isHard = isHard
    )
}

fun com.lloir.ornaassistant.domain.model.DungeonMode.toEntity(): com.lloir.ornaassistant.data.database.entities.DungeonMode {
    return com.lloir.ornaassistant.data.database.entities.DungeonMode(
        type = when (type) {
            com.lloir.ornaassistant.domain.model.DungeonMode.Type.NORMAL -> 
                com.lloir.ornaassistant.data.database.entities.DungeonMode.Type.NORMAL
            com.lloir.ornaassistant.domain.model.DungeonMode.Type.BOSS -> 
                com.lloir.ornaassistant.data.database.entities.DungeonMode.Type.BOSS
            com.lloir.ornaassistant.domain.model.DungeonMode.Type.ENDLESS -> 
                com.lloir.ornaassistant.data.database.entities.DungeonMode.Type.ENDLESS
        },
        isHard = isHard
    )
}

// FloorReward mappers
fun com.lloir.ornaassistant.data.database.entities.FloorReward.toDomain(): com.lloir.ornaassistant.domain.model.FloorReward {
    return com.lloir.ornaassistant.domain.model.FloorReward(
        floor = floor,
        orns = orns,
        gold = gold,
        experience = experience
    )
}

fun com.lloir.ornaassistant.domain.model.FloorReward.toEntity(): com.lloir.ornaassistant.data.database.entities.FloorReward {
    return com.lloir.ornaassistant.data.database.entities.FloorReward(
        floor = floor,
        orns = orns,
        gold = gold,
        experience = experience
    )
}

// KingdomMember mappers
fun KingdomMemberEntity.toDomain(): KingdomMember {
    // Convert the simplified floor storage to GauntletFloor objects
    val gauntletFloors = floors.mapValues { (floorNumber, floorData) ->
        val parts = floorData.split("|")
        if (parts.size >= 3) {
            GauntletFloor(
                number = floorNumber.toIntOrNull() ?: 0,
                mobName = parts[0],
                loss = parts[1].toBoolean(),
                win = parts[2].toBoolean()
            )
        } else {
            GauntletFloor(
                number = floorNumber.toIntOrNull() ?: 0,
                mobName = floorData,
                loss = false,
                win = false
            )
        }
    }
    
    return KingdomMember(
        characterName = characterName,
        discordName = discordName,
        immunity = immunity,
        endTime = endTime,
        endTimeLeftSeconds = endTimeLeftSeconds,
        seenCount = seenCount,
        timezone = timezone,
        floors = gauntletFloors
    )
}

fun KingdomMember.toEntity(): KingdomMemberEntity {
    // Convert GauntletFloor objects to simplified string representation
    val simplifiedFloors = floors.mapValues { (_, floor) ->
        "${floor.mobName}|${floor.loss}|${floor.win}"
    }
    
    return KingdomMemberEntity(
        characterName = characterName,
        discordName = discordName,
        immunity = immunity,
        endTime = endTime,
        endTimeLeftSeconds = endTimeLeftSeconds,
        seenCount = seenCount,
        timezone = timezone,
        floors = simplifiedFloors
    )
}

// ItemAssessment mappers
fun ItemAssessmentEntity.toDomain(gson: Gson = Gson()): ItemAssessment {
    // Convert string attributes to Int map
    val intAttributes = attributes.mapValues { it.value.toIntOrNull() ?: 0 }
    
    // Parse assessment result JSON
    val result = gson.fromJson(assessmentResult, AssessmentResult::class.java)
    
    return ItemAssessment(
        id = id,
        itemName = itemName,
        level = level,
        attributes = intAttributes,
        assessmentResult = result,
        timestamp = timestamp,
        quality = quality
    )
}

fun ItemAssessment.toEntity(gson: Gson = Gson()): ItemAssessmentEntity {
    // Convert Int attributes to string map
    val stringAttributes = attributes.mapValues { it.value.toString() }
    
    // Convert assessment result to JSON
    val resultJson = gson.toJson(assessmentResult)
    
    return ItemAssessmentEntity(
        id = id,
        itemName = itemName,
        level = level,
        attributes = stringAttributes,
        assessmentResult = resultJson,
        timestamp = timestamp,
        quality = quality
    )
}

// Material mappers
fun MaterialEntity.toDomain(): Material {
    return Material(
        id = id,
        name = name,
        currentQuantity = currentQuantity,
        targetQuantity = targetQuantity,
        isTracked = isTracked,
        lastUpdated = lastUpdated
    )
}

fun Material.toEntity(): MaterialEntity {
    return MaterialEntity(
        id = id,
        name = name,
        currentQuantity = currentQuantity,
        targetQuantity = targetQuantity,
        isTracked = isTracked,
        lastUpdated = lastUpdated
    )
}