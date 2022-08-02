package de.jakkoble

import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.Mono


object JKKBot {
    @JvmStatic
    fun main(args: Array<String>) {
        val client = DiscordClient.create("MTAwMzM4MjM2NDg0OTg0ODM2MA.GPahhC._pdE8dfgWEzkEs_bHmBYCQyBqBb98XRv-0ewFg")
        val login = client.withGateway { gateway: GatewayDiscordClient ->
            // ReadyEvent example
            val printOnLogin = gateway.on(ReadyEvent::class.java) { event: ReadyEvent ->
                Mono.fromRunnable<Any?> { println("Logged in as ${event.self.username}#${event.self.discriminator}...") }
            }.then()

            // MessageCreateEvent example
            val handlePingCommand = gateway.on(MessageCreateEvent::class.java) { event: MessageCreateEvent ->
                if (event.message.content.equals("!ping", true)) {
                    println("Responded with Message 'pong!'.")
                    return@on event.message.channel.flatMap { channel -> channel.createMessage("pong!") }
                }
                Mono.empty<Any?>()
            }.then()

            // ChatInputEvent example
            val handleCommand = gateway.on(ChatInputInteractionEvent::class.java) { event: ChatInputInteractionEvent ->
                Mono.fromRunnable<Any?> {
                    if (event.commandName.equals("ping", true)) {
                        event.reply("Pong!")
                    }
                    Mono.empty<Any?>()
                }
            }.then()
            printOnLogin.and(handlePingCommand).and(handleCommand)
        }
        GlobalCommandRegistrar(client).registerCommands(listOf("greet.json", "ping.json"))
        login.block()
        // Test
    }
}