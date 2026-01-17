package me.novoro.cobblemonbroadcaster.events

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent
import com.cobblemon.mod.common.api.pokemon.aspect.AspectProvider
import com.cobblemon.mod.common.api.reactive.ObservableSubscription
import me.novoro.cobblemonbroadcaster.config.Configuration
import me.novoro.cobblemonbroadcaster.util.BlacklistedWorlds
import me.novoro.cobblemonbroadcaster.util.LabelHelper
import me.novoro.cobblemonbroadcaster.util.LangManager
import me.novoro.cobblemonbroadcaster.util.SimpleLogger
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld

class FaintEvent(private val config: Configuration, private val server: MinecraftServer) {

    // Cache to avoid duplicate announcements
    private val faintedPokemonCache = mutableSetOf<String>()
    private var faintEvent: ObservableSubscription<BattleFaintedEvent>? = null

    fun unsubscribe() {
        faintEvent?.unsubscribe()
    }

    init {
        faintEvent = CobblemonEvents.BATTLE_FAINTED.subscribe(priority = Priority.LOWEST) { event ->

            val pokemon = event.killed.effectedPokemon
            val player = event.killed.actor.getName()

            // Blacklist Stuff
            // How tf do I get the world from a pokemon battle properly, a killed entity may not always exist
            val world = server.playerManager.getPlayer(player.string)?.world as? ServerWorld
            val worldName = world?.registryKey?.value.toString()
            if (BlacklistedWorlds.isBlacklisted(worldName)) {
                return@subscribe
            }

            // Ensure the Pokémon is wild and not already processed
            // why tf does isNPCOwned() get ignored if not accompanied with !isWild()!!!
            if (pokemon.isPlayerOwned() || pokemon.isNPCOwned() || !pokemon.isWild()) return@subscribe
            if (faintedPokemonCache.contains(pokemon.uuid.toString())) return@subscribe

            // Check if the Pokémon is a boss (if applicable). Love u Guitar pookie
            val nbt = pokemon.persistentData
            val isBoss = nbt.getBoolean("boss")
            if (isBoss) return@subscribe

            val aspects = AspectProvider.providers.flatMap { it.provide(pokemon) }
            val labels = LabelHelper.filterValidLabels(pokemon.species.labels.map { it })
            val allIdentifiers = mutableSetOf<String>()
            allIdentifiers.addAll(aspects)
            allIdentifiers.addAll(labels)

            SimpleLogger.debug("Pokemon ${pokemon.species.name} killed by ${player.string} has aspects: $aspects, labels: $labels")

            // Dynamically check user-defined aspects first
            config.keys.forEach { customCategory ->
                if (customCategory !in setOf("shiny", "legendary", "mythical", "ultrabeast")) {
                    if (customCategory in allIdentifiers) {
                        if (handlePokemonCategory(pokemon, player.toString(),customCategory) { true }) return@subscribe
                    }
                }
            }

            // Check categories with guard clauses in priority order
            if (handlePokemonCategory(pokemon, player.toString(), "mythical") { pokemon.isMythical() }) return@subscribe
            if (handlePokemonCategory(pokemon, player.toString(),"legendary") { pokemon.isLegendary() }) return@subscribe
            if (handlePokemonCategory(pokemon, player.toString(),"ultrabeast") { pokemon.isUltraBeast() }) return@subscribe
            if (handlePokemonCategory(pokemon, player.toString(),"shiny") { pokemon.shiny }) return@subscribe

            // Add the Pokémon to the cache
            faintedPokemonCache.add(pokemon.uuid.toString())
        }
    }

    private fun handlePokemonCategory(
        pokemon: com.cobblemon.mod.common.pokemon.Pokemon,
        playerName: String?,
        category: String,
        condition: () -> Boolean
    ): Boolean {
        if (!condition()) {
            return false
        }

        val langKey = "$category.FaintMessage"
        val replacements = mapOf(
            "pokemon" to pokemon.species.name
        )

        val isGlobalAlert = config.getBoolean("$category.Global-Alert", true)

        if (isGlobalAlert) {
            server.playerManager.playerList.forEach { player ->
                LangManager.send(player, langKey, replacements)
            }
        } else if (playerName != null) {
            server.playerManager.playerList
                .find { it.name.string.equals(playerName, ignoreCase = true) }
                ?.let { LangManager.send(it, langKey, replacements) }
        }

        return true
    }
}