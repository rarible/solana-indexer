package com.rarible.protocol.solana.common.update

import com.rarible.core.kafka.KafkaMessage
import com.rarible.protocol.solana.common.util.getNftMint
import com.rarible.protocol.solana.dto.ActivityDto
import com.rarible.protocol.solana.dto.BalanceEventDto
import com.rarible.protocol.solana.dto.CollectionEventDto
import com.rarible.protocol.solana.dto.OrderEventDto
import com.rarible.protocol.solana.dto.OrderUpdateEventDto
import com.rarible.protocol.solana.dto.SolanaEventTopicProvider
import com.rarible.protocol.solana.dto.TokenEventDto
import com.rarible.protocol.solana.dto.TokenMetaEventDto
import java.util.*

object KafkaEventFactory {

    private val TOKEN_EVENT_HEADERS = mapOf(
        "protocol.solana.token.event.version" to SolanaEventTopicProvider.VERSION
    )
    private val TOKEN_META_EVENT_HEADERS = mapOf(
        "protocol.solana.token.meta.event.version" to SolanaEventTopicProvider.VERSION
    )
    private val BALANCE_EVENT_HEADERS = mapOf(
        "protocol.solana.balance.event.version" to SolanaEventTopicProvider.VERSION
    )
    private val ORDER_EVENT_HEADERS = mapOf(
        "protocol.solana.order.event.version" to SolanaEventTopicProvider.VERSION
    )
    private val COLLECTION_EVENT_HEADERS = mapOf(
        "protocol.solana.collection.event.version" to SolanaEventTopicProvider.VERSION
    )
    private val ACTIVITY_EVENT_HEADERS = mapOf(
        "protocol.solana.activity.event.version" to SolanaEventTopicProvider.VERSION
    )

    fun tokenEvent(dto: TokenEventDto): KafkaMessage<TokenEventDto> =
        KafkaMessage(
            id = dto.eventId,
            key = dto.address,
            value = dto,
            headers = TOKEN_EVENT_HEADERS
        )

    fun tokenMetaEvent(dto: TokenMetaEventDto): KafkaMessage<TokenMetaEventDto> =
        KafkaMessage(
            id = UUID.randomUUID().toString(),
            key = dto.tokenAddress,
            value = dto,
            headers = TOKEN_META_EVENT_HEADERS
        )

    fun balanceEvent(dto: BalanceEventDto): KafkaMessage<BalanceEventDto> {
        return KafkaMessage(
            id = dto.eventId,
            key = dto.mint,
            value = dto,
            headers = BALANCE_EVENT_HEADERS
        )
    }

    fun orderEvent(dto: OrderEventDto): KafkaMessage<OrderEventDto> {
        val nftMint = when (dto) {
            is OrderUpdateEventDto -> dto.order.take.type.getNftMint() ?: dto.order.make.type.getNftMint()
        }
        return KafkaMessage(
            id = dto.eventId,
            key = nftMint ?: dto.orderId, // If there is no NFT related to order, key doesn't matter
            value = dto,
            headers = ORDER_EVENT_HEADERS
        )
    }

    fun collectionEvent(dto: CollectionEventDto): KafkaMessage<CollectionEventDto> {
        return KafkaMessage(
            id = dto.eventId,
            key = dto.collectionId,
            value = dto,
            headers = COLLECTION_EVENT_HEADERS
        )
    }

    fun activityEvent(dto: ActivityDto): KafkaMessage<ActivityDto> {
        return KafkaMessage(
            id = UUID.randomUUID().toString(),
            key = dto.getNftMint() ?: dto.id,
            value = dto,
            headers = ACTIVITY_EVENT_HEADERS
        )
    }
}
