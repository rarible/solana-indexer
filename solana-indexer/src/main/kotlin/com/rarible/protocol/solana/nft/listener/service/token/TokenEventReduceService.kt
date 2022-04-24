package com.rarible.protocol.solana.nft.listener.service.token

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.solana.nft.listener.consumer.LogRecordEventListenerId
import com.rarible.protocol.solana.nft.listener.consumer.LogRecordEventListener
import com.rarible.protocol.solana.nft.listener.consumer.SolanaLogRecordEvent
import com.rarible.protocol.solana.common.records.SolanaTokenRecord
import com.rarible.protocol.solana.common.records.SubscriberGroup
import com.rarible.protocol.solana.nft.listener.update.TokenUpdateService
import org.springframework.stereotype.Component

@Component
class TokenEventReduceService(
    entityService: TokenUpdateService,
    entityIdService: TokenIdService,
    templateProvider: TokenTemplateProvider,
    reducer: TokenReducer,
    environmentInfo: ApplicationEnvironmentInfo,
    private val tokenEventConverter: TokenEventConverter
) : LogRecordEventListener {
    private val delegate = EventReduceService(entityService, entityIdService, templateProvider, reducer)

    override val id: String = LogRecordEventListenerId.tokenHistoryListenerId(environmentInfo.name)

    override val subscriberGroup: SubscriberGroup = SubscriberGroup.TOKEN

    override suspend fun onEntityEvents(events: List<SolanaLogRecordEvent>) {
        val tokenEvents = events
            .filter { it.record is SolanaTokenRecord }
            .flatMap { tokenEventConverter.convert(it.record as SolanaTokenRecord, it.reversed) }

        delegate.reduceAll(tokenEvents)
    }
}
