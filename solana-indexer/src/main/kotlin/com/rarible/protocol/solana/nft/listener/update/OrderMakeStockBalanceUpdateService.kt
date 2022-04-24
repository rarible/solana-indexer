package com.rarible.protocol.solana.nft.listener.update

import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.common.model.OrderStatus
import com.rarible.protocol.solana.common.records.SolanaAuctionHouseOrderRecord
import com.rarible.protocol.solana.common.records.SolanaOrderUpdateInstruction
import com.rarible.protocol.solana.common.repository.OrderRepository
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Service responsible for generating an event to trigger updating the make stock of a sell order
 * when the corresponding maker's balance changes.
 */
@Component
class OrderMakeStockBalanceUpdateService(
    private val orderRepository: OrderRepository,
    private val internalUpdateEventService: InternalUpdateEventService
) {
    private val logger = LoggerFactory.getLogger(OrderMakeStockBalanceUpdateService::class.java)

    suspend fun updateMakeStockOfSellOrders(balance: Balance) {
        val orders = orderRepository.findSellOrdersByMintAndMaker(
            mint = balance.mint,
            maker = balance.owner,
            statuses = listOf(OrderStatus.ACTIVE, OrderStatus.INACTIVE)
        ).toList()
        if (orders.isEmpty()) {
            return // Just to avoid unnecessary logging
        }

        logger.info("Publishing {} order updates for balance {}", orders.size, balance)
        val records = orders.map { order ->
            SolanaAuctionHouseOrderRecord.InternalOrderUpdateRecord(
                mint = balance.mint,
                timestamp = balance.updatedAt, // This TS taken from record event
                auctionHouse = order.auctionHouse,
                orderId = order.id,
                instruction = SolanaOrderUpdateInstruction.BalanceUpdate(balance.account)
            )
        }
        internalUpdateEventService.sendInternalOrderUpdateRecords(records)
    }
}