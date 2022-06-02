package com.rarible.protocol.solana.common.service.collection

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.solana.common.model.SolanaCollectionId
import com.rarible.protocol.solana.common.model.SolanaCollection
import com.rarible.protocol.solana.common.model.SolanaCollectionV1
import com.rarible.protocol.solana.common.model.SolanaCollectionV2
import com.rarible.protocol.solana.common.model.isEmpty
import com.rarible.protocol.solana.common.repository.CollectionRepository
import com.rarible.protocol.solana.common.update.CollectionUpdateListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CollectionUpdateService(
    private val collectionRepository: CollectionRepository,
    private val collectionUpdateListener: CollectionUpdateListener
) : EntityService<SolanaCollectionId, SolanaCollection> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun get(id: SolanaCollectionId): SolanaCollection? =
        collectionRepository.findById(id)

    override suspend fun update(entity: SolanaCollection): SolanaCollection {
        if (entity.isEmpty) {
            logger.info("Collection in empty state: ${entity.id}")
            return entity
        }
        val existing = collectionRepository.findById(entity.id)
        if (!requireUpdate(entity, existing)) {
            logger.info("Collection $entity not changed")
            return entity
        }
        val solanaCollection = collectionRepository.save(entity)
        logger.info("Saved SolanaCollection: $solanaCollection")
        collectionUpdateListener.onCollectionChanged(solanaCollection)
        return solanaCollection
    }

    private fun requireUpdate(
        updated: SolanaCollection,
        existing: SolanaCollection?
    ): Boolean {
        if (existing == null) return true
        return when {
            // If nothing changed except updateAt, there is no sense to update
            updated is SolanaCollectionV1 && existing is SolanaCollectionV1 ->
                existing != updated.copy(createdAt = existing.createdAt, updatedAt = existing.updatedAt)
            updated is SolanaCollectionV2 && existing is SolanaCollectionV2 ->
                existing != updated.copy(createdAt = existing.createdAt, updatedAt = existing.updatedAt)
            else -> error("Incompatible types $updated and $existing")
        }
    }

}