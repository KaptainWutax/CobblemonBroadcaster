package me.novoro.cobblemonbroadcaster.events

import me.novoro.cobblemonbroadcaster.config.Configuration
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.pokemon.aspect.AspectProvider
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import me.novoro.cobblemonbroadcaster.util.LangManager
import me.novoro.cobblemonbroadcaster.util.LabelHelper
import me.novoro.cobblemonbroadcaster.util.PlaceholderUtils
import me.novoro.cobblemonbroadcaster.util.BlacklistedWorlds
import me.novoro.cobblemonbroadcaster.util.SimpleLogger
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos;

class SpawnEvent(private val config: Configuration) {

    //TODO Add Multiple Spec-Support (Shiny Legendary, Legendary Galarian, etc.)
    //TODO Option to send it to player it spawns on vs Global
    //TODO Load Keys in when reload

    init {
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(priority = Priority.LOWEST) { event ->

            val pokemonEntity = event.entity

            // Aspects and Labels
            val aspects = AspectProvider.providers.flatMap { it.provide(pokemonEntity.pokemon) }
            val labels = pokemonEntity.pokemon.species.labels
            val allIdentifiers = mutableSetOf<String>()
            allIdentifiers.addAll(aspects)

            val label = LabelHelper.filterValidLabels(labels.map { it.toString()})
            allIdentifiers.addAll(label)

            // Spawner + Spawner Name (WTF IS A POKESNACK!!!)
            val spawnerType = event.spawnablePosition.spawner
            val spawnerName = spawnerType.name
            val pos = event.spawnablePosition.position
            val isSnack = spawnerName.startsWith("poke_snack")

            // Blacklist Stuff
            val world = event.entity.world as? ServerWorld
            val worldName = world?.registryKey?.value.toString()
            if (BlacklistedWorlds.isBlacklisted(worldName)) {
                return@subscribe
            }


            // Debugging: Log all aspects of the Pokémon
            SimpleLogger.debug("Pokemon ${pokemonEntity.pokemon.species.name} has aspects: $aspects, labels: $label")

            // Dynamically check user-defined identifiers (aspects AND labels)
            config.keys.forEach { customCategory ->
                if (customCategory !in setOf("shiny", "legendary", "mythical", "ultrabeast")) {
                    if (customCategory in allIdentifiers) {
                        if (handleCategory(pokemonEntity, event.spawnablePosition.spawner.name, customCategory, pos, isSnack) { true }) return@subscribe
                    }
                }
            }

            if (handleCategory(pokemonEntity, event.spawnablePosition.spawner.name, "mythical", pos, isSnack) { pokemonEntity.pokemon.isMythical() }) return@subscribe
            if (handleCategory(pokemonEntity, event.spawnablePosition.spawner.name, "legendary", pos, isSnack) { pokemonEntity.pokemon.isLegendary() }) return@subscribe
            if (handleCategory(pokemonEntity, event.spawnablePosition.spawner.name, "ultrabeast", pos, isSnack) { pokemonEntity.pokemon.isUltraBeast() }) return@subscribe
            if (handleCategory(pokemonEntity, event.spawnablePosition.spawner.name, "shiny", pos, isSnack) { pokemonEntity.pokemon.shiny }) return@subscribe
        }
    }

    private fun handleCategory(
        pokemonEntity: PokemonEntity,
        spawnerName: String,
        category: String,
        spawnPos: BlockPos,
        isSnack: Boolean,
        condition: () -> Boolean
    ): Boolean {

        if (!condition()) return false

        val langKey = if (isSnack) "$category.SpawnMessage-Snack"
        else "$category.SpawnMessage"

        // Get world and biome information
        val world = pokemonEntity.world as ServerWorld
        val biome = world.getBiome(spawnPos).value()
        val biomeRegistry = world.registryManager.get(RegistryKeys.BIOME)
        val biomeId = biomeRegistry.getId(biome) ?: Identifier("minecraft", "plains")

        val replacements = mapOf(
            "pokemon" to pokemonEntity.pokemon.species.name,
            "gender" to PlaceholderUtils.getGenderReplacements(pokemonEntity),
            "player" to spawnerName,
            "x" to spawnPos.x.toString(),
            "y" to spawnPos.y.toString(),
            "z" to spawnPos.z.toString(),
            "dimension" to PlaceholderUtils.getWorldName(pokemonEntity.world as ServerWorld),
            "biome" to PlaceholderUtils.getBiomeTranslatable(biomeId)
        )

        // Send the message to all players
        pokemonEntity.server?.playerManager?.playerList?.forEach { player ->
            LangManager.send(player as ServerPlayerEntity, langKey, replacements)
        }

        return true
    }
}
