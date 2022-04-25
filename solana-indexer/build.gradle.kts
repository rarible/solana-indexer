apply(plugin = "server")

val solanaOpenapiVersion: String by project
val raribleCommonVersion: String by project
val mongockVersion: String by project

dependencies {
    implementation(project(":solana-common"))
    implementation(project(":util-borsh"))
    implementation("com.rarible.core:rarible-core-apm-starter:$raribleCommonVersion")
    implementation("com.rarible.core:rarible-core-lock-redis:$raribleCommonVersion")
    implementation("com.github.cloudyrock.mongock:mongock-spring-v5:$mongockVersion")
    implementation("com.github.cloudyrock.mongock:mongodb-springdata-v3-driver:$mongockVersion")

    implementation("com.rarible.protocol.solana:solana-protocol-subscriber-starter:$solanaOpenapiVersion")
    testImplementation(project(":solana-test-common"))
}
