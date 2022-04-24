package com.rarible.protocol.solana.common.records

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

// Fake log-record instructions for internal updates, caused by indexer logic, not blockchain events
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "BALANCE_META_UPDATED", value = SolanaBalanceUpdateInstruction.BalanceMetaUpdated::class)
)
sealed class SolanaBalanceUpdateInstruction {

    data class BalanceMetaUpdated(
        val account: String
    ) : SolanaBalanceUpdateInstruction()

}
