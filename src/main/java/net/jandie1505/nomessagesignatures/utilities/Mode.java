package net.jandie1505.nomessagesignatures.utilities;

/**
 * For internal use only.
 * Prevents accidentally enabling the packet mode if it has been disabled before.
 */
public final class Mode {
    private boolean packetMode;
    private boolean initialized;

    public Mode() {
        this.packetMode = false;
        this.initialized = false;
    }

    /**
     * Initializes the class if not already done.
     * @param packetMode the packet mode value
     * @return true if init was successful, false if not.
     */
    public boolean init(boolean packetMode) {
        if (this.initialized) return false;
        this.packetMode = packetMode;
        this.initialized = true;
        return true;
    }

    /**
     * Returns if the packet mode is enabled.
     * @return true if packet mode enabled
     */
    public boolean isPacketMode() {
        this.initialized = true;
        return this.packetMode;
    }

    /**
     * Set the packet mode to false.
     */
    public void disablePacketMode() {
        this.packetMode = false;
        this.initialized = true;
    }

}
