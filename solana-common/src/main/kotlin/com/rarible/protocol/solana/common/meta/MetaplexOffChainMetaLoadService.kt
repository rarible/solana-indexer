package com.rarible.protocol.solana.common.meta

import com.rarible.protocol.solana.common.event.UpdateCollectionV1Event
import com.rarible.protocol.solana.common.model.MetaplexOffChainMeta
import com.rarible.protocol.solana.common.model.TokenId
import com.rarible.protocol.solana.common.repository.MetaplexMetaRepository
import com.rarible.protocol.solana.common.service.collection.CollectionEventPublisher
import com.rarible.protocol.solana.common.update.TokenMetaUpdateListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MetaplexOffChainMetaLoadService(
    private val metaplexMetaRepository: MetaplexMetaRepository,
    private val metaplexOffChainMetaLoader: MetaplexOffChainMetaLoader,
    private val collectionEventService: CollectionEventPublisher,
    private val tokenMetaUpdateListener: TokenMetaUpdateListener
) {

    suspend fun loadOffChainTokenMeta(tokenAddress: TokenId): TokenMeta? {
        val onChainMeta = metaplexMetaRepository.findByTokenAddress(tokenAddress) ?: return null
        val metaFields = onChainMeta.metaFields
        val metaplexOffChainMeta = metaplexOffChainMetaLoader.loadMetaplexOffChainMeta(
            tokenAddress = tokenAddress,
            metaplexMetaFields = metaFields
        )
        if (metaplexOffChainMeta != null) {
            createReferencedCollectionV1IfNecessary(metaplexOffChainMeta)
        }

        // TODO[meta]: here we send the TokenUpdateEvent and BalanceUpdateEvent even if the off-chain meta was not loaded.
        //  Better off would be to not send them, because the off-chain meta may contain an important field (such as "off-chain" collection address for V1 NFTs)
        //  without which the events do not make much sense.
        val tokenMeta = TokenMetaParser.mergeOnChainAndOffChainMeta(metaFields, metaplexOffChainMeta?.metaFields)
        tokenMetaUpdateListener.onTokenMetaChanged(tokenAddress, tokenMeta)
        return tokenMeta
    }

    private suspend fun createReferencedCollectionV1IfNecessary(metaplexOffChainMeta: MetaplexOffChainMeta) {
        val offChainCollection = metaplexOffChainMeta.metaFields.collection
        if (offChainCollection != null) {
            collectionEventService.send(
                UpdateCollectionV1Event(
                    id = offChainCollection.hash,
                    name = offChainCollection.name,
                    family = offChainCollection.family
                )
            )
        }
    }

}
