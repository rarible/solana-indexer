apply(plugin = "server")

val solanaOpenapiVersion: String by project

dependencies {
    implementation(project(":solana-common"))
    testImplementation(project(":solana-test-common"))
    testImplementation("com.rarible.solana.protocol:solana-protocol-client:$solanaOpenapiVersion")
}