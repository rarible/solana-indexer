package com.rarible.protocol.solana.nft.listener.update

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.blockchain.scanner.publisher.LogRecordEventPublisher
import com.rarible.protocol.solana.common.records.SolanaAuctionHouseOrderRecord
import com.rarible.protocol.solana.common.records.SolanaBalanceRecord
import com.rarible.protocol.solana.common.records.SolanaTokenRecord
import com.rarible.protocol.solana.common.records.SubscriberGroup
import org.springframework.stereotype.Component

/**
 * Service responsible for triggering internal update events for business entities.
 */
@Component
class InternalUpdateEventService(
    private val logRecordEventPublisher: LogRecordEventPublisher
) {
    suspend fun sendInternalTokenUpdateRecords(records: List<SolanaTokenRecord.InternalTokenUpdateRecord>) {
        logRecordEventPublisher.publish(
            groupId = SubscriberGroup.TOKEN.id,
            logRecordEvents = records.map { LogRecordEvent(it, false) }
        )
    }

    suspend fun sendInternalBalanceUpdateRecords(records: List<SolanaBalanceRecord.InternalBalanceUpdateRecord>) {
        logRecordEventPublisher.publish(
            groupId = SubscriberGroup.BALANCE.id,
            logRecordEvents = records.map { LogRecordEvent(it, false) }
        )
    }

    suspend fun sendInternalOrderUpdateRecords(records: List<SolanaAuctionHouseOrderRecord.InternalOrderUpdateRecord>) {
        logRecordEventPublisher.publish(
            groupId = SubscriberGroup.AUCTION_HOUSE_ORDER.id,
            logRecordEvents = records.map { LogRecordEvent(it, false) }
        )
    }
}