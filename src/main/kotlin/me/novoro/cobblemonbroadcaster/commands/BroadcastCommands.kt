package me.novoro.cobblemonbroadcaster.commands

import me.novoro.cobblemonbroadcaster.CobblemonBroadcaster
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource

object BroadcastCommands {
    // Permission node for the `/cobblemonbroadcaster reload` command
    const val RELOAD_PERMISSION_NODE: String = "cobblemonbroadcaster.reload"

    /**
     * Registers the Cobblemon Broadcaster commands.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    fun register(dispatcher: CommandDispatcher<ServerCommandSource?>) {
        dispatcher.register(
            CommandManager.literal("cobblemonbroadcaster")
                .then(
                    CommandManager.literal("reload")
                        .requires(Permissions.require(RELOAD_PERMISSION_NODE, 2))
                        .executes { ctx -> reloadConfigs(ctx) }
                )
        )
    }

    /**
     * Reloads the Cobblemon Broadcaster configurations.
     *
     * @param ctx The command context.
     * @return 1 if successful, 0 otherwise.
     */
    private fun reloadConfigs(ctx: CommandContext<ServerCommandSource>): Int {
        try {
            CobblemonBroadcaster.reloadConfigurations() // Reload configurations
            ctx.source.sendFeedback(
                { net.minecraft.text.Text.literal("Cobblemon Broadcaster configuration reloaded successfully.") },
                false
            )
            return Command.SINGLE_SUCCESS
        } catch (e: Exception) {
            ctx.source.sendError(net.minecraft.text.Text.literal("Failed to reload configurations: ${e.message}"))
            return 0
        }
    }
}
