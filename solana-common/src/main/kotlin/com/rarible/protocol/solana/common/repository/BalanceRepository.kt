package com.rarible.protocol.solana.common.repository

import com.rarible.protocol.solana.common.continuation.DateIdContinuation
import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.common.model.BalanceId
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
import java.math.BigInteger

@Component
class BalanceRepository(
    private val mongo: ReactiveMongoOperations
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun save(balance: Balance): Balance =
        mongo.save(balance).awaitFirst()

    suspend fun findByAccount(account: BalanceId): Balance? =
        mongo.findById<Balance>(account).awaitFirstOrNull()

    fun findByMintAndOwner(
        mint: String,
        owner: String,
        includeDeleted: Boolean
    ): Flow<Balance> {
        val criteria = Criteria()
            .addMint(mint)
            .addOwner(owner)
            .includeDeleted(includeDeleted)

        val query = Query(criteria).with(Sort.by("_id").ascending())
        return mongo.find(query, Balance::class.java).asFlow()
    }

    fun findByOwner(
        owner: String,
        continuation: DateIdContinuation?,
        includeDeleted: Boolean
    ): Flow<Balance> {
        val criteria = Criteria()
            .addOwner(owner)
            .includeDeleted(includeDeleted)
            .addContinuation(continuation)

        val query = Query(criteria).withSortByLastUpdateAndId()

        return mongo.find(query, Balance::class.java).asFlow()
    }

    fun findByMint(
        mint: String,
        continuation: DateIdContinuation?,
        includeDeleted: Boolean
    ): Flow<Balance> {
        val criteria = Criteria()
            .addMint(mint)
            .includeDeleted(includeDeleted)
            .addContinuation(continuation)

        val query = Query(criteria).withSortByLastUpdateAndId()

        return mongo.find(query, Balance::class.java).asFlow()
    }

    private fun Query.withSortByLastUpdateAndId() =
        with(Sort.by(Sort.Direction.DESC, Balance::updatedAt.name, "_id"))

    private fun Criteria.addContinuation(continuation: DateIdContinuation?) =
        continuation?.let {
            orOperator(
                Criteria(Balance::updatedAt.name).isEqualTo(continuation.date).and("_id").lt(continuation.id),
                Criteria(Balance::updatedAt.name).lt(continuation.date)
            )
        } ?: this

    private fun Criteria.includeDeleted(includeDeleted: Boolean): Criteria {
        return if (includeDeleted) this else and(Balance::value.name).gt(BigInteger.ZERO)
    }

    private fun Criteria.addOwner(owner: String): Criteria {
        return and(Balance::owner.name).isEqualTo(owner)
    }

    private fun Criteria.addMint(owner: String): Criteria {
        return and(Balance::mint.name).isEqualTo(owner)
    }

    suspend fun createIndexes() {
        logger.info("Ensuring indexes on ${Balance.COLLECTION}")
        BalanceIndexes.ALL_INDEXES.forEach { index ->
            mongo.indexOps(Balance.COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    private object BalanceIndexes {

        val OWNER_AND_UPDATED_AT_AND_ID: Index = Index()
            .on(Balance::owner.name, Sort.Direction.ASC)
            .on(Balance::updatedAt.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        val MINT_AND_UPDATED_AT_AND_ID: Index = Index()
            .on(Balance::mint.name, Sort.Direction.ASC)
            .on(Balance::updatedAt.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        val OWNER_AND_MINT: Index = Index()
            .on(Balance::owner.name, Sort.Direction.ASC)
            .on(Balance::mint.name, Sort.Direction.ASC)
            .background()

        val ALL_INDEXES = listOf(
            OWNER_AND_UPDATED_AT_AND_ID,
            MINT_AND_UPDATED_AT_AND_ID,
            OWNER_AND_MINT
        )
    }

}
