package com.rarible.protocol.solana.common.event

import com.rarible.blockchain.scanner.solana.model.SolanaLog
import com.rarible.protocol.solana.common.records.SolanaBalanceUpdateInstruction
import java.math.BigInteger
import java.time.Instant

sealed class BalanceEvent : EntityEvent {
    abstract val timestamp: Instant
    abstract val account: String
    abstract override val reversed: Boolean
    abstract override val log: SolanaLog
    abstract fun invert(): BalanceEvent?
}

data class BalanceInitializeAccountEvent(
    override val timestamp: Instant,
    override val account: String,
    override val reversed: Boolean,
    override val log: SolanaLog,
    val mint: String,
    val owner: String
) : BalanceEvent() {
    override fun invert(): BalanceEvent? = null
}

data class BalanceIncomeEvent(
    override val timestamp: Instant,
    override val account: String,
    override val reversed: Boolean,
    val amount: BigInteger,
    override val log: SolanaLog
) : BalanceEvent() {
    override fun invert() = BalanceOutcomeEvent(timestamp, account, reversed, amount, log)
}

data class BalanceOutcomeEvent(
    override val timestamp: Instant,
    override val account: String,
    override val reversed: Boolean,
    val amount: BigInteger,
    override val log: SolanaLog
) : BalanceEvent() {
    override fun invert() = BalanceIncomeEvent(timestamp, account, reversed, amount, log)
}

data class BalanceInternalUpdateEvent(
    val instruction: SolanaBalanceUpdateInstruction,
    override val account: String,
    override val timestamp: Instant,
    override val log: SolanaLog,
    override val reversed: Boolean,
) : BalanceEvent() {
    override fun invert() = this
}
