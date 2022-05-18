package com.rarible.protocol.solana.common.continuation

import com.rarible.protocol.solana.dto.OrderDto

object OrderContinuation {

    object ByLastUpdatedAndIdAsc : ContinuationFactory<OrderDto, DateIdContinuation> {

        override fun getContinuation(entity: OrderDto): DateIdContinuation {
            return DateIdContinuation(entity.updatedAt, entity.hash, true)
        }
    }

    object ByLastUpdatedAndIdDesc : ContinuationFactory<OrderDto, DateIdContinuation> {

        override fun getContinuation(entity: OrderDto): DateIdContinuation {
            return DateIdContinuation(entity.updatedAt, entity.hash, false)
        }
    }

    object ByDbUpdatedAndIdAsc : ContinuationFactory<OrderDto, DateIdContinuation> {

        override fun getContinuation(entity: OrderDto): DateIdContinuation {
            return DateIdContinuation(entity.dbUpdatedAt ?: entity.updatedAt, entity.hash, true)
        }
    }

    object ByDbUpdatedAndIdDesc : ContinuationFactory<OrderDto, DateIdContinuation> {

        override fun getContinuation(entity: OrderDto): DateIdContinuation {
            return DateIdContinuation(entity.dbUpdatedAt?:entity.updatedAt, entity.hash, false)
        }
    }

    object BySellPriceAndIdAsc : ContinuationFactory<OrderDto, PriceIdContinuation> {

        override fun getContinuation(entity: OrderDto): PriceIdContinuation {
            return PriceIdContinuation(
                entity.makePrice,
                entity.hash,
                true
            )
        }
    }

    object ByBuyPriceAndIdDesc : ContinuationFactory<OrderDto, PriceIdContinuation> {

        override fun getContinuation(entity: OrderDto): PriceIdContinuation {
            return PriceIdContinuation(
                entity.takePrice,
                entity.hash,
                false
            )
        }
    }
}