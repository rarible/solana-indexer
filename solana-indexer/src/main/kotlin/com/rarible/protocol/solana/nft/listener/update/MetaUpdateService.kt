package com.rarible.protocol.solana.nft.listener.update

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.solana.common.model.MetaId
import com.rarible.protocol.solana.common.model.MetaplexMeta
import com.rarible.protocol.solana.common.model.isEmpty
import com.rarible.protocol.solana.common.repository.MetaplexMetaRepository
import com.rarible.protocol.solana.common.update.TokenMetaUpdateListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class MetaUpdateService(
    private val metaplexMetaRepository: MetaplexMetaRepository,
    private val tokenMetaUpdateListener: TokenMetaUpdateListener
) : EntityService<MetaId, MetaplexMeta> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun get(id: MetaId): MetaplexMeta? =
        metaplexMetaRepository.findByMetaAddress(id)

    override suspend fun update(entity: MetaplexMeta): MetaplexMeta {
        if (entity.isEmpty) {
            logger.info("Meta is in empty state: ${entity.id}")
            return entity
        }
        val meta = metaplexMetaRepository.save(entity)
        logger.info("Updated metaplex meta: $meta")
        tokenMetaUpdateListener.triggerTokenMetaLoading(entity.tokenAddress)
        return meta
    }
}
