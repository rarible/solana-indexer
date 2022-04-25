package com.rarible.protocol.solana.nft.api.controller

import com.rarible.protocol.solana.api.controller.BalanceControllerApi
import com.rarible.protocol.solana.common.continuation.BalanceContinuation
import com.rarible.protocol.solana.common.continuation.DateIdContinuation
import com.rarible.protocol.solana.common.continuation.Paging
import com.rarible.protocol.solana.common.converter.BalanceConverter
import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.dto.BalanceDto
import com.rarible.protocol.solana.dto.BalancesDto
import com.rarible.protocol.solana.nft.api.service.BalanceApiService
import com.rarible.protocol.union.dto.continuation.page.PageSize
import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class BalanceController(
    private val balanceApiService: BalanceApiService,
) : BalanceControllerApi {

    override suspend fun getBalanceByMintAndOwner(mint: String, owner: String): ResponseEntity<BalanceDto> {
        val balance = balanceApiService.getBalanceByMintAndOwner(mint, owner)
        return ResponseEntity.ok(BalanceConverter.convert(balance))
    }

    override suspend fun getBalanceByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<BalancesDto> {
        val safeSize = PageSize.BALANCE.limit(size)
        val balances = balanceApiService.getBalancesByOwner(
            owner,
            DateIdContinuation.parse(continuation),
            safeSize
        ).toList()
        val dto = toSlice(balances, safeSize)
        return ResponseEntity.ok(dto)
    }

    override suspend fun getBalanceByMint(
        mint: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<BalancesDto> {
        val safeSize = PageSize.BALANCE.limit(size)
        val balances = balanceApiService.getBalancesByMint(
            mint,
            DateIdContinuation.parse(continuation),
            safeSize
        ).toList()
        val dto = toSlice(balances, safeSize)
        return ResponseEntity.ok(dto)
    }

    private fun toSlice(balances: List<Balance>, size: Int): BalancesDto {
        val dto = balances.map { BalanceConverter.convert(it) }
        val continuationFactory = BalanceContinuation.ByLastUpdatedAndId

        val slice = Paging(continuationFactory, dto).getSlice(size)
        return BalancesDto(slice.entities, slice.continuation)
    }

}
