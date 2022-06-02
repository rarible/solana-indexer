package com.rarible.protocol.solana.common.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.blockchain.scanner.solana.model.SolanaLog
import com.rarible.protocol.solana.common.model.SolanaCollectionId
import com.rarible.protocol.solana.common.records.EMPTY_SOLANA_LOG

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "UPDATE_COLLECTION_V1", value = UpdateCollectionV1Event::class),
    JsonSubTypes.Type(name = "UPDATE_COLLECTION_V2", value = UpdateCollectionV2Event::class),
)
sealed class CollectionEvent : EntityEvent {
    abstract val id: SolanaCollectionId
    override val log: SolanaLog = EMPTY_SOLANA_LOG
    override val reversed: Boolean = false
}

/**
 * Creates a V1 collection if it does not exist yet.
 */
data class UpdateCollectionV1Event(
    override val id: SolanaCollectionId,
    val name: String,
    val family: String?
) : CollectionEvent()

/**
 * Creates a V2 collection if it does not exist yet.
 * Updates the collection's properties (such as meta),
 * and sends "CollectionUpdateEvent" to the outside world, if necessary.
 */
data class UpdateCollectionV2Event(
    override val id: SolanaCollectionId,
) : CollectionEvent()