package dev.valerii.payflo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform