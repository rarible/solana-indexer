package com.rarible.protocol.solana.common.update

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.solana.common.converter.BalanceEventConverter
import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.dto.BalanceEventDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BalanceUpdateListener(
    private val publisher: RaribleKafkaProducer<BalanceEventDto>
) {
    private val logger = LoggerFactory.getLogger(BalanceUpdateListener::class.java)

    suspend fun onBalanceChanged(balance: Balance) {
        val balanceEventDto = BalanceEventConverter.convert(balance)
        publisher.send(KafkaEventFactory.balanceEvent(balanceEventDto)).ensureSuccess()
        logger.info("Balance event sent: $balanceEventDto")
    }
}
