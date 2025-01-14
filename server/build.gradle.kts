plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
}

group = "dev.valerii.payflo"
version = "1.0.0"
application {
    mainClass.set("dev.valerii.payflo.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation("org.jetbrains.exposed:exposed-core:0.58.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.58.0")
    implementation("io.ktor:ktor-server-content-negotiation:3.0.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.2")
    implementation("org.postgresql:postgresql:42.2.23")
  //  implementation(project(":shared"))
   // testImplementation(libs.ktor.server.tests)
    testImplementation("io.ktor:ktor-server-test-host:3.0.2")
    testImplementation(libs.kotlin.test.junit)
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
}