package com.rarible.protocol.solana.common.meta

import com.rarible.protocol.solana.common.converter.CollectionConverter
import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.common.model.BalanceWithMeta
import com.rarible.protocol.solana.common.model.MetaplexMeta
import com.rarible.protocol.solana.common.model.MetaplexOffChainMeta
import com.rarible.protocol.solana.common.model.Token
import com.rarible.protocol.solana.common.model.TokenId
import com.rarible.protocol.solana.common.model.TokenWithMeta
import com.rarible.protocol.solana.common.repository.MetaplexMetaRepository
import com.rarible.protocol.solana.common.repository.MetaplexOffChainMetaRepository
import com.rarible.protocol.solana.common.service.CollectionService
import com.rarible.protocol.solana.common.update.CollectionEventListener
import com.rarible.protocol.solana.common.update.MetaplexMetaUpdateListener
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
class TokenMetaService(
    private val metaplexMetaRepository: MetaplexMetaRepository,
    private val metaplexOffChainMetaRepository: MetaplexOffChainMetaRepository,
    private val metaplexOffChainMetaLoader: MetaplexOffChainMetaLoader,
    private val collectionService: CollectionService,
    private val collectionEventListener: CollectionEventListener,
) {

    private val logger = LoggerFactory.getLogger(TokenMetaService::class.java)

    @Lazy
    @Autowired
    private lateinit var metaplexMetaUpdateListener: MetaplexMetaUpdateListener

    suspend fun getOnChainMeta(tokenAddress: TokenId): MetaplexMeta? =
        metaplexMetaRepository.findByTokenAddress(tokenAddress)

    suspend fun getOffChainMeta(tokenAddress: TokenId): MetaplexOffChainMeta? =
        metaplexOffChainMetaRepository.findByTokenAddress(tokenAddress)

    suspend fun getOnChainMeta(tokenAddresses: Collection<TokenId>): Flow<MetaplexMeta> =
        if (tokenAddresses.isNotEmpty()) metaplexMetaRepository.findByTokenAddresses(tokenAddresses)
        else emptyFlow()

    suspend fun getOffChainMeta(tokenAddresses: Collection<TokenId>): Flow<MetaplexOffChainMeta> =
        if (tokenAddresses.isNotEmpty()) metaplexOffChainMetaRepository.findByTokenAddresses(tokenAddresses)
        else emptyFlow()

    private suspend fun getOnChainMetaByCollection(collection: String, fromTokenAddress: String?) =
        metaplexMetaRepository.findByCollectionAddress(collection, fromTokenAddress)

    private suspend fun getOffChainMetaByCollection(collection: String, fromTokenAddress: String?) =
        metaplexOffChainMetaRepository.findByOffChainCollectionHash(collection, fromTokenAddress)

    suspend fun extendWithAvailableMeta(tokens: Flow<Token>): Flow<TokenWithMeta> {
        val tokenMap = tokens.toList().associateBy { it.mint }
        return getTokenIdToMetaMapByTokenIds(tokenMap.keys)
            .map { (tokenAddress, tokenMeta) ->
                TokenWithMeta(tokenMap[tokenAddress]!!, tokenMeta)
            }
    }

    suspend fun extendBalancesWithAvailableMeta(balances: Flow<Balance>): Flow<BalanceWithMeta> {
        val balanceList = balances.toList()
        val balanceMints = balanceList.map { it.mint }.toSet()
        val balanceMap = getTokenIdToMetaMapByTokenIds(balanceMints).toList().toMap()

        return balanceList.map { balance ->
            BalanceWithMeta(balance, balanceMap[balance.mint])
        }.asFlow()
    }

    private suspend fun getTokenIdToMetaMapByTokenIds(mints: Collection<TokenId>): Flow<Pair<String, TokenMeta>> {
        val offChainMetaMap = getOffChainMeta(mints)
            .toTokenAddressOffChainMetaMap()

        return getOnChainMeta(mints)
            .map { metaplexMeta ->
                metaplexMeta.tokenAddress to TokenMetaParser.mergeOnChainAndOffChainMeta(
                    onChainMeta = metaplexMeta.metaFields,
                    offChainMeta = offChainMetaMap[metaplexMeta.tokenAddress],
                )
            }
    }

    suspend fun getTokensMetaByCollection(collection: String, fromTokenAddress: String?): Map<String, TokenMeta> {
        val (onChainMap, offChainMap) = coroutineScope {
            val onChainMap = async {
                getOnChainMetaByCollection(collection, fromTokenAddress)
                    .toTokenAddressMetaMap()
            }
            val offChainMap = async {
                getOffChainMetaByCollection(collection, fromTokenAddress)
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

    suspend fun loadTokenMeta(tokenAddress: TokenId): TokenMeta? {
        val onChainMeta = getOnChainMeta(tokenAddress) ?: return null
        val metaFields = onChainMeta.metaFields
        val metaplexOffChainMeta = metaplexOffChainMetaLoader.loadMetaplexOffChainMeta(
            tokenAddress = tokenAddress,
            metaplexMetaFields = metaFields
        ) ?: return null
        updateCollection(metaplexOffChainMeta)

        val tokenMeta = TokenMetaParser.mergeOnChainAndOffChainMeta(metaFields, metaplexOffChainMeta.metaFields)
        metaplexMetaUpdateListener.onTokenMetaChanged(tokenAddress, tokenMeta)
        return tokenMeta
    }

    suspend fun onMetaplexMetaChanged(metaplexMeta: MetaplexMeta) {
        val offChainMeta = getOffChainMeta(metaplexMeta.tokenAddress)
        if (offChainMeta == null) {
            logger.info(
                "There is no off-chain meta loaded for ${metaplexMeta.tokenAddress}, " +
                        "so ignoring the on-chain metaplex meta update yet."
            )
            return
        }
        val tokenMeta = TokenMetaParser.mergeOnChainAndOffChainMeta(
            onChainMeta = metaplexMeta.metaFields,
            offChainMeta = offChainMeta.metaFields
        )
        metaplexMetaUpdateListener.onTokenMetaChanged(metaplexMeta.tokenAddress, tokenMeta)
    }

    private suspend fun Flow<MetaplexOffChainMeta>.toTokenAddressOffChainMetaMap() =
        map { it.tokenAddress to it.metaFields }.toList().toMap()

    private suspend fun Flow<MetaplexMeta>.toTokenAddressMetaMap() =
        map { it.tokenAddress to it.metaFields }.toList().toMap()

    private suspend fun updateCollection(metaplexOffChainMeta: MetaplexOffChainMeta) {
        val collection = collectionService.updateCollectionV1(metaplexOffChainMeta) ?: return
        val dto = CollectionConverter.convertV1(collection)
        collectionEventListener.onCollectionChanged(dto)
    }
}
