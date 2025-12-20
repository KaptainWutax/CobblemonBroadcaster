package me.novoro.cobblemonbroadcaster.util

import com.cobblemon.mod.common.api.pokemon.labels.CobblemonPokemonLabels
import java.util.*

object LabelHelper {

    val COBBLEMON_LABELS = setOf(
        CobblemonPokemonLabels.LEGENDARY,
        CobblemonPokemonLabels.MYTHICAL,
        CobblemonPokemonLabels.ULTRA_BEAST,
        CobblemonPokemonLabels.RESTRICTED,
        CobblemonPokemonLabels.FOSSIL,
        CobblemonPokemonLabels.POWERHOUSE,
        CobblemonPokemonLabels.BABY,
        CobblemonPokemonLabels.REGIONAL,

        CobblemonPokemonLabels.KANTONIAN_FORM,
        CobblemonPokemonLabels.JOHTONIAN_FORM,
        CobblemonPokemonLabels.HOENNIAN_FORM,
        CobblemonPokemonLabels.SINNOHAN_FORM,
        CobblemonPokemonLabels.UNOVAN_FORM,
        CobblemonPokemonLabels.KALOSIAN_FORM,
        CobblemonPokemonLabels.ALOLAN_FORM,
        CobblemonPokemonLabels.GALARIAN_FORM,
        CobblemonPokemonLabels.HISUIAN_FORM,
        CobblemonPokemonLabels.PALDEAN_FORM,

        CobblemonPokemonLabels.MEGA,
        CobblemonPokemonLabels.PRIMAL,
        CobblemonPokemonLabels.GMAX,
        CobblemonPokemonLabels.TOTEM,
        CobblemonPokemonLabels.PARADOX,

        CobblemonPokemonLabels.GENERATION_1,
        CobblemonPokemonLabels.GENERATION_2,
        CobblemonPokemonLabels.GENERATION_3,
        CobblemonPokemonLabels.GENERATION_4,
        CobblemonPokemonLabels.GENERATION_5,
        CobblemonPokemonLabels.GENERATION_6,
        CobblemonPokemonLabels.GENERATION_7,
        CobblemonPokemonLabels.GENERATION_7B,
        CobblemonPokemonLabels.GENERATION_8,
        CobblemonPokemonLabels.GENERATION_8A,
        CobblemonPokemonLabels.GENERATION_9,

        CobblemonPokemonLabels.CUSTOM,
        CobblemonPokemonLabels.CUSTOMIZED_OFFICIAL
    )

    fun filterValidLabels(labels: Collection<String>): Set<String> {
        return labels.filter { labelString ->
            COBBLEMON_LABELS.any { cobblemonLabel ->
                cobblemonLabel.equals(labelString, ignoreCase = true)
            }
        }.map { it.lowercase(Locale.getDefault()) }.toSet()
    }
}
