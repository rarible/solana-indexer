package com.rarible.protocol.solana.common.converter

import com.rarible.protocol.solana.common.model.Order
import com.rarible.protocol.solana.common.model.OrderStatus
import com.rarible.protocol.solana.common.records.OrderDirection
import com.rarible.protocol.solana.common.service.PriceNormalizer
import com.rarible.protocol.solana.dto.AuctionHouseOrderDataV1Dto
import com.rarible.protocol.solana.dto.AuctionHouseOrderDto
import com.rarible.protocol.solana.dto.OrderDto
import com.rarible.protocol.solana.dto.OrderStatusDto
import org.springframework.stereotype.Component

@Component
class OrderConverter(
    private val assetConverter: AssetConverter,
    private val priceNormalizer: PriceNormalizer
) {

    suspend fun convert(order: Order): OrderDto =
        AuctionHouseOrderDto(
            auctionHouseFee = order.auctionHouseSellerFeeBasisPoints,
            auctionHouseRequiresSignOff = order.auctionHouseRequiresSignOff,
            maker = order.maker,
            make = assetConverter.convert(order.make),
            take = assetConverter.convert(order.take),
            fill = when (order.direction) {
                OrderDirection.SELL -> priceNormalizer.normalize(order.make.type, order.fill)
                OrderDirection.BUY -> priceNormalizer.normalize(order.take.type, order.fill)
            },
            makePrice = order.makePrice,
            takePrice = order.takePrice,
            makeStock = priceNormalizer.normalize(order.make.type, order.makeStock),
            start = null,
            end = null,
            createdAt = order.createdAt,
            updatedAt = order.updatedAt,
            dbUpdatedAt = order.dbUpdatedAt,
            hash = order.id,
            status = order.status.toDto(),
            data = AuctionHouseOrderDataV1Dto(auctionHouse = order.auctionHouse)
        )

    private fun OrderStatus.toDto() = when (this) {
        OrderStatus.ACTIVE -> OrderStatusDto.ACTIVE
        OrderStatus.CANCELLED -> OrderStatusDto.CANCELLED
        OrderStatus.FILLED -> OrderStatusDto.FILLED
        OrderStatus.INACTIVE -> OrderStatusDto.INACTIVE
    }
}
