package net.jandie1505.nomessagesignatures.utilities;

import net.jandie1505.nomessagesignatures.NoMessageSignatures;
import org.bstats.bukkit.Metrics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MetricsManager {
    @NotNull private final NoMessageSignatures plugin;
    @Nullable private Metrics metrics;

    public MetricsManager(@NotNull NoMessageSignatures plugin) {
        this.plugin = plugin;
    }

    public void enableMetrics() {
        if (this.metrics != null) return;
        this.metrics = new Metrics(this.plugin, 24129);
    }

    public void disableMetrics() {
        if (this.metrics == null) return;
        this.metrics.shutdown();
        this.metrics = null;
    }

    public @NotNull NoMessageSignatures getPlugin() {
        return plugin;
    }

    public @Nullable Metrics getBStats() {
        return metrics;
    }

    public boolean isEnabled() {
        return metrics != null;
    }

}
