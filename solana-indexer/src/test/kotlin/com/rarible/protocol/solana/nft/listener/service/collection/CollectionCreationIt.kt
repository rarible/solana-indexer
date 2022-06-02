package com.rarible.protocol.solana.nft.listener.service.collection

import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.solana.common.event.UpdateCollectionV1Event
import com.rarible.protocol.solana.common.event.UpdateCollectionV2Event
import com.rarible.protocol.solana.common.meta.MetaplexOffChainCollectionHash
import com.rarible.protocol.solana.common.meta.TokenMetaParser
import com.rarible.protocol.solana.common.model.SolanaCollectionV1
import com.rarible.protocol.solana.common.model.SolanaCollectionV2
import com.rarible.protocol.solana.nft.listener.AbstractBlockScannerTest
import com.rarible.protocol.solana.test.createRandomMetaplexMeta
import com.rarible.protocol.solana.test.createRandomMetaplexOffChainMeta
import com.rarible.protocol.solana.test.randomMint
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class CollectionCreationIt : AbstractBlockScannerTest() {
    @Test
    fun `create collection v1`() = runBlocking<Unit> {
        val name = "name"
        val family = "family"
        val id = MetaplexOffChainCollectionHash.calculateCollectionHash(
            name = name,
            family = family,
            creators = listOf(randomString())
        )
        collectionEventPublisher.send(
            UpdateCollectionV1Event(
                id = id,
                name = name,
                family = family
            )
        )
        Wait.waitAssert {
            assertThat(collectionRepository.findById(id))
                .usingRecursiveComparison().ignoringFields(
                    SolanaCollectionV1::createdAt.name,
                    SolanaCollectionV1::updatedAt.name
                )
                .isEqualTo(
                    SolanaCollectionV1(
                        id = id,
                        name = name,
                        family = family,
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH
                    )
                )
        }
    }

    @Test
    fun `create collection v2 - without meta`() = runBlocking<Unit> {
        val collectionMint = randomMint()
        collectionEventPublisher.send(UpdateCollectionV2Event(id = collectionMint))
        Wait.waitAssert {
            assertThat(collectionRepository.findById(collectionMint))
                .usingRecursiveComparison().ignoringFields(
                    SolanaCollectionV2::createdAt.name,
                    SolanaCollectionV2::updatedAt.name
                )
                .isEqualTo(
                    SolanaCollectionV2(
                        id = collectionMint,
                        name = null,
                        collectionMeta = null,
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH
                    )
                )
        }
    }

    @Test
    fun `create collection v2 - attach available meta`() = runBlocking<Unit> {
        val collectionMint = randomMint()

        // Save MetaplexMeta and its off-chain meta to the repository.
        val metaplexMeta = createRandomMetaplexMeta(collectionMint)
        metaplexMetaRepository.save(metaplexMeta)
        val offChainMeta = createRandomMetaplexOffChainMeta(collectionMint)
        metaplexOffChainMetaRepository.save(offChainMeta)
        val tokenMeta = TokenMetaParser.mergeOnChainAndOffChainMeta(metaplexMeta.metaFields, offChainMeta.metaFields)

        collectionEventPublisher.send(UpdateCollectionV2Event(id = collectionMint))
        Wait.waitAssert {
            assertThat(collectionRepository.findById(collectionMint))
                .usingRecursiveComparison().ignoringFields(
                    SolanaCollectionV2::createdAt.name,
                    SolanaCollectionV2::updatedAt.name
                )
                .isEqualTo(
                    SolanaCollectionV2(
                        id = collectionMint,
                        name = tokenMeta.name,
                        collectionMeta = tokenMeta,
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH
                    )
                )
        }
    }
}