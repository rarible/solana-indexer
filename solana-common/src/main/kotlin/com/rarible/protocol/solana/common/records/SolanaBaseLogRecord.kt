package com.rarible.protocol.solana.common.records

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.blockchain.scanner.solana.model.SolanaLogRecord
import java.time.Instant

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    Type(name = "AUCTION_HOUSE_ORDER_BUY", value = SolanaAuctionHouseOrderRecord.BuyRecord::class),
    Type(name = "AUCTION_HOUSE_ORDER_CANCEL", value = SolanaAuctionHouseOrderRecord.CancelRecord::class),
    Type(name = "AUCTION_HOUSE_ORDER_EXECUTE_SALE", value = SolanaAuctionHouseOrderRecord.ExecuteSaleRecord::class),
    Type(name = "AUCTION_HOUSE_ORDER_SELL", value = SolanaAuctionHouseOrderRecord.SellRecord::class),
    Type(
        name = "AUCTION_HOUSE_ORDER_INTERNAL_UPDATE",
        value = SolanaAuctionHouseOrderRecord.InternalOrderUpdateRecord::class
    ),
    Type(name = "AUCTION_HOUSE_CREATE", value = SolanaAuctionHouseRecord.CreateAuctionHouseRecord::class),
    Type(name = "AUCTION_HOUSE_UPDATE", value = SolanaAuctionHouseRecord.UpdateAuctionHouseRecord::class),
    Type(name = "BALANCE_BURN", value = SolanaBalanceRecord.BurnRecord::class),
    Type(name = "BALANCE_INITIALIZE_ACCOUNT", value = SolanaBalanceRecord.InitializeBalanceAccountRecord::class),
    Type(name = "BALANCE_MINT_TO", value = SolanaBalanceRecord.MintToRecord::class),
    Type(name = "BALANCE_TRANSFER_INCOME", value = SolanaBalanceRecord.TransferIncomeRecord::class),
    Type(name = "BALANCE_TRANSFER_OUTCOME", value = SolanaBalanceRecord.TransferOutcomeRecord::class),
    Type(name = "BALANCE_INTERNAL_UPDATE", value = SolanaBalanceRecord.InternalBalanceUpdateRecord::class),
    Type(name = "METAPLEX_META_CREATE_ACCOUNT", value = SolanaMetaRecord.MetaplexCreateMetadataAccountRecord::class),
    Type(name = "METAPLEX_META_SIGN_META", value = SolanaMetaRecord.MetaplexSignMetadataRecord::class),
    Type(name = "METAPLEX_META_UN_VERIFY_COLLECTION", value = SolanaMetaRecord.MetaplexUnVerifyCollectionRecord::class),
    Type(name = "METAPLEX_META_UPDATE", value = SolanaMetaRecord.MetaplexUpdateMetadataRecord::class),
    Type(name = "METAPLEX_META_VERIFY_COLLECTION", value = SolanaMetaRecord.MetaplexVerifyCollectionRecord::class),
    Type(name = "METAPLEX_META_SET_AND_VERIFY", value = SolanaMetaRecord.SetAndVerifyMetadataRecord::class),
    Type(name = "TOKEN_BURN", value = SolanaTokenRecord.BurnRecord::class),
    Type(name = "TOKEN_INITIALIZE_MINT", value = SolanaTokenRecord.InitializeMintRecord::class),
    Type(name = "TOKEN_MINT_TO", value = SolanaTokenRecord.MintToRecord::class),
    Type(name = "TOKEN_INTERNAL_UPDATE", value = SolanaTokenRecord.InternalTokenUpdateRecord::class),
)
sealed class SolanaBaseLogRecord : SolanaLogRecord() {
    abstract val timestamp: Instant
}

/**
 * Do not forget to list a [JsonSubTypes.Type] above for the newly registered class.
 */
@Suppress("unused")
private fun compilationChecker(record: SolanaBaseLogRecord): Unit = when (record) {
    is SolanaAuctionHouseOrderRecord.BuyRecord -> Unit
    is SolanaAuctionHouseOrderRecord.CancelRecord -> Unit
    is SolanaAuctionHouseOrderRecord.ExecuteSaleRecord -> Unit
    is SolanaAuctionHouseOrderRecord.SellRecord -> Unit
    is SolanaAuctionHouseOrderRecord.InternalOrderUpdateRecord -> Unit
    is SolanaAuctionHouseRecord.CreateAuctionHouseRecord -> Unit
    is SolanaAuctionHouseRecord.UpdateAuctionHouseRecord -> Unit
    is SolanaBalanceRecord.BurnRecord -> Unit
    is SolanaBalanceRecord.InitializeBalanceAccountRecord -> Unit
    is SolanaBalanceRecord.MintToRecord -> Unit
    is SolanaBalanceRecord.TransferIncomeRecord -> Unit
    is SolanaBalanceRecord.TransferOutcomeRecord -> Unit
    is SolanaBalanceRecord.InternalBalanceUpdateRecord -> Unit
    is SolanaMetaRecord.MetaplexCreateMetadataAccountRecord -> Unit
    is SolanaMetaRecord.MetaplexSignMetadataRecord -> Unit
    is SolanaMetaRecord.MetaplexUnVerifyCollectionRecord -> Unit
    is SolanaMetaRecord.MetaplexUpdateMetadataRecord -> Unit
    is SolanaMetaRecord.MetaplexVerifyCollectionRecord -> Unit
    is SolanaMetaRecord.SetAndVerifyMetadataRecord -> Unit
    is SolanaTokenRecord.BurnRecord -> Unit
    is SolanaTokenRecord.InitializeMintRecord -> Unit
    is SolanaTokenRecord.MintToRecord -> Unit
    is SolanaTokenRecord.InternalTokenUpdateRecord -> Unit
}
