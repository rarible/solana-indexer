package com.rarible.protocol.solana.common.repository

import com.rarible.protocol.solana.common.model.MetaplexOffChainMeta
import com.rarible.protocol.solana.common.model.TokenId
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.Index
import org.springframework.stereotype.Component

// TODO: for all repositories that need custom indexes introduce a base interface and inject in into "RepositoryConfiguration".
@Component
class MetaplexOffChainMetaRepository(
    private val mongo: ReactiveMongoOperations
) {
    suspend fun save(metaplexOffChainMeta: MetaplexOffChainMeta): MetaplexOffChainMeta =
        mongo.save(metaplexOffChainMeta).awaitFirst()

    suspend fun findByTokenAddress(tokenAddress: TokenId): MetaplexOffChainMeta? =
        mongo.findById<MetaplexOffChainMeta>(tokenAddress).awaitFirstOrNull()

    suspend fun createIndexes() {
        val logger = LoggerFactory.getLogger(MetaplexOffChainMetaRepository::class.java)
        logger.info("Ensuring indexes on ${MetaplexOffChainMeta.COLLECTION}")
        MetaIndexes.ALL_INDEXES.forEach { index ->
            mongo.indexOps(MetaplexOffChainMeta.COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    private object MetaIndexes {
        val ALL_INDEXES = emptyList<Index>()
    }
}
