package com.rarible.protocol.solana.repository

import com.rarible.protocol.solana.AbstractIntegrationTest
import com.rarible.protocol.solana.common.repository.SolanaAuctionHouseOrderRecordsRepository
import com.rarible.protocol.solana.test.ActivityDataFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.query.Criteria

internal class SolanaAuctionHouseOrderRecordsRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var orderRecordsRepository: SolanaAuctionHouseOrderRecordsRepository

    @Test
    fun `save and find record`() = runBlocking<Unit> {
        val record = ActivityDataFactory.randomBuyRecord()
        orderRecordsRepository.save(record)

        val result = orderRecordsRepository.findBy(Criteria()).toList()

        assertEquals(record, result.single())
    }

    @Test
    fun `records order`() = runBlocking<Unit> {
        val records = listOf(
            ActivityDataFactory.randomBuyRecord(),
            ActivityDataFactory.randomCancel(),
            ActivityDataFactory.randomSellRecord(),
            ActivityDataFactory.randomExecuteSaleRecord(),
        )
        records.forEach { orderRecordsRepository.save(it) }

        orderRecordsRepository.findBy(Criteria(), asc = true).toList().let { result ->
            val expected = records.sortedWith { a, b ->
                if (a.timestamp == b.timestamp) a.id.compareTo(b.id) else a.timestamp.compareTo(b.timestamp)
            }

            assertEquals(expected, result)
        }

        orderRecordsRepository.findBy(Criteria(), asc = false).toList().let { result ->
            val expected = records.sortedWith { a, b ->
                if (a.timestamp == b.timestamp) -a.id.compareTo(b.id) else -a.timestamp.compareTo(b.timestamp)
            }

            assertEquals(expected, result)
        }
    }
}