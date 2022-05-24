package com.rarible.protocol.solana.common.update

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.solana.common.converter.OrderConverter
import com.rarible.protocol.solana.common.model.Order
import com.rarible.protocol.solana.dto.OrderEventDto
import com.rarible.protocol.solana.dto.OrderUpdateEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class OrderUpdateListener(
    private val publisher: RaribleKafkaProducer<OrderEventDto>,
    private val orderConverter: OrderConverter
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun onOrderChanged(order: Order) {
        val dto = orderConverter.convert(order)
        val event = OrderUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            orderId = dto.hash,
            order = dto
        )
        val message = KafkaEventFactory.orderEvent(event)
        publisher.send(message).ensureSuccess()
        logger.info("Order event sent: $event")
    }
}