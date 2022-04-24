package com.rarible.protocol.solana.common.converter

import com.rarible.protocol.solana.common.meta.TokenMeta
import com.rarible.protocol.solana.dto.TokenMetaEventDto
import com.rarible.protocol.solana.dto.TokenMetaTriggerEventDto
import com.rarible.protocol.solana.dto.TokenMetaUpdateEventDto

object TokenMetaEventConverter {
    fun convertUpdateEvent(
        tokenAddress: String,
        tokenMeta: TokenMeta
    ): TokenMetaEventDto = TokenMetaUpdateEventDto(
        tokenAddress = tokenAddress,
        tokenMeta = TokenMetaConverter.convert(tokenMeta)
    )

    fun convertTriggerEvent(
        tokenAddress: String
    ): TokenMetaTriggerEventDto = TokenMetaTriggerEventDto(
        tokenAddress = tokenAddress
    )

}
