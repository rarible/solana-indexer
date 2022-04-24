package com.rarible.protocol.solana.common.records

import com.rarible.blockchain.scanner.solana.model.SolanaLog

val EMPTY_SOLANA_LOG = SolanaLog(
    blockNumber = 0,
    blockHash = "",
    transactionIndex = 0,
    transactionHash = "",
    instructionIndex = 0,
    innerInstructionIndex = null
)