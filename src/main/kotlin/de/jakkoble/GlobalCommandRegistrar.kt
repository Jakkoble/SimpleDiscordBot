package de.jakkoble

import discord4j.common.JacksonResources
import discord4j.discordjson.json.ApplicationCommandData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.RestClient
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.stream.Collectors

open class GlobalCommandRegistrar(private val restClient: RestClient) {
    //Since this will only run once on startup, blocking is okay.
    @Throws(IOException::class)
    fun registerCommands(fileNames: List<String>) {
        //Create an ObjectMapper that supports Discord4J classes
        val d4jMapper = JacksonResources.create()

        // Convenience variables for the sake of easier to read code below
        val applicationService = restClient.applicationService
        val applicationId = restClient.applicationId.block() ?: return

        //Get our commands json from resources as command data
        val commands: MutableList<ApplicationCommandRequest> = ArrayList()
        for (json in getCommandsJson(fileNames)) {
            val request = d4jMapper.objectMapper
                .readValue(json, ApplicationCommandRequest::class.java)
            commands.add(request) //Add to our array list
        }

        /* Bulk overwrite commands. This is now idempotent, so it is safe to use this even when only 1 command
        is changed/added/removed
        */applicationService.bulkOverwriteGlobalApplicationCommand(applicationId, commands)
            .doOnNext { cmd: ApplicationCommandData -> println("Successfully registered Global Command " + cmd.name()) }
            .doOnError { e: Throwable? -> System.out.printf("Failed to register global commands %s%n", e) }
            .subscribe()
    }

    companion object {
        // The name of the folder the commands json is in, inside our resources folder
        private const val commandsFolderName = "commands/"

        /* The two below methods are boilerplate that can be completely removed when using Spring Boot */
        @Throws(IOException::class)
        private fun getCommandsJson(fileNames: List<String>): List<String?> {
            // Confirm that the commands' folder exists
            val url = GlobalCommandRegistrar::class.java.classLoader.getResource(commandsFolderName)
            Objects.requireNonNull(url, commandsFolderName + " could not be found")

            //Get all the files inside this folder and return the contents of the files as a list of strings
            val list: MutableList<String?> = ArrayList()
            for (file in fileNames) {
                val resourceFileAsString = getResourceFileAsString(commandsFolderName + file)
                list.add(Objects.requireNonNull(resourceFileAsString, "Command file not found: $file"))
            }
            return list
        }

        /**
         * Gets a specific resource file as String
         *
         * @param fileName The file path omitting "resources/"
         * @return The contents of the file as a String, otherwise throws an exception
         */
        @Throws(IOException::class)
        private fun getResourceFileAsString(fileName: String): String? {
            val classLoader = ClassLoader.getSystemClassLoader()
            classLoader.getResourceAsStream(fileName).use { resourceAsStream ->
                if (resourceAsStream == null) return null
                InputStreamReader(resourceAsStream).use { inputStreamReader ->
                    BufferedReader(inputStreamReader).use { reader ->
                        return reader.lines().collect(
                            Collectors.joining(System.lineSeparator())
                        )
                    }
                }
            }
        }
    }
}