package com.rarible.protocol.solana.common.records

import com.rarible.blockchain.scanner.solana.model.SolanaLog
import com.rarible.protocol.solana.common.model.Order
import java.math.BigInteger
import java.time.Instant

sealed class SolanaAuctionHouseOrderRecord : SolanaBaseLogRecord() {
    abstract val auctionHouse: String
    abstract val mint: String
    abstract val orderId: String

    final override fun getKey(): String = orderId

    data class BuyRecord(
        val maker: String,
        val treasuryMint: String,
        val buyPrice: BigInteger,
        val tokenAccount: String,
        override val mint: String,
        val amount: BigInteger,
        override val log: SolanaLog,
        override val timestamp: Instant,
        override val auctionHouse: String,
        override val orderId: String
    ) : SolanaAuctionHouseOrderRecord() {
        fun withUpdatedOrderId() = copy(
            orderId = Order.calculateAuctionHouseOrderId(
                maker = maker,
                mint = mint,
                direction = OrderDirection.BUY,
                auctionHouse = auctionHouse
            )
        )
    }

    data class SellRecord(
        val maker: String,
        val sellPrice: BigInteger,
        val tokenAccount: String,
        override val mint: String,
        val amount: BigInteger,
        override val log: SolanaLog,
        override val timestamp: Instant,
        override val auctionHouse: String,
        override val orderId: String
    ) : SolanaAuctionHouseOrderRecord() {
        fun withUpdatedOrderId() = copy(
            orderId = Order.calculateAuctionHouseOrderId(
                maker = maker,
                mint = mint,
                direction = OrderDirection.SELL,
                auctionHouse = auctionHouse
            )
        )
    }

    data class ExecuteSaleRecord(
        val buyer: String,
        val seller: String,
        val price: BigInteger,
        override val mint: String,
        val treasuryMint: String,
        val amount: BigInteger,
        val direction: OrderDirection,
        override val log: SolanaLog,
        override val timestamp: Instant,
        override val auctionHouse: String,
        override val orderId: String = Order.calculateAuctionHouseOrderId(
            maker = when (direction) {
                OrderDirection.BUY -> buyer
                OrderDirection.SELL -> seller
            },
            mint = mint,
            direction = direction,
            auctionHouse = auctionHouse
        )
    ) : SolanaAuctionHouseOrderRecord() {

        override val id: String
            get() = super.id + ":" + when (direction) {
                OrderDirection.BUY -> "buy"
                OrderDirection.SELL -> "sell"
            }
    }

    data class CancelRecord(
        val maker: String,
        override val mint: String,
        val price: BigInteger,
        val amount: BigInteger,
        val direction: OrderDirection,
        override val log: SolanaLog,
        override val timestamp: Instant,
        override val auctionHouse: String,
        override val orderId: String
    ) : SolanaAuctionHouseOrderRecord() {

        fun withUpdatedOrderId() = copy(
            orderId = Order.calculateAuctionHouseOrderId(
                maker = maker,
                mint = mint,
                direction = direction,
                auctionHouse = auctionHouse
            )
        )

        override val id: String
            get() = super.id + ":" + when (direction) {
                OrderDirection.BUY -> "buy"
                OrderDirection.SELL -> "sell"
            }

    }

    /**
     * Fake record used to trigger re-calculation of the order's balance.
     * It is not written to the database but only to the message bus (Kafka) to trigger an update.
     */
    data class InternalOrderUpdateRecord(
        override val mint: String,
        override val timestamp: Instant,
        override val auctionHouse: String,
        override val orderId: String,
        override val log: SolanaLog = EMPTY_SOLANA_LOG,
        val instruction: SolanaOrderUpdateInstruction
    ) : SolanaAuctionHouseOrderRecord()

}