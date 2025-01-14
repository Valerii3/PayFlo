plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("org.cqfn.diktat.diktat-gradle-plugin") version "1.2.5"
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("${rootProject.projectDir}/config/detekt.yml")
}

diktat {
    inputs {
        include("shared/**/*.kt")
        include("server/**/*.kt")
        exclude("**/iosMain/**")
        exclude("**/**/Platform**")
    }
    diktatConfigFile = file("$projectDir/config/diktat.yml")
}

tasks.register("diktat") {
    group = "verification"
    dependsOn(tasks.getByName("diktatCheck"))
}