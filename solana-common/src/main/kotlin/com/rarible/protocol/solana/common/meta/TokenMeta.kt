package com.rarible.protocol.solana.common.meta

import com.rarible.protocol.solana.common.model.MetaplexTokenCreator

/**
 * Metaplex aggregated meta of on-chain and off-chain meta.
 */
data class TokenMeta(
    val name: String,
    val symbol: String,
    val description: String?,
    val creators: List<MetaplexTokenCreator>,
    val collection: Collection?,
    val sellerFeeBasisPoints: Int,
    val url: String,
    val attributes: List<Attribute>?,
    val contents: List<Content>,
    val externalUrl: String?
) {
    sealed class Content {
        abstract val url: String
        abstract val mimeType: String?

        data class ImageContent(
            override val url: String,
            override val mimeType: String?
        ) : Content()

        data class VideoContent(
            override val url: String,
            override val mimeType: String?
        ) : Content()
    }

    data class Attribute(
        val key: String,
        val value: String,
        // TODO[meta]: these are not used yet.
        val type: String?,
        val format: String?
    )

    sealed class Collection {
        data class OnChain(
            val address: String,
            val verified: Boolean
        ) : Collection()

        data class OffChain(
            val name: String,
            val family: String?,
            val hash: String
        ) : Collection()
    }
}
