package net.jandie1505.nomessagesigning;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.command.*;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;

public class NoMessageSigning extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private JSONObject config;
    private File configFile;

    @Override
    public void onEnable() {
        this.resetConfig();
        this.configFile = new File(this.getDataFolder(), "config.json");
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

    private boolean skipPacketLevelChecks() {

        boolean skip = true;
        Map<String, Object> packetConfig = Map.copyOf(this.config.optJSONObject("packet_level", new JSONObject()).toMap());

        for (Object object : packetConfig.keySet()) {

            if (object instanceof Boolean && (Boolean) object) {
                skip = false;
                break;
            }

        }

        return skip;
    }

    private Connection getConnection(ServerPlayer serverPlayer) {

        try {
            ServerGamePacketListenerImpl serverGamePacketListener = serverPlayer.connection;
            Field field = serverGamePacketListener.getClass().getField("c");
            field.setAccessible(true);

            return  (Connection) field.get(serverGamePacketListener);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            this.getLogger().log(Level.WARNING, "Exception while getting connection of player " + serverPlayer.getUUID(), e);
            return null;
        }

    }

    // CONFIG

    public void resetConfig() {
        this.config = new JSONObject();

        JSONObject packetLevelConfig = new JSONObject();
        packetLevelConfig.put("block_outgoing_chat_signatures", true);
        packetLevelConfig.put("block_incoming_chat_signatures", true);
        this.config.put("packet_level", packetLevelConfig);

        JSONObject bukkitLevelConfig = new JSONObject();
        bukkitLevelConfig.put("send_as_system_message", false);
        bukkitLevelConfig.put("modify_message", false);
        this.config.put("bukkit_level", bukkitLevelConfig);
    }

    private JSONObject loadConfig() throws IOException, JSONException {
        BufferedReader br = new BufferedReader(new FileReader(this.configFile));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            sb.append(line);
            sb.append(System.lineSeparator());
            line = br.readLine();
        }
        return new JSONObject(sb.toString());
    }

    private void writeConfig() throws IOException {
        FileWriter writer = new FileWriter(this.configFile);
        writer.write(this.config.toString(4));
        writer.flush();
        writer.close();
    }

    public void reloadConfig() {
        try {

            if (!this.configFile.exists()) {
                this.configFile.getParentFile().mkdirs();
                this.configFile.createNewFile();
                this.writeConfig();
            }

            JSONObject loadedConfig = this.loadConfig();

            if (loadedConfig.optBoolean("recreate_config", false)) {
                this.resetConfig();
                this.writeConfig();
            } else {
                for (String key : loadedConfig.keySet()) {
                    this.config.put(key, loadedConfig.get(key));
                }
            }

        } catch (IOException | JSONException e) {
            this.getLogger().log(Level.WARNING, "Error loading config, using defaults");
        }
    }

    // EVENT LISTENER

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {

        if (this.skipPacketLevelChecks()) {
            return;
        }

        Connection connection = this.getConnection(((CraftPlayer) event.getPlayer()).getHandle());

        if (connection == null) {
            return;
        }

        // OUTGOING CHAT MESSAGES (Client --> Server XXX Clients)
        if (this.config.optJSONObject("packet_level", new JSONObject()).optBoolean("block_outgoing_chat_signatures", true)) {

            // This removes all outgoing chat message signatures
            connection.channel.pipeline().addBefore("packet_handler", this.getName() + "-READER", new ChannelInboundHandlerAdapter() {

                public void channelRead(ChannelHandlerContext ctx, Object msg) {

                    if (msg instanceof ClientboundPlayerChatPacket old) {
                        SignedMessageBody.Packed body = new SignedMessageBody.Packed(old.body().content(), old.body().timeStamp(), 0, old.body().lastSeen());
                        ClientboundPlayerChatPacket packet = new ClientboundPlayerChatPacket(old.sender(), old.index(), null, body, old.unsignedContent(), old.filterMask(), old.chatType());
                        ctx.fireChannelRead(packet);
                        return;
                    }

                    ctx.fireChannelRead(msg);
                }

            });

        }

        // INCOMING CHAT MESSAGES (Client XXX Server --> Clients)
        if (this.config.optJSONObject("packet_level", new JSONObject()).optBoolean("block_incoming_chat_signatures", true)) {

            // This removes all incoming chat message signatures (so that the server doesn't even know that there was a signed chat message)
            connection.channel.pipeline().addBefore("packet_handler", this.getName() + "-WRITER", new ChannelOutboundHandlerAdapter() {

                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {

                    if (msg instanceof ServerboundChatPacket old) {
                        ServerboundChatPacket packet = new ServerboundChatPacket(old.message(), old.timeStamp(), 0, null, old.lastSeenMessages());
                        ctx.write(packet, promise);
                        return;
                    }

                    ctx.write(msg, promise);
                }

            });

        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        Connection connection = this.getConnection(((CraftPlayer) event.getPlayer()).getHandle());

        if (connection == null) {
            return;
        }

        try {
            connection.channel.pipeline().remove(this.getName() + "-READER");
            connection.channel.pipeline().remove(this.getName() + "-WRITER");
        } catch (NoSuchElementException e) {
            this.getLogger().log(Level.WARNING, "Failed to remove a packet reader/writer in a player's pipeline. This is not a fatal exception, since it will be cleaned up by garbage collection.", e);
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {

        // Cancels the chat event and sends chat message as system message to all recipients.
        if (this.config.optJSONObject("bukkit_level", new JSONObject()).optBoolean("send_as_system_message", false)) {
            event.setCancelled(true);

            String formattedMessage = event.getFormat().replace("%1", event.getPlayer().getDisplayName()).replace("%2", event.getMessage());

            for (Player player : event.getRecipients()) {
                player.sendMessage(formattedMessage);
            }

            return;
        }

        // Modifies the message that the signature is invalid. This does not prevent the signature to be sent to the client!
        if (this.config.optJSONObject("bukkit_level", new JSONObject()).optBoolean("modify_message", false)) {
            event.setMessage(event.getMessage() + " ");
            return;
        }

    }

    // COMMAND

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length < 1) {
            sender.sendMessage(
                    "This server is running NoMessageSigning.\n" +
                            "Enabled checks:\n" +
                            " - Remove incoming signatures: " + this.config.optJSONObject("packet_level", new JSONObject()).optBoolean("block_incoming_chat_signatures", true) + "\n" +
                            " - Remove outgoing signatures: " + this.config.optJSONObject("packet_level", new JSONObject()).optBoolean("block_outgoing_chat_signatures", true) + "\n" +
                            " - Chat as system messages: " + this.config.optJSONObject("bukkit_level", new JSONObject()).optBoolean("send_as_system_message", false) + "\n" +
                            " - Modify messages: " + this.config.optJSONObject("bukkit_level", new JSONObject()).optBoolean("modify_message", false)
            );
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

    public JSONObject getPluginConfig() {
        return this.config;
    }

}
