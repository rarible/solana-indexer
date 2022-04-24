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
import com.rarible.protocol.solana.common.model.Balance
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
import com.rarible.protocol.solana.common.records.OrderDirection
import java.math.BigInteger
import java.time.Instant

fun createRandomToken(
    mint: String = randomString(),
    tokenMeta: TokenMeta? = createRandomTokenMeta(),
    updatedAt: Instant = nowMillis()
): Token = Token(
    mint = mint,
    supply = randomBigInt(),
    revertableEvents = emptyList(),
    decimals = randomInt(6),
    createdAt = updatedAt,
    updatedAt = updatedAt,
    tokenMeta = tokenMeta
)

fun createRandomMetaplexMeta(
    mint: String = randomString()
): MetaplexMeta = MetaplexMeta(
    metaAddress = randomString(),
    tokenAddress = mint,
    metaFields = createRandomMetaplexMetaFields(),
    isMutable = randomBoolean(),
    revertableEvents = emptyList(),
    createdAt = nowMillis(),
    updatedAt = nowMillis()
)

fun createRandomMetaplexOffChainMeta(
    mint: String = randomString()
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
    val creators = listOf(MetaplexTokenCreator(randomString(), 100, randomBoolean()))
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

fun createRandomTokenMeta(
    collection: TokenMeta.Collection = createRandomTokenMetaCollection(),
    creators: List<MetaplexTokenCreator> = listOf(createRandomTokenCreator()),
    sellerFeeBasisPoints: Int = randomInt()
): TokenMeta =
    TokenMeta(
        name = randomString(),
        description = randomString(),
        symbol = randomString(),
        url = randomUrl(),
        creators = creators,
        sellerFeeBasisPoints = sellerFeeBasisPoints,
        collection = collection,
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

fun createRandomTokenMetaCollection(): TokenMeta.Collection =
    if (randomBoolean()) {
        createRandomTokenMetaCollectionOnChain()
    } else {
        createRandomTokenMetaCollectionOffChain()
    }

fun createRandomTokenMetaCollectionOffChain() = TokenMeta.Collection.OffChain(
    name = randomString(),
    family = randomString(),
    hash = randomString()
)

fun createRandomTokenMetaCollectionOnChain() = TokenMeta.Collection.OnChain(
    address = randomString(),
    verified = randomBoolean()
)

fun createRandomMetaplexMetaFieldsCollection(): MetaplexMetaFields.Collection =
    MetaplexMetaFields.Collection(
        address = randomString(),
        verified = randomBoolean()
    )

fun createRandomTokenCreator(): MetaplexTokenCreator =
    MetaplexTokenCreator(
        address = randomString(),
        share = randomInt(101),
        verified = randomBoolean()
    )

fun createRandomBalance(
    account: String = randomString(),
    owner: String = randomString(),
    mint: String = randomString(),
    updatedAt: Instant = nowMillis(),
    value: BigInteger = randomBigInt()
): Balance = Balance(
    account = account,
    owner = owner,
    mint = mint,
    value = value,
    revertableEvents = emptyList(),
    createdAt = nowMillis(),
    updatedAt = updatedAt,
    tokenName = randomString(),
    collection = createRandomTokenMetaCollection()
)

fun randomUrl(): String = "https://test.com/" + randomString()

/**
 * [SolanaLog] used in tests as a placeholder in places where the log is not necessary to compare.
 */
val ANY_SOLANA_LOG = SolanaLog(
    blockNumber = 0L,
    transactionHash = "",
    blockHash = "",
    transactionIndex = 0,
    instructionIndex = 0,
    innerInstructionIndex = null
)

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
    maker: String = randomString(),
    fill: BigInteger = randomBigInt(2),
    makerAccount: String = randomString()
): Order {
    return Order(
        auctionHouse = randomString(),
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
        states = emptyList()
    )
}

fun randomBuyOrder(
    make: Asset = randomAsset(randomAssetTypeFt()),
    take: Asset = randomAsset(randomAssetTypeNft()),
): Order {
    return Order(
        auctionHouse = randomString(),
        maker = randomString(),
        makerAccount = randomString(),
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
        states = emptyList()
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
        tokenAddress = randomString()
    )
}

fun randomAssetTypeFt(): AssetType {
    return TokenFtAssetType(
        tokenAddress = randomString()
    )
}

