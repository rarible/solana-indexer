package com.rarible.protocol.solana.repository

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.protocol.solana.AbstractIntegrationTest
import com.rarible.protocol.solana.common.continuation.DateIdContinuation
import com.rarible.protocol.solana.common.continuation.PriceIdContinuation
import com.rarible.protocol.solana.common.model.OrderStatus
import com.rarible.protocol.solana.common.model.TokenFtAssetType
import com.rarible.protocol.solana.common.model.TokenNftAssetType
import com.rarible.protocol.solana.common.model.order.filter.OrderFilter
import com.rarible.protocol.solana.common.model.order.filter.OrderFilterSort
import com.rarible.protocol.solana.common.repository.OrderRepository
import com.rarible.protocol.solana.test.randomAsset
import com.rarible.protocol.solana.test.randomBuyOrder
import com.rarible.protocol.solana.test.randomSellOrder
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

class OrderRepositoryIt : AbstractIntegrationTest() {

    @Test
    fun `save and find by id`() = runBlocking<Unit> {
        val order = randomSellOrder()
        val saved = orderRepository.save(order)
        val found = orderRepository.findById(order.id)

        assertThat(saved).isEqualTo(found)
    }

    @Test
    fun `find by ids`() = runBlocking<Unit> {
        val order1 = orderRepository.save(randomSellOrder())
        val order2 = orderRepository.save(randomBuyOrder())

        // non-existing order should be skipped
        val found = orderRepository.findByIds(listOf(order1.id, randomString(), order2.id))
            .toList().associateBy { it.id }

        assertThat(found).hasSize(2)
        assertThat(found[order1.id]).isEqualTo(order1)
        assertThat(found[order2.id]).isEqualTo(order2)
    }

    @Test
    fun `find currency types - sell orders`() = runBlocking<Unit> {
        val mint = randomString()
        val mintAsset = randomAsset(TokenNftAssetType(mint))

        // 2 orders with same currency
        val sellOrder1 = orderRepository.save(randomSellOrder(make = mintAsset))
        orderRepository.save(randomSellOrder(make = mintAsset, take = sellOrder1.take))
        // 1 order with another currency
        val sellOrder3 = orderRepository.save(randomSellOrder(make = mintAsset))
        // 1 order not related to target mint
        orderRepository.save(randomSellOrder())

        // 2 buy orders, their currencies should not be in the result
        orderRepository.save(randomBuyOrder(take = mintAsset))
        orderRepository.save(randomBuyOrder())

        // TODO to make it test useful we need to have various asset types, not only wrapped sol asset type
        val expected = setOf(
            sellOrder1.take.type.tokenAddress,
            sellOrder3.take.type.tokenAddress
        )

        val currencies = orderRepository.findCurrencyTypesOfSellOrders(mint)
            .map { it.tokenAddress }.toList()

        assertThat(currencies).hasSize(2)
        assertThat(currencies.toSet()).isEqualTo(expected)
    }

    @Test
    fun `find currency types - buy orders`() = runBlocking<Unit> {
        val mint = randomString()
        val mintAsset = randomAsset(TokenNftAssetType(mint))

        // 2 orders with same currency
        val buyOrder1 = orderRepository.save(randomBuyOrder(take = mintAsset))
        orderRepository.save(randomBuyOrder(take = mintAsset, make = buyOrder1.make))
        // 1 order with another currency
        val buyOrder3 = orderRepository.save(randomBuyOrder(take = mintAsset))
        // 1 order not related to target mint
        orderRepository.save(randomBuyOrder())

        // 2 sell orders, their currencies should not be in the result
        orderRepository.save(randomSellOrder(make = mintAsset))
        orderRepository.save(randomSellOrder())

        // TODO to make it test useful we need to have various asset types, not only wrapped sol asset type
        val expected = setOf(
            buyOrder1.make.type.tokenAddress,
            buyOrder3.make.type.tokenAddress
        )

        val currencies = orderRepository.findCurrencyTypesOfBuyOrders(mint)
            .map { it.tokenAddress }.toList()

        assertThat(currencies).hasSize(2)
        assertThat(currencies.toSet()).isEqualTo(expected)
    }

    @Test
    fun `find all - no continuation`() = runBlocking<Unit> {
        val order1 = orderRepository.save(randomSellOrder())
        val order2 = orderRepository.save(randomSellOrder())

        // Should be filtered by status
        orderRepository.save(randomSellOrder().copy(status = OrderStatus.FILLED))

        // Sorted asc
        val expectedSorted = listOf(order1, order2).sortedBy { it.updatedAt }

        val filterDesc = OrderFilter.All(
            sort = OrderFilterSort.LAST_UPDATE_DESC,
            statuses = listOf(OrderStatus.ACTIVE)
        )

        val resultDesc = orderRepository.query(filterDesc.getQuery(1)).toList()

        assertThat(resultDesc).hasSize(1)
        assertThat(resultDesc[0]).isEqualTo(expectedSorted.last())

        val filterAsc = OrderFilter.All(
            sort = OrderFilterSort.LAST_UPDATE_ASC,
            statuses = listOf(OrderStatus.ACTIVE)
        )

        val resultAsc = orderRepository.query(filterAsc.getQuery(1)).toList()

        assertThat(resultAsc).hasSize(1)
        assertThat(resultAsc[0]).isEqualTo(expectedSorted.first())
    }

    @Test
    fun `find all - with continuation`() = runBlocking<Unit> {
        val order1 = orderRepository.save(randomSellOrder())
        val order2 = orderRepository.save(randomSellOrder().copy(updatedAt = order1.updatedAt.plusSeconds(1)))

        val continuation = DateIdContinuation(order1.updatedAt, order1.id)

        val filterDesc = OrderFilter.All(
            sort = OrderFilterSort.LAST_UPDATE_DESC,
            continuation = continuation,
            statuses = listOf(OrderStatus.ACTIVE)
        )

        val resultDesc = orderRepository.query(filterDesc.getQuery(1)).toList()

        assertThat(resultDesc).hasSize(0)

        val filterAsc = OrderFilter.All(
            sort = OrderFilterSort.LAST_UPDATE_ASC,
            continuation = continuation,
            statuses = listOf(OrderStatus.ACTIVE)
        )

        val resultAsc = orderRepository.query(filterAsc.getQuery(1)).toList()

        assertThat(resultAsc).hasSize(1)
        assertThat(resultAsc[0]).isEqualTo(order2)
    }

    @Test
    fun `find sell orders by item`() = runBlocking<Unit> {
        val currencyMint = randomString()
        val nftMint = randomString()

        val currencyAsset = randomAsset(TokenFtAssetType(currencyMint))
        val nftAsset = randomAsset(TokenNftAssetType(nftMint))

        val maker = randomString()

        val order1 = orderRepository.save(randomSellOrder(make = nftAsset, take = currencyAsset))
        val order2 = orderRepository.save(randomSellOrder(make = nftAsset, take = currencyAsset))
        val order3 = orderRepository.save(randomSellOrder(make = nftAsset, take = currencyAsset).copy(maker = maker))

        // Should not be found due to status filter
        orderRepository.save(
            randomSellOrder(make = nftAsset, take = currencyAsset)
                .copy(status = OrderStatus.FILLED)
        )
        // Should be not found despite make price is zero (lowest)
        orderRepository.save(randomSellOrder(make = nftAsset).copy(makePrice = BigDecimal.ZERO))

        // Minimal set of filtered fields
        val filter = OrderFilter.SellByItem(
            statuses = listOf(OrderStatus.ACTIVE),
            currency = currencyMint,
            tokenAddress = nftMint,
            makers = null,
            continuation = null
        )

        val expectedSorted = listOf(order1, order2, order3).sortedBy { it.makePrice }
        val result1 = orderRepository.query(filter.getQuery(2)).toList()

        assertThat(result1).hasSize(2)
        assertThat(result1[0]).isEqualTo(expectedSorted[0])
        assertThat(result1[1]).isEqualTo(expectedSorted[1])

        // With maker
        val filterWithMaker = OrderFilter.SellByItem(
            statuses = listOf(OrderStatus.ACTIVE),
            currency = currencyMint,
            tokenAddress = nftMint,
            makers = listOf(maker),
            continuation = null
        )

        val result2 = orderRepository.query(filterWithMaker.getQuery(2)).toList()

        assertThat(result2).hasSize(1)
        assertThat(result2[0]).isEqualTo(order3)

        // With continuation
        val from = expectedSorted[1] // only last from the expected orders should be returned
        val filterWithContinuation = OrderFilter.SellByItem(
            statuses = listOf(OrderStatus.ACTIVE),
            currency = currencyMint,
            tokenAddress = nftMint,
            makers = null,
            continuation = PriceIdContinuation(from.makePrice, from.id)
        )

        val result3 = orderRepository.query(filterWithContinuation.getQuery(2)).toList()

        assertThat(result3).hasSize(1)
        assertThat(result3[0]).isEqualTo(expectedSorted[2])
    }

    @Test
    fun `find buy orders by item`() = runBlocking<Unit> {
        val currencyMint = randomString()
        val nftMint = randomString()

        val currencyAsset = randomAsset(TokenFtAssetType(currencyMint))
        val nftAsset = randomAsset(TokenNftAssetType(nftMint))

        val maker = randomString()
        val old = nowMillis().minusSeconds(120)

        val order1 = orderRepository.save(randomBuyOrder(take = nftAsset, make = currencyAsset))
        val order2 = orderRepository.save(randomBuyOrder(take = nftAsset, make = currencyAsset))
        val order3 = orderRepository.save(randomBuyOrder(take = nftAsset, make = currencyAsset).copy(maker = maker))
        val order4 = orderRepository.save(
            randomBuyOrder(take = nftAsset, make = currencyAsset)
                .copy(maker = maker, createdAt = old)
        )

        // Should not be found due to status filter
        orderRepository.save(
            randomBuyOrder(take = nftAsset, make = currencyAsset)
                .copy(status = OrderStatus.FILLED)
        )
        // Should be not found despite take price is very high
        orderRepository.save(randomBuyOrder(take = nftAsset).copy(takePrice = BigDecimal(Long.MAX_VALUE)))

        // Minimal set of filtered fields
        val filter = OrderFilter.BuyByItem(
            statuses = listOf(OrderStatus.ACTIVE),
            currency = currencyMint,
            tokenAddress = nftMint,
            makers = null,
            continuation = null
        )

        val expectedSorted1 = listOf(order1, order2, order3, order4).sortedByDescending { it.takePrice }
        val result1 = orderRepository.query(filter.getQuery(2)).toList()

        assertThat(result1).hasSize(2)
        assertThat(result1[0]).isEqualTo(expectedSorted1[0])
        assertThat(result1[1]).isEqualTo(expectedSorted1[1])

        // With maker
        val filterWithMaker = OrderFilter.BuyByItem(
            statuses = listOf(OrderStatus.ACTIVE),
            currency = currencyMint,
            tokenAddress = nftMint,
            makers = listOf(maker),
            continuation = null
        )

        val expectedSorted2 = listOf(order3, order4).sortedByDescending { it.takePrice }
        val result2 = orderRepository.query(filterWithMaker.getQuery(3)).toList()

        assertThat(result2).hasSize(2)
        assertThat(result2).isEqualTo(expectedSorted2)

        // With continuation
        val from = expectedSorted1[2] // only last from the expected orders should be returned
        val filterWithContinuation = OrderFilter.BuyByItem(
            statuses = listOf(OrderStatus.ACTIVE),
            currency = currencyMint,
            tokenAddress = nftMint,
            makers = null,
            continuation = PriceIdContinuation(from.takePrice, from.id)
        )

        val result3 = orderRepository.query(filterWithContinuation.getQuery(2)).toList()

        assertThat(result3).hasSize(1)
        assertThat(result3[0]).isEqualTo(expectedSorted1[3])

        // With maker and start/end
        val filterWithMakerStartEnd = OrderFilter.BuyByItem(
            statuses = listOf(OrderStatus.ACTIVE),
            currency = currencyMint,
            tokenAddress = nftMint,
            makers = listOf(maker),
            continuation = null,
            start = old.minusSeconds(1),
            end = old.plusSeconds(1)
        )

        val result4 = orderRepository.query(filterWithMakerStartEnd.getQuery(3)).toList()

        assertThat(result4).hasSize(1)
        assertThat(result4[0]).isEqualTo(order4)
    }

    @Test
    fun `find sell orders`() = runBlocking<Unit> {
        val order1 = orderRepository.save(randomSellOrder())
        val order2 = orderRepository.save(randomSellOrder())
        val order3 = orderRepository.save(randomSellOrder())
        val order4 = orderRepository.save(randomSellOrder().copy(status = OrderStatus.FILLED))

        // No filters
        val filter1 = OrderFilter.Sell(
            sort = OrderFilterSort.LAST_UPDATE_DESC
        )

        val expectedSorted1 = listOf(order1, order2, order3, order4).sortedByDescending { it.updatedAt }

        val result1 = orderRepository.query(filter1.getQuery(5)).toList()

        assertThat(result1).isEqualTo(expectedSorted1)

        // All filters
        val filter2 = OrderFilter.Sell(
            sort = OrderFilterSort.LAST_UPDATE_ASC,
            statuses = listOf(OrderStatus.ACTIVE),
            makers = listOf(order3.maker)
        )

        val result2 = orderRepository.query(filter2.getQuery(5)).toList()

        assertThat(result2).hasSize(1)
        assertThat(result2[0]).isEqualTo(order3)

        // With continuation
        val expectedSorted3 = listOf(order1, order2, order4).sortedBy { it.updatedAt }
        val from = expectedSorted3[1]
        val filter3 = OrderFilter.Sell(
            sort = OrderFilterSort.LAST_UPDATE_ASC,
            statuses = null,
            makers = listOf(order1.maker, order2.maker, order4.maker),
            continuation = DateIdContinuation(from.updatedAt, from.id)
        )

        val result3 = orderRepository.query(filter3.getQuery(5)).toList()

        assertThat(result3).hasSize(1)
        assertThat(result3[0]).isEqualTo(expectedSorted3[2])
    }

    @Test
    fun `find buy orders`() = runBlocking<Unit> {
        val old = nowMillis().minusSeconds(120)

        val order1 = orderRepository.save(randomBuyOrder())
        val order2 = orderRepository.save(randomBuyOrder())
        val order3 = orderRepository.save(randomBuyOrder().copy(status = OrderStatus.CANCELLED))
        val order4 = orderRepository.save(randomBuyOrder().copy(updatedAt = old, createdAt = old))

        // No filters
        val filter = OrderFilter.Buy(
            sort = OrderFilterSort.LAST_UPDATE_DESC
        )

        val expectedSorted1 = listOf(order1, order2, order3, order4).sortedByDescending { it.updatedAt }
        val result1 = orderRepository.query(filter.getQuery(5)).toList()

        assertThat(result1).isEqualTo(expectedSorted1)

        // All filters
        val filterWithMaker = OrderFilter.Buy(
            sort = OrderFilterSort.LAST_UPDATE_DESC,
            statuses = listOf(OrderStatus.ACTIVE),
            makers = listOf(order1.maker, order3.maker, order4.maker),
            start = old.minusSeconds(1),
            end = old.plusSeconds(1),
            continuation = null
        )

        val result2 = orderRepository.query(filterWithMaker.getQuery(3)).toList()

        assertThat(result2).hasSize(1)
        assertThat(result2[0]).isEqualTo(order4)

        // With continuation
        val filterWithContinuation = OrderFilter.Buy(
            sort = OrderFilterSort.LAST_UPDATE_ASC,
            statuses = listOf(OrderStatus.ACTIVE),
            makers = listOf(order1.maker, order4.maker),
            continuation = DateIdContinuation(order4.updatedAt, order4.id) // order4 is older than order1
        )

        val result3 = orderRepository.query(filterWithContinuation.getQuery(2)).toList()

        assertThat(result3).hasSize(1)
        assertThat(result3[0]).isEqualTo(order1)
    }

}