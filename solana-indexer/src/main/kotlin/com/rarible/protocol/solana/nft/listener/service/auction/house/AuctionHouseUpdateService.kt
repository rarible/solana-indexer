package com.rarible.protocol.solana.nft.listener.service.auction.house

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.publisher.LogRecordEventPublisher
import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.solana.common.filter.auctionHouse.SolanaAuctionHouseFilter
import com.rarible.protocol.solana.common.model.AuctionHouse
import com.rarible.protocol.solana.common.model.AuctionHouseId
import com.rarible.protocol.solana.common.model.isEmpty
import com.rarible.protocol.solana.common.records.SolanaAuctionHouseOrderRecord
import com.rarible.protocol.solana.common.records.SolanaOrderUpdateInstruction
import com.rarible.protocol.solana.common.records.SubscriberGroup
import com.rarible.protocol.solana.common.repository.AuctionHouseRepository
import com.rarible.protocol.solana.common.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AuctionHouseUpdateService(
    private val auctionHouseRepository: AuctionHouseRepository,
    private val auctionHouseFilter: SolanaAuctionHouseFilter,
    private val orderRepository: OrderRepository,
    private val logRecordEventPublisher: LogRecordEventPublisher
) : EntityService<AuctionHouseId, AuctionHouse> {
    override suspend fun get(id: AuctionHouseId): AuctionHouse? {
        return auctionHouseRepository.findByAccount(id)
    }

    override suspend fun update(entity: AuctionHouse): AuctionHouse {
        if (entity.isEmpty) {
            logger.info("AuctionHouse is empty, skipping it: {}", entity.account)
            return entity
        }
        if (!auctionHouseFilter.isAcceptableForUpdateAuctionHouse(entity.account)) {
            logger.info("AuctionHouse update is ignored because auction house ${entity.account} is filtered out")
            return entity
        }
        val auctionHouse = auctionHouseRepository.save(entity)

        orderRepository.findByAuctionHouse(entity.id).chunked(1_000).collect { batch ->
            val events = batch.map {
                val fakeBalanceUpdateRecord = SolanaAuctionHouseOrderRecord.InternalOrderUpdateRecord(
                    mint = "",
                    timestamp = it.updatedAt, // This TS taken from record event
                    auctionHouse = it.auctionHouse,
                    orderId = it.id,
                    instruction = SolanaOrderUpdateInstruction.AuctionHouseUpdate(
                        entity.sellerFeeBasisPoints,
                        entity.requiresSignOff
                    )
                )
                LogRecordEvent(fakeBalanceUpdateRecord, false)
            }

            logRecordEventPublisher.publish(
                groupId = SubscriberGroup.AUCTION_HOUSE_ORDER.id,
                logRecordEvents = events
            )
        }
        logger.info("Updated AuctionHouse: $entity")
        return auctionHouse
    }

    private fun <T> Flow<T>.chunked(batchSize: Int): Flow<List<T>> = flow {
        val accumulator = ArrayList<T>()
        var counter = 0

        this@chunked.collect {
            accumulator.add(it)

            if (++counter == batchSize) {
                emit(accumulator)

                accumulator.clear()
                counter = 0
            }
        }

        if (accumulator.size != 0) {
            emit(accumulator)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuctionHouseUpdateService::class.java)
    }
}