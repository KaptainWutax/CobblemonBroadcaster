package me.novoro.cobblemonbroadcaster.util

import me.novoro.cobblemonbroadcaster.CobblemonBroadcaster.Companion.LOGGER
import net.luckperms.api.LuckPermsProvider

object LuckPerms {
    init {
        try {
            LuckPermsProvider.get()
            // Attempt to get an instance of LuckPermsProvider, signaling that permissions have been set up.
            LOGGER.info("Permissions system initialized!")
        } catch (e: Exception) {
            LOGGER.error("Failed to initialize permissions system!", e)
        }
    }
}