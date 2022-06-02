package com.rarible.protocol.solana.nft.listener.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.RetryProperties
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.protocol.solana.common.configuration.InternalEventProducerConfiguration
import com.rarible.protocol.solana.common.configuration.SolanaIndexerProperties
import com.rarible.protocol.solana.common.event.CollectionEvent
import com.rarible.protocol.solana.common.service.collection.CollectionEventReduceService
import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.*

@Configuration
class InternalEventConsumerConfiguration(
    private val solanaIndexerProperties: SolanaIndexerProperties,
    private val meterRegistry: MeterRegistry,
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    private val clientIdPrefix =
        "protocol.${applicationEnvironmentInfo.name}.solana.internal.event.consumer.${applicationEnvironmentInfo.host}.${UUID.randomUUID()}"

    private val groupPrefix = "protocol.${applicationEnvironmentInfo.name}.solana.internal.listener"

    private val workerNamePrefix = "protocol.${applicationEnvironmentInfo.name}.solana.internal.listener"

    @Bean
    fun internalCollectionEventConsumer(
        collectionEventReduceService: CollectionEventReduceService
    ): ConsumerWorkerHolder<CollectionEvent> {
        val clientId = "$clientIdPrefix.collection"
        val consumerGroup = "$groupPrefix.collection"
        val topic = InternalEventProducerConfiguration.getInternalCollectionTopic(applicationEnvironmentInfo.name)
        val kafkaConsumer = RaribleKafkaConsumer(
            clientId = clientId,
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = CollectionEvent::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = topic,
            bootstrapServers = solanaIndexerProperties.kafkaReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            autoCreateTopic = false
        )
        return ConsumerWorkerHolder(
            listOf(
                ConsumerWorker(
                    consumer = kafkaConsumer,
                    // Collection consumer should NOT skip events, so there we're using endless retry
                    retryProperties = RetryProperties(attempts = Integer.MAX_VALUE, delay = Duration.ofMillis(1000)),
                    eventHandler = CollectionEventHandler(collectionEventReduceService),
                    meterRegistry = meterRegistry,
                    workerName = "$workerNamePrefix.collection"
                )
            )
        ).also { it.start() }
    }

    private class CollectionEventHandler(
        private val collectionEventReduceService: CollectionEventReduceService
    ) : ConsumerEventHandler<CollectionEvent> {
        override suspend fun handle(event: CollectionEvent) {
            collectionEventReduceService.onEvents(listOf(event))
        }
    }
}
