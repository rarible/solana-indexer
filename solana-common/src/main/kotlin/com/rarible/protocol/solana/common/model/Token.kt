package com.rarible.protocol.solana.common.model

import com.rarible.protocol.solana.common.event.TokenEvent
import com.rarible.protocol.solana.common.meta.TokenMeta
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigInteger
import java.time.Instant

typealias TokenId = String

@Document(Token.COLLECTION)
data class Token(
    @Id
    val mint: String,
    val supply: BigInteger,
    val decimals: Int,
    /**
     * Denormalized token meta. It is set when the token meta is fully loaded.
     */
    val tokenMeta: TokenMeta?,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    override val revertableEvents: List<TokenEvent>
) : SolanaEntity<TokenId, TokenEvent, Token> {

    override val id: TokenId get() = mint

    override fun withRevertableEvents(events: List<TokenEvent>): Token =
        copy(revertableEvents = events)

    override fun toString(): String =
        buildString {
            append("Token(mint='$mint'")
            when (tokenMeta?.collection) {
                is TokenMeta.Collection.OffChain -> append(", collection=" + tokenMeta.collection.hash)
                is TokenMeta.Collection.OnChain -> append(", collection=" + tokenMeta.collection.address)
                null -> Unit
            }
            if (tokenMeta != null) {
                append(", name='${tokenMeta.name}', symbol='${tokenMeta.symbol}'")
            }
            append(", supply=$supply")
            append(", decimals=$decimals")
            append(", createdAt=$createdAt")
            append(", updatedAt=$updatedAt")
            append(")")
        }

    companion object {

        const val COLLECTION = "token"

        fun empty(mint: String): Token = Token(
            mint = mint,
            supply = BigInteger.ZERO,
            revertableEvents = emptyList(),
            tokenMeta = null,
            decimals = 0,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH
        )
    }
}
