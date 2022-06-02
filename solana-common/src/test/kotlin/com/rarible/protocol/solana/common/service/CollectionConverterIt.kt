package com.rarible.protocol.solana.common.service

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.protocol.solana.common.converter.TokenMetaConverter
import com.rarible.protocol.solana.common.model.SolanaCollectionV1
import com.rarible.protocol.solana.common.model.SolanaCollectionV2
import com.rarible.protocol.solana.common.service.collection.CollectionConverter
import com.rarible.protocol.solana.dto.CollectionDto
import com.rarible.protocol.solana.dto.CollectionMetaDto
import com.rarible.protocol.solana.test.createRandomTokenMeta
import com.rarible.protocol.solana.test.randomMint
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CollectionConverterIt {

    @Test
    fun `to dto - v1`() = runBlocking<Unit> {
        val collection = SolanaCollectionV1(
            id = randomString(),
            name = randomString(),
            family = randomString(),
            createdAt = nowMillis(),
            updatedAt = nowMillis()
        )

        val dto = CollectionConverter.toDto(collection)

        assertThat(dto).isEqualTo(
            CollectionDto(
                address = collection.id,
                name = collection.name
            )
        )
    }

    @Test
    fun `to dto - v2`() = runBlocking<Unit> {
        val collectionMint = randomMint()
        val tokenMeta = createRandomTokenMeta()
        val collection = SolanaCollectionV2(
            id = collectionMint,
            createdAt = nowMillis(),
            updatedAt = nowMillis(),
            collectionMeta = tokenMeta
        )

        val dto = CollectionConverter.toDto(collection)

        assertThat(dto).isEqualTo(
            CollectionDto(
                address = collectionMint,
                parent = null,
                name = tokenMeta.name,
                symbol = tokenMeta.symbol,
                owner = null,
                features = emptyList(),
                creators = tokenMeta.creators.map { it.address },
                meta = CollectionMetaDto(
                    name = tokenMeta.name,
                    description = tokenMeta.description,
                    content = TokenMetaConverter.convert(tokenMeta).content,
                    externalLink = tokenMeta.externalUrl,
                    sellerFeeBasisPoints = tokenMeta.sellerFeeBasisPoints,
                    feeRecipient = null
                )
            )
        )
    }

}