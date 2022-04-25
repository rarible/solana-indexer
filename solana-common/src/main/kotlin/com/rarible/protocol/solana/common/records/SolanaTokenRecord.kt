package com.rarible.protocol.solana.common.records

import com.rarible.blockchain.scanner.solana.model.SolanaLog
import java.math.BigInteger
import java.time.Instant

sealed class SolanaTokenRecord : SolanaBaseLogRecord() {
    abstract val mint: String

    override fun getKey(): String = mint

    data class InitializeMintRecord(
        val mintAuthority: String,
        val decimals: Int,
        override val mint: String,
        override val log: SolanaLog,
        override val timestamp: Instant
    ) : SolanaTokenRecord()

    data class MintToRecord(
        val tokenAccount: String,
        val mintAmount: BigInteger,
        override val mint: String,
        override val log: SolanaLog,
        override val timestamp: Instant
    ) : SolanaTokenRecord()

    data class BurnRecord(
        val tokenAccount: String,
        val burnAmount: BigInteger,
        override val mint: String,
        override val log: SolanaLog,
        override val timestamp: Instant
    ) : SolanaTokenRecord()

    /**
     * Fake record used to trigger update of a token.
     * A concrete update is determined by [instruction].
     * This record is not written to the database but only to the message bus (Kafka) to trigger an update.
     */
    data class InternalTokenUpdateRecord(
        override val mint: String,
        override val timestamp: Instant,
        val instruction: SolanaTokenUpdateInstruction,
        override val log: SolanaLog = EMPTY_SOLANA_LOG,
    ) : SolanaTokenRecord()

}
