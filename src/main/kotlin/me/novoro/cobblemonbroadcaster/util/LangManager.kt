package me.novoro.cobblemonbroadcaster.util

import me.novoro.cobblemonbroadcaster.config.Configuration
import net.kyori.adventure.audience.Audience
import net.minecraft.server.network.ServerPlayerEntity

object LangManager {
    private val lang: MutableMap<String, String> = HashMap()

    /**
     * Load language configuration into the LangManager.
     */
    fun loadConfig(langConfig: Configuration?) {
        fun parseSection(section: Configuration, prefix: String = "") {
            section.keys.forEach { key ->
                val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"

                // Check if the key is a nested section
                if (section.isSection(key)) {
                    val nestedSection = section.getSection(key)
                    if (nestedSection != null) {
                        parseSection(nestedSection, fullKey)
                    }
                } else {
                    // Check if the value is a string
                    val value = section.get(key)
                    when (value) {
                        is String -> {
                            lang[fullKey] = value
                            SimpleLogger.debug("Loaded lang key: $fullKey -> $value")
                        }
                        else -> {
                            SimpleLogger.debug("Skipping non-string key: $fullKey (type: ${value?.javaClass?.simpleName})")
                        }
                    }
                }
            }
        }

        langConfig?.let { parseSection(it) }
    }

    /**
     * Retrieve a language entry by key.
     */
    fun getLang(langKey: String): String? = lang[langKey]

    /**
     * Send a message to a player with optional replacements.
     */
    fun send(player: ServerPlayerEntity, langKey: String, replacements: Map<String, String> = emptyMap()) {
        // Retrieve the raw message from language entries
        var rawMessage = getLang(langKey) ?: return

        // Perform placeholder replacements
        replacements.forEach { (key, value) ->
            rawMessage = rawMessage.replace("{$key}", value)
        }

        // Parse the message for color tags and formatting using ColorUtil
        val formattedMessage = ColorUtil.parseColour(rawMessage)

        // Convert ServerPlayerEntity to Adventure Audience
        val audience: Audience = player

        // Send the formatted message
        audience.sendMessage(formattedMessage)
    }
}
