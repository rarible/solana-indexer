package com.rarible.protocol.solana.nft.api.service

import com.rarible.protocol.solana.common.continuation.DateIdContinuation
import com.rarible.protocol.solana.common.model.Token
import com.rarible.protocol.solana.common.repository.TokenRepository
import com.rarible.protocol.solana.common.util.RoyaltyDistributor
import com.rarible.protocol.solana.dto.RoyaltyDto
import com.rarible.protocol.solana.nft.api.exceptions.EntityNotFoundApiException
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TokenApiService(
    private val tokenRepository: TokenRepository
) {

    suspend fun findAll(
        lastUpdatedFrom: Instant?,
        lastUpdatedTo: Instant?,
        continuation: DateIdContinuation?, limit: Int
    ): Flow<Token> = tokenRepository.findAll(
        lastUpdatedFrom = lastUpdatedFrom,
        lastUpdatedTo = lastUpdatedTo,
        continuation = continuation,
        limit = limit
    )

    suspend fun getTokens(mints: List<String>): Flow<Token> =
        tokenRepository.findByMints(mints)

    suspend fun getToken(tokenAddress: String): Token = tokenRepository.findByMint(tokenAddress)
        ?: throw EntityNotFoundApiException("Token", tokenAddress)

    suspend fun getTokenRoyalties(tokenAddress: String): List<RoyaltyDto> {
        val tokenMeta = getToken(tokenAddress).tokenMeta ?: throw EntityNotFoundApiException("Token", tokenAddress)
        val creators = tokenMeta.creators.associateBy({ it.address }, { it.share })

        return RoyaltyDistributor.distribute(
            sellerFeeBasisPoints = tokenMeta.sellerFeeBasisPoints,
            creators = creators
        ).map { RoyaltyDto(it.key, it.value) }
    }

    suspend fun getTokensByCollection(
        collection: String,
        continuation: String?,
        limit: Int,
    ): Flow<Token> = tokenRepository.findByCollection(
        collection = collection,
        continuation = continuation,
        limit = limit
    )
}
