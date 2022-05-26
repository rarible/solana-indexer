package com.rarible.protocol.solana.common.model

import com.rarible.protocol.solana.common.meta.TokenMeta

data class BalanceWithMeta(
    val balance: Balance,
    val tokenMeta: TokenMeta
)