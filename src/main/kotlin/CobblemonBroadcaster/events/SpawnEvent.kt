package CobblemonBroadcaster.events

import CobblemonBroadcaster.config.Configuration
import CobblemonBroadcaster.util.BlacklistedWorlds
import CobblemonBroadcaster.util.LangManager
import CobblemonBroadcaster.util.SimpleLogger
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.pokemon.aspect.AspectProvider
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld

class SpawnEvent(private val config: Configuration) {

    init {
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(priority = Priority.LOWEST) { event ->
            val pokemonEntity = event.entity
            val aspects = AspectProvider.providers.flatMap { it.provide(pokemonEntity.pokemon) }

            // Blacklist Stuff
            val world = event.entity.world as? ServerWorld
            val worldName = world?.registryKey?.value.toString()
            if (BlacklistedWorlds.isBlacklisted(worldName)) {
                return@subscribe
            }

            // Debugging: Log all aspects of the Pokémon
            //SimpleLogger.debug("Pokemon ${pokemonEntity.pokemon.species.name} has aspects: $aspects")

            // Dynamically check user-defined aspects
            config.keys.forEach { customCategory ->
                if (customCategory !in setOf("shiny", "legendary", "mythical", "ultrabeast")) {
                    if (customCategory in aspects) {
                        if (handleCategory(pokemonEntity, event.ctx.spawner.name, customCategory) { true }) return@subscribe
                    }
                }
            }

            // Default categories with priority
            if (handleCategory(pokemonEntity, event.ctx.spawner.name, "mythical") { pokemonEntity.pokemon.isMythical() }) return@subscribe
            if (handleCategory(pokemonEntity, event.ctx.spawner.name, "legendary") { pokemonEntity.pokemon.isLegendary() }) return@subscribe
            if (handleCategory(pokemonEntity, event.ctx.spawner.name, "ultrabeast") { pokemonEntity.pokemon.isUltraBeast() }) return@subscribe
            if (handleCategory(pokemonEntity, event.ctx.spawner.name, "shiny") { pokemonEntity.pokemon.shiny }) return@subscribe
        }
    }

    private fun handleCategory(
        pokemonEntity: com.cobblemon.mod.common.entity.pokemon.PokemonEntity,
        spawnerName: String,
        category: String,
        condition: () -> Boolean
    ): Boolean {
        if (!condition()) {
            return false
        }

        val isEnabled = config.getBoolean("$category.enabled", true)
        if (!isEnabled) return false

        val langKey = "$category.SpawnMessage"
        val replacements = mapOf(
            "pokemon" to pokemonEntity.pokemon.species.name,
            "player" to spawnerName
        )

        // Send the message to all players
        pokemonEntity.server?.playerManager?.playerList?.forEach { player ->
            LangManager.send(player as ServerPlayerEntity, langKey, replacements)
        }

        return true
    }
}
