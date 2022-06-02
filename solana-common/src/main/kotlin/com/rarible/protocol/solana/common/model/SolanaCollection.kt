package com.rarible.protocol.solana.common.model

import com.rarible.protocol.solana.common.event.CollectionEvent
import com.rarible.protocol.solana.common.meta.TokenMeta
import org.springframework.data.annotation.Id
import java.time.Instant

/**
 * ID of the collection.
 * - For V1 - Rarible specific hash (see `MetaplexOffChainCollectionHash`).
 * - For V2 - Mint address of the collection NFT.
 */
typealias SolanaCollectionId = String

/**
 * Base class for a Solana collection.
 * There are V1 (off-chain) and V2 (on-chain) collections.
 */
sealed class SolanaCollection : SolanaEntity<SolanaCollectionId, CollectionEvent, SolanaCollection> {
    /**
     * Unused. [SolanaCollection] does not need revertible events.
     */
    override val revertableEvents: List<CollectionEvent> get() = emptyList()

    /**
     * Unused. [SolanaCollection] does not need revertible events.
     */
    override fun withRevertableEvents(events: List<CollectionEvent>): SolanaCollection = this

    /**
     * Name of the collection. For V1 it is set immediately on creation.
     * For V2 it is set when the meta is loaded.
     */
    abstract val name: String?

    companion object {
        const val COLLECTION = "collection"

        /**
         * Placeholder for the reducer.
         * The value is actually never used because the fields
         * are overwritten immediately, see `CollectionReducer`.
         */
        fun empty(id: SolanaCollectionId): SolanaCollection =
            SolanaCollectionV2(
                id = id,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
                collectionMeta = null
            )
    }
}

data class SolanaCollectionV1(
    @Id
    override val id: SolanaCollectionId,
    override val name: String,
    val family: String?,
    override val createdAt: Instant?,
    override val updatedAt: Instant
) : SolanaCollection()

data class SolanaCollectionV2(
    @Id
    override val id: SolanaCollectionId,
    override val createdAt: Instant?,
    override val updatedAt: Instant,
    /**
     * Denormalized token meta of the NFT collection.
     * May be `null` until the meta is loaded.
     */
    val collectionMeta: TokenMeta?,
    override val name: String? = collectionMeta?.name
) : SolanaCollection()