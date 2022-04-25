package com.rarible.protocol.solana.common.converter

import com.rarible.protocol.solana.common.model.Token
import com.rarible.protocol.solana.dto.TokenDto

object TokenConverter {
    fun convert(token: Token): TokenDto = TokenDto(
        address = token.id,
        supply = token.supply,
        createdAt = token.createdAt,
        updatedAt = token.updatedAt,
        closed = false,
        collection = token.tokenMeta?.collection?.let { TokenMetaConverter.convert(it) },
        creators = token.tokenMeta?.creators?.map { TokenMetaConverter.convert(it) },
        decimals = token.decimals
    )
}
