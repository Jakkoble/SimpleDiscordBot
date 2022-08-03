package de.jakkoble

import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandInteractionOptionValue
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.entity.User
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec
import discord4j.rest.util.Color
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Mono
import kotlin.system.exitProcess


object JKKBot {
    private lateinit var discordClient: DiscordClient

    // Change for your Situation
    private val botToken: String = System.getenv("TOKEN") ?: exitProcess(1) // Do not write your Bot Token in your Source Code => Save it in an Environmental Variable
    private const val guildId = 1003362781791256596 // ServerID of the Bot
    private const val channelId = 1003392961029087323 // Channel of Status Message

    @JvmStatic
    fun main(args: Array<String>) {
        discordClient = DiscordClient.create(botToken) // Create a new Discord Client with Token => "Bot is still "logged out"
        GlobalCommandRegistrar(discordClient).registerCommands(listOf("buy.json", "greet.json", "user.json")) // Register Commands from json Files

        val login: Mono<Void> = discordClient.withGateway { gateway: GatewayDiscordClient -> // Open up a new GatewayDiscordClient => Bot "logged in"

            // Listen to the ReadyEvent => Bot is launched and ready
            val readyHandler = gateway.on(ReadyEvent::class.java) { event: ReadyEvent ->
                Mono.fromRunnable<Any?> {
                    val self: User = event.self
                    println("Logged in as ${self.username}#${self.discriminator}")

                    // Create a new Embed with some Specs
                    val embed: EmbedCreateSpec = EmbedCreateSpec.builder().color(Color.GREEN).title("Status: Online")
                        .description("JKKBot successfully started and is now online.")
                        .thumbnail("https://i.imgur.com/FMiS7Xg.jpg")
                        .build()

                    // Run in new Coroutine to not interrupt the flow
                    runBlocking {
                        discordClient.getChannelById(Snowflake.of(channelId))
                            .createMessage(embed.asRequest())
                            .subscribe()
                    }
                }
            }.then()

            // Listen to the ChatInputInteractionEvent => on Slash Command
            val commandHandler = gateway.on(ChatInputInteractionEvent::class.java) { event: ChatInputInteractionEvent ->
                when (event.commandName) {
                    "greet" -> {
                        val name = event.getOption("name")
                            .flatMap { obj: ApplicationCommandInteractionOption -> obj.value }
                            .map { obj: ApplicationCommandInteractionOptionValue -> obj.raw }
                            .get()
                        return@on event.reply("Hello $name!")
                    }
                    "user" -> {
                        val userId = event.getOption("username")
                            .flatMap { obj: ApplicationCommandInteractionOption -> obj.value }
                            .map { obj: ApplicationCommandInteractionOptionValue -> obj.raw }
                            .get()

                        // Reply with the created User Embed of getUserEmbed() Methode (Bottom of this class)
                        return@on event.reply(InteractionApplicationCommandCallbackSpec.builder()
                            .addEmbed(getUserEmbed(userId.toLong()))
                            .build())
                    }
                    "buy" -> {
                        val embed = EmbedCreateSpec.builder().color(Color.WHITE).title("Are you sure?")
                            .description("By clicking on yes you order an Apple.")
                            .build()
                         val message = event.reply(InteractionApplicationCommandCallbackSpec.builder()
                             .addEmbed(embed)
                             .addComponent(ActionRow.of(Button.primary("button-1", "yes")))
                             .build())
                         val listener = gateway.on(ButtonInteractionEvent::class.java) { clickEvent: ButtonInteractionEvent ->
                             if (clickEvent.customId.equals("button-1"))
                                 return@on clickEvent.reply("You have successfully bought an Apple")
                             return@on Mono.empty()
                         }.then()
                        return@on message.then(listener)
                    }
                }
                return@on Mono.empty() // If nothing replied yet, an empty Mono replies => "nothing" happens
            }

            // Combine the two Event Listeners (not necessary for just one Event)
            readyHandler.and(commandHandler)
        }
        login.block()
    }
    private fun getUserEmbed(userId: Long): EmbedCreateSpec {
        val memberData = discordClient.getMemberById(Snowflake.of(guildId), Snowflake.of(userId)).data.block() ?: return getErrorEmbed()
        val userData = memberData.user() ?: return getErrorEmbed()
        return EmbedCreateSpec.builder().color(Color.PINK).title("User Data Request")
            .description("These are some Information of the given User <@$userId>")
            .thumbnail("https://cdn.discordapp.com/avatars/$userId/${userData.avatar().get()}?size=64")
            .addField("Name", userData.username(), true)
            .addField("Tag", userData.discriminator(), true)
            .addField("ID", userId.toString(), true)
            .build() ?: return getErrorEmbed()
    }
    // Gets returned in different places when something went wrong (Embed)
    private fun getErrorEmbed(): EmbedCreateSpec = EmbedCreateSpec.builder().color(Color.RED).title("Error")
        .description("Something went wrong. Please contact the Server Team.")
        .build()
}