package me.novoro.cobblemonbroadcaster

import me.novoro.cobblemonbroadcaster.commands.BroadcastCommands
import me.novoro.cobblemonbroadcaster.config.Configuration
import me.novoro.cobblemonbroadcaster.config.YamlConfiguration
import me.novoro.cobblemonbroadcaster.events.CaptureEvent
import me.novoro.cobblemonbroadcaster.events.SpawnEvent
import me.novoro.cobblemonbroadcaster.util.LangManager
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.luckperms.api.LuckPermsProvider
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class CobblemonBroadcaster : ModInitializer {
	val MOD_PREFIX: String = "<red><bold>[<gradient:#FF416C:#FF4B2B><bold>TᴜᴛᴏʀMᴏᴠᴇꜱ</gradient><red><bold>]&f "
	private var server: MinecraftServer? = null

	override fun onInitialize() {
		// novoro signature ;)
		displayAsciiArt()

		// Initialize configuration
		this.configManager()
		// Load language entries into LangManager
		LangManager.loadConfig(mainConfig)

		// Register all the commands available in the mod.
		registerCommands()

		// Defer event registration until the server is fully started
		registerServerLifecycleListeners()

	}

	/**
	 * Register all commands provided by the mod.
	 */
	private fun registerCommands() {
		CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
			BroadcastCommands.register(dispatcher)
		}
	}

	private fun registerServerLifecycleListeners() {
		// Capture server instance and register events
		ServerLifecycleEvents.SERVER_STARTED.register { minecraftServer ->
			server = minecraftServer
			setupPermissions()

			// Register events that require the server instance
			registerEventListeners()
		}

		// Listener for Player Joins (Relevant for FaintEvent)
		ServerPlayConnectionEvents.JOIN.register { handler: ServerPlayNetworkHandler, _, _ ->
			val player = handler.player
			playerLoginTimes[player.uuid] = System.currentTimeMillis()
		}
	}

	/**
	 * Initialize and setup permissions using the LuckPerms API.
	 * This method ensures the permissions system is active and running.
	 */
	private fun setupPermissions() {
		try {
			LuckPermsProvider.get()
			// Attempt to get an instance of LuckPermsProvider, signaling that permissions have been set up.
			LOGGER.info("Permissions system initialized!")
		} catch (e: Exception) {
			LOGGER.error("Failed to initialize permissions system!", e)
		}
	}

	fun configManager() {
		mainConfig = getConfig("config.yml")
		if (mainConfig == null) LOGGER.error("Failed to load main configuration. Default values will be used.")
		else LOGGER.info("Configuration loaded successfully.")

		val blacklistConfig = getConfig("world-blacklist.yml")
		if (blacklistConfig == null) LOGGER.error("Failed to load world-blacklist.yml!")
		else {
			me.novoro.cobblemonbroadcaster.util.BlacklistedWorlds.load(blacklistConfig)
			LOGGER.info("Blacklisted worlds loaded: {}", me.novoro.cobblemonbroadcaster.util.BlacklistedWorlds.getBlacklistedWorlds)
		}
	}

	@Throws(IOException::class)
	fun getOrCreateConfigurationFile(fileName: String): File {
		val configFolder = configFolder
		val configFile = File(configFolder, fileName)

		if (!configFile.exists()) {
			configFolder.mkdirs()
			val resourcePath = "/CobblemonBroadcaster/$fileName"
			val resourceStream = javaClass.getResourceAsStream(resourcePath)
				?: throw IOException("Default configuration file not found in resources: $resourcePath")

			resourceStream.use { input ->
				FileOutputStream(configFile).use { output ->
					input.copyTo(output)
				}
			}
		}
		return configFile
	}

	fun getConfig(fileName: String): Configuration? {
		var config: Configuration? = null
		try {
			config = YamlConfiguration.loadConfiguration(getOrCreateConfigurationFile(fileName))
		} catch (e: IOException) {
			e.printStackTrace()
		}
		return config
	}

	/**
	 *
	 * Displays an ASCII Art representation of the mod's name in the log.
	 */
	private fun displayAsciiArt() {
		LOGGER.info("\u001B[0;31m  ____ ____                      _               _            \u001B[0m")
		LOGGER.info("\u001B[0;31m / ___| __ ) _ __ ___   __ _  __| | ___ __ _ ___| |_ ___ _ __ \u001B[0m")
		LOGGER.info("\u001B[0;31m| |   |  _ \\| '__/ _ \\ / _` |/ _` |/ __/ _` / __| __/ _ \\ '__|\u001B[0m")
		LOGGER.info("\u001B[0;31m| |___| |_) | | | (_) | (_| | (_| | (_| (_| \\__ \\ ||  __/ |   \u001B[0m")
		LOGGER.info("\u001B[0;31m \\____|____/|_|  \\___/ \\__,_|\\__,_|\\___\\__,_|___/\\__\\___|_|   \u001B[0m")
		LOGGER.info("\u001B[0;31m  By Novoro: https://discord.gg/wzpp8jeJ9s                     \u001B[0m")
	}

	private fun registerEventListeners() {
		if (mainConfig != null && server != null) {
			SpawnEvent(mainConfig!!)
			me.novoro.cobblemonbroadcaster.events.FaintEvent(mainConfig!!, server!!)
			CaptureEvent(mainConfig!!)
		} else {
			LOGGER.error("Failed to register events: mainConfig or server is null.")
		}
		LOGGER.info("Event listeners registered!")
	}

	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger("CobblemonBroadcaster")
		// For each player's UUID, store the last time they joined (in ms).
		val playerLoginTimes = mutableMapOf<UUID, Long>()
		private var mainConfig: Configuration? = null
		private var serverInstance: MinecraftServer? = null

		fun getMainConfig(): Configuration? {
			return mainConfig
		}

		val configFolder: File
			get() {
				val configFolder = FabricLoader.getInstance().configDir.resolve("CobblemonBroadcaster").toFile()
				if (!configFolder.exists()) configFolder.mkdirs()
				return configFolder
			}

		/**
		 * Reloads configurations from the config file.
		 */
		fun reloadConfigurations() {
			try {
				mainConfig = YamlConfiguration.loadConfiguration(
					CobblemonBroadcaster().getOrCreateConfigurationFile("config.yml")
				)
				LangManager.loadConfig(mainConfig)
				LOGGER.info("Cobblemon Broadcaster configuration reloaded successfully.")
				val worldBlacklistConfig = YamlConfiguration.loadConfiguration(CobblemonBroadcaster().getOrCreateConfigurationFile("world-blacklist.yml"))
				me.novoro.cobblemonbroadcaster.util.BlacklistedWorlds.load(worldBlacklistConfig)
				LOGGER.info("Blacklisted worlds reloaded: {}", me.novoro.cobblemonbroadcaster.util.BlacklistedWorlds.getBlacklistedWorlds)

			} catch (e: Exception) {
				LOGGER.error("Failed to reload configuration: ${e.message}", e)
				throw e
			}
		}
	}
}