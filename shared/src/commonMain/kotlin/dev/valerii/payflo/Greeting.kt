package dev.valerii.payflo

class Greeting {
    private val platform = getPlatform()

    fun greet() = "Hello, ${platform.name}!"
}