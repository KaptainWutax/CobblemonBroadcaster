package me.novoro.cobblemonbroadcaster.events

import me.novoro.cobblemonbroadcaster.config.Configuration
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.pokemon.aspect.AspectProvider
import com.cobblemon.mod.common.pokemon.Pokemon
import me.novoro.cobblemonbroadcaster.util.LangManager
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld

class CaptureEvent(private val config: Configuration) {

    init {
        CobblemonEvents.POKEMON_CAPTURED.subscribe(priority = Priority.LOWEST) { event ->

            val pokemon = event.pokemon
            val player = event.player

            // Blacklist Stuff
            val world = event.player.world as? ServerWorld
            val worldName = world?.registryKey?.value.toString()
            if (me.novoro.cobblemonbroadcaster.util.BlacklistedWorlds.isBlacklisted(worldName)) {
                return@subscribe
            }

            // Debugging: Log all aspects of the Pokémon
            val aspects = AspectProvider.providers.flatMap { it.provide(pokemon) }
            //SimpleLogger.debug("Pokemon ${pokemon.species.name} has aspects: $aspects")

            // Dynamically check user-defined aspects first
            config.keys.forEach { customCategory ->
                if (customCategory !in setOf("shiny", "legendary", "mythical", "ultrabeast")) {
                    if (customCategory in aspects) {
                        if (handlePokemonCategory(pokemon, player, customCategory) { true }) return@subscribe
                    }
                }
            }

            // Check categories with guard clauses in priority order
            if (handlePokemonCategory(pokemon, player, "mythical") { pokemon.isMythical() }) return@subscribe
            if (handlePokemonCategory(pokemon, player, "legendary") { pokemon.isLegendary() }) return@subscribe
            if (handlePokemonCategory(pokemon, player, "ultrabeast") { pokemon.isUltraBeast() }) return@subscribe
            if (handlePokemonCategory(pokemon, player, "shiny") { pokemon.shiny }) return@subscribe
        }
    }

    private fun handlePokemonCategory(
        pokemon: Pokemon,
        player: ServerPlayerEntity,
        category: String,
        condition: () -> Boolean
    ): Boolean {
        if (!condition()) {
            return false
        }

        val isEnabled = config.getBoolean("$category.enabled", true)
        if (!isEnabled) return false

        val langKey = "$category.CaptureMessage"
        val replacements = mapOf(
            "pokemon" to pokemon.species.name,
            "player" to player.name.string
        )

        // Send the message to all players
        player.server?.playerManager?.playerList?.forEach { targetPlayer ->
            LangManager.send(targetPlayer as ServerPlayerEntity, langKey, replacements)
        }

        // Return true to indicate the category was handled
        return true
    }
}
