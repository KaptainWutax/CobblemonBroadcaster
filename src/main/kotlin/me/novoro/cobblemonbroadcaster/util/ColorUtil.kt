package me.novoro.cobblemonbroadcaster.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.*
import java.util.regex.Pattern

object ColorUtil {
    private val HEX_PATTERN: Pattern = Pattern.compile("&#([A-Fa-f0-9]{6})")
    private val LEGACY_PATTERN: Pattern = Pattern.compile("[&§]([0-9a-fA-Fk-oK-OrR])")
    private val miniMessage: MiniMessage = MiniMessage.miniMessage()

    /**
     * Parses a string containing color codes into an Adventure `Component`.
     */
    fun parseColour(input: String): Component {
        val processedInput = replaceCodes(input)
        return miniMessage.deserialize(processedInput)
    }

    /**
     * Replaces legacy color codes and hex codes with MiniMessage-compatible tags.
     */
    private fun replaceCodes(input: String): String {
        var result = input

        // Replace hex codes
        val hexMatcher = HEX_PATTERN.matcher(result)
        while (hexMatcher.find()) {
            result = result.replace(
                hexMatcher.group(),
                "<reset><c:#${hexMatcher.group(1)}>"
            )
        }

        // Replace legacy codes
        val legacyMatcher = LEGACY_PATTERN.matcher(result)
        while (legacyMatcher.find()) {
            result = result.replace(
                legacyMatcher.group(),
                getLegacyReplacement(legacyMatcher.group(1))
            )
        }

        return result
    }

    /**
     * Converts legacy Minecraft formatting codes (e.g., `&6`) into MiniMessage-compatible tags.
     */
    private fun getLegacyReplacement(input: String): String {
        return when (input.uppercase(Locale.ENGLISH)) {
            "0" -> "<reset><c:#000000>"
            "1" -> "<reset><c:#0000AA>"
            "2" -> "<reset><c:#00AA00>"
            "3" -> "<reset><c:#00AAAA>"
            "4" -> "<reset><c:#AA0000>"
            "5" -> "<reset><c:#AA00AA>"
            "6" -> "<reset><c:#FFAA00>"
            "7" -> "<reset><c:#AAAAAA>"
            "8" -> "<reset><c:#555555>"
            "9" -> "<reset><c:#5555FF>"
            "A" -> "<reset><c:#55FF55>"
            "B" -> "<reset><c:#55FFFF>"
            "C" -> "<reset><c:#FF5555>"
            "D" -> "<reset><c:#FF55FF>"
            "E" -> "<reset><c:#FFFF55>"
            "F" -> "<reset><c:#FFFFFF>"
            "K" -> "<obf>"
            "L" -> "<b>"
            "M" -> "<st>"
            "N" -> "<u>"
            "O" -> "<i>"
            "R" -> "<reset>"
            else -> input
        }
    }

    /**
     * Converts a `ComponentLike` into a string representation with MiniMessage tags.
     */
    fun componentToString(component: ComponentLike): String {
        val styledComponent = component.asComponent()
        val sb = StringBuilder()

        // Add decorations
        for ((key, value) in styledComponent.decorations()) {
            if (value == TextDecoration.State.TRUE) {
                sb.append("<").append(key.toString().lowercase()).append(">")
            }
        }

        // Add color
        val color = styledComponent.color()
        if (color != null) {
            sb.append("<c:").append(color.asHexString()).append(">")
        }

        // Add content
        if (styledComponent is TextComponent) {
            sb.append(styledComponent.content())
        }

        // Handle translatable components
        if (styledComponent is TranslatableComponent) {
            sb.append("<lang:").append(styledComponent.key()).append(">")
        }

        sb.append("<reset>")
        return sb.toString()
    }
}
