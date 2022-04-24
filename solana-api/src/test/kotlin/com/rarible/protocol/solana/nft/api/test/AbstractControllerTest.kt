package com.rarible.protocol.solana.nft.api.test

import com.rarible.protocol.solana.api.client.BalanceControllerApi
import com.rarible.protocol.solana.api.client.FixedSolanaApiServiceUriProvider
import com.rarible.protocol.solana.api.client.NoopWebClientCustomizer
import com.rarible.protocol.solana.api.client.OrderControllerApi
import com.rarible.protocol.solana.api.client.SolanaNftIndexerApiClientFactory
import com.rarible.protocol.solana.api.client.TokenControllerApi
import com.rarible.protocol.solana.common.meta.MetaplexOffChainMetaLoader
import com.rarible.protocol.solana.common.repository.MetaplexMetaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.net.URI
import javax.annotation.PostConstruct

@Import(ControllersTestConfiguration::class)
abstract class AbstractControllerTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var testMetaplexOffChainMetaLoader: MetaplexOffChainMetaLoader

    @Autowired
    lateinit var metaplexMetaRepository: MetaplexMetaRepository

    protected lateinit var tokenControllerApi: TokenControllerApi

    protected lateinit var balanceControllerApi: BalanceControllerApi

    protected lateinit var orderControllerApi: OrderControllerApi

    @PostConstruct
    fun setup() {
        val urlProvider = FixedSolanaApiServiceUriProvider(URI.create("http://127.0.0.1:$port"))
        val clientFactory = SolanaNftIndexerApiClientFactory(urlProvider, NoopWebClientCustomizer())
        tokenControllerApi = clientFactory.createTokenControllerApiClient()
        balanceControllerApi = clientFactory.createBalanceControllerApiClient()
        orderControllerApi = clientFactory.createOrderControllerApiClient()
    }
}
