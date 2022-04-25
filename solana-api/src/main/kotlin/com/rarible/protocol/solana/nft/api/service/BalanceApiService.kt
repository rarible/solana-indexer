package com.rarible.protocol.solana.nft.api.service

import com.rarible.protocol.solana.common.continuation.DateIdContinuation
import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.common.repository.BalanceRepository
import com.rarible.protocol.solana.nft.api.exceptions.EntityNotFoundApiException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BalanceApiService(
    private val balanceRepository: BalanceRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getBalanceByMintAndOwner(
        mint: String,
        owner: String
    ): Balance {
        val balances = balanceRepository.findByMintAndOwner(mint, owner, false).toList()
        if (balances.isEmpty()) throw EntityNotFoundApiException("Balance", "$mint:$owner")
        if (balances.size > 1) {
            logger.warn("Several balances ({}) found for pair mint {} and owner {}", balances.size, mint, owner)
        }
        return balances.first()
    }

    fun getBalancesByOwner(
        owner: String,
        continuation: DateIdContinuation?,
        limit: Int
    ): Flow<Balance> =
        balanceRepository.findByOwner(
            owner = owner,
            continuation = continuation,
            limit = limit,
            includeDeleted = false
        )

    fun getBalancesByMint(
        mint: String,
        continuation: DateIdContinuation?,
        limit: Int
    ): Flow<Balance> =
        balanceRepository.findByMint(
            mint = mint,
            continuation = continuation,
            limit = limit,
            includeDeleted = false
        )
}
