package com.rarible.protocol.solana.common.repository

import com.rarible.protocol.solana.common.model.MetaId
import com.rarible.protocol.solana.common.model.MetaplexMeta
import com.rarible.protocol.solana.common.model.MetaplexMetaFields
import com.rarible.protocol.solana.common.model.TokenId
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

@Component
class MetaplexMetaRepository(
    private val mongo: ReactiveMongoOperations
) {

    suspend fun save(metaplexMeta: MetaplexMeta): MetaplexMeta =
        mongo.save(metaplexMeta).awaitFirst()

    suspend fun findByMetaAddress(metaAddress: MetaId): MetaplexMeta? =
        mongo.findById<MetaplexMeta>(metaAddress).awaitFirstOrNull()

    suspend fun findByTokenAddress(tokenAddress: TokenId): MetaplexMeta? {
        val criteria = Criteria.where(MetaplexMeta::tokenAddress.name).isEqualTo(tokenAddress)
        return mongo.find(Query(criteria), MetaplexMeta::class.java).awaitFirstOrNull()
    }

    fun findByTokenAddresses(tokenAddresses: Collection<TokenId>): Flow<MetaplexMeta> {
        if (tokenAddresses.isEmpty()) return emptyFlow()
        val criteria = Criteria.where(MetaplexMeta::tokenAddress.name).`in`(tokenAddresses)
        return mongo.find(Query(criteria), MetaplexMeta::class.java).asFlow()
    }

    fun findByCollectionAddress(
        collectionAddress: String,
        fromTokenAddress: String? = null,
        limit: Int = PageSize.COLLECTION.default
    ): Flow<MetaplexMeta> {
        val criteria = Criteria.where(collectionAddressKey).isEqualTo(collectionAddress)
            .fromTokenAddress(fromTokenAddress)

        val query = Query(criteria).with(
            Sort.by(
                Sort.Direction.ASC,
                MetaplexMeta::tokenAddress.name,
                "_id"
            )
        ).limit(limit)
        return mongo.find(query, MetaplexMeta::class.java).asFlow()
    }

    fun findAll(fromMint: String?): Flow<MetaplexMeta> {
        val criteria = if (fromMint != null) {
            Criteria(MetaplexMeta::tokenAddress.name).gt(fromMint)
        } else {
            Criteria()
        }
        val query = Query(criteria).with(
            Sort.by(
                Sort.Direction.ASC,
                MetaplexMeta::tokenAddress.name,
                "_id"
            )
        )
        return mongo.find(query, MetaplexMeta::class.java).asFlow()
    }

    suspend fun createIndexes() {
        val logger = LoggerFactory.getLogger(MetaplexMetaRepository::class.java)
        logger.info("Ensuring indexes on ${MetaplexMeta.COLLECTION}")
        MetaIndexes.ALL_INDEXES.forEach { index ->
            mongo.indexOps(MetaplexMeta::class.java).ensureIndex(index).awaitFirst()
        }
    }

    private fun Criteria.fromTokenAddress(fromTokenAddress: String?): Criteria {
        return fromTokenAddress?.let {
            this.and(MetaplexMeta::tokenAddress.name).gt(it)
        } ?: this
    }

    private object MetaIndexes {

        val TOKEN_ADDRESS_ID: Index = Index()
            .on(MetaplexMeta::tokenAddress.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .unique()

        val COLLECTION_ADDRESS_TOKEN_ADDRESS_ID: Index = Index()
            .on(collectionAddressKey, Sort.Direction.ASC)
            .on(MetaplexMeta::tokenAddress.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .sparse()

        val ALL_INDEXES = listOf(
            TOKEN_ADDRESS_ID,
            COLLECTION_ADDRESS_TOKEN_ADDRESS_ID
        )
    }

    companion object {

        val collectionAddressKey =
            MetaplexMeta::metaFields.name + "." + MetaplexMetaFields::collection.name + "." + MetaplexMetaFields.Collection::address.name
    }

}
