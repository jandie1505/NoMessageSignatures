package net.jandie1505.nomessagesignatures;

import net.jandie1505.nomessagesignatures.commands.NMSCommand;
import net.jandie1505.nomessagesignatures.listeners.EventListener;
import net.jandie1505.nomessagesignatures.utilities.ConfigManager;
import net.jandie1505.nomessagesignatures.utilities.Mode;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.bukkit.command.*;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;

public final class NoMessageSignatures extends JavaPlugin {
    public static final String TARGET_VERSION = "v1_21_R3";
    private final Mode packetMode;
    private final ConfigManager configManager;
    private File configFile;

    public NoMessageSignatures() {
        this.packetMode = new Mode();
        this.configManager = new ConfigManager(this);
    }

    @Override
    public void onEnable() {

        // Config

        this.configManager.reloadConfig();

        // Listeners

        this.getServer().getPluginManager().registerEvents(new EventListener(this), this);

        // Commands

        PluginCommand command = this.getCommand(this.getName().toLowerCase());

        if (command != null) {
            NMSCommand cmd = new NMSCommand(this);
            command.setExecutor(cmd);
            command.setTabCompleter(cmd);
        } else {
            this.getLogger().warning("Plugin command is not in plugin.yml");
        }

        // Packet mode

        this.packetMode.init(!this.getConfig().getBoolean("disable_packet_mode", false) && this.hasCorrectVersion());

        // Info message

        this.getLogger().log(Level.INFO, getEnabledMessage());

        if (!this.hasCorrectVersion()) {
            this.getLogger().log(
                    Level.WARNING,
                    "Server version " + TARGET_VERSION + " is not compatible with current server version.\n" +
                    "Some features might not work as intended.\n" +
                    "Consider checking if there is an update at https://github.com/jandie1505/NoMessageSignatures/releases."
            );
        }

        if (!this.packetMode.isPacketMode()) this.getLogger().log(Level.WARNING, "Packet mode not enabled.");

    }

    // UTILITIES

    /**
     * Returns true if the server has the correct version for Packet replacement mode to work.
     */
    private boolean hasCorrectVersion() {
        try {
            Class.forName("org.bukkit.craftbukkit." + TARGET_VERSION + ".entity.CraftPlayer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public String getProtectionMessage() {
        return "§7The following types of chat messages are protected from chat reporting:\n" +
                " " + "§7[§a✔§7]" + " §7Chat messages\n" +
                " " + (this.packetMode.isPacketMode() ? "§7[§a✔§7]" : "§7[§c❌§7]") + " §7/say, /me and other public messaging commands\n" +
                " " + (this.packetMode.isPacketMode() ? "§7[§a✔§7]" : "§7[§c❌§7]") + " §7/msg, /tell and other private messaging commands";
    }

    public Connection getConnection(ServerPlayer serverPlayer) {

        try {
            ServerCommonPacketListenerImpl serverGamePacketListener = serverPlayer.connection;

            Field field = ServerCommonPacketListenerImpl.class.getDeclaredField("e");
            field.setAccessible(true);

            return  (Connection) field.get(serverGamePacketListener);
        } catch (Exception e) {
            this.getLogger().log(Level.WARNING, "Exception while getting connection of player " + serverPlayer.getUUID(), e);
            return null;
        }

    }

    public String getEnabledMessage() {
        return "\n" +
                " _  _     __  __                          ___ _                _                   \n" +
                "| \\| |___|  \\/  |___ ______ __ _ __ _ ___/ __(_)__ _ _ _  __ _| |_ _  _ _ _ ___ ___\n" +
                "| .` / _ \\ |\\/| / -_|_-<_-</ _` / _` / -_)__ \\ / _` | ' \\/ _` |  _| || | '_/ -_|_-<\n" +
                "|_|\\_\\___/_|  |_\\___/__/__/\\__,_\\__, \\___|___/_\\__, |_||_\\__,_|\\__|\\_,_|_| \\___/__/\n" +
                "                                |___/          |___/                               \n" +
                "NoMessageSignatures (version " + this.getDescription().getVersion() + " for " + TARGET_VERSION + "), created by jandie1505\n";
    }

    // GETTER

    public Mode getPacketMode() {
        return this.packetMode;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    @Override
    public @NotNull YamlConfiguration getConfig() {
        return this.configManager.getConfig();
    }

}
