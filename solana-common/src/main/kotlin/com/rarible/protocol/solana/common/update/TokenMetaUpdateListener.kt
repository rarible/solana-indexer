package com.rarible.protocol.solana.common.update

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.solana.common.converter.TokenMetaEventConverter
import com.rarible.protocol.solana.common.meta.TokenMeta
import com.rarible.protocol.solana.common.model.BalanceWithMeta
import com.rarible.protocol.solana.common.model.TokenId
import com.rarible.protocol.solana.common.model.TokenWithMeta
import com.rarible.protocol.solana.common.repository.BalanceRepository
import com.rarible.protocol.solana.common.repository.TokenRepository
import com.rarible.protocol.solana.dto.TokenMetaEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TokenMetaUpdateListener(
    private val tokenRepository: TokenRepository,
    private val balanceRepository: BalanceRepository,
    private val tokenUpdateListener: TokenUpdateListener,
    private val balanceUpdateListener: BalanceUpdateListener,
    private val publisher: RaribleKafkaProducer<TokenMetaEventDto>
) {
    private val logger = LoggerFactory.getLogger(TokenMetaUpdateListener::class.java)

    /**
     * Produce a trigger event that will make the Union-Service load the off-chain meta
     * for this token by calling /items/<SOLANA:itemId>/meta
     *
     * Note that in the integration tests we emulate this behaviour of the Union-Service
     * by calling the `MetaplexOffChainMetaLoadService.loadOffChainTokenMeta`.
     * See `TestUnionServiceMetaLoadingEmulatorConfiguration`.
     */
    suspend fun triggerTokenMetaLoading(tokenAddress: String) {
        logger.info("Trigger token meta $tokenAddress loading on the Union Service")
        send(TokenMetaEventConverter.convertTriggerEvent(tokenAddress))
    }

    /**
     * Sends "TokenUpdateEvent" and "BalanceUpdateEvent" with new meta.
     */
    suspend fun onTokenMetaChanged(tokenAddress: TokenId, tokenMeta: TokenMeta) {
        logger.info("Meta updated for $tokenAddress: $tokenMeta")
        val token = tokenRepository.findByMint(tokenAddress)
        if (token != null) {
            tokenUpdateListener.onTokenChanged(TokenWithMeta(token, tokenMeta))
        }

        balanceRepository.findByMint(
            mint = tokenAddress,
            continuation = null,
            limit = null,
            includeDeleted = false
        ).collect { balance ->
            balanceUpdateListener.onBalanceChanged(BalanceWithMeta(balance, tokenMeta))
        }
    }

    private suspend fun send(tokenMetaEventDto: TokenMetaEventDto) {
        publisher.send(KafkaEventFactory.tokenMetaEvent(tokenMetaEventDto)).ensureSuccess()
        logger.info("TokenMeta event sent: $tokenMetaEventDto")
    }
}
