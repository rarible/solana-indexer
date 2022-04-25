package com.rarible.protocol.solana.nft.listener.update

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.solana.common.model.Order
import com.rarible.protocol.solana.common.model.OrderId
import com.rarible.protocol.solana.common.model.OrderStatus
import com.rarible.protocol.solana.common.model.isEmpty
import com.rarible.protocol.solana.common.records.OrderDirection
import com.rarible.protocol.solana.common.repository.BalanceRepository
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
) : EntityService<OrderId, Order> {

    override suspend fun get(id: OrderId): Order? =
        orderRepository.findById(id)

    override suspend fun update(entity: Order): Order {
        if (entity.isEmpty) {
            logger.info("Order in empty state: ${entity.id}")
            return entity
        }

        val updated = entity.checkForUpdates()
        val exist = orderRepository.findById(entity.id)

        if (!shouldUpdate(updated, exist)) {
            // Nothing changed in order record
            logger.info("Order $entity is not changed, skipping save")
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
        if (direction == OrderDirection.BUY) {
            // TODO[bids]: we don't fully support the bids yet (we don't have a currency reducer),
            //  so we consider the makeStock is always enough (equal to make.amount) if the order is active.
            val makeStock = when (status) {
                OrderStatus.ACTIVE -> make.amount
                OrderStatus.INACTIVE -> BigInteger.ZERO
                OrderStatus.CANCELLED -> BigInteger.ZERO
                OrderStatus.FILLED -> BigInteger.ZERO
            }
            return this.copy(makeStock = makeStock)
        }
        if (status == OrderStatus.CANCELLED || status == OrderStatus.FILLED) {
            return this.copy(makeStock = BigInteger.ZERO)
        }

        val balance = makerAccount?.let { balanceRepository.findByAccount(it) }
        // Workaround for a race: balance has not been reduced yet.
        // Considering the order is active. When the balance changes, the status will become INACTIVE.
            ?: return this.copy(status = OrderStatus.ACTIVE)

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

    private fun shouldUpdate(updated: Order, exist: Order?): Boolean {
        if (exist == null) return true

        // If nothing changed except updateAt, there is no sense to publish events
        return exist != updated.copy(updatedAt = exist.updatedAt)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OrderUpdateService::class.java)
    }
}