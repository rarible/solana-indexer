package com.rarible.protocol.solana.nft.listener.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.solana.nft.listener.service.records.SolanaBalanceRecord
import com.rarible.protocol.solana.nft.listener.service.subscribers.SubscriberGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
@CaptureSpan(SpanType.DB)
class BalanceLogRepository(
    private val mongo: ReactiveMongoOperations
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val collection = SubscriberGroup.BALANCE.collectionName

    suspend fun createIndices() = runBlocking {
        Indices.ALL.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            mongo.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun findBalanceInitializationRecord(
        balanceAccount: String
    ): SolanaBalanceRecord.InitializeBalanceAccountRecord? {
        return mongo.findOne(
            Query.query(SolanaBalanceRecord.InitializeBalanceAccountRecord::balanceAccount isEqualTo balanceAccount),
            SolanaBalanceRecord.InitializeBalanceAccountRecord::class.java,
            collection
        ).awaitFirstOrNull()
    }

    suspend fun findBalanceInitializationRecords(
        balanceAccounts: List<String>
    ): Flow<SolanaBalanceRecord.InitializeBalanceAccountRecord> {
        return mongo.find(
            Query.query(SolanaBalanceRecord.InitializeBalanceAccountRecord::balanceAccount inValues balanceAccounts),
            SolanaBalanceRecord.InitializeBalanceAccountRecord::class.java,
            collection
        ).asFlow()
    }

    object Indices {

        private val BALANCE_ACCOUNT: Index = Index()
            .on(SolanaBalanceRecord.InitializeBalanceAccountRecord::balanceAccount.name, Sort.Direction.ASC)
            .sparse()
            .background()

        val ALL = listOf(
            BALANCE_ACCOUNT,
        )
    }
}

