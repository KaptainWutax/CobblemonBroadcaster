package CobblemonBroadcaster.util

import CobblemonBroadcaster.config.Configuration
import java.util.*

/**
 * Stores and manages the list of blacklisted worlds.
 */
object BlacklistedWorlds {
    private val BLACKLISTED_WORLDS: MutableSet<String> = HashSet()

    /**
     * Loads the blacklisted worlds from the given Configuration object.
     */
    fun load(config: Configuration?) {
        BLACKLISTED_WORLDS.clear()

        if (config == null) {
            return
        }

        // Attempt to get the list from the config
        val list = config.getStringList("Blacklisted-Worlds")
        if (list != null) {
            BLACKLISTED_WORLDS.addAll(list)
        }
    }

    /**
     * Checks if a given world name is on the blacklist.
     */
    fun isBlacklisted(worldName: String): Boolean {
        return BLACKLISTED_WORLDS.contains(worldName)
    }

    val getblacklistedWorlds: Set<String>
        /**
         * If you want direct access to the unmodifiable set, use this
         */
        get() = Collections.unmodifiableSet(BLACKLISTED_WORLDS)
}