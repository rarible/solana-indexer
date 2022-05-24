package com.rarible.protocol.solana.nft.listener.service.order

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.publisher.LogRecordEventPublisher
import com.rarible.protocol.solana.common.event.EscrowMintEvent
import com.rarible.protocol.solana.common.model.Escrow
import com.rarible.protocol.solana.common.model.OrderStatus
import com.rarible.protocol.solana.common.records.SolanaAuctionHouseOrderRecord
import com.rarible.protocol.solana.common.records.SolanaOrderUpdateInstruction
import com.rarible.protocol.solana.common.records.SubscriberGroup
import com.rarible.protocol.solana.common.repository.OrderRepository
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OrderStatusEscrowUpdateService(
    private val orderRepository: OrderRepository,
    private val logRecordEventPublisher: LogRecordEventPublisher
) {
    private val logger = LoggerFactory.getLogger(OrderStatusEscrowUpdateService::class.java)

    suspend fun updateStatusOfBuyOrders(escrow: Escrow) {
        val orders = orderRepository.findBuyOrders(
            maker = escrow.wallet,
            statuses = listOf(OrderStatus.ACTIVE, OrderStatus.INACTIVE),
            auctionHouse = escrow.auctionHouse
        ).toList().filter {
            // skip last mint event, it will be handled in order reducer
            it.id != (escrow.lastEvent as? EscrowMintEvent)?.orderId
        }

        if (orders.isEmpty()) {
            return // Just to avoid unnecessary logging
        }

        logger.info("Publishing {} order updates for escrow {}", orders.size, escrow)

        val events = orders.map { order ->
            val fakeBalanceUpdateRecord = SolanaAuctionHouseOrderRecord.InternalOrderUpdateRecord(
                mint = "",
                timestamp = escrow.updatedAt, // This TS taken from record event
                auctionHouse = order.auctionHouse,
                orderId = order.id,
                instruction = SolanaOrderUpdateInstruction.EscrowUpdate
            )
            LogRecordEvent(fakeBalanceUpdateRecord, false)
        }

        logRecordEventPublisher.publish(
            groupId = SubscriberGroup.AUCTION_HOUSE_ORDER.id,
            logRecordEvents = events
        )

    }
}