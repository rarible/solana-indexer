package com.rarible.protocol.solana.test

import com.rarible.blockchain.scanner.solana.model.SolanaLog
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.solana.common.meta.MetaplexOffChainCollectionHash
import com.rarible.protocol.solana.common.meta.TokenMeta
import com.rarible.protocol.solana.common.model.Asset
import com.rarible.protocol.solana.common.model.AssetType
import com.rarible.protocol.solana.common.model.AuctionHouse
import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.common.model.BalanceWithMeta
import com.rarible.protocol.solana.common.model.MetaplexMeta
import com.rarible.protocol.solana.common.model.MetaplexMetaFields
import com.rarible.protocol.solana.common.model.MetaplexOffChainMeta
import com.rarible.protocol.solana.common.model.MetaplexOffChainMetaFields
import com.rarible.protocol.solana.common.model.MetaplexTokenCreator
import com.rarible.protocol.solana.common.model.Order
import com.rarible.protocol.solana.common.model.OrderStatus
import com.rarible.protocol.solana.common.model.Token
import com.rarible.protocol.solana.common.model.TokenFtAssetType
import com.rarible.protocol.solana.common.model.TokenNftAssetType
import com.rarible.protocol.solana.common.model.TokenWithMeta
import com.rarible.protocol.solana.common.pubkey.Keypair
import com.rarible.protocol.solana.common.records.OrderDirection
import java.math.BigInteger
import java.time.Instant

fun randomAccount() = Keypair.createRandom().publicKey.toBase58()
fun randomMint() = randomAccount()

fun createRandomToken(mint: String = randomMint()): Token = Token(
    mint = mint,
    supply = randomBigInt(),
    revertableEvents = emptyList(),
    decimals = randomInt(6),
    createdAt = nowMillis(),
    updatedAt = nowMillis(),
)

fun createRandomTokenWithMeta(): TokenWithMeta = TokenWithMeta(
    token = createRandomToken(),
    tokenMeta = createRandomTokenMeta()
)

fun createRandomMetaplexMeta(
    mint: String = randomMint()
): MetaplexMeta = MetaplexMeta(
    metaAddress = randomAccount(),
    tokenAddress = mint,
    metaFields = createRandomMetaplexMetaFields(),
    isMutable = randomBoolean(),
    revertableEvents = emptyList(),
    createdAt = nowMillis(),
    updatedAt = nowMillis()
)

fun createRandomMetaplexOffChainMeta(
    mint: String = randomMint()
): MetaplexOffChainMeta = MetaplexOffChainMeta(
    tokenAddress = mint,
    metaFields = createRandomMetaplexOffChainMetaFields(),
    loadedAt = nowMillis()
)

fun createRandomMetaplexMetaFields() = MetaplexMetaFields(
    name = randomString(),
    symbol = randomString(),
    uri = randomUrl(),
    sellerFeeBasisPoints = 100,
    creators = listOf(createRandomTokenCreator()),
    collection = createRandomMetaplexMetaFieldsCollection()
)

fun createRandomMetaplexOffChainMetaFields(): MetaplexOffChainMetaFields {
    val collectionName = randomString()
    val collectionFamily = randomString()
    val creators = listOf(MetaplexTokenCreator(randomAccount(), 100, randomBoolean()))
    return MetaplexOffChainMetaFields(
        name = randomString(),
        symbol = randomString(),
        description = randomString(),
        collection = MetaplexOffChainMetaFields.Collection(
            name = collectionName,
            family = collectionFamily,
            hash = MetaplexOffChainCollectionHash.calculateCollectionHash(
                name = collectionName,
                family = collectionFamily,
                creators = creators.map { it.address }
            )
        ),
        sellerFeeBasisPoints = randomInt(),
        externalUrl = randomUrl(),
        edition = randomString(),
        backgroundColor = randomString(),
        attributes = listOf(
            MetaplexOffChainMetaFields.Attribute(
                traitType = randomString(),
                value = randomString()
            )
        ),
        properties = MetaplexOffChainMetaFields.Properties(
            category = randomString(),
            creators = creators,
            files = listOf(
                MetaplexOffChainMetaFields.Properties.File(
                    uri = randomString(),
                    type = randomString()
                )
            )
        ),
        image = randomString(),
        animationUrl = randomString()
    )
}

fun createRandomTokenMeta(): TokenMeta =
    TokenMeta(
        name = randomString(),
        description = randomString(),
        symbol = randomString(),
        url = randomUrl(),
        creators = listOf(createRandomTokenCreator()),
        sellerFeeBasisPoints = randomInt(),
        collection = createRandomTokenMetadataCollection(),
        attributes = listOf(createRandomTokenMetaAttribute()),
        contents = listOf(createRandomTokenMetaContent()),
        externalUrl = randomUrl()
    )

fun createRandomTokenMetaContent(): TokenMeta.Content =
    if (randomBoolean()) {
        TokenMeta.Content.ImageContent(
            url = randomUrl(),
            mimeType = null
        )
    } else {
        TokenMeta.Content.VideoContent(
            url = randomUrl(),
            mimeType = null
        )
    }

fun createRandomTokenMetaAttribute(): TokenMeta.Attribute =
    TokenMeta.Attribute(
        key = randomString(),
        value = randomString(),
        type = null,
        format = null
    )

fun createRandomTokenMetadataCollection(): TokenMeta.Collection =
    if (randomBoolean()) {
        TokenMeta.Collection.OnChain(
            address = randomAccount(),
            verified = randomBoolean()
        )
    } else {
        TokenMeta.Collection.OffChain(
            name = randomString(),
            family = randomString(),
            hash = randomString()
        )
    }

fun createRandomMetaplexMetaFieldsCollection(): MetaplexMetaFields.Collection =
    MetaplexMetaFields.Collection(
        address = randomAccount(),
        verified = randomBoolean()
    )

fun createRandomTokenCreator(): MetaplexTokenCreator =
    MetaplexTokenCreator(
        address = randomAccount(),
        share = randomInt(101),
        verified = randomBoolean()
    )

fun createRandomBalance(
    account: String = randomAccount(),
    owner: String = randomAccount(),
    mint: String = randomMint(),
    updatedAt: Instant = nowMillis(),
    value: BigInteger = randomBigInt()
): Balance = Balance(
    account = account,
    owner = owner,
    mint = mint,
    value = value,
    revertableEvents = emptyList(),
    createdAt = nowMillis(),
    updatedAt = updatedAt
)

fun createRandomBalanceWithMeta(
    account: String = randomAccount(),
    owner: String = randomAccount(),
    mint: String = randomMint(),
    updatedAt: Instant = nowMillis()
): BalanceWithMeta = BalanceWithMeta(
    balance = createRandomBalance(account = account, owner = owner, mint = mint, updatedAt = updatedAt),
    tokenMeta = createRandomTokenMeta()
)

fun randomUrl(): String = "https://test.com/" + randomString()

fun randomSolanaLog(): SolanaLog {
    return SolanaLog(
        blockNumber = randomLong(1_000_000_000_000),
        transactionHash = randomString(),
        blockHash = randomString(),
        transactionIndex = randomInt(1_000_000),
        instructionIndex = randomInt(1_000_000),
        innerInstructionIndex = randomInt(1_000_000)
    )
}

fun randomSellOrder(
    make: Asset = randomAsset(randomAssetTypeNft()),
    take: Asset = randomAsset(randomAssetTypeFt()),
    maker: String = randomAccount(),
    fill: BigInteger = randomBigInt(2),
    makerAccount: String = randomAccount()
): Order {
    return Order(
        auctionHouse = randomAccount(),
        auctionHouseSellerFeeBasisPoints = randomInt(10000),
        auctionHouseRequiresSignOff = randomBoolean(),
        maker = maker,
        makerAccount = makerAccount,
        status = OrderStatus.ACTIVE,
        make = make,
        take = take,
        takePrice = null,
        makeStock = make.amount,
        makePrice = randomBigDecimal(4, 2),
        fill = fill,
        createdAt = nowMillis(),
        updatedAt = nowMillis(),
        direction = OrderDirection.SELL,
        revertableEvents = emptyList(),
        states = emptyList(),
        dbUpdatedAt = nowMillis()
    )
}

fun randomBuyOrder(
    make: Asset = randomAsset(randomAssetTypeFt()),
    take: Asset = randomAsset(randomAssetTypeNft()),
): Order {
    return Order(
        auctionHouse = randomAccount(),
        auctionHouseSellerFeeBasisPoints = randomInt(10000),
        auctionHouseRequiresSignOff = randomBoolean(),
        maker = randomAccount(),
        makerAccount = randomAccount(),
        status = OrderStatus.ACTIVE,
        make = make,
        take = take,
        takePrice = randomBigDecimal(4, 2),
        makePrice = null,
        makeStock = make.amount,
        fill = randomBigInt(2),
        createdAt = nowMillis(),
        updatedAt = nowMillis(),
        direction = OrderDirection.BUY,
        revertableEvents = emptyList(),
        states = emptyList(),
        dbUpdatedAt = nowMillis()
    )
}

fun randomAsset(type: AssetType = randomAssetTypeNft()): Asset {
    return Asset(
        type = type,
        amount = randomBigInt(6)
    )
}

fun randomAssetTypeNft(): AssetType {
    return TokenNftAssetType(
        tokenAddress = randomMint()
    )
}

fun randomAssetTypeFt(): AssetType {
    return TokenFtAssetType(
        tokenAddress = randomMint()
    )
}

fun createAuctionHouse(order: Order): AuctionHouse {
    return AuctionHouse(
        account = order.auctionHouse,
        sellerFeeBasisPoints = 100,
        requiresSignOff = true,
        states = emptyList(),
        revertableEvents = emptyList(),
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH
    )
}

