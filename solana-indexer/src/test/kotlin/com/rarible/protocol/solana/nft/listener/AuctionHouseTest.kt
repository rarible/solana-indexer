package com.rarible.protocol.solana.nft.listener

import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.solana.common.model.Asset
import com.rarible.protocol.solana.common.model.AuctionHouse
import com.rarible.protocol.solana.common.model.Order
import com.rarible.protocol.solana.common.model.OrderStatus
import com.rarible.protocol.solana.common.model.TokenNftAssetType
import com.rarible.protocol.solana.common.model.WrappedSolAssetType
import com.rarible.protocol.solana.common.pubkey.ProgramDerivedAddressCalc
import com.rarible.protocol.solana.common.pubkey.PublicKey
import com.rarible.protocol.solana.common.records.EMPTY_SOLANA_LOG
import com.rarible.protocol.solana.common.records.OrderDirection
import com.rarible.protocol.solana.common.records.SolanaAuctionHouseOrderRecord
import com.rarible.protocol.solana.common.records.SolanaAuctionHouseRecord
import com.rarible.protocol.solana.common.records.SolanaBalanceRecord
import com.rarible.protocol.solana.common.records.SolanaEscrowRecord
import com.rarible.protocol.solana.common.records.SubscriberGroup
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import java.time.Instant

class AuctionHouseTest : AbstractBlockScannerTest() {
    private val timeout = Duration.ofSeconds(10)

    @Test
    fun escrow() = runBlocking {
        val auctionHouseKeypair = createKeypair(randomString())
        airdrop(10, getWallet(auctionHouseKeypair))
        val house = createAuctionHouse(auctionHouseKeypair)
        val sellerWallet = getWallet(baseKeypair)

        val buyerKeypair = createKeypair(randomString())
        airdrop(10, buyerKeypair)
        val buyerWallet = getWallet(buyerKeypair)
        val token = mintNft(keypair = baseKeypair)

        airdrop(10, house.feePayerAcct)
        airdrop(10, buyerKeypair)

        sell(house.id, baseKeypair, 5, token, 1)

        buy(house.id, buyerKeypair, 3, token, 1)
        val escrow = ProgramDerivedAddressCalc.getEscrowPaymentAccount(
            PublicKey(buyerWallet),
            PublicKey(house.id)
        ).address.toBase58()

        Wait.waitAssert(timeout) {
            val records = findRecordByType(
                collection = SubscriberGroup.ESCROW.collectionName,
                type = SolanaEscrowRecord.BuyRecord::class.java
            ).single()

            assertThat(records).usingRecursiveComparison().ignoringFields(
                SolanaEscrowRecord.BuyRecord::log.name,
                SolanaEscrowRecord.BuyRecord::timestamp.name
            ).isEqualTo(
                SolanaEscrowRecord.BuyRecord(
                    auctionHouse = house.id,
                    escrow = escrow,
                    wallet = buyerWallet,
                    buyPrice = 3.scaleSupply(9),
                    log = EMPTY_SOLANA_LOG,
                    timestamp = Instant.EPOCH
                )
            )

            val balance = escrowRepository.findByAccount(escrow)?.value
            assertThat(balance).isEqualByComparingTo(3.scaleSupply(9))

            assertThat(showEscrow(house.id, buyerKeypair, buyerWallet))
                .isEqualByComparingTo(3.scaleSupply(9).toBigDecimal())
        }

        buy(house.id, buyerKeypair, 5, token, 1)
        Wait.waitAssert(timeout) {
            val records = findRecordByType(
                collection = SubscriberGroup.ESCROW.collectionName,
                type = SolanaEscrowRecord.BuyRecord::class.java
            ).toList().maxByOrNull { it.timestamp }

            assertThat(records).usingRecursiveComparison().ignoringFields(
                SolanaEscrowRecord.BuyRecord::log.name,
                SolanaEscrowRecord.BuyRecord::timestamp.name
            ).isEqualTo(
                SolanaEscrowRecord.BuyRecord(
                    auctionHouse = house.id,
                    escrow = escrow,
                    wallet = buyerWallet,
                    buyPrice = 5.scaleSupply(9),
                    log = EMPTY_SOLANA_LOG,
                    timestamp = Instant.EPOCH
                )
            )

            val balance = escrowRepository.findByAccount(escrow)?.value
            assertThat(balance).isEqualByComparingTo(5.scaleSupply(9))

            assertThat(showEscrow(house.id, buyerKeypair, buyerWallet))
                .isEqualByComparingTo(5.scaleSupply(9).toBigDecimal())
        }

        executeSale(house.id, auctionHouseKeypair, 5, token, 1, buyerWallet = buyerWallet, sellerWallet = sellerWallet)
        Wait.waitAssert(timeout) {
            val records = findRecordByType(
                collection = SubscriberGroup.ESCROW.collectionName,
                type = SolanaEscrowRecord.ExecuteSaleRecord::class.java
            ).single()

            assertThat(records).usingRecursiveComparison().ignoringFields(
                SolanaEscrowRecord.ExecuteSaleRecord::log.name,
                SolanaEscrowRecord.ExecuteSaleRecord::timestamp.name
            ).isEqualTo(
                SolanaEscrowRecord.ExecuteSaleRecord(
                    mint = token,
                    auctionHouse = house.id,
                    escrow = escrow,
                    wallet = buyerWallet,
                    buyPrice = 5.scaleSupply(9),
                    log = EMPTY_SOLANA_LOG,
                    timestamp = Instant.EPOCH
                )
            )

            val balance = escrowRepository.findByAccount(escrow)?.value
            assertThat(balance).isEqualByComparingTo(BigInteger.ZERO)

            assertThat(showEscrow(house.id, buyerKeypair, buyerWallet))
                .isEqualByComparingTo(BigDecimal.ZERO)
        }

        deposit(house.id, buyerKeypair, 2UL)
        Wait.waitAssert(timeout) {
            val records = findRecordByType(
                collection = SubscriberGroup.ESCROW.collectionName,
                type = SolanaEscrowRecord.DepositRecord::class.java
            ).single()

            assertThat(records).usingRecursiveComparison().ignoringFields(
                SolanaEscrowRecord.DepositRecord::log.name,
                SolanaEscrowRecord.DepositRecord::timestamp.name
            ).isEqualTo(
                SolanaEscrowRecord.DepositRecord(
                    auctionHouse = house.id,
                    escrow = escrow,
                    wallet = buyerWallet,
                    amount = 2.scaleSupply(9),
                    log = EMPTY_SOLANA_LOG,
                    timestamp = Instant.EPOCH
                )
            )

            val balance = escrowRepository.findByAccount(escrow)?.value
            assertThat(balance).isEqualByComparingTo(2.scaleSupply(9))

            assertThat(showEscrow(house.id, buyerKeypair, buyerWallet))
                .isEqualByComparingTo(2.scaleSupply(9).toBigDecimal())
        }

        withdraw(house.id, buyerKeypair, 1UL)
        Wait.waitAssert(timeout) {
            val records = findRecordByType(
                collection = SubscriberGroup.ESCROW.collectionName,
                type = SolanaEscrowRecord.WithdrawRecord::class.java
            ).single()

            assertThat(records).usingRecursiveComparison().ignoringFields(
                SolanaEscrowRecord.WithdrawRecord::log.name,
                SolanaEscrowRecord.WithdrawRecord::timestamp.name
            ).isEqualTo(
                SolanaEscrowRecord.WithdrawRecord(
                    auctionHouse = house.id,
                    escrow = escrow,
                    wallet = buyerWallet,
                    amount = 1.scaleSupply(9),
                    log = EMPTY_SOLANA_LOG,
                    timestamp = Instant.EPOCH
                )
            )

            val balance = escrowRepository.findByAccount(escrow)?.value
            assertThat(balance).isEqualByComparingTo(1.scaleSupply(9))

            assertThat(showEscrow(house.id, buyerKeypair, buyerWallet))
                .isEqualByComparingTo(1.scaleSupply(9).toBigDecimal())
        }
    }

    @Test
    fun createAuctionHouseTest() = runBlocking {
        val keypair = createKeypair(randomString())
        val wallet = getWallet(keypair)
        airdrop(10, wallet)
        val house = createAuctionHouse(keypair)

        Wait.waitAssert(timeout) {
            val record = findRecordByType(
                collection = SubscriberGroup.AUCTION_HOUSE.collectionName,
                type = SolanaAuctionHouseRecord.CreateAuctionHouseRecord::class.java
            ).single()

            assertThat(record).usingRecursiveComparison().ignoringFields(
                SolanaAuctionHouseRecord.CreateAuctionHouseRecord::log.name,
                SolanaAuctionHouseRecord.CreateAuctionHouseRecord::timestamp.name
            ).isEqualTo(
                SolanaAuctionHouseRecord.CreateAuctionHouseRecord(
                    sellerFeeBasisPoints = 1000,
                    requiresSignOff = false,
                    treasuryMint = WrappedSolAssetType.SOL,
                    feeWithdrawalDestination = wallet,
                    treasuryWithdrawalDestination = wallet,
                    auctionHouse = house.id,
                    log = EMPTY_SOLANA_LOG,
                    timestamp = Instant.EPOCH
                )
            )

            assertThat(actionHouseRepository.findByAccount(house.id)).usingRecursiveComparison()
                .ignoringFields(
                    AuctionHouse::createdAt.name,
                    AuctionHouse::updatedAt.name,
                    AuctionHouse::revertableEvents.name
                )
                .isEqualTo(
                    AuctionHouse(
                        requiresSignOff = false,
                        sellerFeeBasisPoints = 1000,
                        account = house.id,
                        states = emptyList(),
                        revertableEvents = emptyList(),
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH
                    )
                )
        }

        updateAuctionHouse(house.id, 500, true, keypair)

        Wait.waitAssert(timeout) {
            assertThat(actionHouseRepository.findByAccount(house.id)).usingRecursiveComparison()
                .ignoringFields(
                    AuctionHouse::createdAt.name,
                    AuctionHouse::updatedAt.name,
                    AuctionHouse::states.name,
                    AuctionHouse::revertableEvents.name
                )
                .isEqualTo(
                    AuctionHouse(
                        requiresSignOff = true,
                        sellerFeeBasisPoints = 500,
                        account = house.id,
                        states = emptyList(),
                        revertableEvents = emptyList(),
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH
                    )
                )
        }

        updateAuctionHouse(house.id, 300, null, keypair)

        Wait.waitAssert(timeout) {
            assertThat(actionHouseRepository.findByAccount(house.id)).usingRecursiveComparison()
                .ignoringFields(
                    AuctionHouse::createdAt.name,
                    AuctionHouse::updatedAt.name,
                    AuctionHouse::states.name,
                    AuctionHouse::revertableEvents.name
                )
                .isEqualTo(
                    AuctionHouse(
                        requiresSignOff = true,
                        sellerFeeBasisPoints = 300,
                        account = house.id,
                        states = emptyList(),
                        revertableEvents = emptyList(),
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH
                    )
                )
        }
    }

    // NOTE! this test may be very slow (3+ minutes), it is OK.
    @Test
    fun sellTest() = runBlocking {
        val keypair = createKeypair(randomString())
        val wallet = getWallet(keypair)
        airdrop(10, wallet)
        val house = createAuctionHouse(keypair)
        val token = mintNft(baseKeypair)
        airdrop(10, house.feePayerAcct)
        sell(house.id, baseKeypair, 1, token, 1)

        Wait.waitAssert(timeout) {
            val records = findRecordByType(
                collection = SubscriberGroup.AUCTION_HOUSE_ORDER.collectionName,
                type = SolanaAuctionHouseOrderRecord.SellRecord::class.java
            ).toList()

            assertThat(records).usingElementComparatorIgnoringFields(
                SolanaAuctionHouseOrderRecord.SellRecord::log.name,
                SolanaAuctionHouseOrderRecord.SellRecord::timestamp.name,
                SolanaAuctionHouseOrderRecord.SellRecord::tokenAccount.name // TODO: do not ignore. We can calculate this field by [token] and [baseKeyPair]
            ).isEqualTo(
                listOf(
                    SolanaAuctionHouseOrderRecord.SellRecord(
                        maker = getWallet(baseKeypair),
                        tokenAccount = "",
                        mint = token,
                        amount = 1L.toBigInteger(),
                        sellPrice = 1.scaleSupply(9),
                        auctionHouse = house.id,
                        log = EMPTY_SOLANA_LOG,
                        timestamp = Instant.EPOCH,
                        orderId = ""
                    ).withUpdatedOrderId()
                )
            )
        }
    }

    @Test
    fun sellCancelTest() = runBlocking {
        val keypair = createKeypair(randomString())
        val wallet = getWallet(keypair)
        airdrop(10, wallet)
        val house = createAuctionHouse(keypair)
        val token = mintNft(baseKeypair)
        val sellerWallet = getWallet(baseKeypair)

        airdrop(10, house.feePayerAcct)
        sell(house.id, baseKeypair, 5, token, 1)
        val sellOrder = Wait.waitFor(timeout) {
            val order = orderRepository.findById(
                Order.calculateAuctionHouseOrderId(
                    maker = sellerWallet,
                    mint = token,
                    direction = OrderDirection.SELL,
                    auctionHouse = house.id
                )
            )
            assertThat(order)
                .usingRecursiveComparison()
                .ignoringFields(
                    "createdAt",
                    "updatedAt",
                    Order::dbUpdatedAt.name,
                    "revertableEvents",
                    "makerAccount"
                )
                .isEqualTo(
                    Order(
                        auctionHouse = house.id,
                        auctionHouseSellerFeeBasisPoints = 1000,
                        auctionHouseRequiresSignOff = false,
                        maker = sellerWallet,
                        makerAccount = "", // TODO: calculate maker account.
                        status = OrderStatus.ACTIVE,
                        make = Asset(TokenNftAssetType(token), 1.toBigInteger()),
                        take = Asset(WrappedSolAssetType(), 5.scaleSupply(9)),
                        fill = BigInteger.ZERO,
                        makeStock = BigInteger.ONE,
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH,
                        revertableEvents = emptyList(),
                        direction = OrderDirection.SELL,
                        makePrice = 5.scaleSupply(9).toBigDecimal(9),
                        takePrice = null,
                        states = emptyList(),
                        dbUpdatedAt = Instant.EPOCH
                    )
                )

            order!!
        }
        cancel(house.id, baseKeypair, 5, token, 1)

        Wait.waitAssert(timeout) {
            val records = findRecordByType(
                collection = SubscriberGroup.AUCTION_HOUSE_ORDER.collectionName,
                type = SolanaAuctionHouseOrderRecord.CancelRecord::class.java
            ).toList()
            val cancelRecord = SolanaAuctionHouseOrderRecord.CancelRecord(
                mint = token,
                amount = 1L.toBigInteger(),
                auctionHouse = house.id,
                log = EMPTY_SOLANA_LOG,
                timestamp = Instant.EPOCH,
                orderId = "",
                maker = sellerWallet,
                price = 5.scaleSupply(9),
                direction = OrderDirection.SELL
            ).withUpdatedOrderId()

            assertThat(records).usingElementComparatorIgnoringFields(
                SolanaAuctionHouseOrderRecord.BuyRecord::log.name,
                SolanaAuctionHouseOrderRecord.BuyRecord::timestamp.name,
            ).containsExactlyInAnyOrder(
                cancelRecord,
                cancelRecord.copy(direction = OrderDirection.BUY).withUpdatedOrderId()
            )

            val order = orderRepository.findById(
                Order.calculateAuctionHouseOrderId(
                    maker = sellerWallet,
                    mint = token,
                    direction = OrderDirection.SELL,
                    auctionHouse = house.id
                )
            )
            assertThat(order)
                .usingRecursiveComparison()
                .ignoringFields(
                    "createdAt",
                    "updatedAt",
                    "revertableEvents",
                    Order::dbUpdatedAt.name,
                    "makerAccount"
                )
                .isEqualTo(
                    Order(
                        auctionHouse = house.id,
                        auctionHouseSellerFeeBasisPoints = 1000,
                        auctionHouseRequiresSignOff = false,
                        maker = sellerWallet,
                        makerAccount = "", // TODO: calculate maker account.
                        status = OrderStatus.CANCELLED,
                        make = Asset(TokenNftAssetType(token), 1.toBigInteger()),
                        take = Asset(WrappedSolAssetType(), 5.scaleSupply(9)),
                        makeStock = BigInteger.ZERO,
                        fill = BigInteger.ZERO,
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH,
                        revertableEvents = emptyList(),
                        direction = OrderDirection.SELL,
                        makePrice = 5.scaleSupply(9).toBigDecimal(9),
                        takePrice = null,
                        states = listOf(sellOrder),
                        dbUpdatedAt = Instant.EPOCH
                    )
                )
        }
    }

    @Test
    fun buyCancelTest() = runBlocking {
        val keypair = createKeypair(randomString())
        val wallet = getWallet(keypair)
        airdrop(10, wallet)
        val house = createAuctionHouse(keypair)
        val token = mintNft(baseKeypair)
        airdrop(10, house.feePayerAcct)
        buy(house.id, keypair, 5, token, 1)

        val buyOrder = Wait.waitFor(timeout) {
            val order = orderRepository.findById(
                Order.calculateAuctionHouseOrderId(
                    maker = wallet,
                    mint = token,
                    direction = OrderDirection.BUY,
                    auctionHouse = house.id
                )
            )
            assertThat(order)
                .usingRecursiveComparison()
                .ignoringFields(
                    "createdAt",
                    "updatedAt",
                    Order::dbUpdatedAt.name,
                    "revertableEvents",
                    "makerAccount"
                )
                .isEqualTo(
                    Order(
                        auctionHouse = house.id,
                        auctionHouseSellerFeeBasisPoints = 1000,
                        auctionHouseRequiresSignOff = false,
                        maker = wallet,
                        makerAccount = "", // TODO: calculate maker account.
                        status = OrderStatus.ACTIVE,
                        make = Asset(WrappedSolAssetType(), 5.scaleSupply(9)),
                        take = Asset(TokenNftAssetType(token), 1.toBigInteger()),
                        fill = BigInteger.ZERO,
                        makeStock = 5.scaleSupply(9),
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH,
                        revertableEvents = emptyList(),
                        direction = OrderDirection.BUY,
                        makePrice = null,
                        takePrice = 5.scaleSupply(9).toBigDecimal(9),
                        states = emptyList(),
                        dbUpdatedAt = Instant.EPOCH,
                    )
                )

            order!!
        }
        cancel(house.id, keypair, 5, token, 1)

        Wait.waitAssert(timeout) {
            val records = findRecordByType(
                collection = SubscriberGroup.AUCTION_HOUSE_ORDER.collectionName,
                type = SolanaAuctionHouseOrderRecord.CancelRecord::class.java
            ).toList()
            val cancelRecord = SolanaAuctionHouseOrderRecord.CancelRecord(
                mint = token,
                amount = 1L.toBigInteger(),
                auctionHouse = house.id,
                log = EMPTY_SOLANA_LOG,
                timestamp = Instant.EPOCH,
                orderId = "",
                maker = wallet,
                price = 5.scaleSupply(9),
                direction = OrderDirection.SELL
            ).withUpdatedOrderId()

            assertThat(records).usingElementComparatorIgnoringFields(
                SolanaAuctionHouseOrderRecord.BuyRecord::log.name,
                SolanaAuctionHouseOrderRecord.BuyRecord::timestamp.name,
            ).containsExactlyInAnyOrder(
                cancelRecord,
                cancelRecord.copy(direction = OrderDirection.BUY).withUpdatedOrderId()
            )

            val order = orderRepository.findById(
                Order.calculateAuctionHouseOrderId(
                    maker = wallet,
                    mint = token,
                    direction = OrderDirection.BUY,
                    auctionHouse = house.id
                )
            )
            assertThat(order)
                .usingRecursiveComparison()
                .ignoringFields(
                    "createdAt",
                    "updatedAt",
                    Order::dbUpdatedAt.name,
                    "revertableEvents",
                    "makerAccount"
                )
                .isEqualTo(
                    Order(
                        auctionHouse = house.id,
                        auctionHouseSellerFeeBasisPoints = 1000,
                        auctionHouseRequiresSignOff = false,
                        maker = wallet,
                        makerAccount = "", // TODO: calculate maker account.
                        status = OrderStatus.CANCELLED,
                        make = Asset(WrappedSolAssetType(), 5.scaleSupply(9)),
                        take = Asset(TokenNftAssetType(token), 1.toBigInteger()),
                        makeStock = BigInteger.ZERO,
                        fill = BigInteger.ZERO,
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH,
                        revertableEvents = emptyList(),
                        direction = OrderDirection.BUY,
                        makePrice = null,
                        takePrice = 5.scaleSupply(9).toBigDecimal(9),
                        states = listOf(buyOrder),
                        dbUpdatedAt = Instant.EPOCH
                    )
                )
        }
    }

    @Test
    fun orderStatusTest() = runBlocking {
        val keypair = createKeypair(randomString())
        val wallet = getWallet(keypair)
        airdrop(10, wallet)
        val house = createAuctionHouse(keypair)
        val token = mintNft(baseKeypair)
        airdrop(10, house.feePayerAcct)
        buy(house.id, keypair, 1, token, 1)
        withdraw(house.id, keypair, 1UL)

        Wait.waitAssert(timeout) {
            val order = orderRepository.findBuyOrders(
                maker = getWallet(keypair),
                statuses = listOf(OrderStatus.ACTIVE, OrderStatus.INACTIVE),
                auctionHouse = house.id
            ).single()

            assertThat(order.status).isEqualTo(OrderStatus.INACTIVE)
        }

        deposit(house.id, keypair, 1UL)
        Wait.waitAssert(timeout) {
            val order = orderRepository.findBuyOrders(
                maker = getWallet(keypair),
                statuses = listOf(OrderStatus.ACTIVE, OrderStatus.INACTIVE),
                auctionHouse = house.id
            ).single()

            assertThat(order.status).isEqualTo(OrderStatus.ACTIVE)
        }
    }

    @Test
    fun buyTest() = runBlocking {
        val keypair = createKeypair(randomString())
        val wallet = getWallet(keypair)
        airdrop(10, wallet)
        val house = createAuctionHouse(keypair)
        val token = mintNft(baseKeypair)
        airdrop(10, house.feePayerAcct)
        buy(house.id, keypair, 1, token, 1)

        Wait.waitAssert(timeout) {
            val records = findRecordByType(
                collection = SubscriberGroup.AUCTION_HOUSE_ORDER.collectionName,
                type = SolanaAuctionHouseOrderRecord.BuyRecord::class.java
            ).toList()

            assertThat(records).usingElementComparatorIgnoringFields(
                SolanaAuctionHouseOrderRecord.BuyRecord::log.name,
                SolanaAuctionHouseOrderRecord.BuyRecord::timestamp.name,
                SolanaAuctionHouseOrderRecord.BuyRecord::tokenAccount.name // TODO: do not ignore. We can calculate this field by [token] and [baseKeyPair]
            ).isEqualTo(
                listOf(
                    SolanaAuctionHouseOrderRecord.BuyRecord(
                        maker = wallet,
                        treasuryMint = WrappedSolAssetType.SOL,
                        tokenAccount = "",
                        mint = token,
                        amount = 1L.toBigInteger(),
                        buyPrice = 1.scaleSupply(9),
                        auctionHouse = house.id,
                        log = EMPTY_SOLANA_LOG,
                        timestamp = Instant.EPOCH,
                        orderId = ""
                    ).withUpdatedOrderId()
                )
            )
        }
    }

    @Test
    fun executeSaleTest() = runBlocking<Unit> {
        val auctionHouseKeypair = createKeypair(randomString())
        airdrop(10, getWallet(auctionHouseKeypair))
        val house = createAuctionHouse(auctionHouseKeypair)
        updateAuctionHouse(house.id, 500, false, auctionHouseKeypair)
        val sellerWallet = getWallet(baseKeypair)

        val buyerKeypair = createKeypair(randomString())
        airdrop(10, buyerKeypair)
        val buyerWallet = getWallet(buyerKeypair)
        val token = mintNft(keypair = baseKeypair)

        airdrop(10, house.feePayerAcct)
        airdrop(10, buyerKeypair)

        sell(house.id, baseKeypair, 5, token, 1)
        val originalSellRecord = Wait.waitFor(timeout) {
            val order = orderRepository.findById(
                Order.calculateAuctionHouseOrderId(
                    maker = sellerWallet,
                    mint = token,
                    direction = OrderDirection.SELL,
                    auctionHouse = house.id
                )
            )
            assertThat(order)
                .usingRecursiveComparison()
                .ignoringFields(
                    "createdAt",
                    "updatedAt",
                    Order::dbUpdatedAt.name,
                    "revertableEvents",
                    "makerAccount"
                )
                .isEqualTo(
                    Order(
                        auctionHouse = house.id,
                        auctionHouseSellerFeeBasisPoints = 500,
                        auctionHouseRequiresSignOff = false,
                        maker = sellerWallet,
                        makerAccount = "", // TODO: calculate maker account.
                        status = OrderStatus.ACTIVE,
                        make = Asset(TokenNftAssetType(token), 1.toBigInteger()),
                        take = Asset(WrappedSolAssetType(), 5.scaleSupply(9)),
                        makeStock = 1.toBigInteger(),
                        fill = BigInteger.ZERO,
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH,
                        revertableEvents = emptyList(),
                        direction = OrderDirection.SELL,
                        makePrice = 5.scaleSupply(9).toBigDecimal(9),
                        takePrice = null,
                        states = emptyList(),
                        dbUpdatedAt = Instant.EPOCH
                    )
                )

            order!!
        }
        buy(house.id, buyerKeypair, 5, token, 1)
        val escrow = showEscrow(house.id, buyerKeypair, buyerWallet)
        assertThat(escrow).isEqualByComparingTo(5.scaleSupply(9).toBigDecimal())
        val originalBuyRecord = Wait.waitFor(timeout) {
            val order = orderRepository.findById(
                Order.calculateAuctionHouseOrderId(
                    maker = buyerWallet,
                    mint = token,
                    direction = OrderDirection.BUY,
                    auctionHouse = house.id
                )
            )
            assertThat(order)
                .usingRecursiveComparison()
                .ignoringFields(
                    "createdAt",
                    "updatedAt",
                    Order::dbUpdatedAt.name,
                    "revertableEvents",
                    "makerAccount"
                )
                .isEqualTo(
                    Order(
                        auctionHouse = house.id,
                        auctionHouseSellerFeeBasisPoints = 500,
                        auctionHouseRequiresSignOff = false,
                        maker = buyerWallet,
                        makerAccount = "", // TODO: calculate maker account.
                        status = OrderStatus.ACTIVE,
                        make = Asset(WrappedSolAssetType(), 5.scaleSupply(9)),
                        take = Asset(TokenNftAssetType(token), 1.toBigInteger()),
                        makeStock = 5.scaleSupply(9),
                        fill = BigInteger.ZERO,
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH,
                        revertableEvents = emptyList(),
                        direction = OrderDirection.BUY,
                        makePrice = null,
                        takePrice = 5.scaleSupply(9).toBigDecimal(9),
                        states = emptyList(),
                        dbUpdatedAt = Instant.EPOCH
                    )
                )

            order!!
        }
        val sellerBalanceBefore = getBalance(sellerWallet)
        executeSale(house.id, auctionHouseKeypair, 5, token, 1, buyerWallet = buyerWallet, sellerWallet = sellerWallet)
        Wait.waitAssert(timeout) {
            val balanceRecords = findRecordByType(
                collection = SubscriberGroup.BALANCE.collectionName,
                type = SolanaBalanceRecord.InitializeBalanceAccountRecord::class.java
            ).toList()
            val fromAccount = balanceRecords.single { it.owner == sellerWallet }.account
            val toAccount = balanceRecords.single { it.owner == buyerWallet }.account
            val buyRecords = findRecordByType(
                collection = SubscriberGroup.AUCTION_HOUSE_ORDER.collectionName,
                type = SolanaAuctionHouseOrderRecord.BuyRecord::class.java
            ).toList()
            assertThat(buyRecords).usingElementComparatorIgnoringFields(
                SolanaAuctionHouseOrderRecord.BuyRecord::log.name,
                SolanaAuctionHouseOrderRecord.BuyRecord::timestamp.name
            ).isEqualTo(
                listOf(
                    SolanaAuctionHouseOrderRecord.BuyRecord(
                        maker = buyerWallet,
                        treasuryMint = WrappedSolAssetType.SOL,
                        buyPrice = 5.scaleSupply(9),
                        tokenAccount = fromAccount,
                        mint = token,
                        amount = 1L.toBigInteger(),
                        auctionHouse = house.id,
                        log = EMPTY_SOLANA_LOG,
                        timestamp = Instant.EPOCH,
                        orderId = ""
                    ).withUpdatedOrderId()
                )
            )

            val sellRecords = findRecordByType(
                collection = SubscriberGroup.AUCTION_HOUSE_ORDER.collectionName,
                type = SolanaAuctionHouseOrderRecord.SellRecord::class.java
            ).toList()
            assertThat(sellRecords).usingElementComparatorIgnoringFields(
                SolanaAuctionHouseOrderRecord.SellRecord::log.name,
                SolanaAuctionHouseOrderRecord.SellRecord::timestamp.name
            ).isEqualTo(
                listOf(
                    SolanaAuctionHouseOrderRecord.SellRecord(
                        maker = sellerWallet,
                        sellPrice = 5.scaleSupply(9),
                        tokenAccount = fromAccount,
                        mint = token,
                        amount = 1L.toBigInteger(),
                        auctionHouse = house.id,
                        log = EMPTY_SOLANA_LOG,
                        timestamp = Instant.EPOCH,
                        orderId = ""
                    ).withUpdatedOrderId()
                )
            )

            val saleRecords = findRecordByType(
                collection = SubscriberGroup.AUCTION_HOUSE_ORDER.collectionName,
                type = SolanaAuctionHouseOrderRecord.ExecuteSaleRecord::class.java
            ).toList()
            val sellRecord = SolanaAuctionHouseOrderRecord.ExecuteSaleRecord(
                buyer = buyerWallet,
                seller = sellerWallet,
                mint = token,
                amount = 1L.toBigInteger(),
                price = 5.scaleSupply(9),
                auctionHouse = house.id,
                log = EMPTY_SOLANA_LOG,
                timestamp = Instant.EPOCH,
                direction = OrderDirection.SELL,
                treasuryMint = WrappedSolAssetType.SOL
            )
            assertThat(saleRecords).usingElementComparatorIgnoringFields(
                SolanaAuctionHouseOrderRecord.ExecuteSaleRecord::log.name,
                SolanaAuctionHouseOrderRecord.ExecuteSaleRecord::timestamp.name
            ).containsExactlyInAnyOrder(
                sellRecord,
                sellRecord.copy(direction = OrderDirection.BUY)
            )

            val incomeTransfersRecords = findRecordByType(
                collection = SubscriberGroup.BALANCE.collectionName,
                type = SolanaBalanceRecord.TransferIncomeRecord::class.java
            ).toList()
            assertThat(incomeTransfersRecords).usingElementComparatorIgnoringFields(
                SolanaBalanceRecord.TransferIncomeRecord::log.name,
                SolanaBalanceRecord.TransferIncomeRecord::timestamp.name
            ).isEqualTo(
                listOf(
                    SolanaBalanceRecord.TransferIncomeRecord(
                        from = fromAccount,
                        account = toAccount,
                        mint = token,
                        incomeAmount = 1.toBigInteger(),
                        log = EMPTY_SOLANA_LOG,
                        timestamp = Instant.EPOCH,
                    )
                )
            )

            val outcomeTransfersRecords = findRecordByType(
                collection = SubscriberGroup.BALANCE.collectionName,
                type = SolanaBalanceRecord.TransferOutcomeRecord::class.java
            ).toList()
            assertThat(outcomeTransfersRecords).usingElementComparatorIgnoringFields(
                SolanaBalanceRecord.TransferOutcomeRecord::log.name,
                SolanaBalanceRecord.TransferOutcomeRecord::timestamp.name
            ).isEqualTo(
                listOf(
                    SolanaBalanceRecord.TransferOutcomeRecord(
                        to = toAccount,
                        account = fromAccount,
                        mint = token,
                        outcomeAmount = 1.toBigInteger(),
                        log = EMPTY_SOLANA_LOG,
                        timestamp = Instant.EPOCH,
                    )
                )
            )

            val buyOrder = orderRepository.findById(
                Order.calculateAuctionHouseOrderId(
                    maker = buyerWallet,
                    mint = token,
                    direction = OrderDirection.BUY,
                    auctionHouse = house.id
                )
            )
            assertThat(buyOrder)
                .usingRecursiveComparison()
                .ignoringFields(
                    "createdAt",
                    "updatedAt",
                    Order::dbUpdatedAt.name,
                    "revertableEvents",
                    "makerAccount"
                )
                .isEqualTo(
                    Order(
                        auctionHouse = house.id,
                        auctionHouseSellerFeeBasisPoints = 500,
                        auctionHouseRequiresSignOff = false,
                        maker = buyerWallet,
                        makerAccount = "", // TODO: calculate maker account.
                        status = OrderStatus.FILLED,
                        make = Asset(WrappedSolAssetType(), 5.scaleSupply(9)),
                        take = Asset(TokenNftAssetType(token), 1.toBigInteger()),
                        fill = BigInteger.ONE,
                        makeStock = BigInteger.ZERO,
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH,
                        revertableEvents = emptyList(),
                        direction = OrderDirection.BUY,
                        makePrice = null,
                        takePrice = 5.scaleSupply(9).toBigDecimal(9),
                        states = listOf(originalBuyRecord),
                        dbUpdatedAt = Instant.EPOCH
                    )
                )

            val sellOrder = orderRepository.findById(
                Order.calculateAuctionHouseOrderId(
                    maker = sellerWallet,
                    mint = token,
                    direction = OrderDirection.SELL,
                    auctionHouse = house.id
                )
            )
            assertThat(sellOrder)
                .usingRecursiveComparison()
                .ignoringFields(
                    "createdAt",
                    "updatedAt",
                    Order::dbUpdatedAt.name,
                    "revertableEvents",
                    "makerAccount"
                )
                .isEqualTo(
                    Order(
                        auctionHouse = house.id,
                        auctionHouseSellerFeeBasisPoints = 500,
                        auctionHouseRequiresSignOff = false,
                        maker = sellerWallet,
                        makerAccount = "", // TODO: calculate maker account.
                        status = OrderStatus.FILLED,
                        make = Asset(TokenNftAssetType(token), 1.toBigInteger()),
                        take = Asset(WrappedSolAssetType(), 5.scaleSupply(9)),
                        makeStock = BigInteger.ZERO,
                        fill = BigInteger.ONE,
                        createdAt = Instant.EPOCH,
                        updatedAt = Instant.EPOCH,
                        revertableEvents = emptyList(),
                        direction = OrderDirection.SELL,
                        makePrice = 5.scaleSupply(9).toBigDecimal(9),
                        takePrice = null,
                        states = listOf(originalSellRecord),
                        dbUpdatedAt = Instant.EPOCH
                    )
                )
        }

        assertThat(getBalance(sellerWallet) - sellerBalanceBefore).isEqualByComparingTo(BigDecimal.valueOf(4.5))
        assertThat(getBalance(house.treasuryAcct)).isEqualByComparingTo(BigDecimal.valueOf(0.5))
    }
}
