package com.rarible.protocol.solana.common.converter

import com.rarible.protocol.solana.common.model.Balance
import com.rarible.protocol.solana.dto.BalanceDeleteEventDto
import com.rarible.protocol.solana.dto.BalanceEventDto
import com.rarible.protocol.solana.dto.BalanceUpdateEventDto
import java.math.BigInteger
import java.util.*

object BalanceEventConverter {

    fun convert(balance: Balance): BalanceEventDto {
        val eventId = UUID.randomUUID().toString()
        return if (balance.value > BigInteger.ZERO) {
            BalanceUpdateEventDto(
                eventId = eventId,
                mint = balance.mint,
                account = balance.account,
                balance = BalanceConverter.convert(balance)
            )
        } else {
            BalanceDeleteEventDto(
                eventId = eventId,
                mint = balance.mint,
                account = balance.account,
                balance = BalanceConverter.convert(balance)
            )
        }
    }
}
