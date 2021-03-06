package com.rarible.protocol.solana.common.records

import com.rarible.blockchain.scanner.solana.model.SolanaLog
import java.math.BigInteger
import java.time.Instant

sealed class SolanaBalanceRecord : SolanaBaseLogRecord() {
    abstract val account: String
    abstract val mint: String

    override fun getKey(): String = account

    data class InitializeBalanceAccountRecord(
        override val account: String,
        val owner: String,
        override val mint: String,
        override val log: SolanaLog,
        override val timestamp: Instant
    ) : SolanaBalanceRecord()

    data class MintToRecord(
        val mintAmount: BigInteger,
        override val mint: String,
        override val account: String,
        override val log: SolanaLog,
        override val timestamp: Instant
    ) : SolanaBalanceRecord()

    data class ChangeOwnerRecord(
        val oldOwner: String,
        val newOwner: String,
        override val mint: String,
        override val account: String,
        override val log: SolanaLog,
        override val timestamp: Instant
    ) : SolanaBalanceRecord()

    data class BurnRecord(
        override val mint: String,
        val burnAmount: BigInteger,
        override val account: String,
        override val log: SolanaLog,
        override val timestamp: Instant
    ) : SolanaBalanceRecord()

    data class TransferIncomeRecord(
        val from: String,
        override val account: String,
        override val mint: String,
        val incomeAmount: BigInteger,
        override val log: SolanaLog,
        override val timestamp: Instant
    ) : SolanaBalanceRecord() {

        /**
         * Append ':income' to distinguish this
         * log from [TransferOutcomeRecord] having the same [log].
         */
        override val id: String get() = super.id + ":income"
    }

    data class TransferOutcomeRecord(
        val to: String,
        override val account: String,
        override val mint: String,
        val outcomeAmount: BigInteger,
        override val log: SolanaLog,
        override val timestamp: Instant
    ) : SolanaBalanceRecord() {

        /**
         * Append ':outcome' to distinguish this
         * log from [TransferIncomeRecord] having the same [log].
         */
        override val id: String get() = super.id + ":outcome"
    }
}
