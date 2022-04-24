package com.rarible.protocol.solana.nft.listener.configuration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerBatchWorker
import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.protocol.solana.dto.TokenMetaEventDto
import com.rarible.protocol.solana.nft.listener.service.token.TokenMetaUpdateTrigger
import com.rarible.protocol.solana.subscriber.SolanaEventsConsumerFactory
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration to connect Solana-API module producing the [TokenMetaEventDto]
 * with the indexer module that executes these events.
 */
@Configuration
class MetaListenerConfiguration(
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo,
) {

    private val consumerGroupPrefix get() = applicationEnvironmentInfo.name + ".protocol.solana.indexer"

    @Bean
    fun solanaItemMetaWorker(
        solanaEventsConsumerFactory: SolanaEventsConsumerFactory,
        tokenMetaUpdateTrigger: TokenMetaUpdateTrigger,
        meterRegistry: MeterRegistry
    ): ConsumerWorkerHolder<TokenMetaEventDto> {
        val consumerGroup = "$consumerGroupPrefix.meta.trigger"
        val consumer = solanaEventsConsumerFactory.createTokenMetaEventConsumer(consumerGroup)
        val consumerWorker = ConsumerBatchWorker(
            consumer = consumer,
            eventHandler = tokenMetaUpdateTrigger,
            workerName = consumerGroup,
            meterRegistry = meterRegistry,
            completionHandler = {
                val logger = LoggerFactory.getLogger(MetaListenerConfiguration::class.java)
                logger.info("The meta update listener has stopped", it)
            }
        )
        return ConsumerWorkerHolder(listOf(consumerWorker))
            .apply { start() }
    }
}