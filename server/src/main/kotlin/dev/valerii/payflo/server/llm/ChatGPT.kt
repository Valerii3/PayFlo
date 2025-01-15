package dev.valerii.payflo.server.llm


import dev.valerii.payflo.model.BillData
import dev.valerii.payflo.model.BillItem
import dev.valerii.payflo.server.llm.model.ChatRequest
import dev.valerii.payflo.server.llm.model.Message
import dev.valerii.payflo.server.llm.model.MessageContent
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.*

class ChatGPT {
    private val token = "key"
    suspend fun processBillImage(base64Image: String): BillData {
        try {
            // Create the system message that explains what we want
            val request = ChatRequest(
                model = "gpt-4o-mini",
                messages = listOf(
                    Message(
                        role = "user",
                        content = listOf(
                            MessageContent(type = "text", text = systemMessage),
                            MessageContent(type = "image_url", image_url = mapOf("url" to "data:image/jpeg;base64,$base64Image"))
                        )
                    )
                ),
                max_tokens = 300
            )

            println("1. Request built successfully")
            println("Base64 length: ${base64Image.length}")
            println("First 100 chars of base64: ${base64Image.take(100)}")

            val jsonRequest = Json.encodeToString<ChatRequest>(request)
            println("2. Request serialized to JSON: ${jsonRequest.take(200)}...")

            val client = HttpClient()
            println("3. About to make HTTP request...")

            val response: HttpResponse = client.post("https://api.openai.com/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(jsonRequest)
            }

            println("4. Got HTTP response with status: ${response.status}")

            val responseText = response.bodyAsText()
            client.close()
            println("5. Response body: $responseText")

            val jsonResponse = Json.parseToJsonElement(responseText)
            println("6. Parsed JSON response")

            val messageContent = jsonResponse
                .jsonObject["choices"]
                ?.jsonArray?.get(0)
                ?.jsonObject?.get("message")
                ?.jsonObject?.get("content")
                ?.jsonPrimitive?.content

            println("7. Extracted message content: $messageContent")

            if (messageContent != null) {
                val cleanedJson = messageContent
                    .removePrefix("```json\n")
                    .removeSuffix("\n```")
                println("8. Cleaned JSON: $cleanedJson")

                val billData = Json.decodeFromString<BillData>(cleanedJson)
                println("9. Successfully parsed BillData")
                return billData
            } else {
                throw LLMRequestFailed("Failed to extract message content")
            }

        } catch (e: Exception) {
            println("ERROR occurred at step: ${e.message}")
            e.printStackTrace()
            throw LLMRequestFailed("Failed to process bill image: ${e.message}", e)
        }
    }

    suspend fun analyzeOrder(orderDescription: String, billItems: List<BillItem>): List<String> {
        try {
            val billItemsFormatted = billItems.joinToString("\n") {
                "- ${it.name} (${it.quantity}x ₴${it.price}) [ID: ${it.id}]"
            }
            print("GPTREQUEST")

            val request = ChatRequest(
                model = "gpt-4o-mini",
                messages = listOf(
                    Message(
                        role = "system",
                        content = listOf(
                            MessageContent(
                                type = "text",
                                text = """
                            You are an AI assistant that matches user's order descriptions with items from a bill.
                            Analyze the order description and return ONLY the IDs of matching items.
                            Consider variations in food/drink names and be flexible with matching.
                            Return the response as a JSON array of item IDs.
                            """.trimIndent()
                            )
                        )
                    ),
                    Message(
                        role = "user",
                        content = listOf(
                            MessageContent(
                                type = "text",
                                text = """
                            Order description: "$orderDescription"
                            
                            Available bill items:
                            $billItemsFormatted
                            
                            Return only a JSON array of matching item IDs.
                            """.trimIndent()
                            )
                        )
                    )
                ),
                max_tokens = 150
            )

            val client = HttpClient()
            val response = client.post("https://api.openai.com/v1/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(request))
            }

            val responseText = response.bodyAsText()
            client.close()

            val jsonResponse = Json.parseToJsonElement(responseText)
            val messageContent = jsonResponse
                .jsonObject["choices"]
                ?.jsonArray?.get(0)
                ?.jsonObject?.get("message")
                ?.jsonObject?.get("content")
                ?.jsonPrimitive?.content
                ?: throw LLMRequestFailed("Failed to extract message content")

            // Clean up the response and parse it as a JSON array
            val cleanedJson = messageContent
                .removePrefix("```json\n")
                .removeSuffix("\n```")
                .trim()

            return Json.decodeFromString<List<String>>(cleanedJson)

        } catch (e: Exception) {
            println("ERROR occurred while analyzing order: ${e.message}")
            e.printStackTrace()
            throw LLMRequestFailed("Failed to analyze order: ${e.message}", e)
        }
    }

}