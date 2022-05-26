package com.rarible.protocol.solana.common.repository

import com.rarible.protocol.solana.common.model.MetaplexOffChainMeta
import com.rarible.protocol.solana.common.model.MetaplexOffChainMetaFields
import com.rarible.protocol.solana.common.model.TokenId
import com.rarible.protocol.solana.common.repository.MetaplexOffChainMetaRepository.MetaIndexes.offChainCollectionHashKey
import com.rarible.protocol.union.dto.continuation.page.PageSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
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

    fun findByTokenAddresses(tokenAddresses: Collection<TokenId>): Flow<MetaplexOffChainMeta> {
        if (tokenAddresses.isEmpty()) return emptyFlow()
        val criteria = Criteria.where(MetaplexOffChainMeta::tokenAddress.name).`in`(tokenAddresses)
        return mongo.find(Query(criteria), MetaplexOffChainMeta::class.java).asFlow()
    }

    fun findByOffChainCollectionHash(
        offChainCollectionHash: String,
        fromTokenAddress: String? = null,
        limit: Int = PageSize.COLLECTION.default
    ): Flow<MetaplexOffChainMeta> {
        val criteria = Criteria.where(offChainCollectionHashKey).isEqualTo(offChainCollectionHash)
            .fromTokenAddress(fromTokenAddress)

        val query = Query(criteria).with(
            Sort.by(
                Sort.Direction.ASC,
                MetaplexOffChainMeta::tokenAddress.name,
                "_id"
            )
        ).limit(limit)
        return mongo.find(query, MetaplexOffChainMeta::class.java).asFlow()
    }

    private fun Criteria.fromTokenAddress(fromTokenAddress: String?): Criteria {
        return fromTokenAddress?.let {
            this.and(MetaplexOffChainMeta::tokenAddress.name).gt(it)
        } ?: this
    }

    suspend fun createIndexes() {
        val logger = LoggerFactory.getLogger(MetaplexOffChainMetaRepository::class.java)
        logger.info("Ensuring indexes on ${MetaplexOffChainMeta.COLLECTION}")
        MetaIndexes.ALL_INDEXES.forEach { index ->
            mongo.indexOps(MetaplexOffChainMeta.COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    private object MetaIndexes {
        val offChainCollectionHashKey =
            MetaplexOffChainMeta::metaFields.name + "." + MetaplexOffChainMetaFields::collection.name + "." + MetaplexOffChainMetaFields.Collection::hash.name

        val OFF_CHAIN_COLLECTION_HASH_TOKEN_ADDRESS_ID: Index = Index()
            .on(offChainCollectionHashKey, Sort.Direction.ASC)
            .on(MetaplexOffChainMeta::tokenAddress.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)

        val ALL_INDEXES = listOf(
            OFF_CHAIN_COLLECTION_HASH_TOKEN_ADDRESS_ID
        )
    }
}
