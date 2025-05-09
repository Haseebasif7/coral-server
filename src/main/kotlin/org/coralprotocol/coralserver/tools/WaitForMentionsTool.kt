package org.coralprotocol.coralserver.tools

import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import io.github.oshai.kotlinlogging.KotlinLogging
import org.coralprotocol.coralserver.WaitForMentionsInput
import org.coralprotocol.coralserver.ThreadTools
import org.coralprotocol.coralserver.session.session

private val logger = KotlinLogging.logger {}

/**
 * Extension function to add the wait for mentions tool to a server.
 */
fun Server.addWaitForMentionsTool() {
    addTool(
        name = "wait_for_mentions",
        description = "Wait for new messages mentioning an agent, with timeout",
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "agentId" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("ID of the agent to wait for mentions")
                        )
                    ),
                    "timeoutMs" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("number"),
                            "description" to JsonPrimitive("Timeout in milliseconds (default: 30000)")
                        )
                    )
                )
            ),
            required = listOf("agentId", "timeoutMs")
        )
    ) { request: CallToolRequest ->

        try {
            // Get the session associated with this server
            val session = this.session
            if (session == null) {
                val errorMessage = "No session associated with this server"
                logger.error { errorMessage }
                return@addTool CallToolResult(
                    content = listOf(TextContent(errorMessage))
                )
            }

            val json = Json { ignoreUnknownKeys = true }
            val input = json.decodeFromString<WaitForMentionsInput>(request.arguments.toString())
            logger.info { "Waiting for mentions for agent ${input.agentId} with timeout ${input.timeoutMs}ms" }

            // Use the session to wait for mentions
            val messages = session.waitForMentions(
                agentId = input.agentId,
                timeoutMs = input.timeoutMs
            )

            if (messages.isNotEmpty()) {
                // Format messages in XML-like structure using the session
                val formattedMessages = ThreadTools.formatMessagesAsXml(messages, session)
                CallToolResult(
                    content = listOf(TextContent(formattedMessages))
                )
            } else {
                CallToolResult(
                    content = listOf(TextContent("No new messages received within the timeout period"))
                )
            }
        } catch (e: Exception) {
            val errorMessage = "Error waiting for mentions: ${e.message}"
            logger.error(e) { errorMessage }
            CallToolResult(
                content = listOf(TextContent(errorMessage))
            )
        }
    }
}
