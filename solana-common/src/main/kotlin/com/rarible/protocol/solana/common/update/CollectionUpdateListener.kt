package com.rarible.protocol.solana.common.update

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.solana.common.model.SolanaCollection
import com.rarible.protocol.solana.common.service.collection.CollectionConverter
import com.rarible.protocol.solana.dto.CollectionEventDto
import com.rarible.protocol.solana.dto.CollectionUpdateEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*

@Component
class CollectionUpdateListener(
    private val publisher: RaribleKafkaProducer<CollectionEventDto>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun onCollectionChanged(collection: SolanaCollection) {
        val collectionDto = CollectionConverter.toDto(collection) ?: return
        val event = CollectionUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            collectionId = collectionDto.address,
            collection = collectionDto
        )
        val message = KafkaEventFactory.collectionEvent(event)
        publisher.send(message).ensureSuccess()
        logger.info("Collection event sent: $event")
    }
}