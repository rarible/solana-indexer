package com.rarible.protocol.solana.common.service.collection

import com.rarible.protocol.solana.common.converter.TokenMetaConverter
import com.rarible.protocol.solana.common.meta.TokenMeta
import com.rarible.protocol.solana.common.model.SolanaCollection
import com.rarible.protocol.solana.common.model.SolanaCollectionV1
import com.rarible.protocol.solana.common.model.SolanaCollectionV2
import com.rarible.protocol.solana.dto.CollectionDto
import com.rarible.protocol.solana.dto.CollectionMetaDto

object CollectionConverter {

    fun toDto(collection: SolanaCollection): CollectionDto? {
        return when (collection) {
            is SolanaCollectionV1 -> convertV1(collection)
            is SolanaCollectionV2 -> convertV2(collection)
        }
    }

    private fun convertV1(collection: SolanaCollectionV1): CollectionDto = CollectionDto(
        address = collection.id,
        name = collection.name,
        features = emptyList()
    )

    private fun convertV2(collection: SolanaCollectionV2): CollectionDto? {
        val collectionMeta = collection.collectionMeta ?: return null
        return CollectionDto(
            address = collection.id,
            name = collectionMeta.name,
            symbol = collectionMeta.symbol,
            features = emptyList(),
            creators = collectionMeta.creators.map { it.address },
            meta = convertCollectionMeta(collectionMeta)
        )
    }

    fun convertCollectionMeta(tokenMeta: TokenMeta) = CollectionMetaDto(
        name = tokenMeta.name,
        externalLink = tokenMeta.externalUrl,
        sellerFeeBasisPoints = tokenMeta.sellerFeeBasisPoints,
        feeRecipient = null,
        description = tokenMeta.description,
        content = TokenMetaConverter.convert(tokenMeta).content
    )
}