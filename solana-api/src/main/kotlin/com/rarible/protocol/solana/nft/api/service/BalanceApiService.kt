package com.rarible.protocol.solana.nft.api.service

import com.rarible.protocol.solana.common.continuation.DateIdContinuation
import com.rarible.protocol.solana.common.meta.TokenMetaService
import com.rarible.protocol.solana.common.model.BalanceWithMeta
import com.rarible.protocol.solana.common.repository.BalanceRepository
import com.rarible.protocol.solana.nft.api.exceptions.EntityNotFoundApiException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BalanceApiService(
    private val balanceRepository: BalanceRepository,
    private val tokenMetaService: TokenMetaService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getBalanceWithMetaByMintAndOwner(mint: String, owner: String): BalanceWithMeta {
        val balances = balanceRepository.findByMintAndOwner(mint, owner, false).toList()
        if (balances.isEmpty()) throw EntityNotFoundApiException("Balance", "$mint:$owner")
        if (balances.size > 1) {
            logger.warn("Several balances ({}) found for pair mint {} and owner {}", balances.size, mint, owner)
        }
        return tokenMetaService.extendWithAvailableMeta(balances.first())
    }

    suspend fun getBalanceWithMetaByOwner(
        owner: String,
        continuation: DateIdContinuation?,
        limit: Int,
    ): Flow<BalanceWithMeta> {
        val balanceFlow = balanceRepository.findByOwner(
            owner,
            continuation,
            limit,
            false
        )
        return tokenMetaService.extendBalancesWithAvailableMeta(balanceFlow)
    }

    suspend fun getBalanceWithMetaByMint(
        mint: String,
        continuation: DateIdContinuation?,
        limit: Int,
    ): Flow<BalanceWithMeta> {
        val balanceFlow = balanceRepository.findByMint(
            mint,
            continuation,
            limit,
            false
        )
        return tokenMetaService.extendBalancesWithAvailableMeta(balanceFlow)
    }
}
