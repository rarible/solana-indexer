package com.rarible.protocol.solana.nft.listener.service.meta

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.solana.common.meta.TokenMetaService
import com.rarible.protocol.solana.common.model.MetaId
import com.rarible.protocol.solana.common.model.MetaplexMeta
import com.rarible.protocol.solana.common.repository.MetaplexMetaRepository
import com.rarible.protocol.solana.common.service.CollectionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MetaUpdateService(
    private val metaplexMetaRepository: MetaplexMetaRepository,
    private val tokenMetaService: TokenMetaService,
    private val collectionService: CollectionService
) : EntityService<MetaId, MetaplexMeta> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun get(id: MetaId): MetaplexMeta? =
        metaplexMetaRepository.findByMetaAddress(id)

    override suspend fun update(entity: MetaplexMeta): MetaplexMeta {
        // We need to update collection before item, otherwise there could be situation item belongs to collection
        // which is not exists in DB
        collectionService.updateCollectionV2(entity)
        val meta = metaplexMetaRepository.save(entity)
        logger.info("Updated metaplex meta: $entity")
        tokenMetaService.onMetaplexMetaChanged(meta)
        return meta
    }

}
