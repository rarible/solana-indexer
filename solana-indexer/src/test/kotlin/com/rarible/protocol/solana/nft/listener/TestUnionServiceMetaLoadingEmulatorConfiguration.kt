package com.rarible.protocol.solana.nft.listener

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.RetryProperties
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.protocol.solana.common.meta.MetaplexOffChainMetaLoadService
import com.rarible.protocol.solana.dto.SolanaEventTopicProvider
import com.rarible.protocol.solana.dto.TokenMetaEventDto
import com.rarible.protocol.solana.subscriber.SolanaEventsConsumerFactory
import com.rarible.protocol.solana.subscriber.autoconfigure.SolanaEventsSubscriberProperties
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Emulates logic of the Union-Service on loading the off-chain MetaplexMeta when the Solana indexer triggers a meta refresh event.
 */
@Configuration
class TestUnionServiceMetaLoadingEmulatorConfiguration(
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    private val properties: SolanaEventsSubscriberProperties
) {

    private val clientIdPrefix = "protocol.${applicationEnvironmentInfo.name}.solana"

    private val groupPrefix = "protocol.${applicationEnvironmentInfo.name}.solana"

    private val workerNamePrefix = "protocol.${applicationEnvironmentInfo.name}.solana"

    @Bean
    fun tokenMetaEventConsumer(solanaEventsConsumerFactory: SolanaEventsConsumerFactory): RaribleKafkaConsumer<TokenMetaEventDto> =
        solanaEventsConsumerFactory.createTokenMetaEventConsumer("test")

    @Bean
    fun unionServiceMetaLoadingEmulator(
        tokenMetaEventConsumer: RaribleKafkaConsumer<TokenMetaEventDto>,
        metaplexOffChainMetaLoadService: MetaplexOffChainMetaLoadService
    ): ConsumerWorkerHolder<TokenMetaEventDto> {
        val clientId = "$clientIdPrefix.token.meta"
        val consumerGroup = "$groupPrefix.token.meta"
        val topic = SolanaEventTopicProvider.getTokenMetaTopic(applicationEnvironmentInfo.name)
        val kafkaConsumer = RaribleKafkaConsumer(
            clientId = clientId,
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = TokenMetaEventDto::class.java,
            consumerGroup = consumerGroup,
            defaultTopic = topic,
            bootstrapServers = properties.brokerReplicaSet,
            offsetResetStrategy = OffsetResetStrategy.EARLIEST,
            autoCreateTopic = false
        )
        return ConsumerWorkerHolder(
            listOf(
                ConsumerWorker(
                    consumer = kafkaConsumer,
                    retryProperties = RetryProperties(attempts = Integer.MAX_VALUE, delay = Duration.ofMillis(1000)),
                    eventHandler = UnionServiceEmulatorTokenMetaEventHandler(metaplexOffChainMetaLoadService),
                    meterRegistry = SimpleMeterRegistry(),
                    workerName = "$workerNamePrefix.token.meta"
                )
            )
        ).also { it.start() }
    }

    private class UnionServiceEmulatorTokenMetaEventHandler(
        private val metaplexOffChainMetaLoadService: MetaplexOffChainMetaLoadService
    ) : ConsumerEventHandler<TokenMetaEventDto> {

        private val logger = LoggerFactory.getLogger(javaClass)

        override suspend fun handle(event: TokenMetaEventDto) {
            val loadedMeta = metaplexOffChainMetaLoadService.loadOffChainTokenMeta(event.tokenAddress)
            logger.info("Loaded full meta for ${event.tokenAddress}: $loadedMeta")
        }
    }

}