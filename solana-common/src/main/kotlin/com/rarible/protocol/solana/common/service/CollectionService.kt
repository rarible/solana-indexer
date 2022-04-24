package com.rarible.protocol.solana.common.service

import com.rarible.protocol.solana.common.meta.TokenMeta
import com.rarible.protocol.solana.common.model.MetaplexMeta
import com.rarible.protocol.solana.common.model.MetaplexOffChainMeta
import com.rarible.protocol.solana.common.model.SolanaCollection
import com.rarible.protocol.solana.common.model.SolanaCollectionV1
import com.rarible.protocol.solana.common.model.SolanaCollectionV2
import com.rarible.protocol.solana.common.repository.CollectionRepository
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CollectionService(
    private val collectionRepository: CollectionRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun save(collection: SolanaCollection): SolanaCollection {
        return collectionRepository.save(collection)
    }

    suspend fun findById(id: String): SolanaCollection? {
        return collectionRepository.findById(id)
    }

    suspend fun findAll(fromId: String?, limit: Int): List<SolanaCollection> {
        return collectionRepository.findAll(fromId, limit).toList()
    }

    suspend fun updateCollection(tokenMetaCollection: TokenMeta.Collection?): SolanaCollection? {
        val solanaCollection = when (tokenMetaCollection) {
            is TokenMeta.Collection.OffChain -> SolanaCollectionV1(
                id = tokenMetaCollection.hash,
                name = tokenMetaCollection.name,
                family = tokenMetaCollection.family
            )
            is TokenMeta.Collection.OnChain -> SolanaCollectionV2(
                id = tokenMetaCollection.address
            )
            null -> return null
        }
        val existing = findById(solanaCollection.id)
        if (existing != null) {
            return null
        }
        logger.info("Saved collection ${solanaCollection.id}")
        return save(solanaCollection)
    }
}
