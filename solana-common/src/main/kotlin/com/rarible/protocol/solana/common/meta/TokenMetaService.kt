package com.rarible.protocol.solana.common.meta

import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.common.model.BalanceWithMeta
import com.rarible.protocol.solana.common.model.MetaplexMeta
import com.rarible.protocol.solana.common.model.MetaplexOffChainMeta
import com.rarible.protocol.solana.common.model.TokenId
import com.rarible.protocol.solana.common.repository.MetaplexMetaRepository
import com.rarible.protocol.solana.common.repository.MetaplexOffChainMetaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component

@Component
class TokenMetaService(
    private val metaplexMetaRepository: MetaplexMetaRepository,
    private val metaplexOffChainMetaRepository: MetaplexOffChainMetaRepository,
) {

    suspend fun getTokensMetaByCollection(
        collection: String,
        fromTokenAddress: String?,
        limit: Int
    ): Map<String, TokenMeta> {
        val (onChainMap, offChainMap) = coroutineScope {
            val onChainMap = async {
                metaplexMetaRepository.findByCollectionAddress(collection, fromTokenAddress, limit)
                    .toTokenAddressMetaMap()
            }
            val offChainMap = async {
                metaplexOffChainMetaRepository.findByOffChainCollectionHash(collection, fromTokenAddress, limit)
                    .toTokenAddressOffChainMetaMap()
            }
            onChainMap.await() to offChainMap.await()
        }

        val restOnChainMap = metaplexMetaRepository.findByTokenAddresses(offChainMap.keys - onChainMap.keys)
            .toTokenAddressMetaMap()

        val restOffChainMap = metaplexOffChainMetaRepository.findByTokenAddresses(onChainMap.keys - offChainMap.keys)
            .toTokenAddressOffChainMetaMap()

        val onChainMetaMapFull = onChainMap + restOnChainMap
        val offChainMetaMapFull = offChainMap + restOffChainMap

        return onChainMetaMapFull.map { (tokenAddress, onChainMeta) ->
            tokenAddress to TokenMetaParser.mergeOnChainAndOffChainMeta(
                onChainMeta = onChainMeta,
                offChainMeta = offChainMetaMapFull[tokenAddress]
            )
        }.toMap()
    }

    suspend fun getAvailableTokenMeta(tokenAddress: TokenId): TokenMeta? {
        val onChainMeta = metaplexMetaRepository.findByTokenAddress(tokenAddress) ?: return null
        val offChainMeta = metaplexOffChainMetaRepository.findByTokenAddress(tokenAddress) ?: return null
        return TokenMetaParser.mergeOnChainAndOffChainMeta(
            onChainMeta = onChainMeta.metaFields,
            offChainMeta = offChainMeta.metaFields
        )
    }

    private suspend fun Flow<MetaplexOffChainMeta>.toTokenAddressOffChainMetaMap() =
        map { it.tokenAddress to it.metaFields }.toList().toMap()

    private suspend fun Flow<MetaplexMeta>.toTokenAddressMetaMap() =
        map { it.tokenAddress to it.metaFields }.toList().toMap()

}
