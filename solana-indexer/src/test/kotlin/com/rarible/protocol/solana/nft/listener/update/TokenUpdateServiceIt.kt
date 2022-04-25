package com.rarible.protocol.solana.nft.listener.update

import com.rarible.core.test.data.randomString
import com.rarible.protocol.solana.common.meta.TokenMetaParser
import com.rarible.protocol.solana.common.model.Token
import com.rarible.protocol.solana.common.update.TokenUpdateListener
import com.rarible.protocol.solana.nft.listener.AbstractBlockScannerTest
import com.rarible.protocol.solana.test.createRandomMetaplexMeta
import com.rarible.protocol.solana.test.createRandomMetaplexOffChainMeta
import com.rarible.protocol.solana.test.createRandomToken
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class TokenUpdateServiceIt : AbstractBlockScannerTest() {

    private lateinit var tokenUpdateService: TokenUpdateService

    private val tokenUpdateListener: TokenUpdateListener = mockk()

    @BeforeEach
    fun beforeEach() {
        clearMocks(tokenUpdateListener)
        coEvery { tokenUpdateListener.onTokenChanged(any<Token>()) } returns Unit
        tokenUpdateService = TokenUpdateService(
            tokenRepository,
            tokenUpdateListener,
            tokenMetaGetService
        )
    }

    @Test
    fun `not initialized token`() = runBlocking<Unit> {
        val order = createRandomToken().copy(createdAt = Instant.EPOCH)

        tokenUpdateService.update(order)

        assertThat(orderRepository.findById(order.id)).isNull()
        coVerify(exactly = 0) { tokenUpdateListener.onTokenChanged(any<Token>()) }
    }

    @Test
    fun `new token inserted`() = runBlocking<Unit> {
        val token = createRandomToken()

        tokenUpdateService.update(token)

        // Token saved, event sent
        val saved = tokenRepository.findByMint(token.mint)!!
        coVerify(exactly = 1) { tokenUpdateListener.onTokenChanged(saved) }
    }

    @Test
    fun `existing token not updated`() = runBlocking<Unit> {
        // There is no balance in DB, so we consider this order as ACTIVE
        val token = tokenRepository.save(createRandomToken())

        tokenUpdateService.update(token)

        // Update skipped, order not changed
        val saved = tokenRepository.findByMint(token.mint)!!
        assertThat(saved).isEqualTo(token)
        coVerify(exactly = 0) { tokenUpdateListener.onTokenChanged(saved) }
    }

    @Test
    fun `existing order not updated - meta is not loaded`() = runBlocking<Unit> {
        val mint = randomString()
        val token = tokenRepository.save(createRandomToken(mint = mint, tokenMeta = null))
        tokenUpdateService.update(token)

        // Update skipped, token is not changed
        val saved = tokenRepository.findByMint(token.mint)!!
        assertThat(saved).isEqualTo(token)
        coVerify(exactly = 0) { tokenUpdateListener.onTokenChanged(saved) }
    }

    @Test
    fun `existing token updated - meta is fully loaded`() = runBlocking<Unit> {
        val mint = randomString()
        val token = tokenRepository.save(createRandomToken(mint = mint, tokenMeta = null))

        // Save both on-chain and off-chain meta to the repository.
        val metaplexMeta = createRandomMetaplexMeta(mint = mint)
        metaplexMetaRepository.save(metaplexMeta)

        val metaplexOffChainMeta = createRandomMetaplexOffChainMeta(mint = mint)
        metaplexOffChainMetaRepository.save(metaplexOffChainMeta)

        tokenUpdateService.update(token)

        val tokenMeta = TokenMetaParser.mergeOnChainAndOffChainMeta(
            onChainMeta = metaplexMeta.metaFields,
            offChainMeta = metaplexOffChainMeta.metaFields
        )
        // Token meta must be set
        val saved = tokenRepository.findByMint(token.mint)!!
        assertThat(saved).isEqualTo(token.copy(tokenMeta = tokenMeta))
        coVerify(exactly = 1) { tokenUpdateListener.onTokenChanged(saved) }
    }

}