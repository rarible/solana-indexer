package com.rarible.protocol.solana.nft.listener

import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.solana.common.converter.BalanceWithMetaConverter
import com.rarible.protocol.solana.common.converter.TokenMetaConverter
import com.rarible.protocol.solana.common.converter.TokenWithMetaConverter
import com.rarible.protocol.solana.common.meta.TokenMeta
import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.common.model.BalanceWithMeta
import com.rarible.protocol.solana.common.model.MetaplexTokenCreator
import com.rarible.protocol.solana.common.model.Token
import com.rarible.protocol.solana.common.model.TokenWithMeta
import com.rarible.protocol.solana.common.service.collection.CollectionConverter
import com.rarible.protocol.solana.dto.BalanceDeleteEventDto
import com.rarible.protocol.solana.dto.BalanceDto
import com.rarible.protocol.solana.dto.BalanceEventDto
import com.rarible.protocol.solana.dto.BalanceUpdateEventDto
import com.rarible.protocol.solana.dto.CollectionEventDto
import com.rarible.protocol.solana.dto.CollectionUpdateEventDto
import com.rarible.protocol.solana.dto.TokenDto
import com.rarible.protocol.solana.dto.TokenEventDto
import com.rarible.protocol.solana.dto.TokenUpdateEventDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

abstract class EventAwareBlockScannerTest : AbstractBlockScannerTest() {
    @Autowired
    private lateinit var tokenEventConsumer: RaribleKafkaConsumer<TokenEventDto>

    @Autowired
    private lateinit var collectionEventConsumer: RaribleKafkaConsumer<CollectionEventDto>

    @Autowired
    private lateinit var balanceEventConsumer: RaribleKafkaConsumer<BalanceEventDto>

    protected val tokenEvents = CopyOnWriteArrayList<TokenEventDto>()
    protected val collectionEvents = CopyOnWriteArrayList<CollectionEventDto>()
    protected val balanceEvents = CopyOnWriteArrayList<BalanceEventDto>()

    private val eventsConsumingScope = CoroutineScope(
        SupervisorJob() +
                Executors.newSingleThreadExecutor { Thread(it).apply { isDaemon = true } }.asCoroutineDispatcher()
    )

    @BeforeEach
    fun setUpEventConsumers() {
        eventsConsumingScope.launch {
            tokenEventConsumer.receiveAutoAck().collect { tokenEvents += it.value }
        }
        eventsConsumingScope.launch {
            collectionEventConsumer.receiveAutoAck().collect { collectionEvents += it.value }
        }
        eventsConsumingScope.launch {
            balanceEventConsumer.receiveAutoAck().collect { balanceEvents += it.value }
        }
    }

    @AfterEach
    fun stopConsumers() {
        eventsConsumingScope.cancel()
    }

    protected fun assertUpdateTokenEvent(token: Token, tokenMeta: TokenMeta?) {
        Assertions.assertThat(tokenEvents).anySatisfy { event ->
            Assertions.assertThat(event).isInstanceOfSatisfying(TokenUpdateEventDto::class.java) {
                Assertions.assertThat(it.address).isEqualTo(token.mint)
                assertTokenDtoEqual(it.token, TokenWithMetaConverter.convert(TokenWithMeta(token, tokenMeta)))
            }
        }
    }

    protected fun assertUpdateCollectionEvent(collectionMint: String, collectionTokenMeta: TokenMeta) {
        Assertions.assertThat(collectionEvents).anySatisfy { event ->
            Assertions.assertThat(event).isInstanceOfSatisfying(CollectionUpdateEventDto::class.java) {
                Assertions.assertThat(it.collectionId).isEqualTo(collectionMint)
                Assertions.assertThat(it.collection.meta).isEqualTo(
                    CollectionConverter.convertCollectionMeta(collectionTokenMeta)
                )
            }
        }
    }

    protected fun assertUpdateBalanceEvent(balance: Balance, tokenMeta: TokenMeta?) {
        Assertions.assertThat(balanceEvents).anySatisfy { event ->
            val expectedClass = if (balance.value > BigInteger.ZERO) {
                BalanceUpdateEventDto::class.java
            } else {
                BalanceDeleteEventDto::class.java
            }
            Assertions.assertThat(event).isInstanceOfSatisfying(expectedClass) {
                Assertions.assertThat(it.account).isEqualTo(balance.account)
                val balanceDto = when (it) {
                    is BalanceUpdateEventDto -> it.balance
                    is BalanceDeleteEventDto -> it.balance
                }
                assertBalanceDtoEqual(
                    balanceDto,
                    BalanceWithMetaConverter.convert(BalanceWithMeta(balance, tokenMeta))
                )
            }
        }
    }

    protected suspend fun assertToken(expectedToken: Token) {
        val actualToken = tokenRepository.findByMint(expectedToken.mint)
        assertTokensEqual(actualToken, expectedToken)
    }

    protected fun assertTokenDtoEqual(actualToken: TokenDto?, expectedToken: TokenDto) {
        // Ignore some fields because they are minor and hard to get.
        fun TokenDto.ignore() = this
            .copy(createdAt = Instant.EPOCH)
            .copy(updatedAt = Instant.EPOCH)
        Assertions.assertThat(actualToken?.ignore()).isEqualTo(expectedToken.ignore())

        Assertions.assertThat(actualToken?.createdAt).isNotEqualTo(Instant.EPOCH)
        Assertions.assertThat(actualToken?.updatedAt).isNotEqualTo(Instant.EPOCH)
    }

    protected fun assertTokensEqual(actualToken: Token?, expectedToken: Token) {
        // Ignore some fields because they are minor and hard to get.
        fun Token.ignore() = this
            .copy(createdAt = Instant.EPOCH)
            .copy(updatedAt = Instant.EPOCH)
            .copy(revertableEvents = emptyList())
        Assertions.assertThat(actualToken?.ignore()).isEqualTo(expectedToken.ignore())

        Assertions.assertThat(actualToken?.createdAt).isNotEqualTo(Instant.EPOCH)
        Assertions.assertThat(actualToken?.updatedAt).isNotEqualTo(Instant.EPOCH)
    }

    protected suspend fun assertBalance(expectedBalance: Balance) {
        val actualBalance = balanceRepository.findByAccount(expectedBalance.account)
        assertBalancesEqual(actualBalance, expectedBalance)
    }

    protected fun assertBalanceDtoEqual(actualBalance: BalanceDto?, expectedBalance: BalanceDto) {
        // Ignore some fields because they are minor and hard to get.
        fun BalanceDto.ignore() = this
            .copy(createdAt = Instant.EPOCH)
            .copy(updatedAt = Instant.EPOCH)
        Assertions.assertThat(actualBalance?.ignore()).isEqualTo(expectedBalance.ignore())

        // TODO: consider comparing these fields (get from the blockchain)?
        Assertions.assertThat(actualBalance?.createdAt).isNotEqualTo(Instant.EPOCH)
        Assertions.assertThat(actualBalance?.updatedAt).isNotEqualTo(Instant.EPOCH)
    }

    protected fun assertBalancesEqual(
        actualBalance: Balance?,
        expectedBalance: Balance
    ) {
        // Ignore [revertableEvents] because they are minor and hard to get.
        fun Balance.ignore() = this
            .copy(createdAt = Instant.EPOCH)
            .copy(updatedAt = Instant.EPOCH)
            .copy(revertableEvents = emptyList())
        Assertions.assertThat(actualBalance?.ignore()).isEqualTo(expectedBalance.ignore())

        // TODO: consider comparing these fields (get from the blockchain)?
        Assertions.assertThat(actualBalance?.createdAt).isNotEqualTo(Instant.EPOCH)
        Assertions.assertThat(actualBalance?.updatedAt).isNotEqualTo(Instant.EPOCH)
    }

    protected suspend fun assertTokenMetaUpdatedEvent(
        tokenAddress: String,
        creators: List<MetaplexTokenCreator>? = null,
        collection: TokenMeta.Collection? = null
    ) {
        Wait.waitAssert {
            Assertions.assertThat(tokenEvents).anySatisfy { eventDto ->
                Assertions.assertThat(eventDto).isInstanceOfSatisfying(TokenUpdateEventDto::class.java) { event ->
                    Assertions.assertThat(event.address).isEqualTo(tokenAddress)

                    // Check the meta-related fields.
                    Assertions.assertThat(event.token.creators).isEqualTo(
                        creators?.let {
                            creators.map { TokenMetaConverter.convert(it) }
                        }
                    )
                    Assertions.assertThat(event.token.collection).isEqualTo(
                        collection?.let {
                            TokenMetaConverter.convert(collection)
                        }
                    )
                }
            }
        }
    }

    protected suspend fun assertBalanceMetaUpdatedEvent(
        balanceAccount: String,
        collection: TokenMeta.Collection? = null
    ) {
        Wait.waitAssert {
            Assertions.assertThat(balanceEvents).anySatisfy { eventDto ->
                Assertions.assertThat(eventDto).isInstanceOfSatisfying(BalanceUpdateEventDto::class.java) { event ->
                    Assertions.assertThat(event.account).isEqualTo(balanceAccount)

                    // Check the meta-related fields.
                    Assertions.assertThat(event.balance.collection).isEqualTo(
                        collection?.let {
                            TokenMetaConverter.convert(collection)
                        }
                    )
                }
            }
        }
    }

}
