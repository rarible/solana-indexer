package com.rarible.protocol.solana.nft.listener

import com.rarible.blockchain.scanner.block.Block
import com.rarible.blockchain.scanner.block.BlockRepository
import com.rarible.blockchain.scanner.block.BlockStatus
import com.rarible.blockchain.scanner.solana.client.SolanaClient
import com.rarible.blockchain.scanner.solana.model.SolanaLogRecord
import com.rarible.core.test.containers.KGenericContainer
import com.rarible.core.test.ext.KafkaTest
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.core.test.ext.RedisTest
import com.rarible.protocol.solana.common.meta.MetaplexOffChainMetaLoader
import com.rarible.protocol.solana.common.meta.TokenMetaGetService
import com.rarible.protocol.solana.common.meta.TokenMetaService
import com.rarible.protocol.solana.common.repository.BalanceRepository
import com.rarible.protocol.solana.common.repository.MetaplexMetaRepository
import com.rarible.protocol.solana.common.repository.MetaplexOffChainMetaRepository
import com.rarible.protocol.solana.common.repository.OrderRepository
import com.rarible.protocol.solana.common.repository.TokenRepository
import com.rarible.protocol.solana.nft.listener.service.subscribers.SolanaProgramId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.Testcontainers.exposeHostPorts
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.math.BigInteger

@MongoTest
@MongoCleanup
@ContextConfiguration(classes = [TestSolanaScannerConfiguration::class])
@SpringBootTest(
    classes = [NftListenerApplication::class],
    webEnvironment = WebEnvironment.DEFINED_PORT,
    properties = [
        "application.environment = test",
        "spring.cloud.consul.config.enabled = false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "logging.logstash.tcp-socket.enabled = false"
    ]
)
@ActiveProfiles("test")
@KafkaTest
@RedisTest
@Testcontainers
abstract class AbstractBlockScannerTest {
    protected val baseKeypair = "/home/solana/.config/solana/id.json"

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    private lateinit var client: SolanaClient

    @Autowired
    protected lateinit var metaplexMetaRepository: MetaplexMetaRepository

    @Autowired
    protected lateinit var tokenMetaService: TokenMetaService

    @Autowired
    protected lateinit var tokenMetaGetService: TokenMetaGetService

    @Qualifier("test.solana.meta.loader")
    @Autowired
    protected lateinit var testMetaplexOffChainMetaLoader: MetaplexOffChainMetaLoader

    @Autowired
    @Qualifier("test.metaplex.off.chain.meta.repository")
    protected lateinit var metaplexOffChainMetaRepository: MetaplexOffChainMetaRepository

    @Autowired
    protected lateinit var tokenRepository: TokenRepository

    @Autowired
    protected lateinit var balanceRepository: BalanceRepository

    @Autowired
    protected lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var repository: BlockRepository

    @BeforeEach
    fun cleanDatabase() = runBlocking<Unit> {
        val slot = client.getLatestSlot()
        val currentBlock = client.getBlock(slot) ?: error("Can't get latest block")

        runCatching {
            repository.save(
                Block(
                    id = currentBlock.number,
                    hash = currentBlock.hash,
                    parentHash = currentBlock.parentHash,
                    timestamp = currentBlock.timestamp,
                    status = BlockStatus.SUCCESS
                )
            )
        }
    }

    @BeforeEach
    fun cleanupMocks() {
        clearMocks(metaplexOffChainMetaRepository)
        coJustRun { metaplexOffChainMetaRepository.createIndexes() }
        coEvery { metaplexOffChainMetaRepository.findByTokenAddress(any()) } returns null
        coEvery { metaplexOffChainMetaRepository.save(any()) } throws (UnsupportedOperationException())
    }

    protected fun airdrop(sol: Int, address: String): String {
        val args = buildList {
            add("solana")
            add("airdrop")
            add("$sol")
            add(address)
        }

        return processOperation(args) { it.parse(0, -1) }
    }

    protected fun getBalance(address: String): BigDecimal {
        val args = buildList {
            add("solana")
            add("balance")
            add(address)
        }

        return processOperation(args) { it.parse(0, 0) }.toBigDecimal()
    }

    protected fun createAuctionHouse(keypair: String): AuctionHouse {
        val args = buildList {
            add("ts-node")
            add("/home/solana/metaplex/js/packages/cli/src/auction-house-cli.ts")
            add("create_auction_house")
            add("-sfbp")
            add("1000")
            add("-ccsp")
            add("false")
            add("-rso")
            add("-false")
            add("--keypair")
            add(keypair)
        }

        val house = processOperation(args) { it.parse(4, -1) }

        return getAuctionHouse(house, keypair)
    }

    protected fun getAuctionHouse(auctionHouse: String, keypair: String): AuctionHouse {
        val args = buildList {
            add("ts-node")
            add("/home/solana/metaplex/js/packages/cli/src/auction-house-cli.ts")
            add("show")
            add("--auction-house")
            add(auctionHouse)
            add("--keypair")
            add(keypair)
        }

        return processOperation(args) { it.parse() }
    }

    protected fun updateAuctionHouse(auctionHouse: String, keypair: String) {
        val args = buildList {
            add("ts-node")
            add("/home/solana/metaplex/js/packages/cli/src/auction-house-cli.ts")
            add("update_auction_house")
            add("--auction-house")
            add(auctionHouse)
            add("--keypair")
            add(keypair)
        }

        return processOperation(args) { it.parse(4, -1) }
    }

    protected fun sell(
        auctionHouse: String,
        sellerKeypair: String,
        buyPrice: Long,
        mint: String,
        amount: Long,
    ) {
        val args = buildList {
            add("ts-node")
            add("/home/solana/metaplex/js/packages/cli/src/auction-house-cli.ts")
            add("sell")
            add("--auction-house")
            add(auctionHouse)
            add("--buy-price")
            add("$buyPrice")
            add("--mint")
            add(mint)
            add("--token-size")
            add("$amount")
            add("--keypair")
            add(sellerKeypair)
        }

        return processOperation(args) { it.parse(4, -1) }
    }

    protected fun cancel(
        auctionHouse: String,
        keypair: String,
        buyPrice: Long,
        mint: String,
        amount: Long,
    ) {
        val args = buildList {
            add("ts-node")
            add("/home/solana/metaplex/js/packages/cli/src/auction-house-cli.ts")
            add("cancel")
            add("--auction-house")
            add(auctionHouse)
            add("--buy-price")
            add("$buyPrice")
            add("--mint")
            add(mint)
            add("--token-size")
            add("$amount")
            add("--keypair")
            add(keypair)
        }

        return processOperation(args) { it.parse(4, -1) }
    }

    protected fun buy(
        auctionHouse: String,
        buyerKeypair: String,
        buyPrice: Long,
        mint: String,
        amount: Long
    ) {
        val args = buildList {
            add("ts-node")
            add("/home/solana/metaplex/js/packages/cli/src/auction-house-cli.ts")
            add("buy")
            add("--auction-house")
            add(auctionHouse)
            add("--buy-price")
            add("$buyPrice")
            add("--mint")
            add(mint)
            add("--token-size")
            add("$amount")
            add("--keypair")
            add(buyerKeypair)
        }

        return processOperation(args) { it.parse(4, -1) }
    }

    protected fun showEscrow(
        auctionHouse: String,
        keypair: String,
        wallet: String
    ): BigDecimal {
        val args = buildList {
            add("ts-node")
            add("/home/solana/metaplex/js/packages/cli/src/auction-house-cli.ts")
            add("show_escrow")
            add("--auction-house")
            add(auctionHouse)
            add("--wallet")
            add(wallet)
            add("--keypair")
            add(keypair)
        }

        return processOperation(args) { it.parse(1, -1) }.toBigDecimal()
    }

    protected fun executeSale(
        auctionHouse: String,
        keypair: String,
        buyPrice: Long,
        mint: String,
        amount: Long,
        buyerWallet: String,
        sellerWallet: String,
    ) {
        val args = buildList {
            add("ts-node")
            add("/home/solana/metaplex/js/packages/cli/src/auction-house-cli.ts")
            add("execute_sale")
            add("--auction-house")
            add(auctionHouse)
            add("--buy-price")
            add("$buyPrice")
            add("--mint")
            add(mint)
            add("--token-size")
            add("$amount")
            add("--buyer-wallet")
            add(buyerWallet)
            add("--seller-wallet")
            add(sellerWallet)
            add("--keypair")
            add(keypair)
        }

        return processOperation(args) { it.parse(4, -1) }
    }

    protected fun verifyCollection(mint: String, collection: String): String {
        val args = buildList {
            add("ts-node")
            add("/home/solana/metaplex/js/packages/cli/src/cli-nft.ts")
            add("verify-collection")
            add("-m")
            add(mint)
            add("-c")
            add(collection)
            add("--keypair")
            add("/home/solana/.config/solana/id.json")
        }

        return processOperation(args) { it.parse(4, -1) }
    }

    protected fun mintNft(
        keypair: String,
        collection: String? = null,
        meta: String = "meta.json"
    ): String {
        val args = buildList {
            add("ts-node")
            add("/home/solana/metaplex/js/packages/cli/src/cli-nft.ts")
            add("mint")
            collection?.let {
                add("--collection")
                add(it)
            }
            add("--url")
            add("http://host.testcontainers.internal:8080/meta/$meta")
            add("--keypair")
            add(keypair)
        }

        return processOperation(args) { it.parse(4, -1) }
    }

    protected fun updateMetadata(
        keypair: String,
        mint: String,
        collection: String? = null,
        meta: String = "meta.json"
    ): String {
        val args = buildList {
            add("ts-node")
            add("/home/solana/metaplex/js/packages/cli/src/cli-nft.ts")
            add("update-metadata")
            add("--mint")
            add(mint)
            collection?.let {
                add("--collection")
                add(it)
            }
            add("--url")
            add("http://host.testcontainers.internal:8080/meta/$meta")
            add("--keypair")
            add(keypair)
        }

        return processOperation(args) { it.parse(4, -1) }
    }

    protected fun getWallet(keypair: String = baseKeypair): String {
        val args = buildList {
            add("solana")
            add("address")
            add("--keypair")
            add(keypair)
        }

        return processOperation(args) { it.parse(0, -1) }
    }

    protected fun createWallet(name: String): String {
        val args = listOf("solana-keygen", "new", "--outfile", "/home/solana/.config/solana/$name")

        return processOperation(args) { it.parse(4, -1) }
    }

    protected fun createKeypair(name: String): String {
        val args = listOf("solana-keygen", "new", "--outfile", "/home/solana/.config/solana/$name")

        return processOperation(args) { "/home/solana/.config/solana/$name" }
    }

    protected fun createToken(decimals: Int?): String {
        val args = buildList {
            add("spl-token")
            add("create-token")
            decimals?.let { add("--decimals"); add("$it") }
        }

        return processOperation(args) { it.parse(0, -1) }
    }

    protected fun createAccount(token: String, wallet: String? = null): String {
        val args = buildList {
            add("spl-token")
            add("create-account")
            add(token)
            wallet?.let { add("--owner"); add(it) }
        }

        return processOperation(args) { it.parse(0, -1) }
    }

    protected fun mintToken(token: String, amount: ULong): String {
        val args = listOf("spl-token", "mint", token, "$amount")

        return processOperation(args) { it.parse(0, -1) }
    }

    protected fun burnToken(tokenAccount: String, amount: ULong): String {
        val args = listOf("spl-token", "burn", tokenAccount, "$amount")

        return processOperation(args) { it.parse(0, -1) }
    }

    protected fun transferToken(token: String, amount: ULong, wallet: String): String {
        val args = listOf("spl-token", "transfer", token, "$amount", wallet)

        return processOperation(args) { it.parse(0, -1) }
    }

    private fun <T> processOperation(args: List<String>, parser: (String) -> T): T {
        val exec = solana.execInContainer(*args.toTypedArray())
        assertEquals(0, exec.exitCode, exec.stderr)
        return parser(exec.stdout)
    }

    private fun String.parse(): AuctionHouse {
        val rows = split(System.lineSeparator())

        return AuctionHouse(
            id = rows[3].split(" ").last(),
            mint = rows[4].split(" ").last(),
            authority = rows[5].split(" ").last(),
            creator = rows[6].split(" ").last(),
            feePayerAcct = rows[7].split(" ").last(),
            treasuryAcct = rows[8].split(" ").last(),
            feePayerWithdrawalAcct = rows[9].split(" ").last(),
            treasuryWithdrawalAcct = rows[10].split(" ").last(),
            sellerFeeBasisPoints = rows[13].split(" ").last().toInt()
        )
    }

    private fun String.parse(rowNumber: Int, colNumber: Int): String {
        val rows = split(System.lineSeparator())
        val i = (rowNumber + rows.size) % rows.size
        val cols = rows[i].split(" ")
        val j = (colNumber + cols.size) % cols.size

        return cols[j]
    }

    protected fun Int.scaleSupply(decimals: Int): BigInteger =
        BigInteger.valueOf(this.toLong()) * BigInteger.TEN.pow(decimals)

    protected final inline fun <reified T : SolanaLogRecord> findRecordByType(
        collection: String,
        type: Class<T>
    ): Flow<T> {
        val criteria = Criteria.where("_class").isEqualTo(T::class.java.name)

        return mongo.find(Query(criteria), type, collection).asFlow()
    }

    companion object {
        val solana: GenericContainer<*> = KGenericContainer(
            if (System.getProperty("os.arch") == "aarch64") {
                "rarible/solana"
            } else {
                "rarible/solana:1.8.16-dev-0404"
            }
        )
            .withExposedPorts(8899)
            .withCommand("solana-test-validator --limit-ledger-size=50_000_000 --bpf-program ${SolanaProgramId.TOKEN_METADATA_PROGRAM} /home/solana/mpl_token_metadata.so --bpf-program ${SolanaProgramId.AUCTION_HOUSE_PROGRAM} /home/solana/mpl_auction_house.so")
            .waitingFor(Wait.defaultWaitStrategy())

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            exposeHostPorts(8080)
            solana.start()
            val exec = solana.execInContainer(
                "bash",
                "-c",
                "cd tmp && anchor idl init ${SolanaProgramId.AUCTION_HOUSE_PROGRAM} -f /home/solana/auction_house.json"
            )
            assertEquals(0, exec.exitCode, exec.stderr)
            Thread.sleep(5_000) // for tests
            val port = solana.getMappedPort(8899)

            registry.add("blockchain.scanner.solana.rpcApiUrls") { "http://127.0.0.1:$port" }
        }
    }
}
