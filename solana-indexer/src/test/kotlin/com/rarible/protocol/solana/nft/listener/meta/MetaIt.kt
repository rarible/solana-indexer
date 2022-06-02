package com.rarible.protocol.solana.nft.listener.meta

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.solana.common.meta.TokenMeta
import com.rarible.protocol.solana.common.model.MetaplexMeta
import com.rarible.protocol.solana.common.model.MetaplexMetaFields
import com.rarible.protocol.solana.common.model.MetaplexTokenCreator
import com.rarible.protocol.solana.common.records.EMPTY_SOLANA_LOG
import com.rarible.protocol.solana.common.records.SolanaMetaRecord
import com.rarible.protocol.solana.common.records.SubscriberGroup
import com.rarible.protocol.solana.nft.listener.EventAwareBlockScannerTest
import com.rarible.protocol.solana.test.createRandomBalance
import com.rarible.protocol.solana.test.createRandomMetaplexMeta
import com.rarible.protocol.solana.test.createRandomMetaplexOffChainCollection
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

class MetaIt : EventAwareBlockScannerTest() {
    @Test
    fun `create and update metadata with collection V2 - send token update events`() = runBlocking {
        val wallet = getWallet()

        val collectionOld = mintNft(baseKeypair)
        val collectionNew = mintNft(baseKeypair)
        val tokenAddress = mintNft(baseKeypair, collectionOld)

        updateMetadata(baseKeypair, tokenAddress, collectionNew)

        Wait.waitAssert {
            val createRecord = findRecordByType(
                SubscriberGroup.METAPLEX_META.collectionName,
                SolanaMetaRecord.MetaplexCreateMetadataAccountRecord::class.java
            ).filter { it.mint == tokenAddress }.single()

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
                        mint = tokenAddress,
                        updatedMeta = createRecord.meta.copy(
                            collection = MetaplexMetaFields.Collection(
                                collectionNew,
                                false
                            )
                        ),
                        updatedMutable = null,
                        updateAuthority = null,
                        primarySaleHappened = null,
                        metaAccount = createRecord.metaAccount,
                        log = EMPTY_SOLANA_LOG,
                        timestamp = Instant.EPOCH
                    )
                )
            )
        }

        val onChainCollection = TokenMeta.Collection.OnChain(
            address = collectionNew,
            verified = false
        )
        assertTokenMetaUpdatedEvent(
            tokenAddress = tokenAddress,
            creators = listOf(
                MetaplexTokenCreator(
                    address = wallet,
                    share = 100,
                    verified = true
                )
            ),
            collection = onChainCollection
        )
    }

    @Test
    fun `create metadata with collection V1 - send token update events`() = runBlocking {
        val wallet = getWallet()

        val metaplexOffChainCollection = createRandomMetaplexOffChainCollection(creators = listOf(wallet))
        val metaplexOffChainMeta = createRandomMetaplexOffChainMeta().let {
            it.copy(metaFields = it.metaFields.copy(collection = metaplexOffChainCollection))
        }

        // Mock loading of the off-chain meta for a token. We don't know the token address in advance, so using any() here.
        coEvery {
            testMetaplexOffChainMetaLoader.loadMetaplexOffChainMeta(
                tokenAddress = any(),
                metaplexMetaFields = any()
            )
        } returns metaplexOffChainMeta

        val tokenAddress = mintNft(baseKeypair)

        Wait.waitAssert {
            assertThat(metaplexMetaRepository.findByTokenAddress(tokenAddress))
                .usingRecursiveComparison()
                .ignoringFields(
                    MetaplexMeta::metaAddress.name,
                    MetaplexMeta::updatedAt.name,
                    MetaplexMeta::createdAt.name,
                    MetaplexMeta::revertableEvents.name
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

        val offChainCollection = TokenMeta.Collection.OffChain(
            name = metaplexOffChainCollection.name,
            family = metaplexOffChainCollection.family,
            hash = metaplexOffChainCollection.hash,
        )
        assertTokenMetaUpdatedEvent(
            tokenAddress = tokenAddress,
            creators = listOf(
                MetaplexTokenCreator(
                    address = wallet,
                    share = 100,
                    verified = true
                )
            ),
            collection = offChainCollection
        )
    }

    @Test
    fun `create and verify collection V2 - send token update events`() = runBlocking<Unit> {
        val wallet = getWallet()
        val collection = mintNft(baseKeypair)
        val tokenAddress = mintNft(baseKeypair, collection)

        Wait.waitAssert {
            val meta = metaplexMetaRepository.findByTokenAddress(tokenAddress)
            assertThat(meta?.metaFields?.collection?.verified).isFalse
        }

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

        metaplexOffChainMetaLoadService.loadOffChainTokenMeta(token.mint)

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
}
