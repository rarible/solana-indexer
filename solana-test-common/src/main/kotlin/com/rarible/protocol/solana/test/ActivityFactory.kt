package com.rarible.protocol.solana.test

import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.solana.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.solana.dto.ActivityFilterAllTypeDto
import com.rarible.protocol.solana.dto.ActivityFilterByItemTypeDto
import com.rarible.protocol.solana.dto.AssetDto
import com.rarible.protocol.solana.dto.AssetTypeDto
import com.rarible.protocol.solana.dto.BurnActivityDto
import com.rarible.protocol.solana.dto.MintActivityDto
import com.rarible.protocol.solana.dto.OrderBidActivityDto
import com.rarible.protocol.solana.dto.OrderCancelBidActivityDto
import com.rarible.protocol.solana.dto.OrderCancelListActivityDto
import com.rarible.protocol.solana.dto.OrderListActivityDto
import com.rarible.protocol.solana.dto.OrderMatchActivityDto
import com.rarible.protocol.solana.dto.SolanaNftAssetTypeDto
import com.rarible.protocol.solana.dto.SolanaSolAssetTypeDto
import com.rarible.protocol.solana.dto.TransferActivityDto
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

fun randomBlockchainInfo(
    blockHash: String = randomString(),
    blockNumber: Long = randomLong(),
    innerInstructionIndex: Int? = randomInt(),
    instructionIndex: Int = randomInt(),
    transactionHash: String = randomString(),
    transactionIndex: Int = randomInt(),
) = ActivityBlockchainInfoDto(
    blockNumber = blockNumber,
    blockHash = blockHash,
    transactionIndex = transactionIndex,
    transactionHash = transactionHash,
    instructionIndex = instructionIndex,
    innerInstructionIndex = innerInstructionIndex
)

fun randomMint(
    id: String = randomString(),
    date: Instant = randomTimestamp(),
    reverted: Boolean = false,
    owner: String = randomString(),
    tokenAddress: String = randomString(),
    value: BigInteger = randomBigInt(),
    blockchainInfo: ActivityBlockchainInfoDto = randomBlockchainInfo(),
) = MintActivityDto(
    id = id,
    date = date,
    reverted = reverted,
    owner = owner,
    tokenAddress = tokenAddress,
    value = value,
    blockchainInfo = blockchainInfo
)

fun randomBurn(
    id: String = randomString(),
    date: Instant = randomTimestamp(),
    reverted: Boolean = false,
    owner: String = randomString(),
    tokenAddress: String = randomString(),
    value: BigInteger = randomBigInt(),
    blockchainInfo: ActivityBlockchainInfoDto = randomBlockchainInfo(),
) = BurnActivityDto(
    id, date, reverted, owner, tokenAddress, value, blockchainInfo
)

fun randomTransfer(
    id: String = randomString(),
    date: Instant = randomTimestamp(),
    reverted: Boolean = false,
    from: String = randomString(),
    owner: String = randomString(),
    tokenAddress: String = randomString(),
    value: BigInteger = randomBigInt(),
    purchase: Boolean = randomBoolean(),
    blockchainInfo: ActivityBlockchainInfoDto = randomBlockchainInfo(),
) = TransferActivityDto(
    id, date, reverted, from, owner, tokenAddress, value, purchase, blockchainInfo
)

fun randomAssetSol(
    type: AssetTypeDto = SolanaSolAssetTypeDto(),
    value: BigDecimal = randomBigDecimal(),
) = AssetDto(type, value)

fun randomAssetNft(
    type: AssetTypeDto = SolanaNftAssetTypeDto(mint = randomString()),
    value: BigDecimal = BigDecimal.ONE,
) = AssetDto(type, value)

fun randomList(
    id: String = randomString(),
    date: Instant = randomTimestamp(),
    reverted: Boolean = false,
    hash: String = randomString(),
    maker: String = randomString(),
    make: AssetDto = randomAssetNft(),
    take: AssetDto = randomAssetSol(),
    price: BigDecimal = randomBigDecimal(),
    blockchainInfo: ActivityBlockchainInfoDto = randomBlockchainInfo(),
) = OrderListActivityDto(
    id, date, reverted, hash, maker, make, take, price, blockchainInfo
)

fun randomCancelList(
    id: String = randomString(),
    date: Instant = randomTimestamp(),
    reverted: Boolean = false,
    hash: String = randomString(),
    maker: String = randomString(),
    make: AssetTypeDto = SolanaNftAssetTypeDto(mint = randomString()),
    take: AssetTypeDto = SolanaSolAssetTypeDto(),
    blockchainInfo: ActivityBlockchainInfoDto = randomBlockchainInfo(),
) = OrderCancelListActivityDto(
    id, date, reverted, hash, maker, make, take, blockchainInfo
)

fun randomBid(
    id: String = randomString(),
    date: Instant = randomTimestamp(),
    reverted: Boolean = false,
    hash: String = randomString(),
    maker: String = randomString(),
    make: AssetDto = randomAssetSol(),
    take: AssetDto = randomAssetNft(),
    price: BigDecimal = randomBigDecimal(),
    blockchainInfo: ActivityBlockchainInfoDto = randomBlockchainInfo(),
) = OrderBidActivityDto(
    id, date, reverted, hash, maker, make, take, price, blockchainInfo
)

fun randomCancelBid(
    id: String = randomString(),
    date: Instant = randomTimestamp(),
    reverted: Boolean = false,
    hash: String = randomString(),
    maker: String = randomString(),
    make: AssetTypeDto = SolanaNftAssetTypeDto(mint = randomString()),
    take: AssetTypeDto = SolanaSolAssetTypeDto(),
    blockchainInfo: ActivityBlockchainInfoDto = randomBlockchainInfo(),
) = OrderCancelBidActivityDto(
    id, date, reverted, hash, maker, make, take, blockchainInfo
)

fun randomSell(
    id: String = randomString(),
    date: Instant = randomTimestamp(),
    reverted: Boolean = false,
    nft: AssetDto = randomAssetNft(),
    payment: AssetDto = randomAssetSol(),
    buyer: String = randomString(),
    seller: String = randomString(),
    buyerOrderHash: String = randomString(),
    sellerOrderHash: String = randomString(),
    price: BigDecimal = randomBigDecimal(),
    blockchainInfo: ActivityBlockchainInfoDto = randomBlockchainInfo(),
    type: OrderMatchActivityDto.Type = OrderMatchActivityDto.Type.SELL,
) = OrderMatchActivityDto(
    id, date, reverted, nft, payment, buyer, seller, buyerOrderHash, sellerOrderHash, price, blockchainInfo, type
)

fun randomTimestamp() = Instant
    .ofEpochMilli(randomLong(1648771200000, 1651363200000))
    .truncatedTo(ChronoUnit.MILLIS)!!

fun activityClassByType(type: ActivityFilterAllTypeDto) = when (type) {
    ActivityFilterAllTypeDto.TRANSFER -> TransferActivityDto::class.java
    ActivityFilterAllTypeDto.MINT -> MintActivityDto::class.java
    ActivityFilterAllTypeDto.BURN -> BurnActivityDto::class.java
    ActivityFilterAllTypeDto.BID -> OrderBidActivityDto::class.java
    ActivityFilterAllTypeDto.LIST -> OrderListActivityDto::class.java
    ActivityFilterAllTypeDto.SELL -> OrderMatchActivityDto::class.java
    ActivityFilterAllTypeDto.CANCEL_BID -> OrderCancelBidActivityDto::class.java
    ActivityFilterAllTypeDto.CANCEL_LIST -> OrderCancelListActivityDto::class.java
}

fun activityClassByType(type: ActivityFilterByItemTypeDto) = when (type) {
    ActivityFilterByItemTypeDto.TRANSFER -> TransferActivityDto::class.java
    ActivityFilterByItemTypeDto.MINT -> MintActivityDto::class.java
    ActivityFilterByItemTypeDto.BURN -> BurnActivityDto::class.java
    ActivityFilterByItemTypeDto.BID -> OrderBidActivityDto::class.java
    ActivityFilterByItemTypeDto.LIST -> OrderListActivityDto::class.java
    ActivityFilterByItemTypeDto.SELL -> OrderMatchActivityDto::class.java
    ActivityFilterByItemTypeDto.CANCEL_BID -> OrderCancelBidActivityDto::class.java
    ActivityFilterByItemTypeDto.CANCEL_LIST -> OrderCancelListActivityDto::class.java
}
