package com.rarible.protocol.solana.nft.listener.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.solana.common.event.UpdateCollectionV2Event
import com.rarible.protocol.solana.common.meta.TokenMetaService
import com.rarible.protocol.solana.common.model.SolanaCollectionV2
import com.rarible.protocol.solana.common.repository.CollectionRepository
import com.rarible.protocol.solana.common.service.collection.CollectionEventPublisher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Background job to set `collectionMeta` for all V2 collections.
 * Used as a migration.
 */
@Component
class CollectionMetaSetterTaskHandler(
    private val collectionRepository: CollectionRepository,
    private val collectionEventPublisher: CollectionEventPublisher,
    private val tokenMetaService: TokenMetaService
) : TaskHandler<String> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val type: String = "COLLECTION_META_SETTER"

    override fun runLongTask(from: String?, param: String): Flow<String> {
        logger.info("Starting $type with from: $from, param: $param")
        return collectionRepository.findAll(from).map { collection ->
            if (collection is SolanaCollectionV2 && collection.collectionMeta == null) {
                val tokenMeta = tokenMetaService.getAvailableTokenMeta(tokenAddress = collection.id)
                if (tokenMeta == null) {
                    logger.info("CollectionMetaSetterTaskHandler: no meta found for collection ${collection.id}")
                } else {
                    logger.info("CollectionMetaSetterTaskHandler: updating meta for collection ${collection.id}")
                    collectionEventPublisher.send(UpdateCollectionV2Event(collection.id))
                }
            }
            collection.id
        }
    }
}