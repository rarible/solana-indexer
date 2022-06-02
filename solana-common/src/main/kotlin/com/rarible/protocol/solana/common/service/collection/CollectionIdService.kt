package com.rarible.protocol.solana.common.service.collection

import com.rarible.core.entity.reducer.service.EntityIdService
import com.rarible.protocol.solana.common.event.CollectionEvent
import com.rarible.protocol.solana.common.model.SolanaCollectionId
import org.springframework.stereotype.Component

@Component
class CollectionIdService : EntityIdService<CollectionEvent, SolanaCollectionId> {
    override fun getEntityId(event: CollectionEvent): SolanaCollectionId = event.id
}