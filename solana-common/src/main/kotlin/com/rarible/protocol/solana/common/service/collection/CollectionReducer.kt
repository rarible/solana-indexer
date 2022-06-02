package com.rarible.protocol.solana.common.service.collection

import com.rarible.core.common.nowMillis
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.solana.common.event.CollectionEvent
import com.rarible.protocol.solana.common.event.UpdateCollectionV1Event
import com.rarible.protocol.solana.common.event.UpdateCollectionV2Event
import com.rarible.protocol.solana.common.meta.TokenMetaService
import com.rarible.protocol.solana.common.model.SolanaCollection
import com.rarible.protocol.solana.common.model.SolanaCollectionV1
import com.rarible.protocol.solana.common.model.SolanaCollectionV2
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class CollectionReducer(
    private val tokenMetaService: TokenMetaService
) : Reducer<CollectionEvent, SolanaCollection> {
    override suspend fun reduce(
        entity: SolanaCollection,
        event: CollectionEvent
    ): SolanaCollection {
        val nowTime = nowMillis()
        return when (event) {
            is UpdateCollectionV1Event -> {
                SolanaCollectionV1(
                    id = event.id,
                    name = event.name,
                    family = event.family,
                    createdAt = nowTime,
                    updatedAt = nowTime
                )
            }
            is UpdateCollectionV2Event -> {
                val tokenMeta = tokenMetaService.getAvailableTokenMeta(event.id)
                val createdAt = entity.createdAt?.takeIf { it != Instant.EPOCH } ?: nowTime
                SolanaCollectionV2(
                    id = event.id,
                    createdAt = createdAt,
                    updatedAt = nowTime,
                    collectionMeta = tokenMeta
                )
            }
        }
    }
}