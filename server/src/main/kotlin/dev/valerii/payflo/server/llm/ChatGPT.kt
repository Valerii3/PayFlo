package dev.valerii.payflo.server.llm

import dev.valerii.payflo.model.BillData
import dev.valerii.payflo.model.BillItem
import dev.valerii.payflo.server.llm.model.ChatRequest
import dev.valerii.payflo.server.llm.model.Message
import dev.valerii.payflo.server.llm.model.MessageContent
import dev.valerii.payflo.server.llm.utils.JsonUtils
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Suppress("CLASS_NAME_INCORRECT")
class ChatGPT {
    private val client: HttpClient = HttpClient()
    private val token: String = "YOUR_API_KEY"
    suspend fun processBillImage(base64Image: String): BillData {
        val request = createChatRequest(
            systemMessage = SYSTEM_MESSAGE_BILL_PROCESSING,
            userMessages = listOf(
                MessageContent(type = "text", text = SYSTEM_MESSAGE_BILL_PROCESSING),
                MessageContent(type = "image_url", image_url = mapOf("url" to "data:image/jpeg;base64,$base64Image"))
            )
        )

        val responseText = sendChatRequest(request)
        return JsonUtils.parseResponse<BillData>(responseText)
    }

    suspend fun analyzeOrder(orderDescription: String, billItems: List<BillItem>): List<String> {
        val formattedItems = billItems.joinToString("\n") {
            "- ${it.name} (${it.quantity}x €${it.price}) [ID: ${it.id}]"
        }
        val userMessage = """
            Order description: "$orderDescription"
            
            Available bill items:
            $formattedItems
            
            Return only a JSON array of matching item IDs.
        """.trimIndent()

        val request = createChatRequest(
            systemMessage = SYSTEM_MESSAGE_ORDER_ANALYSIS,
            userMessages = listOf(MessageContent(type = "text", text = userMessage))
        )

        val responseText = sendChatRequest(request)
        return JsonUtils.parseResponse(responseText)
    }

    private suspend fun sendChatRequest(request: ChatRequest): String {
        val response: HttpResponse = client.post("https://api.openai.com/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }
        return response.bodyAsText()
    }

    private fun createChatRequest(
        systemMessage: String,
        userMessages: List<MessageContent>
    ) = ChatRequest(
            model = "gpt-4o",
            messages = listOf(
                Message(role = "system", content = listOf(MessageContent(type = "text", text = systemMessage))),
                Message(role = "user", content = userMessages)
            ),
            max_tokens = 300
        )
}
