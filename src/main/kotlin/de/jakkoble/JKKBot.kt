package de.jakkoble

import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import reactor.core.publisher.Mono


object JKKBot {
    @JvmStatic
    fun main(args: Array<String>) {
        val discordClient = DiscordClient.create("MTAwMzM4MjM2NDg0OTg0ODM2MA.GPahhC._pdE8dfgWEzkEs_bHmBYCQyBqBb98XRv-0ewFg") ?: return
        val login: Mono<Void> = discordClient.withGateway { gateway: GatewayDiscordClient ->
            val readyEventFlux = gateway.on(ReadyEvent::class.java) { event: ReadyEvent ->
                Mono.fromRunnable<Any?> {
                    val self: User = event.self
                    System.out.printf("Logged in as %s#%s%n", self.username, self.discriminator)
                }
            }.then()
            val messageCreateFlux = gateway.on(MessageCreateEvent::class.java) { event: MessageCreateEvent ->
                val message: Message = event.message
                if (message.content.equals("!ping", true)) {
                    return@on message.channel.flatMap { channel -> channel.createMessage("pong!") }
                }
                return@on Mono.empty()
            }
            readyEventFlux.and(messageCreateFlux)
        }
        login.block()
        //GlobalCommandRegistrar(client).registerCommands(listOf("greet.json", "ping.json"))

        /*val login = client.withGateway { gateway: GatewayDiscordClient ->
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
            val handleCommand = client.login().block()?.on(object : ReactiveEventAdapter() {
                override fun onChatInputInteraction(event: ChatInputInteractionEvent): Publisher<*> {
                    if (event.commandName == "ping") {
                        println("HOHOHO")
                        return event.reply("pong!")
                    }
                    return Mono.empty<Any>()
                }
            })?.then()
            printOnLogin.and(handlePingCommand).and(handleCommand)
        }
        login.block()*/
    }
}