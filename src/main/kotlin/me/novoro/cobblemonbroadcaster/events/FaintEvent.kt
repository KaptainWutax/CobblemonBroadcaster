package me.novoro.cobblemonbroadcaster.events

import me.novoro.cobblemonbroadcaster.config.Configuration
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.pokemon.aspect.AspectProvider
import me.novoro.cobblemonbroadcaster.util.LangManager
import net.minecraft.server.MinecraftServer

class FaintEvent(private val config: Configuration, private val server: MinecraftServer) {

    private val faintedPokemonCache = mutableSetOf<String>() // Cache to avoid duplicate announcements

    init {
        CobblemonEvents.BATTLE_FAINTED.subscribe(priority = Priority.LOWEST) { event ->

            val pokemon = event.killed.effectedPokemon


            // Ensure the Pokémon is wild and not already processed
            if (pokemon.isPlayerOwned()) return@subscribe
            if (faintedPokemonCache.contains(pokemon.uuid.toString())) return@subscribe

            // Check if the Pokémon is a boss (if applicable). Love u Guitar pookie
            val nbt = pokemon.persistentData
            val isBoss = nbt.getBoolean("boss")
            if (isBoss) return@subscribe

            val aspects = AspectProvider.providers.flatMap { it.provide(pokemon) }
            //SimpleLogger.debug("Pokemon ${pokemon.species.name} has aspects: $aspects")

            // Dynamically check user-defined aspects first
            config.keys.forEach { customCategory ->
                if (customCategory !in setOf("shiny", "legendary", "mythical", "ultrabeast")) {
                    if (customCategory in aspects) {
                        if (handlePokemonCategory(pokemon, customCategory) { true }) return@subscribe
                    }
                }
            }

            // Check categories with guard clauses in priority order
            if (handlePokemonCategory(pokemon, "mythical") { pokemon.isMythical() }) return@subscribe
            if (handlePokemonCategory(pokemon, "legendary") { pokemon.isLegendary() }) return@subscribe
            if (handlePokemonCategory(pokemon, "ultrabeast") { pokemon.isUltraBeast() }) return@subscribe
            if (handlePokemonCategory(pokemon, "shiny") { pokemon.shiny }) return@subscribe

            // Add the Pokémon to the cache
            faintedPokemonCache.add(pokemon.uuid.toString())
        }
    }

    private fun handlePokemonCategory(
        pokemon: com.cobblemon.mod.common.pokemon.Pokemon,
        category: String,
        condition: () -> Boolean
    ): Boolean {
        if (!condition()) {
            return false
        }

        val isEnabled = config.getBoolean("$category.enabled", true)
        if (!isEnabled) return false

        val langKey = "$category.FaintMessage"
        val replacements = mapOf(
            "pokemon" to pokemon.species.name
        )

        // Send the message to all online players
        server.playerManager.playerList.forEach { player ->
            LangManager.send(player, langKey, replacements)
        }

        // Return true to indicate the category was handled
        return true
    }
}
