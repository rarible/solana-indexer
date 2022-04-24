package com.rarible.protocol.solana.common.meta

import com.rarible.protocol.solana.common.model.TokenId
import com.rarible.protocol.solana.common.repository.MetaplexMetaRepository
import com.rarible.protocol.solana.common.service.CollectionConverter
import com.rarible.protocol.solana.common.service.CollectionService
import com.rarible.protocol.solana.common.update.CollectionEventListener
import com.rarible.protocol.solana.common.update.TokenMetaUpdateListener
import org.springframework.stereotype.Component

@Component
class TokenMetaService(
    private val metaplexMetaRepository: MetaplexMetaRepository,
    private val metaplexOffChainMetaLoader: MetaplexOffChainMetaLoader,

    private val collectionService: CollectionService,
    private val collectionConverter: CollectionConverter,
    private val collectionEventListener: CollectionEventListener,
    private val tokenMetaUpdateListener: TokenMetaUpdateListener
) {

    suspend fun loadAndSaveTokenMeta(tokenAddress: TokenId): TokenMeta? {
        val onChainMeta = metaplexMetaRepository.findByTokenAddress(tokenAddress) ?: return null
        val offChainMeta = metaplexOffChainMetaLoader.loadMetaplexOffChainMeta(
            tokenAddress = tokenAddress,
            metaplexMetaFields = onChainMeta.metaFields
        ) ?: return null
        val tokenMeta = TokenMetaParser.mergeOnChainAndOffChainMeta(
            onChainMeta = onChainMeta.metaFields,
            offChainMeta = offChainMeta.metaFields
        )
        tokenMetaUpdateListener.onTokenMetaChanged(tokenAddress, tokenMeta)
        updateCollection(tokenMeta)
        return tokenMeta
    }

    private suspend fun updateCollection(tokenMeta: TokenMeta) {
        val collection = collectionService.updateCollection(tokenMeta.collection) ?: return
        val dto = collectionConverter.toDto(collection)
        collectionEventListener.onCollectionChanged(dto)
    }
}
