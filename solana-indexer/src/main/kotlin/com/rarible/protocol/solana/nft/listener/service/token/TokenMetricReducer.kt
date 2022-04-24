package com.rarible.protocol.solana.nft.listener.service.token

import com.rarible.protocol.solana.common.configuration.SolanaIndexerProperties
import com.rarible.protocol.solana.common.event.BurnEvent
import com.rarible.protocol.solana.common.event.InitializeMintEvent
import com.rarible.protocol.solana.common.event.TokenInternalUpdateEvent
import com.rarible.protocol.solana.common.event.MintEvent
import com.rarible.protocol.solana.common.event.TokenEvent
import com.rarible.protocol.solana.common.model.Token
import com.rarible.protocol.solana.nft.listener.service.AbstractMetricReducer
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class TokenMetricReducer(
    properties: SolanaIndexerProperties,
    meterRegistry: MeterRegistry,
) : AbstractMetricReducer<TokenEvent, Token>(meterRegistry, properties, "token") {

    override fun getMetricName(event: TokenEvent): String {
        return when (event) {
            is BurnEvent -> "burn"
            is MintEvent -> "mint"
            is InitializeMintEvent -> "initialize_mint"
            is TokenInternalUpdateEvent -> "internal_update"
        }
    }
}
