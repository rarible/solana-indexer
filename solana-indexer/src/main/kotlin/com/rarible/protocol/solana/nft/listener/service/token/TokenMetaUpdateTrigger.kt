package com.rarible.protocol.solana.nft.listener.service.token

import com.rarible.core.common.nowMillis
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.daemon.sequential.ConsumerBatchWorker
import com.rarible.protocol.solana.common.meta.TokenMeta
import com.rarible.protocol.solana.common.model.TokenId
import com.rarible.protocol.solana.common.records.SolanaBalanceRecord
import com.rarible.protocol.solana.common.records.SolanaBalanceUpdateInstruction
import com.rarible.protocol.solana.common.records.SolanaTokenRecord
import com.rarible.protocol.solana.common.records.SolanaTokenUpdateInstruction
import com.rarible.protocol.solana.common.repository.BalanceRepository
import com.rarible.protocol.solana.common.repository.TokenRepository
import com.rarible.protocol.solana.dto.TokenMetaEventDto
import com.rarible.protocol.solana.dto.TokenMetaTriggerEventDto
import com.rarible.protocol.solana.dto.TokenMetaUpdateEventDto
import com.rarible.protocol.solana.nft.listener.update.InternalUpdateEventService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Service that sends internal update events for token and balances when the corresponding meta is fully loaded.
 */
@Component
class TokenMetaUpdateTrigger(
    private val tokenRepository: TokenRepository,
    private val balanceRepository: BalanceRepository,
    private val internalUpdateEventService: InternalUpdateEventService
): ConsumerBatchEventHandler<TokenMetaEventDto> {

    private val logger = LoggerFactory.getLogger(TokenMetaUpdateTrigger::class.java)

    override suspend fun handle(event: List<TokenMetaEventDto>) {
        for (metaEventDto in event) {
            when (metaEventDto) {
                is TokenMetaTriggerEventDto -> Unit
                is TokenMetaUpdateEventDto -> triggerMetaUpdateForTokenAndBalances(metaEventDto.tokenAddress)
            }
        }
    }

    private suspend fun triggerMetaUpdateForTokenAndBalances(mint: TokenId) {
        logger.info("Meta updated for token $mint")
        val token = tokenRepository.findByMint(mint)
        if (token != null) {
            internalUpdateEventService.sendInternalTokenUpdateRecords(
                listOf(
                    SolanaTokenRecord.InternalTokenUpdateRecord(
                        mint = mint,
                        timestamp = nowMillis(),
                        instruction = SolanaTokenUpdateInstruction.TokenMetaUpdated(mint)
                    )
                )
            )
        }

        balanceRepository.findByMint(
            mint = mint,
            continuation = null,
            limit = Int.MAX_VALUE,
            includeDeleted = false
        ).map { balance ->
            SolanaBalanceRecord.InternalBalanceUpdateRecord(
                account = balance.account,
                mint = balance.mint,
                timestamp = nowMillis(),
                instruction = SolanaBalanceUpdateInstruction.BalanceMetaUpdated(
                    account = balance.account
                )
            )
        }.toList().let { internalUpdateEventService.sendInternalBalanceUpdateRecords(it) }
    }

}