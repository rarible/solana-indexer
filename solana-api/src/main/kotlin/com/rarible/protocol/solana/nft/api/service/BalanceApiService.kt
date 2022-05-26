package com.rarible.protocol.solana.nft.api.service

import com.rarible.protocol.solana.common.continuation.DateIdContinuation
import com.rarible.protocol.solana.common.meta.TokenMetaService
import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.common.model.BalanceWithMeta
import com.rarible.protocol.solana.common.repository.BalanceRepository
import com.rarible.protocol.solana.nft.api.exceptions.EntityNotFoundApiException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        return extendWithAvailableMeta(balances.first())
    }

    suspend fun getBalancesWithMetaByMintAndOwner(
        mint: String,
        owner: String
    ): Flow<BalanceWithMeta> {
        return balanceRepository.findByMintAndOwner(mint, owner, false)
            .map { extendWithAvailableMeta(it) }
    }

    fun getBalanceWithMetaByOwner(
        owner: String,
        continuation: DateIdContinuation?
    ): Flow<BalanceWithMeta> =
        balanceRepository.findByOwner(
            owner = owner,
            continuation = continuation,
            includeDeleted = false
        ).map { extendWithAvailableMeta(it) }

    fun getBalanceWithMetaByMint(
        mint: String,
        continuation: DateIdContinuation?
    ): Flow<BalanceWithMeta> =
        balanceRepository.findByMint(
            mint = mint,
            continuation = continuation,
            includeDeleted = false
        ).map { extendWithAvailableMeta(it) }

    private suspend fun extendWithAvailableMeta(balance: Balance): BalanceWithMeta {
        val tokenMeta = tokenMetaService.getAvailableTokenMeta(balance.mint)
        return BalanceWithMeta(balance, tokenMeta)
    }
}
