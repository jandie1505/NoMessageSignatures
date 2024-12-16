package net.jandie1505.nomessagesignatures.listeners;

import net.jandie1505.nomessagesignatures.NoMessageSignatures;
import net.minecraft.network.Connection;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public final class EventListener implements Listener {
    @NotNull private final NoMessageSignatures plugin;

    public EventListener(@NotNull NoMessageSignatures plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {

        if (this.getPlugin().getPacketMode().isPacketMode()) {

            // Get connection of player

            Connection connection = this.getPlugin().getConnection(((CraftPlayer) event.getPlayer()).getHandle());

            if (connection == null) {
                this.plugin.getPacketMode().disablePacketMode();
                this.plugin.getLogger().log(Level.WARNING, "Failed to get connection of player " + event.getPlayer().getUniqueId());
                return;
            }

            // Add outgoing channel handler to manage chat and server data packets (Client --> Server XXX Clients)

            try {

                connection.channel.pipeline().addBefore("packet_handler", this.plugin.getName() + "-WRITER", new PacketHandler(this.plugin));

            } catch (Exception e) {
                this.plugin.getPacketMode().disablePacketMode();
                this.plugin.getLogger().log(Level.WARNING, "Failed to add channel handler to pipeline of " + event.getPlayer().getUniqueId(), e);
            }

        }

        // Show protected message to players

        if (this.getPlugin().getConfig().getBoolean("announce_protections", false)) {
            event.getPlayer().sendMessage(this.getPlugin().getProtectionMessage());
        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        if (this.plugin.getPacketMode().isPacketMode()) {

            Connection connection = this.plugin.getConnection(((CraftPlayer) event.getPlayer()).getHandle());

            if (connection == null) {
                return;
            }

            try {
                connection.channel.pipeline().remove(this.plugin.getName() + "-WRITER");
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
        if (this.getPlugin().getPacketMode().isPacketMode()) return;
        event.setCancelled(true);

        String formattedMessage = event.getFormat().replace("%1$s", event.getPlayer().getDisplayName()).replace("%2$s", event.getMessage());
        this.getPlugin().getLogger().log(Level.INFO, "Chat message: " + formattedMessage);

        for (Player player : event.getRecipients()) {
            player.sendMessage(formattedMessage);
        }

    }

    public @NotNull NoMessageSignatures getPlugin() {
        return plugin;
    }

}
