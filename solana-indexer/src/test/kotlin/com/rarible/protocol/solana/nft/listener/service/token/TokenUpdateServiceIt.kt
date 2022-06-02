package com.rarible.protocol.solana.nft.listener.service.token

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.solana.nft.listener.EventAwareBlockScannerTest
import com.rarible.protocol.solana.test.createRandomCollectionV2
import com.rarible.protocol.solana.test.createRandomToken
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TokenUpdateServiceIt : EventAwareBlockScannerTest() {

    @Autowired
    private lateinit var tokenUpdateService: TokenUpdateService

    @Test
    fun `new token inserted`() = runBlocking<Unit> {
        val token = createRandomToken()
        tokenUpdateService.update(token)
        assertThat(tokenRepository.findByMint(token.mint)).isEqualTo(token)
        assertTokenMetaUpdatedEvent(token.mint, null, null)
    }

    /**
     * Tests that a collection update event will be sent when collection NFT is changed.
     */
    @Test
    fun `collection NFT updated`() = runBlocking<Unit> {
        val collectionToken = createRandomToken()
        val collection = createRandomCollectionV2(id = collectionToken.mint)
        collectionRepository.save(collection)
        tokenUpdateService.update(collectionToken)
        Wait.waitAssert {
            assertUpdateCollectionEvent(
                collectionMint = collectionToken.mint,
                collectionTokenMeta = collection.collectionMeta!!
            )
        }
    }

}