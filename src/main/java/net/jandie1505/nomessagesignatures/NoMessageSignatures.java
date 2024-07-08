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
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.command.*;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.management.AttributeNotFoundException;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;

public class NoMessageSignatures extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private YamlConfiguration config;
    private boolean useAlternativeMode;
    private File configFile;
    private boolean error;

    @Override
    public void onEnable() {
        this.error = false;

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

    }

    // UTILITIES

    private void setUseAlternativeMode() {

        switch (this.config.getString("mode", "auto")) {
            case "remove_signatures" -> this.useAlternativeMode = false;
            case "system_messages" -> this.useAlternativeMode = true;
            default -> {

                try {
                    Class.forName("org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer");
                    this.useAlternativeMode = true;
                } catch (ClassNotFoundException e) {
                    this.useAlternativeMode = true;
                }

            }
        }

    }

    public String getProtectionMessage() {
        return "§7The following types of chat messages are protected from chat reporting:\n" +
                " " + "§7[§a✔§7]" + " §7Chat messages\n" +
                " " + (!this.useAlternativeMode ? "§7[§a✔§7]" : "§7[§c❌§7]") + " §7/say, /me and other public messaging commands\n" +
                " " + (!this.useAlternativeMode ? "§7[§a✔§7]" : "§7[§c❌§7]") + " §7/msg, /tell and other private messaging commands";
    }

    public String getNotProtectedMessage() {
        return "\n§4§l ---------- WARNING! ----------\n" +
                "§4An error with NoMessageSignatures occurred.§r\n" +
                "§4§lYOU ARE NO LONGER PROTECTED FROM CHAT REPORTING!§r\n" +
                "§4Consider not using the chat at all until this error is fixed.§r\n" +
                "§4§l--------------------§r\n";
    }

    private Connection getConnection(ServerPlayer serverPlayer) {

        try {
            ServerCommonPacketListenerImpl serverGamePacketListener = serverPlayer.connection;

            Field field = ServerCommonPacketListenerImpl.class.getDeclaredField("e");
            field.setAccessible(true);

            return  (Connection) field.get(serverGamePacketListener);
        } catch (Exception e) {
            this.error = true;
            this.getLogger().log(Level.WARNING, "Exception while getting connection of player " + serverPlayer.getUUID(), e);
            return null;
        }

    }

    // CONFIG

    public void resetConfig() {
        this.config = new YamlConfiguration();

        this.config.set("mode", "auto");
        this.config.setComments("mode", List.of(
                "This value changes the method the plugin is using to remove message signatures.",
                "Keep this value on 'auto' if you don't know what you're doing.",
                "",
                "Available Values:",
                "- auto:",
                "  Use 'remove_signatures' if available, else use 'system_messages'.",
                "- remove_signatures:",
                "  Replaces all ClientboundPlayerChatPackets with DisguisedPlayerChatPackets (which have no signature).",
                "  Any chat plugin should work as normal in this mode, since the modification is made after everything has already changed.",
                "  This mode only works if the server is running the version that the plugin was made for.",
                "- system_messages:",
                "  Cancels the chat event at the HIGHEST priority and sends the message as system message to all recipients (if the event has not been cancelled before).",
                "  Any chat plugin should work as normal when it is not using the HIGHEST priority for its chat events.",
                "  SINCE PRIVATE MESSAGES ARE NOT AFFECTED BY THE CHAT EVENT, THEY ARE STILL SIGNED AND REPORTABLE!",
                "  This mode also should work when the server does not have the version the plugin was made for (except the chat event has been changed)."
        ));

        this.config.set("hide_banner", false);
        this.config.setComments("hide_banner", List.of(
                "Hides the 'Chat Messages cannot be verified on this server' banner.",
                "This does only work if the mode is set to 'auto' or 'remove_signatures'."
        ));

        this.config.set("announce_protections", true);
        this.config.setComments("announce_protections", List.of("If this is enabled, which type of messages are encrypted and which are not."));

        this.setUseAlternativeMode();

    }

    public void reloadConfig() {
        try {

            if (!this.configFile.exists()) {
                this.configFile.getParentFile().mkdirs();
                this.configFile.createNewFile();
                this.config.save(this.configFile);
            }

            this.config.load(this.configFile);
            this.setUseAlternativeMode();

        } catch (IOException | InvalidConfigurationException e) {
            this.getLogger().log(Level.WARNING, "Exception while loading config, using defaults", e);
        }
    }

    // EVENT LISTENER

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {

        if (!this.useAlternativeMode) {

            Connection connection = this.getConnection(((CraftPlayer) event.getPlayer()).getHandle());

            if (connection == null) {
                event.getPlayer().sendMessage(this.getNotProtectedMessage());
                this.error = true;
                return;
            }

            // Show protected message to player
            if (this.config.getBoolean("announce_protections", false) && !this.error) {
                event.getPlayer().sendMessage(this.getProtectionMessage());
            }

            // OUTGOING CHAT MESSAGES (Client --> Server XXX Clients)
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

        } else {

            // Show protected message in alternative mode
            if (this.config.getBoolean("announce_protections", false) && !this.error) {
                event.getPlayer().sendMessage(this.getProtectionMessage());
            }

        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        if (!this.useAlternativeMode) {

            Connection connection = this.getConnection(((CraftPlayer) event.getPlayer()).getHandle());

            if (connection == null) {
                return;
            }

            try {
                connection.channel.pipeline().remove(this.getName() + "-WRITER");
            } catch (NoSuchElementException ignored) {
                // normally, the packer writer is already removed at this point
            }

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {

        // Cancels the chat event and sends chat message as system message to all recipients.
        if (this.useAlternativeMode) {

            if (event.isCancelled()) {
                return;
            }

            event.setCancelled(true);

            String formattedMessage = event.getFormat().replace("%1$s", event.getPlayer().getDisplayName()).replace("%2$s", event.getMessage());

            for (Player player : event.getRecipients()) {
                player.sendMessage(formattedMessage);
            }

            return;
        }

    }

    // COMMAND

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length < 1) {
            sender.sendMessage(this.error ? this.getNotProtectedMessage() : this.getProtectionMessage());

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
                sender.sendMessage("Config reloaded");
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {

        if (sender == this.getServer().getConsoleSender()) {
            return List.of("reload");
        } else {
            return List.of();
        }

    }

    // GETTER

    public YamlConfiguration getPluginConfig() {
        return this.config;
    }

}
