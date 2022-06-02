package com.rarible.protocol.solana.common.meta

import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.common.model.BalanceWithMeta
import com.rarible.protocol.solana.common.model.MetaplexMeta
import com.rarible.protocol.solana.common.model.MetaplexOffChainMeta
import com.rarible.protocol.solana.common.model.Token
import com.rarible.protocol.solana.common.model.TokenId
import com.rarible.protocol.solana.common.model.TokenWithMeta
import com.rarible.protocol.solana.common.repository.MetaplexMetaRepository
import com.rarible.protocol.solana.common.repository.MetaplexOffChainMetaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component

@Component
class TokenMetaService(
    private val metaplexMetaRepository: MetaplexMetaRepository,
    private val metaplexOffChainMetaRepository: MetaplexOffChainMetaRepository,
) {

    private suspend fun getOnChainMeta(tokenAddress: TokenId): MetaplexMeta? =
        metaplexMetaRepository.findByTokenAddress(tokenAddress)

    private suspend fun getOffChainMeta(tokenAddress: TokenId): MetaplexOffChainMeta? =
        metaplexOffChainMetaRepository.findByTokenAddress(tokenAddress)

    private suspend fun getOnChainMeta(tokenAddresses: Collection<TokenId>): Flow<MetaplexMeta> =
        if (tokenAddresses.isNotEmpty()) metaplexMetaRepository.findByTokenAddresses(tokenAddresses)
        else emptyFlow()

    private suspend fun getOffChainMeta(tokenAddresses: Collection<TokenId>): Flow<MetaplexOffChainMeta> =
        if (tokenAddresses.isNotEmpty()) metaplexOffChainMetaRepository.findByTokenAddresses(tokenAddresses)
        else emptyFlow()

    private suspend fun getOnChainMetaByCollection(collection: String, fromTokenAddress: String?, limit: Int?) =
        metaplexMetaRepository.findByCollectionAddress(collection, fromTokenAddress, limit)

    private suspend fun getOffChainMetaByCollection(collection: String, fromTokenAddress: String?, limit: Int?) =
        metaplexOffChainMetaRepository.findByOffChainCollectionHash(collection, fromTokenAddress, limit)

    suspend fun extendWithAvailableMeta(tokens: Flow<Token>): Flow<TokenWithMeta> {
        return tokens.mapNotNull { token ->
            val metaplexMeta = getOnChainMeta(token.mint) ?: return@mapNotNull null
            val metaplexOffChainMeta = getOffChainMeta(token.mint) ?: return@mapNotNull null
            val tokenMeta = TokenMetaParser.mergeOnChainAndOffChainMeta(
                onChainMeta = metaplexMeta.metaFields,
                offChainMeta = metaplexOffChainMeta.metaFields
            )
            TokenWithMeta(token, tokenMeta)
        }
    }

    suspend fun getTokensMetaByCollection(
        collection: String,
        fromTokenAddress: String?,
        limit: Int?
    ): Map<String, TokenMeta> {
        val (onChainMap, offChainMap) = coroutineScope {
            val onChainMap = async {
                getOnChainMetaByCollection(collection, fromTokenAddress, limit)
                    .toTokenAddressMetaMap()
            }
            val offChainMap = async {
                getOffChainMetaByCollection(collection, fromTokenAddress, limit)
                    .toTokenAddressOffChainMetaMap()
            }
            onChainMap.await() to offChainMap.await()
        }

        val restOnChainMap = getOnChainMeta(offChainMap.keys - onChainMap.keys)
            .toTokenAddressMetaMap()

        val restOffChainMap = getOffChainMeta(onChainMap.keys - offChainMap.keys)
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

    suspend fun extendWithAvailableMeta(token: Token): TokenWithMeta {
        val tokenMeta = getAvailableTokenMeta(token.mint)
        return TokenWithMeta(token, tokenMeta)
    }

    suspend fun extendWithAvailableMeta(balance: Balance): BalanceWithMeta {
        val tokenMeta = getAvailableTokenMeta(balance.mint)
        return BalanceWithMeta(balance, tokenMeta)
    }

    suspend fun getAvailableTokenMeta(tokenAddress: TokenId): TokenMeta? {
        val onChainMeta = getOnChainMeta(tokenAddress) ?: return null
        val offChainMeta = getOffChainMeta(tokenAddress)
        return TokenMetaParser.mergeOnChainAndOffChainMeta(
            onChainMeta = onChainMeta.metaFields,
            offChainMeta = offChainMeta?.metaFields
        )
    }

    private suspend fun Flow<MetaplexOffChainMeta>.toTokenAddressOffChainMetaMap() =
        map { it.tokenAddress to it.metaFields }.toList().toMap()

    private suspend fun Flow<MetaplexMeta>.toTokenAddressMetaMap() =
        map { it.tokenAddress to it.metaFields }.toList().toMap()

}
