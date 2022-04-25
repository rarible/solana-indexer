package com.rarible.protocol.solana.nft.listener.service.token

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.solana.common.event.BurnEvent
import com.rarible.protocol.solana.common.event.InitializeMintEvent
import com.rarible.protocol.solana.common.event.TokenInternalUpdateEvent
import com.rarible.protocol.solana.common.event.MintEvent
import com.rarible.protocol.solana.common.event.TokenEvent
import com.rarible.protocol.solana.common.model.Token
import com.rarible.protocol.solana.common.model.isEmpty
import com.rarible.protocol.solana.common.records.SolanaTokenUpdateInstruction
import org.springframework.stereotype.Component

@Component
class ForwardValueTokenReducer : Reducer<TokenEvent, Token> {
    override suspend fun reduce(entity: Token, event: TokenEvent): Token {
        if (event !is InitializeMintEvent && entity.isEmpty) {
            return entity
        }
        return when (event) {
            is InitializeMintEvent -> entity.copy(
                createdAt = event.timestamp,
                decimals = event.decimals
            )
            is MintEvent -> entity.copy(
                supply = entity.supply + event.amount
            )
            is BurnEvent -> entity.copy(
                supply = entity.supply - event.amount
            )
            is TokenInternalUpdateEvent -> when (event.instruction) {
                // We do not need to execute any updates here, meta will be set in the TokenUpdateService.
                is SolanaTokenUpdateInstruction.TokenMetaUpdated -> entity
            }
        }.copy(updatedAt = event.timestamp)
    }
}
