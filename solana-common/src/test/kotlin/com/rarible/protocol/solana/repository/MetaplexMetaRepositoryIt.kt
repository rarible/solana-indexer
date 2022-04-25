package com.rarible.protocol.solana.repository

import com.rarible.protocol.solana.AbstractIntegrationTest
import com.rarible.protocol.solana.common.repository.MetaplexMetaRepository
import com.rarible.protocol.solana.test.createRandomMetaplexMeta
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MetaplexMetaRepositoryIt : AbstractIntegrationTest() {

    @Test
    fun `save and find by meta address`() = runBlocking<Unit> {
        val metaplexMeta = createRandomMetaplexMeta()
        metaplexMetaRepository.save(metaplexMeta)
        assertThat(metaplexMetaRepository.findByMetaAddress(metaplexMeta.metaAddress)).isEqualTo(metaplexMeta)
    }

    @Test
    fun `save and find by meta collection address`() = runBlocking<Unit> {
        val metaplexMeta = createRandomMetaplexMeta()
        val metaplexMeta2 = createRandomMetaplexMeta().let {
            it.copy(metaFields = it.metaFields.copy(collection = metaplexMeta.metaFields.collection))
        }
        val metaplexMeta3 = createRandomMetaplexMeta()
        metaplexMetaRepository.save(metaplexMeta)
        metaplexMetaRepository.save(metaplexMeta2)
        metaplexMetaRepository.save(metaplexMeta3)
        assertThat(metaplexMetaRepository.findByCollectionAddress(metaplexMeta.metaFields.collection!!.address).toList())
            .isEqualTo(listOf(metaplexMeta, metaplexMeta2).sortedBy { it.tokenAddress })
    }

    @Test
    fun `save and find by token address`() = runBlocking<Unit> {
        val metaplexMeta = createRandomMetaplexMeta()
        metaplexMetaRepository.save(metaplexMeta)
        assertThat(metaplexMetaRepository.findByTokenAddress(metaplexMeta.tokenAddress)).isEqualTo(metaplexMeta)
    }

    @Test
    fun `save and find multiple metas by token address`() = runBlocking<Unit> {
        val metaplexMetas = (0..10).map {
            createRandomMetaplexMeta().also { metaplexMetaRepository.save(it) }
        }
        val expectedAddresses = metaplexMetas.map { it.tokenAddress }.toSet()
        val resultAddresses = metaplexMetaRepository
            .findByTokenAddresses(expectedAddresses).map { it.tokenAddress }.toSet()
        assertThat(resultAddresses).isEqualTo(expectedAddresses)
    }
}
