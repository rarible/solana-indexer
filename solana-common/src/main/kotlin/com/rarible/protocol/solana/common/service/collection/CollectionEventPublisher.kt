package com.rarible.protocol.solana.common.service.collection

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.solana.common.event.CollectionEvent
import org.springframework.stereotype.Component
import java.util.*

@Component
class CollectionEventPublisher(
    private val producer: RaribleKafkaProducer<CollectionEvent>
) {
    suspend fun send(collectionEvent: CollectionEvent) {
        val kafkaMessage = createMessage(collectionEvent)
        producer.send(kafkaMessage)
    }

    private fun createMessage(collectionEvent: CollectionEvent) =
        KafkaMessage(
            id = UUID.randomUUID().toString(),
            key = collectionEvent.id,
            value = collectionEvent
        )
}