package de.jakkoble

import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.Message
import reactor.core.publisher.Mono


object JKKBot {
    @JvmStatic
    fun main(args: Array<String>) {
        val client = DiscordClient.create("MTAwMzM4MjM2NDg0OTg0ODM2MA.GPahhC._pdE8dfgWEzkEs_bHmBYCQyBqBb98XRv-0ewFg")
        val login = client.withGateway { gateway: GatewayDiscordClient ->
            val printOnLogin = gateway.on(ReadyEvent::class.java) { event: ReadyEvent ->
                Mono.fromRunnable<Any?> {
                    val self = event.self
                    println("Logged in as ${self.username}#${self.discriminator}")
                }
            }.then()
            val handlePingCommand = gateway.on(MessageCreateEvent::class.java) { event: MessageCreateEvent ->
                val message: Message = event.message
                if (message.content.equals("!ping", true))
                    return@on message.channel.flatMap { channel -> channel.createMessage("pong!") }
                Mono.empty<Any?>()
            }.then()
            printOnLogin.and(handlePingCommand)
        }
        login.block()
    }
}