package com.rarible.protocol.solana.common.repository

import com.rarible.protocol.solana.common.continuation.DateIdContinuation
import com.rarible.protocol.solana.common.meta.TokenMeta
import com.rarible.protocol.solana.common.model.Token
import com.rarible.protocol.solana.common.model.TokenId
import com.rarible.protocol.solana.common.repository.TokenRepository.TokenIndexes.tokenCollectionKey
import kotlinx.coroutines.flow.Flow
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
import java.time.Instant

@Component
class TokenRepository(
    private val mongo: ReactiveMongoOperations
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun save(token: Token): Token =
        mongo.save(token).awaitFirst()

    suspend fun findByMint(mint: TokenId): Token? =
        mongo.findById<Token>(mint).awaitFirstOrNull()

    suspend fun findByMints(mints: Collection<TokenId>): Flow<Token> =
        mongo.find(
            Query(Criteria.where("_id").`in`(mints))
                .with(Sort.by("_id")), Token::class.java
        ).asFlow()

    suspend fun findAll(
        lastUpdatedFrom: Instant?,
        lastUpdatedTo: Instant?,
        continuation: DateIdContinuation?,
        limit: Int
    ): Flow<Token> {

        val criteria = continuation?.let {
            // in continuation date will always less than param form query
            Criteria().orOperator(
                Criteria(Token::updatedAt.name).isEqualTo(continuation.date).and("_id").lt(continuation.id),
                fromToCriteria(lastUpdatedFrom, continuation.date)
            )
        } ?: fromToCriteria(lastUpdatedFrom, lastUpdatedTo)

        val query = Query(criteria).withSortByLastUpdateAndId()
        query.limit(limit)

        return mongo.find(query, Token::class.java).asFlow()
    }

    suspend fun findByCollection(
        collection: String,
        continuation: String?,
        limit: Int
    ): Flow<Token> {
        val criteria = Criteria(tokenCollectionKey).`is`(collection)
            .addContinuation(continuation)
        val query = Query(criteria).with(Sort.by("_id").ascending())
        query.limit(limit)
        return mongo.find(query, Token::class.java).asFlow()
    }

    private fun Criteria.addContinuation(
        continuation: String?
    ) = if (continuation == null) {
        this
    } else {
        and("_id").gt(continuation)
    }

    private fun Query.withSortByLastUpdateAndId() =
        with(Sort.by(Sort.Direction.DESC, Token::updatedAt.name, "_id"))

    private fun fromToCriteria(
        lastUpdatedFrom: Instant?,
        lastUpdatedTo: Instant?,
    ): Criteria {
        if (lastUpdatedFrom == null && lastUpdatedTo == null) return Criteria()

        val criteria = Criteria(Token::updatedAt.name)
        lastUpdatedFrom?.let { criteria.gt(it) }
        lastUpdatedTo?.let { criteria.lt(it) }
        return criteria
    }

    suspend fun createIndexes() {
        logger.info("Ensuring indexes on ${Token.COLLECTION}")
        TokenIndexes.ALL_INDEXES.forEach { index ->
            mongo.indexOps(Token.COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    private object TokenIndexes {

        val tokenCollectionKey = Token::tokenMeta.name + "." + TokenMeta::collection.name + "." + "_id"

        val UPDATED_AT_AND_ID: Index = Index()
            .on(Token::updatedAt.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        val COLLECTION_AND_ID: Index = Index()
            .on(tokenCollectionKey, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .sparse()
            .background()

        val ALL_INDEXES = listOf(
            UPDATED_AT_AND_ID,
            COLLECTION_AND_ID
        )
    }
}
