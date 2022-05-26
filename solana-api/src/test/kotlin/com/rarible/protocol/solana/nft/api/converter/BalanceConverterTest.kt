package com.rarible.protocol.solana.nft.api.converter

import com.rarible.protocol.solana.common.converter.BalanceWithMetaConverter
import com.rarible.protocol.solana.common.model.BalanceWithMeta
import com.rarible.protocol.solana.common.pubkey.Keypair
import com.rarible.protocol.solana.common.pubkey.ProgramDerivedAddressCalc
import com.rarible.protocol.solana.test.createRandomBalance
import com.rarible.protocol.solana.test.createRandomTokenMeta
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class BalanceConverterTest {
    @Test
    fun `balance converter - associated token account and secondary account`() {
        val mint = Keypair.createRandom().publicKey
        val owner = Keypair.createRandom().publicKey
        val associatedTokenAccount = ProgramDerivedAddressCalc.getAssociatedTokenAccount(mint, owner).address
        val secondaryAccount = Keypair.createRandom().publicKey
        val associatedTokenBalance = createRandomBalance(
            account = associatedTokenAccount.toBase58(),
            owner = owner.toBase58(),
            mint = mint.toBase58()
        )
        val secondaryBalance = createRandomBalance(
            account = secondaryAccount.toBase58(),
            owner = owner.toBase58(),
            mint = mint.toBase58()
        )
        Assertions.assertThat(
            BalanceWithMetaConverter.convert(
                BalanceWithMeta(
                    associatedTokenBalance,
                    createRandomTokenMeta()
                )
            ).isAssociatedTokenAccount
        ).isTrue
        Assertions.assertThat(
            BalanceWithMetaConverter.convert(
                BalanceWithMeta(
                    secondaryBalance,
                    createRandomTokenMeta()
                )
            ).isAssociatedTokenAccount
        ).isFalse
    }
}