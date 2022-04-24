package com.rarible.protocol.solana.common.records

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

// Fake log-record instructions for internal updates, caused by indexer logic, not blockchain events
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "TOKEN_META_UPDATED", value = SolanaTokenUpdateInstruction.TokenMetaUpdated::class)
)
sealed class SolanaTokenUpdateInstruction {

    data class TokenMetaUpdated(
        val mint: String
    ) : SolanaTokenUpdateInstruction()

}
