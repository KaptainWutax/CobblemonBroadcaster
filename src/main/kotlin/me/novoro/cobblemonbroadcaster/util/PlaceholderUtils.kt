package me.novoro.cobblemonbroadcaster.util

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Gender
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier


object PlaceholderUtils {

    fun getGenderReplacements(pokemonEntity: PokemonEntity): String {
        return when (pokemonEntity.pokemon.gender) {
            Gender.MALE -> "♂"
            Gender.FEMALE -> "♀"
            else -> "⚲"
        }
    }

    fun getWorldName(world: ServerWorld): String {
        val worldKey = world.registryKey.value
        return formatDimensionName(worldKey.toString())
    }

    fun getBiomeTranslatable(biomeId: Identifier): String {
        return "<lang:biome.${biomeId.namespace}.${biomeId.path}>"
    }

    private fun formatDimensionName(dimensionId: String): String {
        val withoutNamespace =
            if (dimensionId.contains(":")) dimensionId.substring(dimensionId.indexOf(":") + 1)
            else dimensionId

        return withoutNamespace
            // Replace underscores with spaces
            .replace("_", " ")
            // Split by spaces
            .split(" ")
            .joinToString(" ") { word ->
                // Capitalize first letter of each word
                word.replaceFirstChar { it.uppercase() }
            }
            .trim()
    }

}