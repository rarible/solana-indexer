package com.rarible.protocol.solana.nft.listener.service.order

import com.rarible.core.common.nowMillis
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.solana.common.event.ExecuteSaleEvent
import com.rarible.protocol.solana.common.event.InternalUpdateEvent
import com.rarible.protocol.solana.common.event.OrderBuyEvent
import com.rarible.protocol.solana.common.event.OrderCancelEvent
import com.rarible.protocol.solana.common.event.OrderEvent
import com.rarible.protocol.solana.common.event.OrderSellEvent
import com.rarible.protocol.solana.common.model.Asset
import com.rarible.protocol.solana.common.model.Order
import com.rarible.protocol.solana.common.model.OrderStatus
import com.rarible.protocol.solana.common.model.WrappedSolAssetType
import com.rarible.protocol.solana.common.model.isEmpty
import com.rarible.protocol.solana.common.records.OrderDirection
import com.rarible.protocol.solana.common.records.SolanaOrderUpdateInstruction
import com.rarible.protocol.solana.common.repository.AuctionHouseRepository
import com.rarible.protocol.solana.common.service.PriceNormalizer
import com.rarible.protocol.solana.nft.listener.service.auction.house.AuctionHouseNotReadyException
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class ForwardOrderReducer(
    private val priceNormalizer: PriceNormalizer,
    private val auctionHouseRepository: AuctionHouseRepository
) : Reducer<OrderEvent, Order> {
    override suspend fun reduce(entity: Order, event: OrderEvent): Order {
        if (event !is OrderSellEvent && event !is OrderBuyEvent && entity.isEmpty) {
            return entity
        }
        val states = if (entity.isEmpty) emptyList() else entity.states + entity.copy(states = emptyList())
        return when (event) {

            is ExecuteSaleEvent -> {
                val newFill = entity.fill + event.amount
                val isFilled = when (event.direction) {
                    OrderDirection.BUY -> newFill == entity.take.amount
                    OrderDirection.SELL -> newFill == entity.make.amount
                }
                val newStatus = if (isFilled) OrderStatus.FILLED else OrderStatus.ACTIVE
                entity.copy(
                    fill = newFill,
                    status = newStatus,
                    updatedAt = event.timestamp
                )
            }

            is OrderBuyEvent -> {
                val auctionHouse = auctionHouseRepository.findByAccount(event.auctionHouse)
                    ?: throw AuctionHouseNotReadyException("Auction house: ${event.auctionHouse} missed in repository")

                Order(
                    auctionHouseSellerFeeBasisPoints = auctionHouse.sellerFeeBasisPoints,
                    auctionHouseRequiresSignOff = auctionHouse.requiresSignOff,
                    auctionHouse = event.auctionHouse,
                    maker = event.maker,
                    makerAccount = event.makerAccount,
                    status = OrderStatus.ACTIVE,
                    make = Asset(
                        type = WrappedSolAssetType(),
                        amount = event.buyPrice
                    ),
                    take = event.buyAsset,
                    fill = BigInteger.ZERO,
                    createdAt = event.timestamp,
                    updatedAt = event.timestamp,
                    revertableEvents = emptyList(),
                    id = Order.calculateAuctionHouseOrderId(
                        maker = event.maker,
                        mint = event.buyAsset.type.tokenAddress,
                        direction = OrderDirection.BUY,
                        auctionHouse = event.auctionHouse
                    ),
                    direction = OrderDirection.BUY,
                    makePrice = null,
                    takePrice = null,
                    states = emptyList(),
                    dbUpdatedAt = nowMillis()
                ).let { order -> priceNormalizer.withUpdatedMakeAndTakePrice(order) }
            }

            is OrderSellEvent -> {
                val auctionHouse = auctionHouseRepository.findByAccount(event.auctionHouse)
                    ?: throw AuctionHouseNotReadyException("Auction house: ${event.auctionHouse} missed in repository")

                Order(
                    auctionHouseSellerFeeBasisPoints = auctionHouse.sellerFeeBasisPoints,
                    auctionHouseRequiresSignOff = auctionHouse.requiresSignOff,
                    auctionHouse = event.auctionHouse,
                    maker = event.maker,
                    status = OrderStatus.ACTIVE,
                    make = event.sellAsset,
                    makerAccount = event.makerAccount,
                    take = Asset(
                        type = WrappedSolAssetType(),
                        amount = event.sellPrice
                    ),
                    fill = BigInteger.ZERO,
                    createdAt = event.timestamp,
                    updatedAt = event.timestamp,
                    revertableEvents = emptyList(),
                    id = Order.calculateAuctionHouseOrderId(
                        maker = event.maker,
                        mint = event.sellAsset.type.tokenAddress,
                        direction = OrderDirection.SELL,
                        auctionHouse = event.auctionHouse
                    ),
                    direction = OrderDirection.SELL,
                    makePrice = null,
                    takePrice = null,
                    states = emptyList(),
                    dbUpdatedAt = nowMillis()
                ).let { order -> priceNormalizer.withUpdatedMakeAndTakePrice(order) }
            }

            is OrderCancelEvent -> entity.copy(
                status = OrderStatus.CANCELLED,
                updatedAt = event.timestamp
            )

            is InternalUpdateEvent -> {
                when (val instruction = event.instruction) {
                    // We do not need to execute any updates here, balance will be checked in UpdateService
                    is SolanaOrderUpdateInstruction.BalanceUpdate -> entity
                    is SolanaOrderUpdateInstruction.EscrowUpdate -> entity
                    is SolanaOrderUpdateInstruction.AuctionHouseUpdate ->
                        entity.copy(
                            auctionHouseSellerFeeBasisPoints = instruction.sellerFeeBasisPoints,
                            auctionHouseRequiresSignOff = instruction.requiresSignOff
                        )
                }.copy(updatedAt = event.timestamp)
            }
        }.copy(states = states)
    }
}
