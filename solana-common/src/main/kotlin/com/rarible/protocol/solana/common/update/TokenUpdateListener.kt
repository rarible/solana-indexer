package com.rarible.protocol.solana.common.update

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.solana.common.converter.TokenWithMetaEventConverter
import com.rarible.protocol.solana.common.meta.TokenMetaService
import com.rarible.protocol.solana.common.model.Token
import com.rarible.protocol.solana.common.model.TokenWithMeta
import com.rarible.protocol.solana.dto.TokenEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TokenUpdateListener(
    private val publisher: RaribleKafkaProducer<TokenEventDto>,
    private val tokenMetaService: TokenMetaService
) {
    private val logger = LoggerFactory.getLogger(TokenUpdateListener::class.java)

    suspend fun onTokenChanged(token: Token) {
        val tokenMeta = tokenMetaService.getAvailableTokenMeta(token.mint)
        if (tokenMeta == null) {
            logger.info("Token ${token.mint} meta is not available yet, so ignoring the token update event")
            return
        }
        val tokenWithMeta = TokenWithMeta(token, tokenMeta)
        onTokenChanged(tokenWithMeta)
    }

    suspend fun onTokenChanged(tokenWithMeta: TokenWithMeta) {
        val tokenEventDto = TokenWithMetaEventConverter.convert(tokenWithMeta)
        publisher.send(KafkaEventFactory.tokenEvent(tokenEventDto)).ensureSuccess()
        logger.info("Token event sent: $tokenEventDto")
    }
}
