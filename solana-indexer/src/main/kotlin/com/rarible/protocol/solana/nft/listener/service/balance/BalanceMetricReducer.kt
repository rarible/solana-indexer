package com.rarible.protocol.solana.nft.listener.service.balance

import com.rarible.protocol.solana.common.configuration.SolanaIndexerProperties
import com.rarible.protocol.solana.common.event.BalanceEvent
import com.rarible.protocol.solana.common.event.BalanceIncomeEvent
import com.rarible.protocol.solana.common.event.BalanceInitializeAccountEvent
import com.rarible.protocol.solana.common.event.BalanceOutcomeEvent
import com.rarible.protocol.solana.common.event.BalanceInternalUpdateEvent
import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.nft.listener.service.AbstractMetricReducer
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class BalanceMetricReducer(
    properties: SolanaIndexerProperties,
    meterRegistry: MeterRegistry,
) : AbstractMetricReducer<BalanceEvent, Balance>(meterRegistry, properties, "balance") {

    override fun getMetricName(event: BalanceEvent): String {
        return when (event) {
            is BalanceInitializeAccountEvent -> "initialize_account"
            is BalanceOutcomeEvent -> "transfer_from"
            is BalanceIncomeEvent -> "transfer_to"
            is BalanceInternalUpdateEvent -> "internal_update"
        }
    }
}
