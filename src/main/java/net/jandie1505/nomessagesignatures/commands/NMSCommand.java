package net.jandie1505.nomessagesignatures.commands;

import net.jandie1505.nomessagesignatures.NoMessageSignatures;
import org.bukkit.command.*;
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
            case "metrics-disable" -> {

                if (sender != this.plugin.getServer().getConsoleSender()) {
                    sender.sendMessage("This command can only be executed by console");
                    return true;
                }

                this.plugin.getMetrics().disableMetrics();
                sender.sendMessage("Metrics disabled until restart (if it was active before)");
                sender.sendMessage("You can make this change permanent by setting enable-metrics in config to false");
            }
            case "metrics-status" -> {
                sender.sendMessage("§7Current metrics status: " + (this.plugin.getMetrics().isEnabled() ? "enabled" : "disabled"));
                sender.sendMessage("§7You can change metrics in the config using: \"enable-metrics: (true|false)\"");
            }
            case "accept-metrics" -> {

                if (sender != this.plugin.getServer().getConsoleSender()) {
                    sender.sendMessage("This command can only be executed by console");
                    return true;
                }

                if (this.plugin.getConfig().contains("enable-metrics")) {
                    sender.sendMessage("Decision already made. Use enable-metrics in config to change.");
                    return true;
                }

                this.plugin.getConfigManager().getConfig().set("enable-metrics", true);
                this.plugin.getConfigManager().getConfig().setComments("enable-metrics", List.of("If set to true, the plugin sends anonymous statistics to bstats.org.", "This allows us to see, for example, how many servers are using the plugin.", "If set to false or the option does not exist, no data is sent."));
                this.plugin.saveConfig();
                this.plugin.getMetrics().enableMetrics();
                sender.sendMessage("Metrics enabled");

            }
            case "deny-metrics" -> {

                if (sender != this.plugin.getServer().getConsoleSender()) {
                    sender.sendMessage("This command can only be executed by console");
                    return true;
                }

                if (this.plugin.getConfig().contains("enable-metrics")) {
                    sender.sendMessage("Decision already made. Use enable-metrics in config to change.");
                    return true;
                }

                this.plugin.getConfigManager().getConfig().set("enable-metrics", false);
                this.plugin.getConfigManager().getConfig().setComments("enable-metrics", List.of("If set to true, the plugin sends anonymous statistics to bstats.org.", "This allows us to see, for example, how many servers are using the plugin.", "If set to false or the option does not exist, no data is sent."));
                this.plugin.saveConfig();
                sender.sendMessage("Metrics disabled");

            }
        }

        return true;
    }

    @Override
    @NotNull
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) return List.of();

        if (sender == this.getPlugin().getServer().getConsoleSender()) {
            return List.of("mode", "reload", "metrics-status", "metrics-disable");
        } else {
            return List.of("mode", "metrics-status");
        }

    }

    public @NotNull NoMessageSignatures getPlugin() {
        return plugin;
    }

}
