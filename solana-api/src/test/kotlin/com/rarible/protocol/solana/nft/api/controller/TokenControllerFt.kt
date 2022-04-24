package com.rarible.protocol.solana.nft.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.protocol.solana.common.converter.TokenConverter
import com.rarible.protocol.solana.common.converter.TokenMetaConverter
import com.rarible.protocol.solana.common.meta.TokenMeta
import com.rarible.protocol.solana.common.meta.TokenMetaParser
import com.rarible.protocol.solana.common.model.MetaplexTokenCreator
import com.rarible.protocol.solana.common.model.Token
import com.rarible.protocol.solana.common.repository.TokenRepository
import com.rarible.protocol.solana.dto.RoyaltyDto
import com.rarible.protocol.solana.dto.TokensDto
import com.rarible.protocol.solana.nft.api.test.AbstractControllerTest
import com.rarible.protocol.solana.test.createRandomMetaplexMeta
import com.rarible.protocol.solana.test.createRandomMetaplexOffChainMeta
import com.rarible.protocol.solana.test.createRandomToken
import com.rarible.protocol.solana.test.createRandomTokenMeta
import com.rarible.protocol.solana.test.createRandomTokenMetaCollection
import com.rarible.protocol.solana.test.createRandomTokenMetaCollectionOffChain
import com.rarible.protocol.solana.test.createRandomTokenMetaCollectionOnChain
import io.mockk.coEvery
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class TokenControllerFt : AbstractControllerTest() {

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Test
    fun `find token by address`() = runBlocking<Unit> {
        val token = saveToken()

        val result = tokenControllerApi.getTokenByAddress(token.mint).awaitFirst()
        val expected = TokenConverter.convert(token)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `find all tokens - without filter`() = runBlocking<Unit> {
        val now = nowMillis()

        val token1 = saveToken(updatedAt = now)
        val token2 = saveToken(updatedAt = now.plusSeconds(1))

        // Should be sorted from newest to oldest

        //Page 1
        val expected1 = TokensDto(
            tokens = listOf(token2).map { TokenConverter.convert(it) },
            continuation = "${token2.updatedAt.toEpochMilli()}_${token2.mint}"
        )
        val page1 = getAllTokens(continuation = null, size = 1)
        assertThat(page1).isEqualTo(expected1)

        // Page 2
        val expected2 = TokensDto(
            tokens = listOf(token1).map { TokenConverter.convert(it) },
            continuation = "${token1.updatedAt.toEpochMilli()}_${token1.mint}"
        )
        val page2 = getAllTokens(continuation = page1.continuation, size = 1)
        assertThat(page2).isEqualTo(expected2)

        // Empty page
        val expected3 = TokensDto()
        val page3 = getAllTokens(continuation = page2.continuation, size = 1)
        assertThat(page3).isEqualTo(expected3)
    }

    @Test
    fun `find all tokens - with time filter`() = runBlocking<Unit> {
        val now = nowMillis()

        val token1 = saveToken(updatedAt = now)
        val token2 = saveToken(updatedAt = now.minusSeconds(10))
        val token3 = saveToken(updatedAt = now.minusSeconds(20))
        val token4 = saveToken(updatedAt = now.minusSeconds(30))

        val from = token4.updatedAt
        val to = token1.updatedAt

        // Should be sorted from newest to oldest

        //Page 1
        val expected1 = TokensDto(
            tokens = listOf(token2).map { TokenConverter.convert(it) },
            continuation = "${token2.updatedAt.toEpochMilli()}_${token2.mint}"
        )
        val page1 = getAllTokens(from, to, null, 1)
        assertThat(page1).isEqualTo(expected1)

        // Page 2
        val expected2 = TokensDto(
            tokens = listOf(token3).map { TokenConverter.convert(it) },
            continuation = null
        )
        val page2 = getAllTokens(from, to, page1.continuation, 2)
        assertThat(page2).isEqualTo(expected2)
    }

    @Test
    fun `get token royalties`() = runBlocking<Unit> {
        val token = tokenRepository.save(
            createRandomToken(
                tokenMeta = createRandomTokenMeta(
                    creators = listOf(
                        MetaplexTokenCreator("a", 40),
                        MetaplexTokenCreator("b", 60)
                    ),
                    sellerFeeBasisPoints = 1000
                )
            )
        )

        val result = tokenControllerApi.getTokenRoyaltiesByAddress(token.mint).awaitFirst().royalties

        val expected = listOf(
            RoyaltyDto("a", 400),
            RoyaltyDto("b", 600)
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `find tokens by collection V1`() = runBlocking<Unit> {

        val collection = createRandomTokenMetaCollectionOffChain()
        val token1 = saveToken(mint = "b", collection = collection)
        val token2 = saveToken(mint = "a", collection = collection)

        // Should not be found
        saveToken()

        // Should be sorted a -> b
        val expected = TokensDto(
            tokens = listOf(token2, token1).map { TokenConverter.convert(it) },
            continuation = null
        )

        val result = tokenControllerApi.getTokensByCollection(collection.hash, null, 50).awaitFirst()

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `find tokens by collection V2`() = runBlocking<Unit> {

        val collection = createRandomTokenMetaCollectionOnChain()
        val token1 = saveToken(mint = "b", collection = collection)
        val token2 = saveToken(mint = "a", collection = collection)

        // Should not be found
        saveToken()

        // Should be sorted a -> b
        val expected = TokensDto(
            tokens = listOf(token2, token1).map { TokenConverter.convert(it) },
            continuation = null
        )

        val result = tokenControllerApi.getTokensByCollection(collection.address, null, 50).awaitFirst()

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `load meta by address`() = runBlocking<Unit> {
        val mint = randomString()
        val tokenMeta = metaplexMetaRepository.save(createRandomMetaplexMeta(mint))
        val offChainMeta = createRandomMetaplexOffChainMeta()

        val expected = TokenMetaParser.mergeOnChainAndOffChainMeta(
            onChainMeta = tokenMeta.metaFields,
            offChainMeta = offChainMeta.metaFields
        )

        coEvery {
            testMetaplexOffChainMetaLoader.loadMetaplexOffChainMeta(
                tokenAddress = eq(mint),
                metaplexMetaFields = tokenMeta.metaFields
            )
        } returns offChainMeta

        assertThat(tokenControllerApi.getTokenMetaByAddress(mint).awaitFirst())
            .isEqualTo(TokenMetaConverter.convert(expected))
    }

    private suspend fun saveToken(
        mint: String = randomString(),
        updatedAt: Instant = nowMillis(),
        collection: TokenMeta.Collection? = null
    ): Token {
        return tokenRepository.save(
            createRandomToken(
                mint = mint,
                updatedAt = updatedAt,
                tokenMeta = createRandomTokenMeta(collection = collection ?: createRandomTokenMetaCollection())
            )
        )
    }

    private suspend fun getAllTokens(
        lastUpdatedFrom: Instant? = null,
        lastUpdatedTo: Instant? = null,
        continuation: String? = null,
        size: Int? = 50
    ): TokensDto {
        return tokenControllerApi.getAllTokens(
            false,
            lastUpdatedFrom?.toEpochMilli(),
            lastUpdatedTo?.toEpochMilli(),
            continuation,
            size
        ).awaitFirst()
    }

}
