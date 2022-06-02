package com.rarible.protocol.solana.common.service.collection

import com.rarible.core.entity.reducer.service.EntityTemplateProvider
import com.rarible.protocol.solana.common.model.SolanaCollection
import com.rarible.protocol.solana.common.model.SolanaCollectionId
import org.springframework.stereotype.Component

@Component
class CollectionTemplateProvider : EntityTemplateProvider<SolanaCollectionId, SolanaCollection> {
    override fun getEntityTemplate(id: SolanaCollectionId): SolanaCollection =
        SolanaCollection.empty(id)
}