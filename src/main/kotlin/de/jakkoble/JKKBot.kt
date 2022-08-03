package de.jakkoble

import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec
import discord4j.rest.util.Color
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Mono


object JKKBot {
    private lateinit var discordClient: DiscordClient

    @JvmStatic
    fun main(args: Array<String>) {
        discordClient = DiscordClient.create("MTAwMzM4MjM2NDg0OTg0ODM2MA.GPahhC._pdE8dfgWEzkEs_bHmBYCQyBqBb98XRv-0ewFg") ?: return
        GlobalCommandRegistrar(discordClient).registerCommands(listOf("greet.json", "ping.json", "user.json"))

        val login: Mono<Void> = discordClient.withGateway { gateway: GatewayDiscordClient ->
            val readyHandler = gateway.on(ReadyEvent::class.java) { event: ReadyEvent ->
                Mono.fromRunnable<Any?> {
                    val self: User = event.self
                    println("Logged in as ${self.username}#${self.discriminator}")
                    val embed: EmbedCreateSpec = EmbedCreateSpec.builder().color(Color.GREEN).title("Status: Online")
                        .description("JKKBot successfully started and is now online.")
                        .thumbnail("https://i.imgur.com/FMiS7Xg.jpg")
                        .build()
                    runBlocking {
                        discordClient.getChannelById(Snowflake.of(1003392961029087323)).createMessage(embed.asRequest()).subscribe()
                    }
                }
            }.then()

            val messageCreate = gateway.on(MessageCreateEvent::class.java) { event: MessageCreateEvent ->
                val message: Message = event.message
                if (message.content.equals("!ping", true)) return@on message.channel.flatMap { channel -> channel.createMessage("pong!") }
                return@on Mono.empty()
            }.then()

            val commandHandler = gateway.on(ChatInputInteractionEvent::class.java) { event: ChatInputInteractionEvent ->
                if (event.commandName.equals("ping")) return@on event.reply("pong!")
                else if (event.commandName.equals("user")) {
                    val userId = event.getOption("username").flatMap { obj: ApplicationCommandInteractionOption -> obj.value }.map { obj: ApplicationCommandInteractionOptionValue -> obj.raw }.get()
                    return@on event.reply(InteractionApplicationCommandCallbackSpec.builder().addEmbed(getUserEmbed(userId.toLong())).build())
                }
                return@on Mono.empty()
            }
            readyHandler.and(messageCreate).and(commandHandler)
        }
        login.block()
    }

    private fun getUserEmbed(userId: Long): EmbedCreateSpec {
        val memberData = discordClient.getMemberById(Snowflake.of(1003362781791256596), Snowflake.of(userId)).data.block() ?: return getErrorEmbet()
        val userData = memberData.user() ?: return getErrorEmbet()
        return EmbedCreateSpec.builder().color(Color.PINK).title("User Data Request")
            .description("These are some Information of the given User <@$userId>")
            .thumbnail("https://cdn.discordapp.com/avatars/$userId/${userData.avatar().get()}?size=64")
            .addField("Name", userData.username(), true)
            .addField("Tag", userData.discriminator(), true)
            .addField("ID", userId.toString(), true)
            .build() ?: return getErrorEmbet()
    }
    private fun getErrorEmbet(): EmbedCreateSpec = EmbedCreateSpec.builder().color(Color.RED).title("Error")
        .description("Something went wrong. Please contact the Server Team.")
        .build()
}