package com.rarible.protocol.solana.nft.listener.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.solana.nft.listener.model.AccountToMintAssociation
import com.rarible.protocol.solana.nft.listener.repository.AccountToMintAssociationRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AccountToMintAssociationService(
    private val accountToMintAssociationRepository: AccountToMintAssociationRepository,
    @Autowired(required = false) private val accountToMintAssociationCache: AccountToMintAssociationCache?
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureSpan(type = SpanType.APP)
    suspend fun getMintsByAccounts(accounts: Collection<String>): Map<String, String> {
        val fromCache = accountToMintAssociationCache?.getMintsByAccounts(accounts).orEmpty()
        if (fromCache.size == accounts.size) {
            logger.info("Account to mint cache hit: {} of {}", fromCache.size, accounts.size)
            return fromCache
        }

        val notCached = accounts.filterNot { fromCache.containsKey(it) }

        val fromDb = HashMap<String, String>()
        accountToMintAssociationRepository.findAll(notCached)
            .forEach { fromDb[it.account] = it.mint }

        accountToMintAssociationCache?.saveMintsByAccounts(fromDb)

        logger.info("Account to mint cache hit: {} of {}, {} found in DB", fromCache.size, accounts.size, fromDb.size)
        return fromCache + fromDb
    }

    @CaptureSpan(type = SpanType.APP)
    suspend fun saveMintsByAccounts(associations: Map<String, String>) {
        accountToMintAssociationRepository.saveAll(associations.map { AccountToMintAssociation(it.key, it.value) })
        accountToMintAssociationCache?.saveMintsByAccounts(associations)
    }

}
