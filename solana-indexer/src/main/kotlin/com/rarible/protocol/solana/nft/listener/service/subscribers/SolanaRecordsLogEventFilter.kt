package com.rarible.protocol.solana.nft.listener.service.subscribers

import com.rarible.blockchain.scanner.framework.data.LogEvent
import com.rarible.blockchain.scanner.solana.model.SolanaDescriptor
import com.rarible.blockchain.scanner.solana.model.SolanaLogRecord
import com.rarible.blockchain.scanner.solana.subscriber.SolanaLogEventFilter
import com.rarible.protocol.solana.common.configuration.SolanaIndexerProperties
import com.rarible.protocol.solana.common.filter.auctionHouse.SolanaAuctionHouseFilter
import com.rarible.protocol.solana.common.filter.token.SolanaTokenFilter
import com.rarible.protocol.solana.common.records.SolanaAuctionHouseOrderRecord
import com.rarible.protocol.solana.common.records.SolanaAuctionHouseRecord
import com.rarible.protocol.solana.common.records.SolanaBalanceRecord
import com.rarible.protocol.solana.common.records.SolanaBaseLogRecord
import com.rarible.protocol.solana.common.records.SolanaMetaRecord
import com.rarible.protocol.solana.common.records.SolanaTokenRecord
import com.rarible.protocol.solana.nft.listener.service.AccountToMintAssociationService
import com.rarible.protocol.solana.nft.listener.util.AccountGraph
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Interceptor (not really a filter) of Solana scanner's generated records, which is responsible for
 * - filtering out events of ignored mints (coins and blacklisted mints),
 * - setting up the "mint" field in log records that do not have it (where possible)
 * - and more
 */
@Component
class SolanaRecordsLogEventFilter(
    private val accountToMintAssociationService: AccountToMintAssociationService,
    private val solanaIndexerProperties: SolanaIndexerProperties,
    private val tokenFilter: SolanaTokenFilter,
    private val auctionHouseFilter: SolanaAuctionHouseFilter
) : SolanaLogEventFilter {

    private val logger = LoggerFactory.getLogger(SolanaRecordsLogEventFilter::class.java)

    override suspend fun filter(
        events: List<LogEvent<SolanaLogRecord, SolanaDescriptor>>,
    ): List<LogEvent<SolanaLogRecord, SolanaDescriptor>> {
        if (events.isEmpty()) {
            return emptyList()
        }

        val knownInMemoryAccountMappings = getKnownInMemoryAccountToMintMapping(events)

        // Retrieving mapping for ALL found non-currency accounts to know what we really need to update in DB
        // TODO here we can query only accounts without known mint if we sure all mapping from init records already in DB
        // TODO If we are not starting to index from the 1st block, we MUST query mints for all accounts here
        val nonCurrencyAccounts = filterCurrencyBalances(knownInMemoryAccountMappings)
        val existMapping = accountToMintAssociationService.getMintsByAccounts(nonCurrencyAccounts)

        // Set mint for groups - since reference of AccountGroup is the same for all mapped accounts,
        // it will be automatically referenced for each account of the group
        existMapping.forEach { knownInMemoryAccountMappings[it.key]!!.mint = it.value }

        // Now we have full mapping account->mint from events and DB
        val accountToMintMapping = HashMap<String, String>()
        knownInMemoryAccountMappings.forEach { group ->
            val account = group.key
            group.value.mint?.let { accountToMintMapping[account] = it }
        }

        return coroutineScope {
            // Saving new non-currency mappings in background while filtering event list
            val mappingToSave = HashMap(accountToMintMapping.filter {
                tokenFilter.isAcceptableToken(it.value)
            })
            val updateMappingDeferred = async {
                // Saving only non-existing mapping, including account references
                existMapping.keys.forEach { mappingToSave.remove(it) }
                accountToMintAssociationService.saveMintsByAccounts(mappingToSave)
            }

            // Adding mapping from cache/db
            accountToMintMapping.putAll(existMapping)
            val result = filter(events, accountToMintMapping)

            updateMappingDeferred.await()

            result
        }
    }

    private fun filter(
        events: List<LogEvent<SolanaLogRecord, SolanaDescriptor>>,
        accountToMints: Map<String, String>,
    ): List<LogEvent<SolanaLogRecord, SolanaDescriptor>> =
        events.map { event ->
            if (event.logRecordsToInsert.isEmpty()) {
                return@map event
            }
            val filteredRecords = event.logRecordsToInsert.mapNotNull {
                if (it is SolanaBaseLogRecord) {
                    filterRecord(it, accountToMints)
                } else {
                    // Keep non-target logs
                    it
                }
            }
            event.copy(logRecordsToInsert = filteredRecords)
        }

    private fun getKnownInMemoryAccountToMintMapping(
        events: List<LogEvent<SolanaLogRecord, SolanaDescriptor>>,
    ): Map<String, AccountGraph.AccountGroup> {
        val accountToMintMapping = HashMap<String, String>()
        val accounts = AccountGraph()

        events.asSequence().flatMap { it.logRecordsToInsert }.forEach { r ->
            @Suppress("UNUSED_VARIABLE")
            val exhaustiveWhen = when (val record = r as? SolanaBaseLogRecord) {
                is SolanaMetaRecord.MetaplexUpdateMetadataRecord -> {
                    accounts.addRib(record.metaAccount, record.metaAccount)
                }
                is SolanaMetaRecord.MetaplexCreateMetadataAccountRecord -> {
                    accounts.addRib(record.metaAccount, record.metaAccount)
                    accountToMintMapping[record.metaAccount] = record.mint
                }
                // In-memory account mapping
                is SolanaBalanceRecord.InitializeBalanceAccountRecord -> {
                    // Artificial reference for init-record
                    accounts.addRib(record.account, record.account)
                    accountToMintMapping[record.account] = record.mint
                }
                is SolanaBalanceRecord.TransferOutcomeRecord -> {
                    accounts.addRib(record.account, record.to)
                    record.mint.takeIf { it.isNotEmpty() }?.let { accountToMintMapping[record.account] = it }
                }
                is SolanaBalanceRecord.TransferIncomeRecord -> {
                    accounts.addRib(record.from, record.account)
                    record.mint.takeIf { it.isNotEmpty() }?.let { accountToMintMapping[record.account] = it }
                }
                is SolanaBalanceRecord.BurnRecord -> Unit
                is SolanaBalanceRecord.MintToRecord -> Unit
                is SolanaBalanceRecord.InternalBalanceUpdateRecord -> Unit
                is SolanaAuctionHouseOrderRecord -> when (record) {
                    is SolanaAuctionHouseOrderRecord.BuyRecord -> {
                        accounts.addRib(record.tokenAccount, record.tokenAccount)
                    }
                    is SolanaAuctionHouseOrderRecord.SellRecord -> {
                        accounts.addRib(record.tokenAccount, record.tokenAccount)
                    }
                    is SolanaAuctionHouseOrderRecord.CancelRecord -> Unit
                    is SolanaAuctionHouseOrderRecord.ExecuteSaleRecord -> Unit
                    is SolanaAuctionHouseOrderRecord.InternalOrderUpdateRecord -> Unit
                }
                is SolanaAuctionHouseRecord -> Unit
                is SolanaMetaRecord -> Unit
                is SolanaTokenRecord -> Unit
                null -> Unit
            }
        }

        val accountGroups = accounts.findGroups()

        // Set mints known from log events
        accountToMintMapping.forEach {
            accountGroups[it.key]!!.mint = it.value
        }

        return accountGroups
    }

    private fun filterRecord(
        record: SolanaBaseLogRecord,
        accountToMintMapping: Map<String, String>,
    ): SolanaBaseLogRecord? = when (record) {
        is SolanaBalanceRecord -> filterBalanceRecord(record, accountToMintMapping)
        is SolanaTokenRecord -> filterTokenRecord(record)
        is SolanaAuctionHouseRecord -> filterAuctionHouseRecord(record)
        is SolanaMetaRecord -> filterMetaRecord(record, accountToMintMapping)
        is SolanaAuctionHouseOrderRecord -> filterAuctionHouseOrderRecord(record, accountToMintMapping)
    }

    private fun filterTokenRecord(
        record: SolanaTokenRecord
    ): SolanaBaseLogRecord? {
        if (record is SolanaTokenRecord.InternalTokenUpdateRecord) {
            return null
        }
        return keepIfNft(record, record.mint)
    }

    private fun filterBalanceRecord(
        record: SolanaBalanceRecord,
        accountToMintMapping: Map<String, String>
    ) = when (record) {
        is SolanaBalanceRecord.MintToRecord -> keepIfNft(record, record.mint)
        is SolanaBalanceRecord.BurnRecord -> keepIfNft(record, record.mint)
        is SolanaBalanceRecord.TransferOutcomeRecord -> keepBalanceRecordIfNft(
            record = record,
            accountToMints = accountToMintMapping,
            updateMint = { record.copy(mint = it) }
        )
        is SolanaBalanceRecord.TransferIncomeRecord -> keepBalanceRecordIfNft(
            record = record,
            accountToMints = accountToMintMapping,
            updateMint = { record.copy(mint = it) }
        )
        is SolanaBalanceRecord.InitializeBalanceAccountRecord -> keepIfNft(record, record.mint)
        is SolanaBalanceRecord.InternalBalanceUpdateRecord -> null
    }

    private fun filterAuctionHouseRecord(
        record: SolanaAuctionHouseRecord
    ): SolanaBaseLogRecord? = record.takeIf { auctionHouseFilter.isAcceptableAuctionHouse(it.auctionHouse) }

    private fun filterMetaRecord(
        record: SolanaMetaRecord,
        accountToMintMapping: Map<String, String>
    ): SolanaBaseLogRecord? = when (record) {
        is SolanaMetaRecord.MetaplexCreateMetadataAccountRecord -> keepIfNft(record, record.mint)
        is SolanaMetaRecord.MetaplexUpdateMetadataRecord -> keepUpdateMetaIfNft(
            record = record,
            accountToMints = accountToMintMapping,
            updateMint = { record.copy(mint = it) }
        )

        // TODO: for these we can't determine 'mint' easily, so just keep them. Reducers will ignore them.
        is SolanaMetaRecord.MetaplexSignMetadataRecord -> record
        is SolanaMetaRecord.MetaplexUnVerifyCollectionRecord -> record
        is SolanaMetaRecord.MetaplexVerifyCollectionRecord -> record
        is SolanaMetaRecord.SetAndVerifyMetadataRecord -> record
    }

    private fun filterAuctionHouseOrderRecord(
        record: SolanaAuctionHouseOrderRecord,
        accountToMintMapping: Map<String, String>
    ): SolanaAuctionHouseOrderRecord? {
        if (!auctionHouseFilter.isAcceptableAuctionHouse(record.auctionHouse)) {
            return null
        }
        return when (record) {
            is SolanaAuctionHouseOrderRecord.ExecuteSaleRecord -> record
            is SolanaAuctionHouseOrderRecord.BuyRecord -> {
                if (record.mint.isNotEmpty()) {
                    record
                } else {
                    val mint = accountToMintMapping[record.tokenAccount]
                    if (mint != null) {
                        record.copy(mint = mint).withUpdatedOrderId()
                    } else {
                        val message = "Buy record is skipped: unknown mint by account ${record.tokenAccount}: $record"
                        if (solanaIndexerProperties.featureFlags.isIndexingFromBeginning) {
                            logger.error(message)
                        } else {
                            logger.info(message)
                        }
                        null
                    }
                }
            }
            is SolanaAuctionHouseOrderRecord.SellRecord -> {
                if (record.mint.isNotEmpty()) {
                    record
                } else {
                    val mint = accountToMintMapping[record.tokenAccount]
                    if (mint != null) {
                        record.copy(mint = mint).withUpdatedOrderId()
                    } else {
                        val message = "Sell record is skipped: unknown mint by account ${record.tokenAccount}: $record"
                        if (solanaIndexerProperties.featureFlags.isIndexingFromBeginning) {
                            logger.error(message)
                        } else {
                            logger.info(message)
                        }
                        null
                    }
                }
            }
            is SolanaAuctionHouseOrderRecord.CancelRecord -> record
            // Internal records, should not be produced by subscribers
            is SolanaAuctionHouseOrderRecord.InternalOrderUpdateRecord -> null
        }
    }

    private fun keepIfNft(record: SolanaBaseLogRecord, mint: String): SolanaBaseLogRecord? {
        return if (tokenFilter.isAcceptableToken(mint)) {
            record
        } else {
            null
        }
    }

    private fun keepUpdateMetaIfNft(
        record: SolanaMetaRecord.MetaplexUpdateMetadataRecord,
        accountToMints: Map<String, String>,
        updateMint: (String) -> SolanaMetaRecord.MetaplexUpdateMetadataRecord
    ): SolanaBaseLogRecord? {
        val knownMint = record.mint.takeIf { it.isNotEmpty() }
        val mint = knownMint ?: accountToMints[record.metaAccount]
        // Skip records with unknown mint. We must have seen the account<->mint association before.
        ?: return null

        if (!tokenFilter.isAcceptableToken(mint)) {
            return null
        }

        return if (knownMint == null) {
            updateMint(mint)
        } else {
            record
        }
    }

    private fun keepBalanceRecordIfNft(
        record: SolanaBalanceRecord,
        accountToMints: Map<String, String>,
        updateMint: (String) -> SolanaBalanceRecord,
    ): SolanaBalanceRecord? {
        val knownMint = record.mint.takeIf { it.isNotEmpty() }
        val mint = knownMint ?: accountToMints[record.account]
        // Skip records with unknown mint. We must have seen the account<->mint association before.
        ?: return null

        if (!tokenFilter.isAcceptableToken(mint)) {
            return null
        }

        return if (knownMint == null) {
            updateMint(mint)
        } else {
            record
        }
    }

    private fun filterCurrencyBalances(groups: Map<String, AccountGraph.AccountGroup>): Set<String> {
        return groups.mapNotNull {
            val account = it.key
            val mint = it.value.mint
            if (mint == null || tokenFilter.isAcceptableToken(mint)) {
                account
            } else {
                null
            }
        }.toMutableSet()
    }

}
