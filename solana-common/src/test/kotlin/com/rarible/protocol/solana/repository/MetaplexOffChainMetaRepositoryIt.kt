package com.rarible.protocol.solana.repository

import com.rarible.protocol.solana.AbstractIntegrationTest
import com.rarible.protocol.solana.test.createRandomMetaplexOffChainMeta
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MetaplexOffChainMetaRepositoryIt : AbstractIntegrationTest() {

    @Test
    fun `save and find by token address`() = runBlocking<Unit> {
        val metaplexOffChainMeta = createRandomMetaplexOffChainMeta()
        metaplexOffChainMetaRepository.save(metaplexOffChainMeta)
        assertThat(metaplexOffChainMetaRepository.findByTokenAddress(metaplexOffChainMeta.tokenAddress))
            .isEqualTo(metaplexOffChainMeta)
    }
}
