package com.rarible.protocol.solana.common.service

import com.rarible.protocol.solana.AbstractIntegrationTest
import com.rarible.protocol.solana.common.repository.CollectionRepository
import com.rarible.protocol.solana.test.createRandomCollectionV1
import com.rarible.protocol.solana.test.createRandomCollectionV2
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CollectionRepositoryIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var collectionRepository: CollectionRepository

    @Test
    fun `save and get`() = runBlocking<Unit> {
        val v1 = createRandomCollectionV1()
        val v2 = createRandomCollectionV2()

        collectionRepository.save(v1)
        collectionRepository.save(v2)

        assertThat(collectionRepository.findById(v1.id)).isEqualTo(v1)
        assertThat(collectionRepository.findById(v2.id)).isEqualTo(v2)
    }

    @Test
    fun `find by ids`() = runBlocking<Unit> {
        val c1 = collectionRepository.save(createRandomCollectionV1())
        val c2 = collectionRepository.save(createRandomCollectionV1())
        val c3 = collectionRepository.save(createRandomCollectionV1())

        val actual = collectionRepository.findByIds(listOf(c1.id, c2.id, c3.id)).toList()
        assertThat(actual).containsExactlyInAnyOrder(c1, c2, c3)
    }

    @Test
    fun `find all`() = runBlocking<Unit> {
        val c3 = collectionRepository.save(createRandomCollectionV1("3"))
        val c2 = collectionRepository.save(createRandomCollectionV2("2"))
        val c1 = collectionRepository.save(createRandomCollectionV2("1"))
        assertThat(collectionRepository.findAll(null).toList()).isEqualTo(listOf(c1, c2, c3))
        assertThat(collectionRepository.findAll("111").toList()).isEqualTo(listOf(c2, c3))
    }
}