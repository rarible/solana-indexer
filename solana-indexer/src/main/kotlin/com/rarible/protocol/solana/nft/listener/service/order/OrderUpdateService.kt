package com.rarible.protocol.solana.nft.listener.service.order

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.solana.common.filter.auctionHouse.SolanaAuctionHouseFilter
import com.rarible.protocol.solana.common.model.Order
import com.rarible.protocol.solana.common.model.OrderId
import com.rarible.protocol.solana.common.model.OrderStatus
import com.rarible.protocol.solana.common.model.isEmpty
import com.rarible.protocol.solana.common.pubkey.ProgramDerivedAddressCalc
import com.rarible.protocol.solana.common.pubkey.PublicKey
import com.rarible.protocol.solana.common.records.OrderDirection
import com.rarible.protocol.solana.common.repository.BalanceRepository
import com.rarible.protocol.solana.common.repository.EscrowRepository
import com.rarible.protocol.solana.common.repository.OrderRepository
import com.rarible.protocol.solana.common.update.OrderUpdateListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class OrderUpdateService(
    private val balanceRepository: BalanceRepository,
    private val orderRepository: OrderRepository,
    private val orderUpdateListener: OrderUpdateListener,
    private val escrowRepository: EscrowRepository,
    private val auctionHouseFilter: SolanaAuctionHouseFilter
) : EntityService<OrderId, Order> {

    override suspend fun get(id: OrderId): Order? =
        orderRepository.findById(id)

    override suspend fun update(entity: Order): Order {
        if (entity.isEmpty) {
            logger.info("Order in empty state: ${entity.id}")
            return entity
        }
        if (!auctionHouseFilter.isAcceptableAuctionHouse(entity.auctionHouse)) {
            logger.info("Order update is ignored because auction house ${entity.auctionHouse} is filtered out")
            return entity
        }

        val updated = entity.checkForUpdates()
        val exist = orderRepository.findById(entity.id)

        if (!requireUpdate(updated, exist)) {
            // Nothing changed in order record
            logger.info("Order $entity not changed")
            return entity
        }

        val order = orderRepository.save(updated)
        logger.info("Updated order: $order")

        orderUpdateListener.onOrderChanged(order)
        return order
    }

    private suspend fun Order.checkForUpdates(): Order {
        return updateMakeStock() // continue update chain if needed
    }

    private suspend fun Order.updateMakeStock(): Order {
        if (status == OrderStatus.CANCELLED || status == OrderStatus.FILLED) {
            return this.copy(makeStock = BigInteger.ZERO)
        }

        if (direction == OrderDirection.BUY) {
            val escrowAccount = ProgramDerivedAddressCalc.getEscrowPaymentAccount(
                PublicKey(maker),
                PublicKey(auctionHouse)
            ).address.toBase58()
            // if escrow not reduced atm, we consider that makeStock = make.amount
            val escrow = escrowRepository.findByAccount(escrowAccount)?.value ?: make.amount

            val makeStock =  if (escrow >= make.amount) make.amount else BigInteger.ZERO
            val updatedStatus = if (status == OrderStatus.ACTIVE && makeStock == BigInteger.ZERO) {
                OrderStatus.INACTIVE
            } else if (status == OrderStatus.INACTIVE && makeStock == make.amount) {
                OrderStatus.ACTIVE
            } else {
                status
            }
            return this.copy(makeStock = makeStock, status = updatedStatus)
        }

        val balance = makerAccount?.let { balanceRepository.findByAccount(it) }
        // Workaround for a race: balance has not been reduced yet.
        // Considering the order is active. When the balance changes, the status will become INACTIVE.
            ?: return this.copy(status = OrderStatus.ACTIVE)

        if (balance.owner != maker) {
            /**
             * CHARLIE-282: when the owner of the seller token account gets changed, delegate authority for the AuctionHouse is automatically ended.
             * AuctionHouse will not be able to execute the order because it cannot take the NFT from the token account anymore.
             * We mark such an order CANCELLED. Even if the token account's ownership returns to the seller account, the delegate authority of the AH is gone,
             * so we mark the order with non-changeable CANCELLED status other than INACTIVE, which can otherwise become ACTIVE after the owner changes.
             * To activate the order again, the user needs to explicitly put the item on sale.
             *
             * This is a side effect of how MagicEden marketplace works. When a user puts item on sale on MagicEden, the MagicEden
             * takes the token account from the seller (by SetAuthority instruction).
             * In fact, all AuctionHouse sell orders get invalidated from this moment.
             */
            logger.info("Cancelling the SELL order $id because the owner of the sell account was changed from $maker to ${balance.owner}")
            return this.copy(
                status = OrderStatus.CANCELLED,
                makeStock = BigInteger.ZERO
            )
        }

        val notFilledValue = maxOf(make.amount - fill, BigInteger.ZERO)
        val makeStock = minOf(notFilledValue, balance.value)

        val status = when {
            makeStock > BigInteger.ZERO -> OrderStatus.ACTIVE
            else -> OrderStatus.INACTIVE
        }

        return this.copy(
            status = status,
            makeStock = makeStock
        )
    }

    private fun requireUpdate(updated: Order, exist: Order?): Boolean {
        if (exist == null) return true

        // If nothing changed except updateAt, there is no sense to publish events
        return exist != updated.copy(updatedAt = exist.updatedAt)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OrderUpdateService::class.java)
    }
}