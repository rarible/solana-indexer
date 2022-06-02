package com.rarible.protocol.solana.nft.api.controller

import com.rarible.protocol.solana.api.controller.CollectionControllerApi
import com.rarible.protocol.solana.common.continuation.CollectionContinuation
import com.rarible.protocol.solana.common.continuation.Paging
import com.rarible.protocol.solana.common.event.UpdateCollectionV2Event
import com.rarible.protocol.solana.common.pubkey.PublicKey
import com.rarible.protocol.solana.common.service.collection.CollectionEventPublisher
import com.rarible.protocol.solana.dto.CollectionDto
import com.rarible.protocol.solana.dto.CollectionsByIdRequestDto
import com.rarible.protocol.solana.dto.CollectionsDto
import com.rarible.protocol.solana.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.solana.nft.api.service.CollectionApiService
import com.rarible.protocol.union.dto.continuation.page.PageSize
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class CollectionController(
    private val collectionApiService: CollectionApiService,
    private val collectionEventPublisher: CollectionEventPublisher
) : CollectionControllerApi {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int?
    ): ResponseEntity<CollectionsDto> {
        val safeSize = PageSize.COLLECTION.limit(size)

        val collectionsDto = collectionApiService.findAll(continuation)
            .take(safeSize).toList()
        val dto = toSlice(collectionsDto, safeSize)
        return ResponseEntity.ok(dto)
    }

    override suspend fun getCollectionById(collection: String): ResponseEntity<CollectionDto> {
        val collectionDto = collectionApiService.findById(collection)
            ?: throw EntityNotFoundApiException("Collection", collection)
        return ResponseEntity.ok(collectionDto)
    }

    override suspend fun searchCollectionsByIds(collectionsByIdRequestDto: CollectionsByIdRequestDto): ResponseEntity<CollectionsDto> {
        val collections = collectionApiService.findByIds(collectionsByIdRequestDto.ids).toList()
        return ResponseEntity.ok(
            CollectionsDto(
                collections = collections,
                continuation = null,
            )
        )
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<CollectionsDto> {
        // TODO implement
        return ResponseEntity.ok(CollectionsDto())
    }

    override suspend fun refreshCollectionMeta(collection: String): ResponseEntity<Unit> {
        if (!PublicKey.isPubKey(collection)) {
            return ResponseEntity.ok().build()
        }
        markNftAsCollection(collection)
        return ResponseEntity.ok().build()
    }

    /**
     * Hint by the SDK that a concrete NFT is actually a collection NFT.
     * We cannot distinguish individual NFTs from collection NFTs while the collection is empty.
     */
    private suspend fun markNftAsCollection(collectionMint: String) {
        logger.info("Marking NFT $collectionMint as collection V2")
        collectionEventPublisher.send(UpdateCollectionV2Event(collectionMint))
    }

    private suspend fun toSlice(collections: List<CollectionDto>, size: Int): CollectionsDto {
        val continuationFactory = CollectionContinuation.ById
        val slice = Paging(continuationFactory, collections).getSlice(size)
        return CollectionsDto(slice.entities, slice.continuation)
    }
}