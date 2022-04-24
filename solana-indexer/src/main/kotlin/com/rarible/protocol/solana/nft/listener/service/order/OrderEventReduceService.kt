package com.rarible.protocol.solana.nft.listener.service.order

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.solana.common.converter.SolanaAuctionHouseOrderActivityConverter
import com.rarible.protocol.solana.common.records.SolanaAuctionHouseOrderRecord
import com.rarible.protocol.solana.common.records.SubscriberGroup
import com.rarible.protocol.solana.common.update.ActivityEventListener
import com.rarible.protocol.solana.nft.listener.consumer.LogRecordEventListener
import com.rarible.protocol.solana.nft.listener.consumer.LogRecordEventListenerId
import com.rarible.protocol.solana.nft.listener.consumer.SolanaLogRecordEvent
import com.rarible.protocol.solana.nft.listener.update.OrderUpdateService
import org.springframework.stereotype.Component

@Component
class OrderEventReduceService(
    entityService: OrderUpdateService,
    entityIdService: OrderIdService,
    templateProvider: OrderTemplateProvider,
    reducer: OrderReducer,
    environmentInfo: ApplicationEnvironmentInfo,
    val converter: OrderEventConverter,
    private val orderActivityConverter: SolanaAuctionHouseOrderActivityConverter,
    private val activityEventListener: ActivityEventListener
) : LogRecordEventListener {
    private val delegate = EventReduceService(entityService, entityIdService, templateProvider, reducer)

    override val id: String = LogRecordEventListenerId.auctionHouseListenerId(environmentInfo.name)

    override val subscriberGroup: SubscriberGroup = SubscriberGroup.AUCTION_HOUSE_ORDER

    override suspend fun onEntityEvents(events: List<SolanaLogRecordEvent>) {
        val auctionHouseEvents = events
            .filter { it.record is SolanaAuctionHouseOrderRecord }
            .flatMap { converter.convert(it.record as SolanaAuctionHouseOrderRecord, it.reversed) }

        delegate.reduceAll(auctionHouseEvents)
        publishActivityEventsWithRevertedFlagCheck(events)
    }

    private suspend fun publishActivityEventsWithRevertedFlagCheck(events: List<SolanaLogRecordEvent>) {
        // Window events by reverted/not reverted flag and process with activity converter only events of the same type.
        val current = arrayListOf<SolanaLogRecordEvent>()
        for (event in events) {
            if (current.lastOrNull()?.reversed == event.reversed) {
                 current.add(event)
            } else {
                publishActivityEvents(current)
                current.clear()
                current.add(event)
            }
        }
        publishActivityEvents(current)
    }

    private suspend fun publishActivityEvents(events: List<SolanaLogRecordEvent>) {
        if (events.isEmpty()) {
            return
        }
        val activitiesDto = events.mapNotNull {
            orderActivityConverter.convert(
                it.record as SolanaAuctionHouseOrderRecord,
                it.reversed
            )
        }
        activityEventListener.onActivities(activitiesDto)
    }

}
