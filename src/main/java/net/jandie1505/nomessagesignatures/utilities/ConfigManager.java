package net.jandie1505.nomessagesignatures.utilities;

import net.jandie1505.nomessagesignatures.NoMessageSignatures;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public final class ConfigManager {
    @NotNull private final NoMessageSignatures plugin;
    @NotNull private YamlConfiguration config;

    public ConfigManager(@NotNull NoMessageSignatures plugin) {
        this.plugin = plugin;
        this.resetConfig();
    }

    /**
     * Resets the config to the default values.
     */
    public void resetConfig() {
        this.config = new YamlConfiguration();

        this.config.set("disable_packet_mode", false);
        this.config.setComments("disable_packet_mode", List.of(
                "The plugin can use 2 different ways to prevent chat reporting.",
                "- Packet replacement:",
                "  This will remove the messages signatures on packet level.",
                "  It replaces the ClientboundPlayerChatPackets with DisguisedPlayerChatPackets (which have no signature).",
                "  Any chat plugin should work as intended in this mode, since the modification is made after the AsyncPlayerChatEvent of Bukkit.",
                "  The drawback of this mode is that it requires the exact server version the plugin is made for.",
                "- System messages:",
                "  Cancels the AsyncPlayerChatEvent at the HIGHEST priority and sends the message as a system message to all recipients (if the event has not been cancelled before).",
                "  Any chat plugin should work as intended AS LONG AS it doesn't modify the chat event in the HIGHEST or MONITOR priority.",
                "  SINCE PRIVATE MESSAGES (/me, /say, /msg, /tell, ...) ARE NOT AFFECTED BY THE CHAT EVENT, THEY ARE STILL SIGNED AND REPORTABLE!",
                "  This mode also should work if the server does not have the version the plugin was made for (but only if the AsyncPlayerChatEvent has not been changed).",
                "The plugin automatically uses 'Packet replacement' mode if it is available. If not, 'System messages' mode will be used.",
                "",
                "This option forces the plugin to use the 'System messages' mode and disables the 'Packet replacement' mode (not recommended, read the information above)."
        ));

        this.config.set("hide_banner", false);
        this.config.setComments("hide_banner", List.of(
                "Hides the 'Chat Messages cannot be verified on this server' banner.",
                "This does only work if the plugin is using the 'Packet replacement' mode."
        ));

        this.config.set("announce_protections", true);
        this.config.setComments("announce_protections", List.of("If this is enabled, which type of messages are encrypted and which are not."));

    }

    /**
     * Reloads the config file.
     */
    public void reloadConfig() {
        boolean success = this.loadConfig();
        if (!success) this.saveConfig();
    }

    public boolean loadConfig() {
        try {

            File configFile = this.getConfigFile();

            if (!configFile.exists()) {
                this.plugin.getLogger().log(Level.WARNING, "Failed to load config file: File does not exist");
                return false;
            }

            this.config.load(configFile);

            this.plugin.getLogger().log(Level.INFO, "Config loaded successfully");
            return true;
        } catch (IOException | InvalidConfigurationException e) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to load config file: Exception occurred", e);
            return false;
        }
    }

    public boolean saveConfig() {
        try {

            File configFile = this.getConfigFile();

            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            }

            this.config.save(configFile);

            this.plugin.getLogger().log(Level.INFO, "Config saved successfully");
            return true;
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to save config file: Exception occurred", e);
            return false;
        }
    }

    private File getConfigFile() {
        return new File(this.plugin.getDataFolder(), "config.yml");
    }

    public @NotNull NoMessageSignatures getPlugin() {
        return plugin;
    }

    public @NotNull YamlConfiguration getConfig() {
        return config;
    }

}
