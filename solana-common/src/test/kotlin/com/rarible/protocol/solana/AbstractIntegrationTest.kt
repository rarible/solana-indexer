package com.rarible.protocol.solana

import com.rarible.core.mongo.configuration.IncludePersistProperties
import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.protocol.solana.common.meta.TokenMetaGetService
import com.rarible.protocol.solana.common.meta.TokenMetaService
import com.rarible.protocol.solana.common.repository.BalanceRepository
import com.rarible.protocol.solana.common.repository.MetaplexMetaRepository
import com.rarible.protocol.solana.common.repository.MetaplexOffChainMetaRepository
import com.rarible.protocol.solana.common.repository.OrderRepository
import com.rarible.protocol.solana.common.repository.TokenRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@MongoTest
@MongoCleanup
@KafkaTest
@SpringBootTest(
    properties = [
        "application.environment = e2e",
        "spring.application.name = test",
        "spring.cloud.consul.config.enabled = false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@ActiveProfiles("test")
@ContextConfiguration(classes = [TestContext::class])
abstract class AbstractIntegrationTest {
    @Autowired
    lateinit var mongo: ReactiveMongoOperations

    @Autowired
    lateinit var balanceRepository: BalanceRepository

    @Autowired
    lateinit var tokenRepository: TokenRepository

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var tokenMetaService: TokenMetaService

    @Autowired
    lateinit var tokenMetaGetService: TokenMetaGetService

    @Autowired
    lateinit var metaplexMetaRepository: MetaplexMetaRepository

    @Autowired
    lateinit var metaplexOffChainMetaRepository: MetaplexOffChainMetaRepository
}

@Configuration
@EnableAutoConfiguration
@IncludePersistProperties
class TestContext
