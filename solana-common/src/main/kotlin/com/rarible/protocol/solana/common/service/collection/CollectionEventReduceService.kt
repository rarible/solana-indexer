package com.rarible.protocol.solana.common.service.collection

import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.solana.common.event.CollectionEvent
import org.springframework.stereotype.Component

@Component
class CollectionEventReduceService(
    collectionUpdateService: CollectionUpdateService,
    collectionIdService: CollectionIdService,
    collectionTemplateProvider: CollectionTemplateProvider,
    collectionReducer: CollectionReducer
) {
    private val delegate = EventReduceService(
        collectionUpdateService,
        collectionIdService,
        collectionTemplateProvider,
        collectionReducer
    )

    suspend fun onEvents(events: List<CollectionEvent>) {
        delegate.reduceAll(events)
    }
}