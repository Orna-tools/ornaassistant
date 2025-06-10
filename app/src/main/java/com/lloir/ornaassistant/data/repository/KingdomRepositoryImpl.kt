package com.lloir.ornaassistant.data.repository

import com.lloir.ornaassistant.data.database.dao.KingdomMemberDao
import com.lloir.ornaassistant.data.database.entities.KingdomMemberEntity
import com.lloir.ornaassistant.domain.model.KingdomMember
import com.lloir.ornaassistant.domain.repository.KingdomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KingdomRepositoryImpl @Inject constructor(
    private val kingdomMemberDao: KingdomMemberDao
) : KingdomRepository {

    override fun getAllMembers(): Flow<List<KingdomMember>> {
        return kingdomMemberDao.getAllMembers().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getMemberByName(characterName: String): KingdomMember? {
        return kingdomMemberDao.getMemberByName(characterName)?.toDomainModel()
    }

    override suspend fun getActiveMembers(): List<KingdomMember> {
        return kingdomMemberDao.getActiveMembers(LocalDateTime.now()).map { it.toDomainModel() }
    }

    override suspend fun insertMember(member: KingdomMember) {
        kingdomMemberDao.insertMember(member.toEntity())
    }

    override suspend fun updateMember(member: KingdomMember) {
        kingdomMemberDao.updateMember(member.toEntity())
    }

    override suspend fun deleteMember(member: KingdomMember) {
        kingdomMemberDao.deleteMember(member.toEntity())
    }

    override suspend fun deleteAllMembers() {
        kingdomMemberDao.deleteAllMembers()
    }
}

private fun KingdomMemberEntity.toDomainModel(): KingdomMember {
    return KingdomMember(
        characterName = characterName,
        discordName = discordName,
        immunity = immunity,
        endTime = endTime,
        endTimeLeftSeconds = endTimeLeftSeconds,
        seenCount = seenCount,
        timezone = timezone,
        floors = emptyMap() // Convert floors map if needed
    )
}

private fun KingdomMember.toEntity(): KingdomMemberEntity {
    return KingdomMemberEntity(
        characterName = characterName,
        discordName = discordName,
        immunity = immunity,
        endTime = endTime,
        endTimeLeftSeconds = endTimeLeftSeconds,
        seenCount = seenCount,
        timezone = timezone,
        floors = emptyMap() // Convert floors map if needed
    )
}