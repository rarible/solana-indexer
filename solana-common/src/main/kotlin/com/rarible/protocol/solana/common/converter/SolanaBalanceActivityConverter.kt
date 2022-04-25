package com.rarible.protocol.solana.common.converter

import com.rarible.protocol.solana.common.configuration.SolanaIndexerProperties
import com.rarible.protocol.solana.common.records.SolanaBalanceRecord
import com.rarible.protocol.solana.common.repository.BalanceRepository
import com.rarible.protocol.solana.common.repository.SolanaBalanceRecordsRepository
import com.rarible.protocol.solana.dto.ActivityDto
import com.rarible.protocol.solana.dto.BurnActivityDto
import com.rarible.protocol.solana.dto.MintActivityDto
import com.rarible.protocol.solana.dto.TransferActivityDto
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component

@Component
class SolanaBalanceActivityConverter(
    private val balanceRepository: BalanceRepository,
    private val balanceRecordsRepository: SolanaBalanceRecordsRepository,
    private val solanaIndexerProperties: SolanaIndexerProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(source: SolanaBalanceRecord, reverted: Boolean): ActivityDto? {
        return when (source) {
            is SolanaBalanceRecord.MintToRecord -> createMintActivity(source, reverted)
            is SolanaBalanceRecord.BurnRecord -> createBurnActivity(source, reverted)
            is SolanaBalanceRecord.TransferIncomeRecord -> createTransferIncomeActivity(source, reverted)
            is SolanaBalanceRecord.TransferOutcomeRecord -> null
            is SolanaBalanceRecord.InitializeBalanceAccountRecord -> null
            is SolanaBalanceRecord.InternalBalanceUpdateRecord -> null
        }
    }

    private suspend fun createMintActivity(
        record: SolanaBalanceRecord.MintToRecord,
        reverted: Boolean,
    ): ActivityDto? {
        val owner = findOwner(record.account) ?: return null
        return MintActivityDto(
            id = record.id,
            date = record.timestamp,
            owner = owner,
            tokenAddress = record.mint,
            value = record.mintAmount,
            blockchainInfo = SolanaLogToActivityBlockchainInfoConverter.convert(record.log),
            reverted = reverted
        )
    }

    private suspend fun createBurnActivity(
        record: SolanaBalanceRecord.BurnRecord,
        reverted: Boolean,
    ): ActivityDto? {
        val owner = findOwner(record.account) ?: return null
        return BurnActivityDto(
            id = record.id,
            date = record.timestamp,
            owner = owner,
            tokenAddress = record.mint,
            value = record.burnAmount,
            blockchainInfo = SolanaLogToActivityBlockchainInfoConverter.convert(record.log),
            reverted = reverted
        )
    }

    private suspend fun createTransferIncomeActivity(
        record: SolanaBalanceRecord.TransferIncomeRecord,
        reverted: Boolean,
    ): TransferActivityDto? {
        @Suppress("DuplicatedCode")
        val fromOwner = findOwner(record.from) ?: return null
        val toOwner = findOwner(record.account) ?: return null
        return TransferActivityDto(
            id = record.id,
            date = record.timestamp,
            from = fromOwner,
            owner = toOwner,
            tokenAddress = record.mint,
            value = record.incomeAmount,
            blockchainInfo = SolanaLogToActivityBlockchainInfoConverter.convert(record.log),
            reverted = reverted,
            purchase = false // TODO should be evaluated
            // Purchase = true/false should be evaluated in next way (at least, it is done in such way at ETH):
            // If item transferred to owner from contract, it means item has been purchased, if "to" is not a
            // contract but regular balance, this transfer is not a purchase.

            // To determine is transfer related to a contract we did next things at ETH:
            // 1. We started to write field "to" (from - prev owner, owner - end owner, to - exchange contract)
            // 2. We introduced configuration field "exchage contract addresses" contains most popular exchange contracts
            // 3. If 'to' contains value from this contract set - it means this transfer is 'purchase'
        )
    }

    private suspend fun createTransferOutcomeActivity(
        record: SolanaBalanceRecord.TransferOutcomeRecord,
        reverted: Boolean,
    ): TransferActivityDto? {
        val toOwner = findOwner(record.to) ?: return null
        val fromOwner = findOwner(record.account) ?: return null
        return TransferActivityDto(
            id = record.id,
            date = record.timestamp,
            from = fromOwner,
            owner = toOwner,
            tokenAddress = record.mint,
            value = record.outcomeAmount,
            blockchainInfo = SolanaLogToActivityBlockchainInfoConverter.convert(record.log),
            reverted = reverted,
            purchase = false // TODO should be evaluated
        )
    }

    private suspend fun findOwner(account: String): String? {
        val owner = balanceRepository.findByAccount(account)?.owner
        if (owner == null) {
            logger.info("Cannot determine 'owner' of balance by account $account")
            /**
             * This workaround is necessary until we properly fix https://rarible.atlassian.net/browse/CHARLIE-221.
             * It may happen that during processing of transfer event either "from" or "to" balance is not reduced yet.
             */
            val criteria = Criteria.where(SolanaBalanceRecord::account.name).`is`(account)
            val initializeBalanceAccountRecord = balanceRecordsRepository.findBy(
                criteria = criteria,
                sort = Sort.by(Sort.Direction.ASC, "_id"),
                size = 1
            ).filterIsInstance<SolanaBalanceRecord.InitializeBalanceAccountRecord>().firstOrNull()
            if (initializeBalanceAccountRecord == null) {
                val message = "Cannot determine 'owner' by account $account"
                if (solanaIndexerProperties.featureFlags.isIndexingFromBeginning) {
                    logger.error(message)
                } else {
                    logger.info(message)
                }
            } else {
                logger.info("Successfully determined owner of account $account to be ${initializeBalanceAccountRecord.owner}")
            }
            return initializeBalanceAccountRecord?.owner
        }
        return owner
    }

}