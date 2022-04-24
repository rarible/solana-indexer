package com.rarible.protocol.solana.nft.listener.service.balance

import com.rarible.protocol.solana.common.event.BalanceEvent
import com.rarible.protocol.solana.common.event.BalanceIncomeEvent
import com.rarible.protocol.solana.common.event.BalanceInitializeAccountEvent
import com.rarible.protocol.solana.common.event.BalanceOutcomeEvent
import com.rarible.protocol.solana.common.event.BalanceInternalUpdateEvent
import com.rarible.protocol.solana.common.records.SolanaBalanceRecord
import org.springframework.stereotype.Component

@Component
class BalanceEventConverter {
    suspend fun convert(
        record: SolanaBalanceRecord,
        reversed: Boolean
    ): List<BalanceEvent> = when (record) {
        is SolanaBalanceRecord.MintToRecord -> listOf(
            BalanceIncomeEvent(
                reversed = reversed,
                account = record.account,
                amount = record.mintAmount,
                log = record.log,
                timestamp = record.timestamp
            )
        )
        is SolanaBalanceRecord.BurnRecord -> listOf(
            BalanceOutcomeEvent(
                reversed = reversed,
                account = record.account,
                amount = record.burnAmount,
                log = record.log,
                timestamp = record.timestamp
            )
        )
        is SolanaBalanceRecord.TransferIncomeRecord -> listOf(
            BalanceIncomeEvent(
                reversed = reversed,
                account = record.account,
                amount = record.incomeAmount,
                log = record.log,
                timestamp = record.timestamp
            )
        )
        is SolanaBalanceRecord.TransferOutcomeRecord -> listOf(
            BalanceOutcomeEvent(
                reversed = reversed,
                account = record.account,
                amount = record.outcomeAmount,
                log = record.log,
                timestamp = record.timestamp
            )
        )
        is SolanaBalanceRecord.InitializeBalanceAccountRecord -> listOf(
            BalanceInitializeAccountEvent(
                reversed = reversed,
                account = record.account,
                owner = record.owner,
                mint = record.mint,
                log = record.log,
                timestamp = record.timestamp
            )
        )
        is SolanaBalanceRecord.InternalBalanceUpdateRecord -> listOf(
            BalanceInternalUpdateEvent(
                instruction = record.instruction,
                account = record.account,
                timestamp = record.timestamp,
                log = record.log,
                reversed = reversed
            )
        )
    }
}
