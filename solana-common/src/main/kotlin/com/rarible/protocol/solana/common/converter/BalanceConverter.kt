package com.rarible.protocol.solana.common.converter

import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.dto.BalanceDto

object BalanceConverter {

    fun convert(balance: Balance): BalanceDto = BalanceDto(
        account = balance.account,
        value = balance.value,
        mint = balance.mint,
        owner = balance.owner,
        createdAt = balance.createdAt,
        updatedAt = balance.updatedAt,
        collection = balance.collection?.let { TokenMetaConverter.convert(it) }
    )
}
