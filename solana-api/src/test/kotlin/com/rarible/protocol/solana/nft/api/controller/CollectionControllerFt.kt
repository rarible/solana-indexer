package com.rarible.protocol.solana.nft.api.controller

import com.rarible.protocol.solana.common.repository.CollectionRepository
import com.rarible.protocol.solana.common.service.collection.CollectionConverter
import com.rarible.protocol.solana.dto.CollectionsByIdRequestDto
import com.rarible.protocol.solana.nft.api.test.AbstractControllerTest
import com.rarible.protocol.solana.test.createRandomCollectionV1
import com.rarible.protocol.solana.test.createRandomCollectionV2
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CollectionControllerFt : AbstractControllerTest() {

    @Autowired
    private lateinit var collectionRepository: CollectionRepository

    @Test
    fun `find collection v1 by id`() = runBlocking<Unit> {
        val collectionV1 = createRandomCollectionV1()
        collectionRepository.save(collectionV1)
        val result = collectionControllerApi.getCollectionById(collectionV1.id).awaitFirst()
        assertThat(result).isEqualTo(CollectionConverter.toDto(collectionV1))
    }

    @Test
    fun `find collection v2 by id`() = runBlocking<Unit> {
        val collectionV2 = createRandomCollectionV2()

        collectionRepository.save(collectionV2)

        val expected = CollectionConverter.toDto(collectionV2)
        val result = collectionControllerApi.getCollectionById(collectionV2.id).awaitFirst()
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `search by ids`() = runBlocking<Unit> {
        val c1 = createRandomCollectionV1()
        collectionRepository.save(c1)
        val c2 = createRandomCollectionV1()
        collectionRepository.save(c2)
        val expected1 = CollectionConverter.toDto(c1)
        val expected2 = CollectionConverter.toDto(c2)

        val actual = collectionControllerApi.searchCollectionsByIds(
            CollectionsByIdRequestDto(listOf(c1.id, c2.id))
        ).awaitFirst()

        assertThat(actual.collections).containsExactlyInAnyOrder(expected1, expected2)
        assertThat(actual.continuation).isNull()
    }
}
