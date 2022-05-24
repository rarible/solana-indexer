package com.rarible.protocol.solana.nft.api.controller

import com.rarible.protocol.solana.api.controller.OrderControllerApi
import com.rarible.protocol.solana.common.continuation.ContinuationFactory
import com.rarible.protocol.solana.common.continuation.DateIdContinuation
import com.rarible.protocol.solana.common.continuation.OrderContinuation
import com.rarible.protocol.solana.common.continuation.Paging
import com.rarible.protocol.solana.common.continuation.PriceIdContinuation
import com.rarible.protocol.solana.common.converter.AssetConverter
import com.rarible.protocol.solana.common.converter.OrderConverter
import com.rarible.protocol.solana.common.model.Order
import com.rarible.protocol.solana.common.model.OrderStatus
import com.rarible.protocol.solana.common.model.order.filter.OrderFilter
import com.rarible.protocol.solana.common.model.order.filter.OrderFilterSort
import com.rarible.protocol.solana.dto.OrderCurrenciesDto
import com.rarible.protocol.solana.dto.OrderDto
import com.rarible.protocol.solana.dto.OrderIdsDto
import com.rarible.protocol.solana.dto.OrderSortDto
import com.rarible.protocol.solana.dto.OrderStatusDto
import com.rarible.protocol.solana.dto.OrdersDto
import com.rarible.protocol.solana.dto.SyncSortDto
import com.rarible.protocol.solana.nft.api.service.OrderApiService
import com.rarible.protocol.union.dto.continuation.page.PageSize
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class OrderController(
    private val orderApiService: OrderApiService,
    private val orderConverter: OrderConverter,
    private val assetConverter: AssetConverter
) : OrderControllerApi {

    override suspend fun getOrderById(id: String): ResponseEntity<OrderDto> {
        val order = orderApiService.getOrderById(id)

        return ResponseEntity.ok(orderConverter.convert(order))
    }

    override suspend fun getOrdersByIds(orderIdsDto: OrderIdsDto): ResponseEntity<OrdersDto> {
        val orders = orderApiService.findByIds(orderIdsDto.ids)
            .map { orderConverter.convert(it) }

        return ResponseEntity.ok(OrdersDto(null, orders))
    }

    override suspend fun getOrdersSync(
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val safeSort = sort?.fromDto() ?: OrderFilterSort.DB_UPDATE_ASC
        val orderFilter = OrderFilter.SyncAll(
            sort = safeSort,
            continuation = DateIdContinuation.parse(continuation)
        )

        val orders = orderApiService.getOrders(orderFilter, safeSize)
        return ResponseEntity.ok(toSlice(orders, safeSort, safeSize))
    }

    override suspend fun getOrdersAll(
        continuation: String?,
        size: Int?,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val safeSort = sort?.fromDto() ?: OrderFilterSort.LAST_UPDATE_DESC

        val orderFilter = OrderFilter.All(
            statuses = status?.fromDto(),
            sort = safeSort,
            continuation = DateIdContinuation.parse(continuation)
        )

        val orders = orderApiService.getOrders(orderFilter, safeSize)

        return ResponseEntity.ok(toSlice(orders, safeSort, safeSize))
    }

    override suspend fun getSellCurrencies(itemId: String): ResponseEntity<OrderCurrenciesDto> {
        val assetTypes = orderApiService.getSellOrderCurrencies(itemId)
        val dto = assetTypes.map { assetConverter.convert(it) }
        return ResponseEntity.ok(OrderCurrenciesDto(dto))
    }

    override suspend fun getSellOrders(
        origin: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val sort = OrderFilterSort.LAST_UPDATE_DESC

        val orderFilter = OrderFilter.Sell(
            statuses = listOf(OrderStatus.ACTIVE),
            makers = null,
            sort = sort,
            continuation = DateIdContinuation.parse(continuation)
        )

        val orders = orderApiService.getOrders(orderFilter, safeSize)

        return ResponseEntity.ok(toSlice(orders, sort, safeSize))
    }

    override suspend fun getSellOrdersByItem(
        itemId: String,
        currencyId: String,
        maker: List<String>?,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)

        val filter = OrderFilter.SellByItem(
            statuses = status?.fromDto(),
            currency = currencyId,
            tokenAddress = itemId,
            makers = maker,
            continuation = PriceIdContinuation.parse(continuation)
        )

        val orders = orderApiService.getOrders(filter, safeSize)

        val dto = toSlice(orders, OrderContinuation.BySellPriceAndIdAsc, safeSize)
        return ResponseEntity.ok(dto)
    }

    override suspend fun getSellOrdersByMaker(
        maker: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val sort = OrderFilterSort.LAST_UPDATE_DESC

        val orderFilter = OrderFilter.Sell(
            statuses = status?.fromDto(),
            makers = listOf(maker),
            sort = sort,
            continuation = DateIdContinuation.parse(continuation)
        )

        val orders = orderApiService.getOrders(orderFilter, safeSize)

        return ResponseEntity.ok(toSlice(orders, sort, safeSize))
    }

    override suspend fun getBidCurrencies(itemId: String): ResponseEntity<OrderCurrenciesDto> {
        val assetTypes = orderApiService.getBuyOrderCurrencies(itemId)
        val dto = assetTypes.map { assetConverter.convert(it) }
        return ResponseEntity.ok(OrderCurrenciesDto(dto))
    }

    override suspend fun getOrderBidsByItem(
        itemId: String,
        currencyId: String,
        status: List<OrderStatusDto>,
        maker: List<String>?,
        origin: String?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)

        val filter = OrderFilter.BuyByItem(
            statuses = status.fromDto(),
            currency = currencyId,
            tokenAddress = itemId,
            makers = maker,
            continuation = PriceIdContinuation.parse(continuation),
            start = start?.let { Instant.ofEpochMilli(it) },
            end = end?.let { Instant.ofEpochMilli(it) }
        )

        val orders = orderApiService.getOrders(filter, safeSize)

        val dto = toSlice(orders, OrderContinuation.ByBuyPriceAndIdDesc, safeSize)
        return ResponseEntity.ok(dto)
    }

    override suspend fun getOrderBidsByMaker(
        maker: String,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OrdersDto> {
        val safeSize = PageSize.ORDER.limit(size)
        val sort = OrderFilterSort.LAST_UPDATE_DESC

        val filter = OrderFilter.Buy(
            sort = sort,
            statuses = status?.fromDto(),
            makers = listOf(maker),
            continuation = DateIdContinuation.parse(continuation),
            start = start?.let { Instant.ofEpochMilli(it) },
            end = start?.let { Instant.ofEpochMilli(it) },
        )

        val orders = orderApiService.getOrders(filter, safeSize)

        val dto = toSlice(orders, sort, safeSize)
        return ResponseEntity.ok(dto)
    }

    private suspend fun toSlice(
        orders: List<Order>,
        sort: OrderFilterSort,
        size: Int
    ): OrdersDto {
        val continuationFactory = when (sort) {
            OrderFilterSort.LAST_UPDATE_ASC -> OrderContinuation.ByLastUpdatedAndIdAsc
            OrderFilterSort.LAST_UPDATE_DESC -> OrderContinuation.ByLastUpdatedAndIdDesc
            OrderFilterSort.DB_UPDATE_DESC -> OrderContinuation.ByDbUpdatedAndIdDesc
            OrderFilterSort.DB_UPDATE_ASC -> OrderContinuation.ByDbUpdatedAndIdAsc
        }
        return toSlice(orders, continuationFactory, size)
    }

    private suspend fun toSlice(
        orders: List<Order>,
        continuationFactory: ContinuationFactory<OrderDto, *>,
        size: Int
    ): OrdersDto {
        val dto = orders.map { orderConverter.convert(it) }
        val slice = Paging(continuationFactory, dto).getSlice(size)

        return OrdersDto(slice.continuation, slice.entities)
    }

    private fun List<OrderStatusDto>.fromDto(): List<OrderStatus> = map { it.fromDto() }
    private fun OrderStatusDto.fromDto(): OrderStatus = when (this) {
        OrderStatusDto.ACTIVE -> OrderStatus.ACTIVE
        OrderStatusDto.FILLED -> OrderStatus.FILLED
        OrderStatusDto.CANCELLED -> OrderStatus.CANCELLED
        OrderStatusDto.INACTIVE -> OrderStatus.INACTIVE
    }

    private fun OrderSortDto.fromDto(): OrderFilterSort = when (this) {
        OrderSortDto.LAST_UPDATE_ASC -> OrderFilterSort.LAST_UPDATE_ASC
        OrderSortDto.LAST_UPDATE_DESC -> OrderFilterSort.LAST_UPDATE_DESC
    }

    private fun SyncSortDto.fromDto(): OrderFilterSort = when (this) {
        SyncSortDto.DB_UPDATE_ASC -> OrderFilterSort.DB_UPDATE_ASC
        SyncSortDto.DB_UPDATE_DESC -> OrderFilterSort.DB_UPDATE_DESC
    }
}
