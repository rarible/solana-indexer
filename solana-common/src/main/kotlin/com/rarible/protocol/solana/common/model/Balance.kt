package com.rarible.protocol.solana.common.model

import com.rarible.protocol.solana.common.event.BalanceEvent
import com.rarible.protocol.solana.common.meta.TokenMeta
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigInteger
import java.time.Instant

typealias BalanceId = String

@Document(Balance.COLLECTION)
data class Balance(
    @Id
    val account: BalanceId,
    val owner: String,
    val mint: String,
    val value: BigInteger,
    /**
     * Denormalized name of the NFT taken from the Metaplex on-chain meta.
     * It is set when the meta is fully loaded.
     */
    val tokenName: String?,
    /**
     * Denormalized collection of the NFT.
     * It is set when the meta is fully loaded.
     */
    val collection: TokenMeta.Collection?,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val revertableEvents: List<BalanceEvent>
) : SolanaEntity<BalanceId, BalanceEvent, Balance> {

    override val id: BalanceId get() = account

    override fun withRevertableEvents(events: List<BalanceEvent>): Balance =
        copy(revertableEvents = events)

    override fun toString(): String =
        "Balance(account='$account', value=$value, mint='$mint', owner='$owner', createdAt=$createdAt, updatedAt=$updatedAt)"

    companion object {
        const val COLLECTION = "balance"

        fun empty(account: String): Balance = Balance(
            account = account,
            owner = "",
            mint = "",
            tokenName = null,
            collection = null,
            value = BigInteger.ZERO,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
            revertableEvents = emptyList()
        )
    }
}
