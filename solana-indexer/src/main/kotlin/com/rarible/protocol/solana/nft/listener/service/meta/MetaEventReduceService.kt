package com.rarible.protocol.solana.nft.listener.service.meta

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.solana.nft.listener.consumer.LogRecordEventListenerId
import com.rarible.protocol.solana.nft.listener.consumer.LogRecordEventListener
import com.rarible.protocol.solana.nft.listener.consumer.SolanaLogRecordEvent
import com.rarible.protocol.solana.common.records.SolanaMetaRecord
import com.rarible.protocol.solana.common.records.SubscriberGroup
import com.rarible.protocol.solana.nft.listener.update.MetaUpdateService
import org.springframework.stereotype.Component

@Component
class MetaEventReduceService(
    entityService: MetaUpdateService,
    entityIdService: MetaIdService,
    templateProvider: MetaTemplateProvider,
    reducer: MetaReducer,
    environmentInfo: ApplicationEnvironmentInfo,
    private val metaEventConverter: MetaEventConverter
) : LogRecordEventListener {
    private val delegate = EventReduceService(entityService, entityIdService, templateProvider, reducer)

    override val id: String = LogRecordEventListenerId.metaHistoryListenerId(environmentInfo.name)

    override val subscriberGroup: SubscriberGroup = SubscriberGroup.METAPLEX_META

    override suspend fun onEntityEvents(events: List<SolanaLogRecordEvent>) {
        val metaEvents = events
            .filter { it.record is SolanaMetaRecord }
            .flatMap { metaEventConverter.convert(it.record as SolanaMetaRecord, it.reversed) }

        delegate.reduceAll(metaEvents)
    }
}
