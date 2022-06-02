package com.rarible.protocol.solana.common.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.protocol.solana.common.event.CollectionEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InternalEventProducerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: SolanaIndexerProperties
) {

    private val env = applicationEnvironmentInfo.name
    private val kafkaReplicaSet = properties.kafkaReplicaSet

    @Bean
    fun internalCollectionEventProducer(): RaribleKafkaProducer<CollectionEvent> =
        RaribleKafkaProducer(
            clientId = "protocol.$env.solana.internal.collection",
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = CollectionEvent::class.java,
            defaultTopic = getInternalCollectionTopic(env),
            bootstrapServers = kafkaReplicaSet
        )

    companion object {
        fun getInternalCollectionTopic(env: String): String =
            "protocol.$env.solana.internal.collection"
    }
}
