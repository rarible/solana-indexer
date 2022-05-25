package com.rarible.protocol.solana.nft.api.controller

import com.rarible.protocol.solana.api.controller.BalanceControllerApi
import com.rarible.protocol.solana.common.continuation.BalanceContinuation
import com.rarible.protocol.solana.common.continuation.DateIdContinuation
import com.rarible.protocol.solana.common.continuation.Paging
import com.rarible.protocol.solana.common.converter.BalanceWithMetaConverter
import com.rarible.protocol.solana.common.model.BalanceWithMeta
import com.rarible.protocol.solana.dto.BalanceDto
import com.rarible.protocol.solana.dto.BalancesDto
import com.rarible.protocol.solana.nft.api.service.BalanceApiService
import com.rarible.protocol.union.dto.continuation.page.PageSize
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class BalanceController(
    private val balanceApiService: BalanceApiService,
) : BalanceControllerApi {

    override suspend fun getBalanceByMintAndOwner(mint: String, owner: String): ResponseEntity<BalanceDto> {
        val balanceWithMeta = balanceApiService.getBalanceWithMetaByMintAndOwner(mint, owner)
        return ResponseEntity.ok(BalanceWithMetaConverter.convert(balanceWithMeta))
    }

    override suspend fun getBalanceByOwner(
        owner: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<BalancesDto> {
        val safeSize = PageSize.BALANCE.limit(size)
        val balancesWithMeta = balanceApiService.getBalanceWithMetaByOwner(
            owner,
            DateIdContinuation.parse(continuation)
        ).toList()

        val dto = toSlice(balancesWithMeta, safeSize)
        return ResponseEntity.ok(dto)
    }

    override suspend fun getBalancesByMintAndOwner(
        mint: String,
        owner: String
    ): ResponseEntity<BalancesDto> {
        val balancesWithMeta = balanceApiService.getBalancesWithMetaByMintAndOwner(
            mint = mint,
            owner = owner
        ).map { BalanceWithMetaConverter.convert(it) }.toList()
        return ResponseEntity.ok(BalancesDto(balancesWithMeta, null))
    }

    override suspend fun getBalanceByMint(
        mint: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<BalancesDto> {
        val safeSize = PageSize.BALANCE.limit(size)
        val balancesWithMeta = balanceApiService.getBalanceWithMetaByMint(
            mint = mint,
            continuation = DateIdContinuation.parse(continuation)
        ).take(safeSize).toList()

        val dto = toSlice(balancesWithMeta, safeSize)
        return ResponseEntity.ok(dto)
    }

    private fun toSlice(balances: List<BalanceWithMeta>, size: Int): BalancesDto {
        val dto = balances.map { BalanceWithMetaConverter.convert(it) }
        val continuationFactory = BalanceContinuation.ByLastUpdatedAndId

        val slice = Paging(continuationFactory, dto).getSlice(size)
        return BalancesDto(slice.entities, slice.continuation)
    }

}
