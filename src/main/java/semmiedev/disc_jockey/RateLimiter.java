package semmiedev.disc_jockey;

public class RateLimiter {
    // Used to check and enforce packet rate limits to not get kicked
    private long last100MsSpanAt;
    private int last100MsSpanEstimatedPackets;
    // At how many packets/100ms should the player just reduce / stop sending packets for a while
    private long reducePacketsUntil, stopPacketsUntil;

    // Use to limit swings and look to only each tick. More will not be visually visible anyway due to interpolation
    private long lastLookSentAt, lastSwingSentAt;

    public RateLimiter() {
        reset();
    }

    public void reset() {
        last100MsSpanAt = Util.TIMESTAMP_UNINITIALIZED;
        last100MsSpanEstimatedPackets = 0;
        reducePacketsUntil = Util.TIMESTAMP_UNINITIALIZED;
        stopPacketsUntil = Util.TIMESTAMP_UNINITIALIZED;
        lastLookSentAt = Util.TIMESTAMP_UNINITIALIZED;
        lastSwingSentAt = Util.TIMESTAMP_UNINITIALIZED;
    }

    public int getMaxCosmeticPacketsPer100ms() {
        return Main.config.playbackPacketRatelimit.getReducePacketsPer100Millis();
    }

    public int getMaxPacketsPer100ms() {
        return Main.config.playbackPacketRatelimit.getMaxPacketsPer100Millis();
    }

    /**
     * Should run each tick, but does not need to.
     * Just run before running a lot of ŕate limit checks / on*Packet()
     */
    public void tick() {
        final long now = Util.now();
        if(last100MsSpanAt != Util.TIMESTAMP_UNINITIALIZED && now - last100MsSpanAt >= 100) {
            last100MsSpanEstimatedPackets = 0;
            last100MsSpanAt = now;
        }else if (last100MsSpanAt == Util.TIMESTAMP_UNINITIALIZED) {
            last100MsSpanAt = now;
            last100MsSpanEstimatedPackets = 0;
        }
    }

    public void onPacketSent() {
        last100MsSpanEstimatedPackets++;
        checkLimits();
    }

    public void onLookPacketSent() {
        lastLookSentAt = Util.now();
        onPacketSent();
    }

    public void onSwingPacketSent() {
        lastSwingSentAt = Util.now();
        onPacketSent();
    }

    public void checkLimits() {
        if(last100MsSpanEstimatedPackets >= getMaxCosmeticPacketsPer100ms()) {
            reducePacketsUntil = Math.max(reducePacketsUntil, Util.now() + 500);
        }
        if(last100MsSpanEstimatedPackets >= getMaxPacketsPer100ms()) {
            Main.LOGGER.warn("Stopping all packets for a bit!");
            final long now = Util.now();
            stopPacketsUntil = Math.max(stopPacketsUntil, now + 250);
            reducePacketsUntil = Math.max(reducePacketsUntil, now + 10000);
        }
    }

    public boolean canSendCosmeticPacket() {
        return last100MsSpanEstimatedPackets < getMaxCosmeticPacketsPer100ms() && (reducePacketsUntil == Util.TIMESTAMP_UNINITIALIZED || reducePacketsUntil < Util.now());
    }

    public boolean canSendAnyPacket() {
        return last100MsSpanEstimatedPackets < getMaxPacketsPer100ms() && (stopPacketsUntil == Util.TIMESTAMP_UNINITIALIZED || stopPacketsUntil < Util.now());
    }

    public boolean canSendLookPacket() {
        return (lastLookSentAt == Util.TIMESTAMP_UNINITIALIZED || Util.now() - lastLookSentAt >= 50) && canSendCosmeticPacket();
    }

    public boolean canSendSwingPacket() {
        return (lastSwingSentAt == Util.TIMESTAMP_UNINITIALIZED || Util.now() - lastSwingSentAt >= 50) && canSendCosmeticPacket();
    }

}
