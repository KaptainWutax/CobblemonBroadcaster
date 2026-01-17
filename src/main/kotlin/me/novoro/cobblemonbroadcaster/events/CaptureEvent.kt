package me.novoro.cobblemonbroadcaster.events

import me.novoro.cobblemonbroadcaster.config.Configuration
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent
import com.cobblemon.mod.common.api.pokemon.aspect.AspectProvider
import com.cobblemon.mod.common.api.reactive.ObservableSubscription
import com.cobblemon.mod.common.pokemon.Pokemon
import me.novoro.cobblemonbroadcaster.util.BlacklistedWorlds
import me.novoro.cobblemonbroadcaster.util.LabelHelper
import me.novoro.cobblemonbroadcaster.util.LangManager
import me.novoro.cobblemonbroadcaster.util.SimpleLogger
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld

class CaptureEvent(private val config: Configuration) {

    private var captureEvent: ObservableSubscription<PokemonCapturedEvent>? = null

    fun unsubscribe() {
        captureEvent?.unsubscribe()
    }

    init {
        captureEvent = CobblemonEvents.POKEMON_CAPTURED.subscribe(priority = Priority.LOWEST) { event ->

            val pokemon = event.pokemon
            val player = event.player

            // Blacklist Stuff
            val world = event.player.world as? ServerWorld
            val worldName = world?.registryKey?.value.toString()
            if (BlacklistedWorlds.isBlacklisted(worldName)) return@subscribe

            val aspects = AspectProvider.providers.flatMap { it.provide(pokemon) }
            val labels = LabelHelper.filterValidLabels(pokemon.species.labels.map { it })
            val allIdentifiers = mutableSetOf<String>()
            allIdentifiers.addAll(aspects)
            allIdentifiers.addAll(labels)

            // Debugging: Log all aspects of the Pokémon
            SimpleLogger.debug("Pokemon ${pokemon.species.name} caught by ${player.name.string} has aspects: $aspects, labels: $labels")

            // Dynamically check user-defined aspects first
            config.keys.forEach { customCategory ->
                if (customCategory !in setOf("shiny", "legendary", "mythical", "ultrabeast")) {
                    if (customCategory in allIdentifiers) {
                        if (handleCategory(pokemon, player, customCategory) { true }) return@subscribe
                    }
                }
            }

            // Check categories with guard clauses in priority order
            if (handleCategory(pokemon, player, "mythical") { pokemon.isMythical() }) return@subscribe
            if (handleCategory(pokemon, player, "legendary") { pokemon.isLegendary() }) return@subscribe
            if (handleCategory(pokemon, player, "ultrabeast") { pokemon.isUltraBeast() }) return@subscribe
            if (handleCategory(pokemon, player, "shiny") { pokemon.shiny }) return@subscribe
        }
    }

    private fun handleCategory(
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

        val isGlobalAlert = config.getBoolean("$category.Global-Alert", true)

        // Send message based on broadcast setting
        if (isGlobalAlert) {
            player.server?.playerManager?.playerList?.forEach { targetPlayer ->
                LangManager.send(targetPlayer as ServerPlayerEntity, langKey, replacements)
            }
        }
        else LangManager.send(player, langKey, replacements)

        return true
    }
}
