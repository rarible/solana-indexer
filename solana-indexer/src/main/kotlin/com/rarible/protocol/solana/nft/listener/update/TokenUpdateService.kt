package com.rarible.protocol.solana.nft.listener.update

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.solana.common.meta.TokenMetaGetService
import com.rarible.protocol.solana.common.model.Token
import com.rarible.protocol.solana.common.model.TokenId
import com.rarible.protocol.solana.common.model.isEmpty
import com.rarible.protocol.solana.common.repository.TokenRepository
import com.rarible.protocol.solana.common.update.TokenUpdateListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TokenUpdateService(
    private val tokenRepository: TokenRepository,
    private val tokenUpdateListener: TokenUpdateListener,
    private val tokenMetaGetService: TokenMetaGetService
) : EntityService<TokenId, Token> {

    override suspend fun get(id: TokenId): Token? =
        tokenRepository.findByMint(id)

    override suspend fun update(entity: Token): Token {
        if (entity.isEmpty) {
            logger.info("Token without Initialize record, skipping it: {}", entity.mint)
            return entity
        }
        val enriched = entity.checkForUpdates()
        val existing = tokenRepository.findByMint(enriched.mint)
        if (existing != null && !shouldUpdate(enriched, existing)) {
            // Nothing changed in the token
            logger.info("Token $enriched is not changed, skipping save")
            return existing
        }

        val token = tokenRepository.save(enriched)
        logger.info("Updated token: $token")

        tokenUpdateListener.onTokenChanged(token)
        return token
    }

    private suspend fun Token.checkForUpdates(): Token =
        updateTokenMeta()

    private suspend fun Token.updateTokenMeta(): Token {
        val tokenMeta = tokenMetaGetService.getTokenMeta(mint) ?: return this
        return copy(tokenMeta = tokenMeta)
    }

    private fun shouldUpdate(updated: Token, existing: Token): Boolean {
        // If nothing changed except updateAt, there is no sense to publish events
        return existing != updated.copy(updatedAt = existing.updatedAt)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TokenUpdateService::class.java)
    }
}
