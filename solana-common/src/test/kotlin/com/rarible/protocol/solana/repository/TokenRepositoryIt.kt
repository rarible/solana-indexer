package com.rarible.protocol.solana.repository

import com.rarible.core.test.data.randomString
import com.rarible.protocol.solana.AbstractIntegrationTest
import com.rarible.protocol.solana.common.util.toBigInteger
import com.rarible.protocol.solana.test.createRandomToken
import com.rarible.protocol.solana.test.createRandomTokenMeta
import com.rarible.protocol.solana.test.createRandomTokenMetaCollectionOffChain
import com.rarible.protocol.solana.test.createRandomTokenMetaCollectionOnChain
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TokenRepositoryIt : AbstractIntegrationTest() {

    @Test
    fun `save and find by mint`() = runBlocking<Unit> {
        val token = createRandomToken()
        tokenRepository.save(token)
        assertThat(tokenRepository.findByMint(token.mint)).isEqualTo(token)
    }

    @Test
    fun `save and find by mints`() = runBlocking<Unit> {
        val tokens = (1..3).map { createRandomToken() }.sortedBy { it.id }
        tokens.asReversed().forEach { tokenRepository.save(it) }

        assertThat(tokenRepository.findByMints(tokens.map { it.id }).toList()).isEqualTo(tokens)
    }

    @Test
    fun `save with max ULong supply and find by mint`() = runBlocking<Unit> {
        val token = createRandomToken().copy(supply = ULong.MAX_VALUE.toBigInteger())
        tokenRepository.save(token)
        assertThat(tokenRepository.findByMint(token.mint)).isEqualTo(token)
    }

    @Test
    fun `save and find by V1 collection hash`() = runBlocking<Unit> {
        val collection = createRandomTokenMetaCollectionOffChain()
        val (token1Mint, token2Mint) = listOf(randomString(), randomString()).sorted()
        val token1 = createRandomToken(mint = token1Mint, tokenMeta = createRandomTokenMeta(collection = collection))
        val token2 = createRandomToken(mint = token2Mint, tokenMeta = createRandomTokenMeta(collection = collection))
        val token3 = createRandomToken()
        tokenRepository.save(token1)
        tokenRepository.save(token2)
        tokenRepository.save(token3)

        assertThat(
            tokenRepository.findByCollection(
                collection = collection.hash,
                continuation = null,
                limit = 1
            ).toList()
        ).isEqualTo(listOf(token1))

        assertThat(
            tokenRepository.findByCollection(
                collection = collection.id!!,
                continuation = token1.id,
                limit = 3
            ).toList()
        ).isEqualTo(listOf(token2))
    }

    @Test
    fun `save and find by V2 collection address`() = runBlocking<Unit> {
        val collection = createRandomTokenMetaCollectionOnChain()
        val (token1Mint, token2Mint) = listOf(randomString(), randomString()).sorted()
        val token1 = createRandomToken(mint = token1Mint, tokenMeta = createRandomTokenMeta(collection = collection))
        val token2 = createRandomToken(mint = token2Mint, tokenMeta = createRandomTokenMeta(collection = collection))
        val token3 = createRandomToken()
        tokenRepository.save(token1)
        tokenRepository.save(token2)
        tokenRepository.save(token3)

        assertThat(
            tokenRepository.findByCollection(
                collection = collection.address,
                continuation = null,
                limit = 1
            ).toList()
        ).isEqualTo(listOf(token1))

        assertThat(
            tokenRepository.findByCollection(
                collection = collection.id!!,
                continuation = token1.id,
                limit = 3
            ).toList()
        ).isEqualTo(listOf(token2))
    }

}
