package com.rarible.protocol.solana.nft.api.service

import com.rarible.protocol.solana.common.continuation.DateIdContinuation
import com.rarible.protocol.solana.common.meta.TokenMetaService
import com.rarible.protocol.solana.common.model.TokenWithMeta
import com.rarible.protocol.solana.common.repository.TokenRepository
import com.rarible.protocol.solana.common.util.RoyaltyDistributor
import com.rarible.protocol.solana.dto.RoyaltyDto
import com.rarible.protocol.solana.nft.api.exceptions.EntityNotFoundApiException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TokenApiService(
    private val tokenRepository: TokenRepository,
    private val tokenMetaService: TokenMetaService
) {

    suspend fun findAll(
        lastUpdatedFrom: Instant?,
        lastUpdatedTo: Instant?,
        continuation: DateIdContinuation?,
        limit: Int
    ): Flow<TokenWithMeta> {
        val tokens = tokenRepository.findAll(
            lastUpdatedFrom,
            lastUpdatedTo,
            continuation
        )
        return tokenMetaService.extendWithAvailableMeta(tokens).take(limit)
    }

    suspend fun getTokens(tokenAddresses: List<String>): Flow<TokenWithMeta> {
        val tokens = tokenRepository.findByMints(tokenAddresses)

        return tokenMetaService.extendWithAvailableMeta(tokens)
    }

    suspend fun getToken(tokenAddress: String): TokenWithMeta {
        val token = tokenRepository.findByMint(tokenAddress)
            ?: throw EntityNotFoundApiException("Token", tokenAddress)
        val tokenWithMeta = tokenMetaService.extendWithAvailableMeta(token).takeIf { it.hasMeta }
        return tokenWithMeta ?: throw EntityNotFoundApiException("Meta", tokenAddress)
    }

    suspend fun getTokenRoyalties(tokenAddress: String): List<RoyaltyDto> {
        val tokenMeta = tokenMetaService.getAvailableTokenMeta(tokenAddress) ?: return emptyList()
        val creators = tokenMeta.creators.associateBy({ it.address }, { it.share })

        return RoyaltyDistributor.distribute(
            tokenMeta.sellerFeeBasisPoints,
            creators
        ).map { RoyaltyDto(it.key, it.value) }
    }

    suspend fun getTokensByCollection(
        collection: String,
        continuation: String?,
        limit: Int,
    ): Flow<TokenWithMeta> {
        val metas = tokenMetaService.getTokensMetaByCollection(collection, continuation, limit)
        val tokens = tokenRepository.findByMints(metas.keys)

        return tokens.map { TokenWithMeta(it, metas[it.mint]) }
    }
}
