package net.jandie1505.nomessagesignatures.commands;

import net.jandie1505.nomessagesignatures.NoMessageSignatures;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NMSCommand implements CommandExecutor, TabCompleter {
    @NotNull private final NoMessageSignatures plugin;

    public NMSCommand(@NotNull NoMessageSignatures plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {

        if (args.length < 1) {
            sender.sendMessage(this.plugin.getProtectionMessage());
            return true;
        }

        switch (args[0]) {
            default -> sender.sendMessage("Unknown subcommand");
            case "reload" -> {

                if (sender != this.plugin.getServer().getConsoleSender()) {
                    sender.sendMessage("This command can only be executed by console");
                    return true;
                }

                this.plugin.getConfigManager().reloadConfig();
                sender.sendMessage("Config reloaded.\nPlease note that this will NOT update the mode the plugin is using to prevent chat reporting.");
            }
            case "mode" -> sender.sendMessage("§7Current mode: " + (this.plugin.getPacketMode().isPacketMode() ? "§aPacket replacement" : "§cSystem messages"));
        }

        return true;
    }

    @Override
    @NotNull
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) return List.of();

        if (sender == this.getPlugin().getServer().getConsoleSender()) {
            return List.of("mode", "reload");
        } else {
            return List.of("mode");
        }

    }

    public @NotNull NoMessageSignatures getPlugin() {
        return plugin;
    }

}
