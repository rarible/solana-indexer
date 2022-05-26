package com.rarible.protocol.solana.nft.api.service

import com.rarible.protocol.solana.common.continuation.DateIdContinuation
import com.rarible.protocol.solana.common.meta.TokenMetaService
import com.rarible.protocol.solana.common.model.Token
import com.rarible.protocol.solana.common.model.TokenWithMeta
import com.rarible.protocol.solana.common.repository.TokenRepository
import com.rarible.protocol.solana.common.util.RoyaltyDistributor
import com.rarible.protocol.solana.dto.RoyaltyDto
import com.rarible.protocol.solana.nft.api.exceptions.EntityNotFoundApiException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TokenApiService(
    private val tokenRepository: TokenRepository,
    private val tokenMetaService: TokenMetaService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

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
        return extendWithAvailableMeta(tokens).take(limit)
    }

    suspend fun getTokensWithMeta(tokenAddresses: List<String>): Flow<TokenWithMeta> {
        val tokens = tokenRepository.findByMints(tokenAddresses)

        return extendWithAvailableMeta(tokens)
    }

    private suspend fun extendWithAvailableMeta(tokens: Flow<Token>): Flow<TokenWithMeta> {
        return tokens.mapNotNull { token ->
            val tokenMeta = tokenMetaService.getAvailableTokenMeta(token.mint)
            if (tokenMeta == null) {
                logger.info("Token ${token.mint} meta is not loaded yet, so skipping the token")
                return@mapNotNull null
            }
            TokenWithMeta(token, tokenMeta)
        }
    }

    suspend fun getTokenWithMeta(tokenAddress: String): TokenWithMeta {
        val token = tokenRepository.findByMint(tokenAddress)
            ?: throw EntityNotFoundApiException("Token", tokenAddress)
        val availableMeta = tokenMetaService.getAvailableTokenMeta(tokenAddress)
        if (availableMeta == null) {
            logger.info("Token $tokenAddress meta is not loaded yet, so returning 404")
            throw EntityNotFoundApiException("Token meta", tokenAddress)
        }
        return TokenWithMeta(token, availableMeta)
    }

    suspend fun getTokenRoyalties(tokenAddress: String): List<RoyaltyDto> {
        val tokenMeta = tokenMetaService.getAvailableTokenMeta(tokenAddress)
            ?: throw EntityNotFoundApiException("Token meta", tokenAddress)

        val creators = tokenMeta.creators.associateBy({ it.address }, { it.share })

        return RoyaltyDistributor.distribute(
            sellerFeeBasisPoints = tokenMeta.sellerFeeBasisPoints,
            creators = creators
        ).map { RoyaltyDto(it.key, it.value) }
    }

    suspend fun getTokensWithMetaByCollection(
        collection: String,
        continuation: String?,
        limit: Int,
    ): Flow<TokenWithMeta> {
        val metas = tokenMetaService.getTokensMetaByCollection(collection, continuation, limit)
        val tokens = tokenRepository.findByMints(metas.keys)

        return tokens.mapNotNull {
            val tokenMeta = metas[it.mint] ?: return@mapNotNull null
            TokenWithMeta(it, tokenMeta)
        }
    }
}
