package com.rarible.protocol.solana.nft.listener.service.balance

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.solana.common.event.BalanceEvent
import com.rarible.protocol.solana.common.event.BalanceIncomeEvent
import com.rarible.protocol.solana.common.event.BalanceInitializeAccountEvent
import com.rarible.protocol.solana.common.event.BalanceInternalUpdateEvent
import com.rarible.protocol.solana.common.event.BalanceOutcomeEvent
import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.common.model.isEmpty
import com.rarible.protocol.solana.common.records.SolanaBalanceUpdateInstruction
import org.springframework.stereotype.Component

@Component
class ForwardBalanceReducer : Reducer<BalanceEvent, Balance> {

    override suspend fun reduce(entity: Balance, event: BalanceEvent): Balance {
        if (event !is BalanceInitializeAccountEvent && entity.isEmpty) {
            return entity
        }
        return when (event) {
            is BalanceInitializeAccountEvent -> entity.copy(
                createdAt = event.timestamp,
                owner = event.owner,
                mint = event.mint
            )
            is BalanceIncomeEvent -> entity.copy(
                value = entity.value + event.amount
            )
            is BalanceOutcomeEvent -> entity.copy(
                value = entity.value - event.amount
            )
            is BalanceInternalUpdateEvent -> when (event.instruction) {
                // We do not need to execute any updates here, meta will be set in the BalanceUpdateService.
                is SolanaBalanceUpdateInstruction.BalanceMetaUpdated -> entity
            }
        }.copy(updatedAt = event.timestamp)
    }
}
