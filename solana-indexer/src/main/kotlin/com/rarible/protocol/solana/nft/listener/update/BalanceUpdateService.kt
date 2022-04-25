package com.rarible.protocol.solana.nft.listener.update

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.solana.common.meta.TokenMetaGetService
import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.common.model.BalanceId
import com.rarible.protocol.solana.common.model.isEmpty
import com.rarible.protocol.solana.common.repository.BalanceRepository
import com.rarible.protocol.solana.common.update.BalanceUpdateListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BalanceUpdateService(
    private val balanceRepository: BalanceRepository,
    private val balanceUpdateListener: BalanceUpdateListener,
    private val orderMakeStockBalanceUpdateService: OrderMakeStockBalanceUpdateService,
    private val tokenMetaGetService: TokenMetaGetService
) : EntityService<BalanceId, Balance> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun get(id: BalanceId): Balance? =
        balanceRepository.findByAccount(id)

    override suspend fun update(entity: Balance): Balance {
        if (entity.isEmpty) {
            logger.info("Balance without Initialize record, skipping it: {}", entity)
            return entity
        }
        val enriched = entity.checkForUpdates()
        val existing = balanceRepository.findByAccount(enriched.account)
        if (!shouldUpdate(enriched, existing)) {
            // Nothing changed in the balance
            logger.info("Balance $enriched is not changed, skipping save")
            return enriched
        }

        val balance = balanceRepository.save(enriched)
        logger.info("Updated balance: $balance")
        balanceUpdateListener.onBalanceChanged(balance)
        if (existing?.value != balance.value) {
            orderMakeStockBalanceUpdateService.updateMakeStockOfSellOrders(balance)
        }
        return balance
    }

    private suspend fun Balance.checkForUpdates(): Balance =
        updateTokenMeta()

    private suspend fun Balance.updateTokenMeta(): Balance {
        val tokenMeta = tokenMetaGetService.getTokenMeta(mint) ?: return this
        return copy(
            tokenName = tokenMeta.name,
            collection = tokenMeta.collection
        )
    }

    private fun shouldUpdate(updated: Balance, existing: Balance?): Boolean {
        if (existing == null) return true

        // If nothing changed except updateAt, there is no sense to publish events
        return existing != updated.copy(updatedAt = existing.updatedAt)
    }

}
