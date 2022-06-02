package com.rarible.protocol.solana.nft.listener

import com.rarible.blockchain.scanner.solana.EnableSolanaScanner
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.protocol.solana.common.meta.MetaplexOffChainMetaLoader
import com.rarible.protocol.solana.dto.BalanceEventDto
import com.rarible.protocol.solana.dto.CollectionEventDto
import com.rarible.protocol.solana.dto.TokenEventDto
import com.rarible.protocol.solana.subscriber.SolanaEventsConsumerFactory
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.ConcurrentHashMap

@Configuration
@EnableSolanaScanner
@EnableAutoConfiguration
@Import(TestUnionServiceMetaLoadingEmulatorConfiguration::class)
class TestSolanaScannerConfiguration {
    @Bean
    fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()

    @Bean
    fun tokenEventConsumer(solanaEventsConsumerFactory: SolanaEventsConsumerFactory): RaribleKafkaConsumer<TokenEventDto> =
        solanaEventsConsumerFactory.createTokenEventConsumer("test")

    @Bean
    fun collectionEventConsumer(solanaEventsConsumerFactory: SolanaEventsConsumerFactory): RaribleKafkaConsumer<CollectionEventDto> =
        solanaEventsConsumerFactory.createCollectionEventConsumer("test")

    @Bean
    fun balanceEventConsumer(solanaEventsConsumerFactory: SolanaEventsConsumerFactory): RaribleKafkaConsumer<BalanceEventDto> =
        solanaEventsConsumerFactory.createBalanceEventConsumer("test")

    @Bean
    @Primary
    @Qualifier("test.solana.meta.loader")
    fun testSolanaMetaLoader(): MetaplexOffChainMetaLoader = mockk()

    @RestController
    class MetaController {
        private val map = ConcurrentHashMap<String, String>()

        init {
            val meta = "meta.json"

            map[meta] = MetaController::class.java.classLoader.getResource(meta).readText()
        }

        @PostMapping("/meta/{id}")
        fun upload(@PathVariable id: String, @RequestBody meta: String) {
            map[id] = meta
        }

        @GetMapping("/meta/{id}")
        fun download(@PathVariable id: String): String? {
            return map[id]
        }
    }
}
