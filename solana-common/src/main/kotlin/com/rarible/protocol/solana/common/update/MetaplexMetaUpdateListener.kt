package com.rarible.protocol.solana.common.update

import com.rarible.protocol.solana.common.meta.TokenMeta
import com.rarible.protocol.solana.common.model.BalanceWithMeta
import com.rarible.protocol.solana.common.model.TokenId
import com.rarible.protocol.solana.common.model.TokenWithMeta
import com.rarible.protocol.solana.common.repository.BalanceRepository
import com.rarible.protocol.solana.common.repository.TokenRepository
import com.rarible.protocol.solana.common.service.CollectionUpdateService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MetaplexMetaUpdateListener(
    private val tokenRepository: TokenRepository,
    private val balanceRepository: BalanceRepository,
    private val tokenUpdateListener: TokenUpdateListener,
    private val balanceUpdateListener: BalanceUpdateListener,
    private val collectionUpdateService: CollectionUpdateService
) {

    private val logger = LoggerFactory.getLogger(MetaplexMetaUpdateListener::class.java)

    suspend fun onTokenMetaChanged(tokenAddress: TokenId, tokenMeta: TokenMeta) {
        logger.info("Meta updated for $tokenAddress: $tokenMeta")
        val token = tokenRepository.findByMint(tokenAddress)
        if (token != null) {
            tokenUpdateListener.onTokenChanged(TokenWithMeta(token, tokenMeta))
            collectionUpdateService.onTokenChanged(token)
        }

        balanceRepository.findByMint(
            mint = tokenAddress,
            continuation = null,
            includeDeleted = false
        ).collect { balance ->
            balanceUpdateListener.onBalanceChanged(BalanceWithMeta(balance, tokenMeta))
        }
    }
}
