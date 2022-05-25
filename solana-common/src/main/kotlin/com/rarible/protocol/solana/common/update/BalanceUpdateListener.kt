package com.rarible.protocol.solana.common.update

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.solana.common.converter.BalanceWithMetaEventConverter
import com.rarible.protocol.solana.common.meta.TokenMetaService
import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.common.model.BalanceWithMeta
import com.rarible.protocol.solana.dto.BalanceEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BalanceUpdateListener(
    private val publisher: RaribleKafkaProducer<BalanceEventDto>,
    private val tokenMetaService: TokenMetaService
) {
    private val logger = LoggerFactory.getLogger(BalanceUpdateListener::class.java)

    suspend fun onBalanceChanged(balance: Balance) {
        val tokenMeta = tokenMetaService.getAvailableTokenMeta(balance.mint)
        if (tokenMeta == null) {
            logger.info("Balance's ${balance.account} meta of token ${balance.mint} is not available, so ignoring the update event")
            return
        }
        val balanceWithMeta = BalanceWithMeta(balance, tokenMeta)
        onBalanceChanged(balanceWithMeta)
    }

    suspend fun onBalanceChanged(balanceWithMeta: BalanceWithMeta) {
        val balanceEventDto = BalanceWithMetaEventConverter.convert(balanceWithMeta)
        publisher.send(KafkaEventFactory.balanceEvent(balanceEventDto)).ensureSuccess()
        logger.info("Balance event sent: $balanceEventDto")
    }
}
