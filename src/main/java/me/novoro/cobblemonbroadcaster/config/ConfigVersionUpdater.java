package me.novoro.cobblemonbroadcaster.config;

import me.novoro.cobblemonbroadcaster.CobblemonBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigVersionUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigVersionUpdater.class);
    private final Configuration mainConfig;
    private final String currentVersion;

    public ConfigVersionUpdater(Configuration mainConfig, String currentVersion) {
        this.mainConfig = mainConfig;
        this.currentVersion = currentVersion;
    }

    public void updateConfig() {
        updateConfigFile(mainConfig, "config.yml");
    }

    private void updateConfigFile(Configuration config, String fileName) {
        String configVersion = config.getString("Config-Version", "1.2");

        // Only update if the current version is older than the new version
        if (isNewerVersion(configVersion, currentVersion)) {
            LOGGER.info("Updating " + fileName + " from version " + configVersion + " to " + currentVersion);
            config.set("Config-Version", currentVersion);

            saveConfigPreservingComments(new File(CobblemonBroadcaster.Companion.getConfigFolder(), fileName), currentVersion);
        } else {
            LOGGER.info(fileName + " is already up to date (version " + configVersion + "). No update needed.");
        }
    }

    private boolean isNewerVersion(String currentVersion, String newVersion) {
        String[] currentParts = currentVersion.split("\\.");
        String[] newParts = newVersion.split("\\.");

        int maxLength = Math.max(currentParts.length, newParts.length);

        for (int i = 0; i < maxLength; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int newPart = i < newParts.length ? Integer.parseInt(newParts[i]) : 0;

            if (currentPart < newPart) {
                return true; // The new version is greater
            } else if (currentPart > newPart) {
                return false; // The current version is greater, no update needed
            }
        }
        return false; // The versions are the same
    }

    private void saveConfigPreservingComments(File file, String newVersion) {
        try {
            Path filePath = file.toPath();
            String originalContent = Files.readString(filePath, StandardCharsets.UTF_8);
            String updatedContent = originalContent.replaceFirst("(?m)^Config-Version: .*$", "Config-Version: " + newVersion);
            Path tempFilePath = filePath.getParent().resolve(filePath.getFileName() + ".tmp");
            Files.writeString(tempFilePath, updatedContent, StandardCharsets.UTF_8);
            Files.move(tempFilePath, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Failed to preserve comments while saving config: " + file.getName(), e);
        }
    }
}
