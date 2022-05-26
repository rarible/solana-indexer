package com.rarible.protocol.solana.common.model

import com.rarible.protocol.solana.common.meta.TokenMeta

data class TokenWithMeta(
    val token: Token,
    val tokenMeta: TokenMeta
)
