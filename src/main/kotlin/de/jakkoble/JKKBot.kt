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
import discord4j.rest.util.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import reactor.core.publisher.Mono
import java.time.Instant


object JKKBot {
    private lateinit var discordClient: DiscordClient
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking{
        discordClient = DiscordClient.create("MTAwMzM4MjM2NDg0OTg0ODM2MA.GPahhC._pdE8dfgWEzkEs_bHmBYCQyBqBb98XRv-0ewFg") ?: return@runBlocking
        GlobalCommandRegistrar(discordClient).registerCommands(listOf("greet.json", "ping.json", "user.json"))

        val login: Mono<Void> = discordClient.withGateway { gateway: GatewayDiscordClient ->
            val readyHandler = gateway.on(ReadyEvent::class.java) { event: ReadyEvent ->
                Mono.fromRunnable<Any?> {
                    val self: User = event.self
                    println("Logged in as ${self.username}#${self.discriminator}")
                    launch {
                        val embed: EmbedCreateSpec =
                            EmbedCreateSpec.builder().color(Color.GREEN).title("Status: Online")
                                .description("JKKBot successfully started and is now online.")
                                .thumbnail("https://i.imgur.com/FMiS7Xg.jpg")
                                .addField("\u200B", "\u200B", true)
                                .timestamp(Instant.now()).footer("JKKBot", "https://i.imgur.com/FMiS7Xg.jpg").build()
                        discordClient.getChannelById(Snowflake.of(1003392961029087323)).createMessage(embed.asRequest()).subscribe()
                    }
                }
            }.then()

            val messageCreate = gateway.on(MessageCreateEvent::class.java) { event: MessageCreateEvent ->
                val message: Message = event.message
                if (message.content.equals("!ping", true))
                    return@on message.channel.flatMap { channel -> channel.createMessage("pong!") }
                return@on Mono.empty()
            }.then()

            val commandHandler = gateway.on(ChatInputInteractionEvent::class.java) {event: ChatInputInteractionEvent ->
                if (event.commandName.equals("ping"))
                    return@on event.reply("pong!")
                else if (event.commandName.equals("user")) {
                        val userId = event.getOption("username")
                            .flatMap { obj: ApplicationCommandInteractionOption -> obj.value }
                            .map { obj: ApplicationCommandInteractionOptionValue ->  obj.raw}
                            .get()
                        return@on event.reply("test")
                        // TODO: Reply with the Embed
                }
                return@on Mono.empty()
            }
            readyHandler.and(messageCreate).and(commandHandler)
        }
        withContext(Dispatchers.IO) {
            login.block()
        }
    }
    private fun getUserEmbed(userId: Long):EmbedCreateSpec? = runBlocking {
        val userData = withContext(Dispatchers.IO) {
            discordClient.getMemberById(
                Snowflake.of(1003362781791256596),
                Snowflake.of(userId)
            ).data.block()
        }?.user()
        val embed = userData?.avatar()?.get()?.let {
            EmbedCreateSpec.builder().color(Color.PINK).title("User Data of <@${userData.locale()}>").url("https://jakkoble.de")
                .description("JKKBot successfully started and is now online.")
                .thumbnail(it)
                .addField("Username", userData.username(), true)
                .addField("Usertag", userData.discriminator(), true)
                .addField("Bot Account", userData.bot().toString(), true)
                .addField("\u200B", "\u200B", true)
                .timestamp(Instant.now()).footer("JKKBot", "https://i.imgur.com/FMiS7Xg.jpg").build()
        }
        return@runBlocking embed
    }
}