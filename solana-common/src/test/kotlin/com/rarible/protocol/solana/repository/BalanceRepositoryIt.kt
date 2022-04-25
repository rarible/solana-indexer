package com.rarible.protocol.solana.repository

import com.rarible.core.common.nowMillis
import com.rarible.protocol.solana.AbstractIntegrationTest
import com.rarible.protocol.solana.common.repository.BalanceRepository
import com.rarible.protocol.solana.common.util.toBigInteger
import com.rarible.protocol.solana.test.createRandomBalance
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger

class BalanceRepositoryIt : AbstractIntegrationTest() {

    @Test
    fun `save and find by account`() = runBlocking<Unit> {
        val balance = balanceRepository.save(createRandomBalance())
        assertThat(balanceRepository.findByAccount(balance.account)).isEqualTo(balance)
    }

    @Test
    fun `find by owner`() = runBlocking<Unit> {
        val old = nowMillis().minusSeconds(1)
        val balance1 = balanceRepository.save(createRandomBalance(updatedAt = old))
        val balance2 = balanceRepository.save(createRandomBalance(mint = balance1.mint, owner = balance1.owner))
        balanceRepository.save(createRandomBalance(mint = balance1.mint))

        val expected = listOf(balance1, balance2).sortedByDescending { it.updatedAt }
        val result = balanceRepository.findByOwner(balance1.owner, null, 100, true).toList()

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `find by owner - without deleted`() = runBlocking<Unit> {
        val balance = balanceRepository.save(createRandomBalance(value = BigInteger.ZERO))

        val result = balanceRepository.findByOwner(balance.owner, null, 100, false)
            .toList()

        assertThat(result).hasSize(0)
    }

    @Test
    fun `find by mint`() = runBlocking<Unit> {
        val old = nowMillis().minusSeconds(1)
        val balance = balanceRepository.save(createRandomBalance(updatedAt = old))
        val balance2 = balanceRepository.save(createRandomBalance(mint = balance.mint, owner = balance.owner))
        balanceRepository.save(createRandomBalance(owner = balance.owner))

        val expected = listOf(balance, balance2).sortedByDescending { it.updatedAt }

        val result = balanceRepository.findByMint(balance.mint, null, 100, true).toList()

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `find by mint - without deleted`() = runBlocking<Unit> {
        val balance = balanceRepository.save(createRandomBalance(value = BigInteger.ZERO))

        val result = balanceRepository.findByMint(balance.mint, null, 100, false).toList()

        assertThat(result).hasSize(0)
    }

    @Test
    fun `find by mint and owner`() = runBlocking<Unit> {
        val balance1 = balanceRepository.save(createRandomBalance())
        val balance2 = balanceRepository.save(createRandomBalance(mint = balance1.mint, owner = balance1.owner))
        balanceRepository.save(createRandomBalance(owner = balance1.owner))

        val expected = setOf(balance1, balance2)

        val result = balanceRepository.findByMintAndOwner(balance1.mint, balance1.owner, true).toSet()

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `find by mint and owner - without deleted`() = runBlocking<Unit> {
        val balance = balanceRepository.save(createRandomBalance(value = BigInteger.ZERO))

        val result = balanceRepository.findByMintAndOwner(balance.mint, balance.owner, false).toList()

        assertThat(result).hasSize(0)
    }

    @Test
    fun `save with max ULong value and find by account`() = runBlocking<Unit> {
        val balance = createRandomBalance().copy(value = ULong.MAX_VALUE.toBigInteger())
        balanceRepository.save(balance)
        assertThat(balanceRepository.findByAccount(balance.account)).isEqualTo(balance)
    }

}
