package com.rarible.protocol.solana.common.meta

import com.rarible.protocol.solana.common.model.TokenId
import com.rarible.protocol.solana.common.repository.MetaplexMetaRepository
import com.rarible.protocol.solana.common.repository.MetaplexOffChainMetaRepository
import org.springframework.stereotype.Component

@Component
class TokenMetaGetService(
    private val metaplexMetaRepository: MetaplexMetaRepository,
    private val metaplexOffChainMetaRepository: MetaplexOffChainMetaRepository,
) {

    suspend fun getTokenMeta(
        tokenAddress: TokenId,
        acceptWithoutOffChainMeta: Boolean = false
    ): TokenMeta? {
        val onChainMeta = metaplexMetaRepository.findByTokenAddress(tokenAddress) ?: return null
        val offChainMeta = metaplexOffChainMetaRepository.findByTokenAddress(tokenAddress)
        if (offChainMeta == null && !acceptWithoutOffChainMeta) {
            return null
        }
        return TokenMetaParser.mergeOnChainAndOffChainMeta(
            onChainMeta = onChainMeta.metaFields,
            offChainMeta = offChainMeta?.metaFields
        )
    }

}
