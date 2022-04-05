package com.rarible.protocol.solana.nft.listener.service.order

import com.rarible.core.test.data.randomString
import com.rarible.protocol.solana.common.model.Asset
import com.rarible.protocol.solana.common.model.OrderStatus
import com.rarible.protocol.solana.common.model.TokenNftAssetType
import com.rarible.protocol.solana.common.update.OrderUpdateListener
import com.rarible.protocol.solana.nft.listener.AbstractBlockScannerTest
import com.rarible.protocol.solana.test.createRandomBalance
import com.rarible.protocol.solana.test.randomBuyOrder
import com.rarible.protocol.solana.test.randomSellOrder
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

class OrderUpdateServiceIt : AbstractBlockScannerTest() {

    lateinit var orderUpdateService: OrderUpdateService

    private val orderUpdateListener: OrderUpdateListener = mockk()

    @BeforeEach
    fun beforeEach() {
        clearMocks(orderUpdateListener)
        coEvery { orderUpdateListener.onOrderChanged(any()) } returns Unit
        orderUpdateService = OrderUpdateService(
            balanceRepository,
            orderRepository,
            orderUpdateListener
        )
    }

    @Test
    fun `not initialized order`() = runBlocking<Unit> {
        val order = randomSellOrder().copy(createdAt = Instant.EPOCH)

        orderUpdateService.update(order)

        assertThat(orderRepository.findById(order.id)).isNull()
        coVerify(exactly = 0) { orderUpdateListener.onOrderChanged(any()) }
    }

    @Test
    fun `new order inserted`() = runBlocking<Unit> {
        val order = randomSellOrder()

        orderUpdateService.update(order)

        // Order saved, event sent
        val saved = orderRepository.findById(order.id)!!
        coVerify(exactly = 1) { orderUpdateListener.onOrderChanged(saved) }
    }

    @Test
    fun `existing order not updated`() = runBlocking<Unit> {
        // There is no balance in DB, so we consider this order as INACTIVE
        val order = orderRepository.save(randomSellOrder().copy(status = OrderStatus.INACTIVE))

        orderUpdateService.update(order)

        // Update skipped, order not changed
        val saved = orderRepository.findById(order.id)!!
        assertThat(saved).isEqualTo(order)
        coVerify(exactly = 0) { orderUpdateListener.onOrderChanged(saved) }
    }

    @Test
    fun `existing order not updated - without balance, not a sell order`() = runBlocking<Unit> {
        // ACTIVE order, but without balance - it should stay ACTIVE since it is a BUY order
        val order = orderRepository.save(randomBuyOrder())

        orderUpdateService.update(order)

        // Update skipped, order not changed
        val saved = orderRepository.findById(order.id)!!
        assertThat(saved).isEqualTo(order)
        coVerify(exactly = 0) { orderUpdateListener.onOrderChanged(saved) }
    }

    @Test
    fun `existing order not updated - without balance, not a target status`() = runBlocking<Unit> {
        // ACTIVE order, but without balance - it should stay ACTIVE since it has FILLED status
        val order = orderRepository.save(randomBuyOrder().copy(status = OrderStatus.FILLED))

        orderUpdateService.update(order)

        // Update skipped, order not changed
        val saved = orderRepository.findById(order.id)!!
        assertThat(saved).isEqualTo(order)
        coVerify(exactly = 0) { orderUpdateListener.onOrderChanged(saved) }
    }

    @Test
    fun `existing order not updated - balance not changed`() = runBlocking<Unit> {
        val mint = randomString()
        val owner = randomString()

        val balance = balanceRepository.save(createRandomBalance(owner = owner, mint = mint))

        val order = orderRepository.save(
            randomSellOrder(make = Asset(TokenNftAssetType(mint), balance.value), maker = owner)
        )

        orderUpdateService.update(order)

        // Update skipped, order not changed
        val saved = orderRepository.findById(order.id)!!
        assertThat(saved).isEqualTo(order)
        coVerify(exactly = 0) { orderUpdateListener.onOrderChanged(saved) }
    }

    @Test
    fun `existing order updated - balance less than make stock`() = runBlocking<Unit> {
        val mint = randomString()
        val owner = randomString()

        balanceRepository.save(createRandomBalance(owner = owner, mint = mint, value = BigInteger.ONE))

        val order = orderRepository.save(
            randomSellOrder(make = Asset(TokenNftAssetType(mint), BigInteger.TEN), maker = owner)
        )

        orderUpdateService.update(order)

        // Order status should be changed from ACTIVE to INACTIVE
        val saved = orderRepository.findById(order.id)!!
        assertThat(saved).isEqualTo(order.copy(status = OrderStatus.INACTIVE))
        coVerify(exactly = 1) { orderUpdateListener.onOrderChanged(saved) }
    }

    @Test
    fun `existing order updated`() = runBlocking<Unit> {
        val order = orderRepository.save(randomSellOrder())
        val updated = order.copy(takePrice = BigDecimal.TEN)

        orderUpdateService.update(updated)

        // Order updated, event sent
        val saved = orderRepository.findById(order.id)!!
        assertThat(saved.takePrice).isEqualTo(updated.takePrice)
        coVerify(exactly = 1) { orderUpdateListener.onOrderChanged(saved) }
    }

    @Test
    fun `existing order updated - balance greater than make stock`() = runBlocking<Unit> {
        val mint = randomString()
        val owner = randomString()

        balanceRepository.save(createRandomBalance(owner = owner, mint = mint, value = BigInteger.TEN))

        val order = orderRepository.save(
            randomSellOrder(make = Asset(TokenNftAssetType(mint), BigInteger.ONE), maker = owner)
                .copy(status = OrderStatus.INACTIVE)
        )

        orderUpdateService.update(order)

        // Order status should be changed from INACTIVE to ACTIVE
        val saved = orderRepository.findById(order.id)!!
        assertThat(saved).isEqualTo(order.copy(status = OrderStatus.ACTIVE))
        coVerify(exactly = 1) { orderUpdateListener.onOrderChanged(saved) }
    }

}