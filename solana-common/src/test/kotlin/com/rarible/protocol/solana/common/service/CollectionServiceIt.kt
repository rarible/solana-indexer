package com.rarible.protocol.solana.common.service

import com.rarible.core.test.data.randomString
import com.rarible.protocol.solana.AbstractIntegrationTest
import com.rarible.protocol.solana.common.model.SolanaCollectionV1
import com.rarible.protocol.solana.common.model.SolanaCollectionV2
import com.rarible.protocol.solana.test.createRandomTokenMetaCollectionOffChain
import com.rarible.protocol.solana.test.createRandomTokenMetaCollectionOnChain
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CollectionServiceIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var collectionService: CollectionService

    @Test
    fun `save and get`() = runBlocking<Unit> {
        val v1 = SolanaCollectionV1(randomString(), randomString(), randomString())
        val v2 = SolanaCollectionV2(randomString())

        collectionService.save(v1)
        collectionService.save(v2)

        assertThat(collectionService.findById(v1.id)).isEqualTo(v1)
        assertThat(collectionService.findById(v2.id)).isEqualTo(v2)
    }

    @Test
    fun `find all`() = runBlocking<Unit> {
        val c3 = collectionService.save(SolanaCollectionV1("3", randomString(), randomString()))
        val c2 = collectionService.save(SolanaCollectionV2("2"))
        val c1 = collectionService.save(SolanaCollectionV2("1"))

        val page1 = collectionService.findAll(null, 1)
        val page2 = collectionService.findAll("111", 2)

        assertThat(page1).hasSize(1)
        assertThat(page2).hasSize(2)

        assertThat(page1[0]).isEqualTo(c1)
        assertThat(page2[0]).isEqualTo(c2)
        assertThat(page2[1]).isEqualTo(c3)
    }

    @Test
    fun `update collection v1 - new collection`() = runBlocking<Unit> {
        val collection = createRandomTokenMetaCollectionOffChain()
        collectionService.updateCollection(collection)
        val saved = collectionService.findById(collection.hash)
        assertThat(saved).isEqualTo(SolanaCollectionV1(collection.hash, collection.name, collection.family))
    }

    @Test
    fun `update collection v1 - collection exists`() = runBlocking<Unit> {
        val collection = createRandomTokenMetaCollectionOffChain()

        val exists = SolanaCollectionV1(
            id = collection.hash,
            name = randomString(),
            family = randomString()
        )
        collectionService.save(exists)

        collectionService.updateCollection(collection)
        val saved = collectionService.findById(collection.hash)

        // Should not be updated
        assertThat(saved).isEqualTo(exists)
    }

    @Test
    fun `update collection v2`() = runBlocking<Unit> {
        val collection = createRandomTokenMetaCollectionOnChain()

        val expected = SolanaCollectionV2(collection.address)

        collectionService.updateCollection(collection)
        val saved = collectionService.findById(collection.address)

        assertThat(saved).isEqualTo(expected)
    }

}