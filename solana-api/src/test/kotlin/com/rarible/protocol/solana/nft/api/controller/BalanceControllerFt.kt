package com.rarible.protocol.solana.nft.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomString
import com.rarible.protocol.solana.common.converter.BalanceConverter
import com.rarible.protocol.solana.common.repository.BalanceRepository
import com.rarible.protocol.solana.dto.BalancesDto
import com.rarible.protocol.solana.nft.api.test.AbstractControllerTest
import com.rarible.protocol.solana.test.createRandomBalance
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BalanceControllerFt : AbstractControllerTest() {

    @Autowired
    private lateinit var balanceRepository: BalanceRepository

    @Test
    fun `find balance by mint and owner`() = runBlocking<Unit> {
        val balance = balanceRepository.save(createRandomBalance())

        val expected = BalanceConverter.convert(balance)

        val result = balanceControllerApi.getBalanceByMintAndOwner(
            /* mint = */ balance.mint,
            /* owner = */ balance.owner
        ).awaitFirst()

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `find balances by owner - first page`() = runBlocking<Unit> {
        val now = nowMillis()
        val owner = randomString()

        val balance1 = balanceRepository.save(createRandomBalance(owner = owner, updatedAt = now))
        val balance2 = balanceRepository.save(createRandomBalance(owner = owner, updatedAt = now.minusSeconds(1)))
        // different owner
        balanceRepository.save(createRandomBalance())

        val expected = BalancesDto(
            balances = listOf(balance1, balance2).map { BalanceConverter.convert(it) },
            continuation = null
        )

        val result = balanceControllerApi.getBalanceByOwner(owner, null, 50).awaitFirst()

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `find balances by owner - second page`() = runBlocking<Unit> {
        val owner = randomString()
        val updatedAt = nowMillis()
        val balance = balanceRepository.save(createRandomBalance(owner = owner, updatedAt = updatedAt))

        val pageBreakUpdatedAt = updatedAt.plusSeconds(1)
        // Same TS as in continuation, should be included by ID filter
        val pageBreakBalance =
            balanceRepository.save(createRandomBalance(owner = owner, updatedAt = pageBreakUpdatedAt))

        val continuation = "${pageBreakUpdatedAt.toEpochMilli()}_zzzzzzzz" // zzz... is 'greater' than any other str

        val expected = BalancesDto(
            balances = listOf(pageBreakBalance, balance).map { BalanceConverter.convert(it) },
            continuation = "${updatedAt.toEpochMilli()}_${balance.account}"
        )

        val result = balanceControllerApi.getBalanceByOwner(owner, continuation, 2).awaitFirst()
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `find balances by mint`() = runBlocking<Unit> {
        val now = nowMillis()
        val mint = randomString()

        val balance1 = balanceRepository.save(createRandomBalance(mint = mint, updatedAt = now))
        val balance2 = balanceRepository.save(createRandomBalance(mint = mint, updatedAt = now.minusSeconds(1)))
        // different mint
        balanceRepository.save(createRandomBalance())

        val expected = BalancesDto(
            balances = listOf(balance1, balance2).map { BalanceConverter.convert(it) },
            continuation = null
        )

        assertThat(balanceControllerApi.getBalanceByMint(mint, null, 50).awaitFirst())
            .isEqualTo(expected)
    }

    @Test
    fun `find balances by mint - second page`() = runBlocking<Unit> {
        val mint = randomString()
        val updatedAt = nowMillis()
        val balance = balanceRepository.save(createRandomBalance(mint = mint, updatedAt = updatedAt))

        // From prev page, should be not included into result
        val prevUpdatedAt = updatedAt.plusSeconds(1)
        val prevBalance = balanceRepository.save(createRandomBalance(mint = mint, updatedAt = prevUpdatedAt))

        val continuation = "${prevUpdatedAt.toEpochMilli()}_${prevBalance.account}"

        val expected = BalancesDto(
            balances = listOf(BalanceConverter.convert(balance)),
            continuation = "${updatedAt.toEpochMilli()}_${balance.account}"
        )

        assertThat(balanceControllerApi.getBalanceByMint(mint, continuation, 1).awaitFirst())
            .isEqualTo(expected)
    }

}
