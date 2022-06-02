package com.rarible.protocol.solana.nft.api.service

import com.rarible.protocol.solana.common.repository.CollectionRepository
import com.rarible.protocol.solana.common.service.collection.CollectionConverter
import com.rarible.protocol.solana.dto.CollectionDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.springframework.stereotype.Component

@Component
class CollectionApiService(
    private val collectionRepository: CollectionRepository
) {
    suspend fun findById(id: String): CollectionDto? =
        collectionRepository.findById(id)?.let { CollectionConverter.toDto(it) }

    suspend fun findByIds(ids: List<String>): Flow<CollectionDto> =
        collectionRepository.findByIds(ids).mapNotNull { CollectionConverter.toDto(it) }

    suspend fun findAll(fromId: String?): Flow<CollectionDto> =
        collectionRepository.findAll(fromId).mapNotNull { CollectionConverter.toDto(it) }
}