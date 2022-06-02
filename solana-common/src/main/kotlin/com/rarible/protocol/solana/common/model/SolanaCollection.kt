package com.rarible.protocol.solana.common.model

import org.springframework.data.annotation.Id

sealed class SolanaCollection {
    abstract val id: String // hash for V1, token address for V2

    companion object {
        const val COLLECTION = "collection"
    }
}

data class SolanaCollectionV1(
    @Id
    override val id: String,
    val name: String,
    val family: String?
) : SolanaCollection()

data class SolanaCollectionV2(
    @Id
    override val id: String
) : SolanaCollection()