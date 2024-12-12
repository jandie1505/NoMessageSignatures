package net.jandie1505.nomessagesignatures;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.bukkit.command.*;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;

public class NoMessageSignatures extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    public static final String TARGET_VERSION = "v1_21_R3";
    private final Mode packetMode;
    private YamlConfiguration config;
    private File configFile;

    public NoMessageSignatures() {
        this.packetMode = new Mode();
    }

    @Override
    public void onEnable() {

        // The important stuff

        this.resetConfig();
        this.configFile = new File(this.getDataFolder(), "config.yml");

        this.reloadConfig();

        this.getServer().getPluginManager().registerEvents(this, this);

        PluginCommand command = this.getCommand(this.getName().toLowerCase());

        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        } else {
            this.getLogger().warning("Plugin command is not in plugin.yml");
        }

        this.packetMode.init(!this.config.getBoolean("disable_packet_mode", false) && this.hasCorrectVersion());

        // Info message

        this.getLogger().log(
                Level.INFO,
                "\n" +
                " _  _     __  __                          ___ _                _                   \n" +
                "| \\| |___|  \\/  |___ ______ __ _ __ _ ___/ __(_)__ _ _ _  __ _| |_ _  _ _ _ ___ ___\n" +
                "| .` / _ \\ |\\/| / -_|_-<_-</ _` / _` / -_)__ \\ / _` | ' \\/ _` |  _| || | '_/ -_|_-<\n" +
                "|_|\\_\\___/_|  |_\\___/__/__/\\__,_\\__, \\___|___/_\\__, |_||_\\__,_|\\__|\\_,_|_| \\___/__/\n" +
                "                                |___/          |___/                               \n" +
                "NoMessageSignatures (version " + this.getDescription().getVersion() + " for " + TARGET_VERSION + "), created by jandie1505\n"
        );

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

    private Connection getConnection(ServerPlayer serverPlayer) {

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

    // CONFIG

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
        try {

            if (!this.configFile.exists()) {
                this.configFile.getParentFile().mkdirs();
                this.configFile.createNewFile();
                this.config.save(this.configFile);
            }

            this.config.load(this.configFile);

        } catch (IOException | InvalidConfigurationException e) {
            this.getLogger().log(Level.WARNING, "Exception while loading config, using defaults", e);
        }
    }

    // EVENT LISTENER

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {

        if (this.packetMode.isPacketMode()) {

            // Get connection of player

            Connection connection = this.getConnection(((CraftPlayer) event.getPlayer()).getHandle());

            if (connection == null) {
                this.packetMode.disablePacketMode();
                this.getLogger().log(Level.WARNING, "Failed to get connection of player " + event.getPlayer().getUniqueId());
                return;
            }

            // Add outgoing channel handler to manage chat and server data packets (Client --> Server XXX Clients)

            try {

                connection.channel.pipeline().addBefore("packet_handler", this.getName() + "-WRITER", new ChannelOutboundHandlerAdapter() {

                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {

                        if (msg instanceof ClientboundPlayerChatPacket old) {
                            ClientboundDisguisedChatPacket packet = new ClientboundDisguisedChatPacket(old.unsignedContent() != null ? old.unsignedContent() : Component.literal(old.body().content()), old.chatType());
                            ctx.write(packet, promise);
                            return;
                        } else if (msg instanceof ClientboundServerDataPacket old) {

                            if (config.getBoolean("hide_banner", false)) {
                                ClientboundServerDataPacket packet = new ClientboundServerDataPacket(old.motd(), old.iconBytes());
                                ctx.write(packet, promise);
                                return;
                            }

                        }

                        ctx.write(msg, promise);
                    }

                });

            } catch (Exception e) {
                this.packetMode.disablePacketMode();
                this.getLogger().log(Level.WARNING, "Failed to add channel handler to pipeline of " + event.getPlayer().getUniqueId(), e);
            }

        }

        // Show protected message to players

        if (this.config.getBoolean("announce_protections", false)) {
            event.getPlayer().sendMessage(this.getProtectionMessage());
        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        if (this.packetMode.isPacketMode()) {

            Connection connection = this.getConnection(((CraftPlayer) event.getPlayer()).getHandle());

            if (connection == null) {
                return;
            }

            try {
                connection.channel.pipeline().remove(this.getName() + "-WRITER");
            } catch (Exception ignored) {
                // normally, the packer writer is already removed at this point
            }

        }

    }

    /**
     * This event is used if the packet mode is disabled.
     * It will cancel the chat event and send its message as server message to all players.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        if (this.packetMode.isPacketMode()) return;
        event.setCancelled(true);

        String formattedMessage = event.getFormat().replace("%1$s", event.getPlayer().getDisplayName()).replace("%2$s", event.getMessage());
        this.getLogger().log(Level.INFO, "Chat message: " + formattedMessage);

        for (Player player : event.getRecipients()) {
            player.sendMessage(formattedMessage);
        }

    }

    // COMMAND

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {

        if (args.length < 1) {
            sender.sendMessage(this.getProtectionMessage());
            return true;
        }

        switch (args[0]) {
            default -> sender.sendMessage("Unknown subcommand");
            case "reload" -> {

                if (sender != this.getServer().getConsoleSender()) {
                    sender.sendMessage("This command can only be executed by console");
                    return true;
                }

                this.reloadConfig();
                sender.sendMessage("Config reloaded.\nPlease note that this will NOT update the mode the plugin is using to prevent chat reporting.");
            }
            case "mode" -> sender.sendMessage("§7Current mode: " + (this.packetMode.isPacketMode() ? "§aPacket replacement" : "§cSystem messages"));
        }

        return true;
    }

    @Override
    @NotNull
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) return List.of();

        if (sender == this.getServer().getConsoleSender()) {
            return List.of("mode", "reload");
        } else {
            return List.of("mode");
        }

    }

    // GETTER

    public boolean isPacketMode() {
        return this.packetMode.isPacketMode();
    }

    public YamlConfiguration getPluginConfig() {
        return this.config;
    }

}
