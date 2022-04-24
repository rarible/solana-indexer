package com.rarible.protocol.solana.nft.listener.service.order

import com.rarible.protocol.solana.common.configuration.SolanaIndexerProperties
import com.rarible.protocol.solana.common.event.ExecuteSaleEvent
import com.rarible.protocol.solana.common.event.InternalUpdateEvent
import com.rarible.protocol.solana.common.event.OrderBuyEvent
import com.rarible.protocol.solana.common.event.OrderCancelEvent
import com.rarible.protocol.solana.common.event.OrderEvent
import com.rarible.protocol.solana.common.event.OrderSellEvent
import com.rarible.protocol.solana.common.model.Order
import com.rarible.protocol.solana.nft.listener.service.AbstractMetricReducer
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class OrderMetricReducer(
    properties: SolanaIndexerProperties,
    meterRegistry: MeterRegistry,
) : AbstractMetricReducer<OrderEvent, Order>(meterRegistry, properties, "auction_house_order") {

    override fun getMetricName(event: OrderEvent): String {
        return when (event) {
            is ExecuteSaleEvent -> "auction_house_order_execute_sale"
            is OrderBuyEvent -> "auction_house_order_buy"
            is OrderSellEvent -> "auction_house_order_sell"
            is OrderCancelEvent -> "auction_house_order_cancel"
            is InternalUpdateEvent -> "auction_house_internal_update"
        }
    }
}
