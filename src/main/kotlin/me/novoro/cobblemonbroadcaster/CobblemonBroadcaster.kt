package me.novoro.cobblemonbroadcaster

import com.cobblemon.mod.common.api.events.pokemon.FossilRevivedEvent
import me.novoro.cobblemonbroadcaster.commands.BroadcastCommands
import me.novoro.cobblemonbroadcaster.config.Configuration
import me.novoro.cobblemonbroadcaster.config.YamlConfiguration
import me.novoro.cobblemonbroadcaster.events.CaptureEvent
import me.novoro.cobblemonbroadcaster.events.FaintEvent
import me.novoro.cobblemonbroadcaster.events.FossilEvent
import me.novoro.cobblemonbroadcaster.events.SpawnEvent
import me.novoro.cobblemonbroadcaster.util.LangManager
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CobblemonBroadcaster : ModInitializer {

	override fun onInitialize() {
		displayAsciiArt()
		this.configManager()
		LangManager.loadConfig(mainConfig)
		registerCommands()
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
			serverInstance = minecraftServer

			// Register events that require the server instance
			registerEventListeners()
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

	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger("CobblemonBroadcaster")
		private var mainConfig: Configuration? = null
		private var serverInstance: MinecraftServer? = null
		private var spawnEvent: SpawnEvent? = null
		private var faintEvent: FaintEvent? = null
		private var captureEvent: CaptureEvent? = null
        private var fossilEvent: FossilEvent? = null

		val configFolder: File
			get() {
				val configFolder = FabricLoader.getInstance().configDir.resolve("CobblemonBroadcaster").toFile()
				if (!configFolder.exists()) configFolder.mkdirs()
				return configFolder
			}

		// i hate kotlin and refuse to learn it
		private fun registerEventListeners() {
			if (mainConfig != null && serverInstance != null) {
				spawnEvent?.unsubscribe()
				faintEvent?.unsubscribe()
				captureEvent?.unsubscribe()
                fossilEvent?.unsubscribe()
				spawnEvent = SpawnEvent(mainConfig!!)
				faintEvent = FaintEvent(mainConfig!!, serverInstance!!)
				captureEvent = CaptureEvent(mainConfig!!)
                fossilEvent = FossilEvent(mainConfig!!)
			} else {
				LOGGER.error("Failed to register events: mainConfig or server is null.")
			}
			LOGGER.info("Event listeners registered!")
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
				registerEventListeners()
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
