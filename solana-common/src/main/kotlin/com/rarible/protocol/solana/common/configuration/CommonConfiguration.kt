package com.rarible.protocol.solana.common.configuration

import com.rarible.protocol.solana.common.converter.PackageConverters
import com.rarible.protocol.solana.common.filter.auctionHouse.SolanaAuctionHouseFilter
import com.rarible.protocol.solana.common.filter.token.CompositeSolanaTokenFilter
import com.rarible.protocol.solana.common.filter.token.CurrencyTokenReader
import com.rarible.protocol.solana.common.filter.token.InMemoryCachingSolanaTokenFilter
import com.rarible.protocol.solana.common.filter.token.SolanaTokenFilter
import com.rarible.protocol.solana.common.filter.token.SolanaWhiteListOnlyUpdatableTokenFilter
import com.rarible.protocol.solana.common.filter.token.StaticSolanaBlackListTokenFilter
import com.rarible.protocol.solana.common.filter.token.TokenListFileReader
import com.rarible.protocol.solana.common.filter.token.dynamic.DynamicBlacklistSolanaTokenFilter
import com.rarible.protocol.solana.common.repository.DynamicBlacklistedTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.time.Clock

@Configuration
@Import(
    RepositoryConfiguration::class,
    EventProducerConfiguration::class,
    SolanaMetaConfiguration::class,
    SolanaServiceConfiguration::class,
    InternalEventProducerConfiguration::class
)
@EnableConfigurationProperties(SolanaIndexerProperties::class)
@ComponentScan(basePackageClasses = [PackageConverters::class])
class CommonConfiguration(
    private val properties: SolanaIndexerProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun solanaClock(): Clock = Clock.systemUTC()

    @Bean
    fun featureFlags(): FeatureFlags {
        logger.info("Activated feature flags: {}", properties.featureFlags)
        return properties.featureFlags
    }

    @Bean
    fun tokenFilter(
        featureFlags: FeatureFlags,
        dynamicBlacklistedTokenRepository: DynamicBlacklistedTokenRepository,
        solanaIndexerProperties: SolanaIndexerProperties
    ): SolanaTokenFilter {
        return when (featureFlags.tokenFilter) {
            TokenFilterType.NONE -> {
                object : SolanaTokenFilter {
                    override suspend fun isAcceptableToken(mint: String): Boolean = true

                    override suspend fun addToBlacklist(mintsAndReasons: Map<String, String>) = Unit
                }
            }
            TokenFilterType.BLACKLIST, TokenFilterType.WHITELIST_V2 -> {
                val blacklistTokens = featureFlags.blacklistTokens
                val coinTokens = CurrencyTokenReader().readCurrencyTokens().tokens.mapTo(hashSetOf()) { it.address }

                val whitelistFilter = if (featureFlags.tokenFilter == TokenFilterType.WHITELIST_V2) {
                    val whitelistTokens = TokenListFileReader("/whitelist_v2").readTokens(WHITELIST_FILES)
                    listOf(SolanaWhiteListOnlyUpdatableTokenFilter(whitelistTokens))
                } else {
                    emptyList()
                }

                CompositeSolanaTokenFilter(
                    listOf(
                        StaticSolanaBlackListTokenFilter(
                            blacklistedTokens = blacklistTokens + coinTokens
                        ),
                        InMemoryCachingSolanaTokenFilter(
                            delegate = DynamicBlacklistSolanaTokenFilter(
                                dynamicBlacklistedTokenRepository = dynamicBlacklistedTokenRepository
                            ),
                            cacheMaxSize = solanaIndexerProperties.featureFlags.tokenFilterInMemoryCacheSize
                        )
                    ) + whitelistFilter
                )
            }
        }
    }

    @Bean
    fun auctionHouseFilter(featureFlags: FeatureFlags): SolanaAuctionHouseFilter {
        val auctionHouses = featureFlags.auctionHouses
        return object : SolanaAuctionHouseFilter {
            override fun isAcceptableAuctionHouse(auctionHouse: String): Boolean =
                auctionHouses.isEmpty() || auctionHouse in auctionHouses
        }
    }

    private companion object {
        val WHITELIST_FILES = listOf(
            "degenape",
            "degenerate_ape_kindergarten",
            "degeneratetrashpandas"
        )
    }
}
