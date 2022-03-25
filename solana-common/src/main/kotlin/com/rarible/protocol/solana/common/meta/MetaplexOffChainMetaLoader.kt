package com.rarible.protocol.solana.common.meta

import com.rarible.protocol.solana.common.configuration.SolanaIndexerProperties
import com.rarible.protocol.solana.common.model.MetaplexOffChainMeta
import com.rarible.protocol.solana.common.model.TokenId
import com.rarible.protocol.solana.common.repository.MetaplexOffChainMetaRepository
import com.rarible.protocol.solana.common.service.CollectionService
import com.rarible.protocol.solana.common.util.nowMillis
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.bodyToMono
import java.net.URL
import java.time.Clock
import java.time.Duration

@Component
class MetaplexOffChainMetaLoader(
    private val metaplexOffChainMetaRepository: MetaplexOffChainMetaRepository,
    private val collectionService: CollectionService,
    private val externalHttpClient: ExternalHttpClient,
    private val metaMetrics: MetaMetrics,
    private val solanaIndexerProperties: SolanaIndexerProperties,
    private val clock: Clock
) {

    private companion object {

        private val logger = LoggerFactory.getLogger(MetaplexOffChainMetaLoader::class.java)
    }

    private suspend fun loadOffChainMetadataJson(metadataUrl: URL): String =
        externalHttpClient
            .get(metadataUrl)
            .bodyToMono<String>()
            // TODO[meta]: limit the size of the loaded JSON.
            .timeout(Duration.ofMillis(solanaIndexerProperties.metaplexOffChainMetaLoadingTimeout))
            .awaitFirst()

    suspend fun loadMetaplexOffChainMeta(tokenAddress: TokenId, metadataUrl: URL): MetaplexOffChainMeta? {
        // TODO: when meta gets changed, we have to send a token update event.
        logger.info("Loading off-chain metadata for token $tokenAddress by URL $metadataUrl")
        val offChainMetadataJsonContent = try {
            loadOffChainMetadataJson(metadataUrl)
        } catch (e: Exception) {
            logger.error("Failed to load metadata for token $tokenAddress by URL $metadataUrl", e)
            metaMetrics.onMetaLoadingError()
            return null
        }
        val metaplexOffChainMetaFields = try {
            MetaplexOffChainMetadataParser.parseMetaplexOffChainMetaFields(offChainMetadataJsonContent)
        } catch (e: Exception) {
            logger.error("Failed to parse metadata for token $tokenAddress by URL $metadataUrl", e)
            metaMetrics.onMetaParsingError()
            return null
        }
        val metaplexOffChainMeta = MetaplexOffChainMeta(
            tokenAddress = tokenAddress,
            metaFields = metaplexOffChainMetaFields,
            loadedAt = clock.nowMillis()
        )
        collectionService.updateCollectionV1(metaplexOffChainMeta)
        return metaplexOffChainMetaRepository.save(metaplexOffChainMeta)
    }

}
