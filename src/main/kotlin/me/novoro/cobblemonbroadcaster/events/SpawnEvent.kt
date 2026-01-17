package me.novoro.cobblemonbroadcaster.events

import me.novoro.cobblemonbroadcaster.config.Configuration
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.cooking.PokeSnackSpawnPokemonEvent
import com.cobblemon.mod.common.api.events.entity.SpawnEvent
import com.cobblemon.mod.common.api.pokemon.aspect.AspectProvider
import com.cobblemon.mod.common.api.reactive.ObservableSubscription
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition
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
    var spawnEvent: ObservableSubscription<SpawnEvent<PokemonEntity>>? = null
    var snackSpawnEvent: ObservableSubscription<PokeSnackSpawnPokemonEvent.Post>? = null

    fun unsubscribe() {
        spawnEvent?.unsubscribe()
        snackSpawnEvent?.unsubscribe()
    }

    init {
        spawnEvent = CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(priority = Priority.LOWEST) { event ->
            // Ignore PokéSnacks - They don't have any aspects influenced by the snack in this event
            // Also ignore cancelled events - The Pokémon didn't fully spawn
            if (event.spawnablePosition.spawner.name.startsWith("poke_snack")
                || event.isCanceled) return@subscribe

            handleSpawn(event.entity, event.spawnablePosition)
        }
        snackSpawnEvent = CobblemonEvents.POKE_SNACK_SPAWN_POKEMON_POST.subscribe(priority = Priority.LOWEST) { event ->
            handleSpawn(event.pokemonEntity, event.spawnAction.spawnablePosition, true)
        }
    }

    private fun handleSpawn(pokemonEntity: PokemonEntity, spawnablePosition: SpawnablePosition, isSnack: Boolean = false) {
        // Blacklist Stuff
        val world = pokemonEntity.world as? ServerWorld
        val worldName = world?.registryKey?.value.toString()
        if (BlacklistedWorlds.isBlacklisted(worldName)) return

        // Aspects and Labels
        val aspects = AspectProvider.providers.flatMap { it.provide(pokemonEntity.pokemon) }
        val labels = pokemonEntity.pokemon.species.labels
        val allIdentifiers = mutableSetOf<String>()
        allIdentifiers.addAll(aspects)

        val label = LabelHelper.filterValidLabels(labels.map { it })
        allIdentifiers.addAll(label)

        val pos = spawnablePosition.position

        // Debugging: Log all aspects of the Pokémon
        SimpleLogger.debug("Pokemon ${pokemonEntity.pokemon.species.name} spawned by ${spawnablePosition.spawner.name} has aspects: $aspects, labels: $label")

        // Dynamically check user-defined identifiers (aspects AND labels)
        config.keys.forEach { customCategory ->
            if (customCategory !in setOf("shiny", "legendary", "mythical", "ultrabeast")) {
                if (customCategory in allIdentifiers) {
                    if (handleCategory(pokemonEntity, spawnablePosition.spawner.name, customCategory, pos, isSnack) { true }) return
                }
            }
        }

        if (handleCategory(pokemonEntity, spawnablePosition.spawner.name, "mythical", pos, isSnack) { pokemonEntity.pokemon.isMythical() }) return
        if (handleCategory(pokemonEntity, spawnablePosition.spawner.name, "legendary", pos, isSnack) { pokemonEntity.pokemon.isLegendary() }) return
        if (handleCategory(pokemonEntity, spawnablePosition.spawner.name, "ultrabeast", pos, isSnack) { pokemonEntity.pokemon.isUltraBeast() }) return
        if (handleCategory(pokemonEntity, spawnablePosition.spawner.name, "shiny", pos, isSnack) { pokemonEntity.pokemon.shiny }) return
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
            "dimension" to PlaceholderUtils.getWorldName(world),
            "biome" to PlaceholderUtils.getBiomeTranslatable(biomeId)
        )

        val isGlobalAlert = config.getBoolean("$category.Global-Alert", true)

        // Send message based on broadcast setting
        if (isGlobalAlert || isSnack) {
            pokemonEntity.server?.playerManager?.playerList?.forEach { player ->
                LangManager.send(player as ServerPlayerEntity, langKey, replacements)
            }
        }
        else {
            val targetPlayer = pokemonEntity.server?.playerManager?.playerList?.find { player ->
                player.name.string.equals(spawnerName, ignoreCase = true)
            }

            targetPlayer?.let {
                LangManager.send(it, langKey, replacements)
            }
        }

        return true
    }
}
