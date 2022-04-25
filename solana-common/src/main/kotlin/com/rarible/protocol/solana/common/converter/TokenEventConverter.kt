package com.rarible.protocol.solana.common.converter

import com.rarible.protocol.solana.common.model.Token
import com.rarible.protocol.solana.dto.TokenEventDto
import com.rarible.protocol.solana.dto.TokenUpdateEventDto
import java.util.*

object TokenEventConverter {
    fun convert(token: Token): TokenEventDto {
        val eventId = UUID.randomUUID().toString()
        return TokenUpdateEventDto(
            eventId = eventId,
            address = token.id,
            token = TokenConverter.convert(token)
        )
    }
}
