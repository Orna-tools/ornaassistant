package com.lloir.ornaassistant.data.repository

import android.util.Log
import com.lloir.ornaassistant.data.database.dao.MaterialDao
import com.lloir.ornaassistant.data.database.entities.MaterialEntity
import com.lloir.ornaassistant.domain.model.Material
import com.lloir.ornaassistant.domain.repository.MaterialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of the MaterialRepository interface.
 *
 * This class provides the concrete implementation of the MaterialRepository
 * interface, using Room database as the data source.
 */
@Singleton
class MaterialRepositoryImpl @Inject constructor(
    private val materialDao: MaterialDao
) : MaterialRepository {

    private val TAG = "MaterialRepositoryImpl"

    override fun getAllMaterials(): Flow<List<Material>> {
        return materialDao.getAllMaterials().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getMaterialsPaginated(limit: Int, offset: Int): List<Material> {
        return materialDao.getMaterialsPaginated(limit, offset).map { it.toDomainModel() }
    }

    override suspend fun getMaterialsCount(): Int {
        return materialDao.getMaterialsCount()
    }

    override fun getTrackedMaterials(): Flow<List<Material>> {
        return materialDao.getTrackedMaterials().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getTrackedMaterialsPaginated(limit: Int, offset: Int): List<Material> {
        return materialDao.getTrackedMaterialsPaginated(limit, offset).map { it.toDomainModel() }
    }

    override suspend fun getMaterialByName(name: String): Material? {
        return materialDao.getMaterialByName(name)?.toDomainModel()
    }

    override suspend fun getMaterialById(id: Long): Material? {
        return materialDao.getMaterialById(id)?.toDomainModel()
    }

    override suspend fun insertMaterial(material: Material): Long {
        return materialDao.insertMaterial(material.toEntity())
    }

    override suspend fun updateMaterial(material: Material) {
        materialDao.updateMaterial(material.toEntity())
    }

    override suspend fun deleteMaterial(material: Material) {
        materialDao.deleteMaterial(material.toEntity())
    }

    override suspend fun deleteAllMaterials() {
        materialDao.deleteAllMaterials()
    }

    override suspend fun updateMaterialTracking(materialId: Long, isTracked: Boolean) {
        materialDao.updateMaterialTracking(materialId, isTracked)
    }

    override suspend fun updateMaterialTarget(materialId: Long, targetQuantity: Int) {
        materialDao.updateMaterialTarget(materialId, targetQuantity)
    }

    override suspend fun updateMaterialQuantity(materialId: Long, currentQuantity: Int) {
        materialDao.updateMaterialQuantity(materialId, currentQuantity, LocalDateTime.now())
    }

    override suspend fun getTrackedMaterialsCount(): Int {
        return materialDao.getTrackedMaterialsCount()
    }

    override suspend fun searchMaterials(searchTerm: String): List<Material> {
        return materialDao.searchMaterials(searchTerm).map { it.toDomainModel() }
    }

    override suspend fun searchMaterialsPaginated(searchTerm: String, limit: Int, offset: Int): List<Material> {
        return materialDao.searchMaterialsPaginated(searchTerm, limit, offset).map { it.toDomainModel() }
    }

    override fun getDashboardMaterials(): Flow<List<Material>> {
        return materialDao.getDashboardMaterials().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getDashboardMaterialsPaginated(limit: Int, offset: Int): List<Material> {
        return materialDao.getDashboardMaterialsPaginated(limit, offset).map { it.toDomainModel() }
    }

    override suspend fun getDashboardMaterialsCount(): Int {
        return materialDao.getDashboardMaterialsCount()
    }

    override suspend fun updateMaterialDashboardDisplay(materialId: Long, displayOnDashboard: Boolean) {
        materialDao.updateMaterialDashboardDisplay(materialId, displayOnDashboard)
    }
}

/**
 * Extension function to convert a MaterialEntity to a Material domain model.
 */
fun MaterialEntity.toDomainModel(): Material {
    return Material(
        id = id,
        name = name,
        currentQuantity = currentQuantity,
        targetQuantity = targetQuantity,
        isTracked = isTracked,
        displayOnDashboard = displayOnDashboard,
        lastUpdated = lastUpdated
    )
}

/**
 * Extension function to convert a Material domain model to a MaterialEntity.
 */
fun Material.toEntity(): MaterialEntity {
    return MaterialEntity(
        id = id,
        name = name,
        currentQuantity = currentQuantity,
        targetQuantity = targetQuantity,
        isTracked = isTracked,
        displayOnDashboard = displayOnDashboard,
        lastUpdated = lastUpdated
    )
}
