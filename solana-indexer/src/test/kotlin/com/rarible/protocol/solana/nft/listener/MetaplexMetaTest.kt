package com.rarible.protocol.solana.nft.listener

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.solana.common.meta.MetaplexOffChainCollectionHash
import com.rarible.protocol.solana.common.meta.TokenMeta
import com.rarible.protocol.solana.common.model.MetaplexMeta
import com.rarible.protocol.solana.common.model.MetaplexMetaFields
import com.rarible.protocol.solana.common.model.MetaplexOffChainMeta
import com.rarible.protocol.solana.common.model.MetaplexOffChainMetaFields
import com.rarible.protocol.solana.common.model.MetaplexTokenCreator
import com.rarible.protocol.solana.common.records.SolanaMetaRecord
import com.rarible.protocol.solana.common.records.SubscriberGroup
import com.rarible.protocol.solana.test.ANY_SOLANA_LOG
import com.rarible.protocol.solana.test.createRandomBalance
import com.rarible.protocol.solana.test.createRandomMetaplexMeta
import com.rarible.protocol.solana.test.createRandomMetaplexOffChainMeta
import com.rarible.protocol.solana.test.createRandomToken
import io.mockk.coEvery
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class MetaplexMetaTest : EventAwareBlockScannerTest() {
    @Test
    fun `Update metadata`() = runBlocking {
        val collectionOld = mintNft(baseKeypair)
        val collectionNew = mintNft(baseKeypair)
        val token = mintNft(baseKeypair, collectionOld)

        updateMetadata(baseKeypair, token, collectionNew)

        Wait.waitAssert {
            val createRecord = findRecordByType(
                SubscriberGroup.METAPLEX_META.collectionName,
                SolanaMetaRecord.MetaplexCreateMetadataAccountRecord::class.java
            ).filter { it.mint == token }.single()

            val updateRecords = findRecordByType(
                SubscriberGroup.METAPLEX_META.collectionName,
                SolanaMetaRecord.MetaplexUpdateMetadataRecord::class.java
            ).toList()

            assertThat(updateRecords).usingElementComparatorIgnoringFields(
                SolanaMetaRecord.MetaplexUpdateMetadataRecord::log.name,
                SolanaMetaRecord.MetaplexUpdateMetadataRecord::timestamp.name
            ).isEqualTo(
                listOf(
                    SolanaMetaRecord.MetaplexUpdateMetadataRecord(
                        mint = token,
                        updatedMeta = createRecord.meta.copy(collection = MetaplexMetaFields.Collection(collectionNew, false)),
                        updatedMutable = null,
                        updateAuthority = null,
                        primarySaleHappened = null,
                        metaAccount = createRecord.metaAccount,
                        log = ANY_SOLANA_LOG,
                        timestamp = Instant.EPOCH
                    )
                )
            )
        }
    }

    @Test
    fun `create metadata - send token and balance update events`() = runBlocking {
        val wallet = getWallet()

        val metaplexOffChainMeta = createRandomMetaplexOffChainMeta().let {
            it.copy(
                metaFields = it.metaFields.copy(
                    collection = MetaplexOffChainMetaFields.Collection(
                        name = "name",
                        family = "family",
                        hash = MetaplexOffChainCollectionHash.calculateCollectionHash(
                            name = "name",
                            family = "family",
                            creators = listOf(wallet)
                        )
                    )
                )
            )
        }
        mockOffChainMeta(metaplexOffChainMeta)

        val tokenAddress = mintNft(baseKeypair)

        Wait.waitAssert {
            assertThat(metaplexMetaRepository.findByTokenAddress(tokenAddress))
                .usingRecursiveComparison()
                .ignoringFields(
                    "metaAddress",
                    "updatedAt",
                    "createdAt",
                    "revertableEvents"
                ).isEqualTo(
                    MetaplexMeta(
                        tokenAddress = tokenAddress,
                        metaFields = MetaplexMetaFields(
                            name = "My NFT #1",
                            uri = "http://host.testcontainers.internal:8080/meta/meta.json",
                            symbol = "MY_SYMBOL",
                            sellerFeeBasisPoints = 420,
                            creators = listOf(
                                MetaplexTokenCreator(
                                    address = wallet,
                                    share = 100,
                                    verified = true
                                )
                            ),
                            collection = null
                        ),
                        isMutable = true,
                        metaAddress = "",
                        updatedAt = Instant.EPOCH,
                        createdAt = Instant.EPOCH,
                        revertableEvents = emptyList()
                    )
                )
        }

        assertTokenMetaUpdatedEvent(
            tokenAddress = tokenAddress,
            creators = listOf(
                MetaplexTokenCreator(
                    address = wallet,
                    share = 100,
                    verified = true
                )
            ),
            collection = TokenMeta.Collection.OffChain(
                name = metaplexOffChainMeta.metaFields.collection!!.name,
                family = metaplexOffChainMeta.metaFields.collection!!.family,
                hash = metaplexOffChainMeta.metaFields.collection!!.hash,
            )
        )
    }

    @Test
    fun `create and verify collection - send token and balance update events`() = runBlocking<Unit> {
        // Off-chain collection will be overridden with on-chain one.
        mockOffChainMeta(createRandomMetaplexOffChainMeta())

        val wallet = getWallet()
        val collection = mintNft(baseKeypair)
        val tokenAddress = mintNft(baseKeypair, collection)

        Wait.waitAssert {
            val meta = metaplexMetaRepository.findByTokenAddress(tokenAddress)
            assertThat(meta?.metaFields?.collection?.verified).isFalse
        }

        // Insert some balance that should be updated.
        val balance = createRandomBalance().copy(
            mint = tokenAddress
        )
        balanceRepository.save(balance)

        verifyCollection(tokenAddress, collection)

        Wait.waitAssert {
            val meta = metaplexMetaRepository.findByTokenAddress(tokenAddress)
            assertThat(meta?.metaFields?.collection?.verified).isTrue
        }

        val onChainCollection = TokenMeta.Collection.OnChain(
            address = collection,
            verified = true
        )
        assertTokenMetaUpdatedEvent(
            tokenAddress = tokenAddress,
            creators = listOf(
                MetaplexTokenCreator(
                    address = wallet,
                    share = 100,
                    verified = false
                )
            ),
            collection = onChainCollection
        )

        assertBalanceMetaUpdatedEvent(
            balanceAccount = balance.account,
            collection = onChainCollection
        )
    }

    @Test
    fun `off-chain meta is loaded - send token and balance update events`() = runBlocking<Unit> {
        val token = createRandomToken()
        tokenRepository.save(token)

        val balance = createRandomBalance().copy(
            mint = token.mint
        )
        balanceRepository.save(balance)

        val metaplexMeta = createRandomMetaplexMeta().copy(
            tokenAddress = token.mint
        )
        metaplexMetaRepository.save(metaplexMeta)

        val metaplexOffChainMeta = createRandomMetaplexOffChainMeta().copy(
            tokenAddress = token.mint
        )
        coEvery {
            testMetaplexOffChainMetaLoader.loadMetaplexOffChainMeta(
                tokenAddress = token.mint,
                metaplexMetaFields = metaplexMeta.metaFields
            )
        } returns metaplexOffChainMeta

        // TODO: rework.
//        metaplexOffChainMetaLoadService.loadOffChainTokenMeta(token.mint)

        // On-chain collection takes precedence.
        val onChainCollection = TokenMeta.Collection.OnChain(
            address = metaplexMeta.metaFields.collection!!.address,
            verified = metaplexMeta.metaFields.collection!!.verified
        )
        assertTokenMetaUpdatedEvent(
            tokenAddress = token.mint,
            creators = metaplexMeta.metaFields.creators,
            collection = onChainCollection
        )

        assertBalanceMetaUpdatedEvent(
            balanceAccount = balance.account,
            collection = onChainCollection
        )
    }

    // Mock off-chain meta before minting the token because we don't know the token address.
    // If we don't have off-chain meta before the on-chain is loaded, there will be no event.
    // TODO: in cli-nft.ts provide a way to mint token to a deterministic address.
    private fun mockOffChainMeta(metaplexOffChainMeta: MetaplexOffChainMeta) {
        coEvery {
            metaplexOffChainMetaRepository.findByTokenAddress(any())
        } returns metaplexOffChainMeta
    }
}
