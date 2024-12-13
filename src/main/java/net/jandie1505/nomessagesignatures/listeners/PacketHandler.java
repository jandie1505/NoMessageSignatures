package net.jandie1505.nomessagesignatures.listeners;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.jandie1505.nomessagesignatures.NoMessageSignatures;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import org.jetbrains.annotations.NotNull;

public final class PacketHandler extends ChannelOutboundHandlerAdapter {
    @NotNull private final NoMessageSignatures plugin;

    public PacketHandler(@NotNull NoMessageSignatures plugin) {
        this.plugin = plugin;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {

        if (msg instanceof ClientboundPlayerChatPacket old) {

            ClientboundDisguisedChatPacket packet = new ClientboundDisguisedChatPacket(old.unsignedContent() != null ? old.unsignedContent() : Component.literal(old.body().content()), old.chatType());
            ctx.write(packet, promise);
            return;

        } else if (msg instanceof ClientboundServerDataPacket old) {

            if (this.plugin.getConfig().getBoolean("hide_banner", false)) {
                ClientboundServerDataPacket packet = new ClientboundServerDataPacket(old.motd(), old.iconBytes());
                ctx.write(packet, promise);
                return;
            }

        }

        ctx.write(msg, promise);
    }

    public @NotNull NoMessageSignatures getPlugin() {
        return plugin;
    }

}
