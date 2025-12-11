package semmiedev.disc_jockey;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;

public class SongPlayer implements ClientTickEvents.StartWorldTick {
    private static boolean warned;
    public boolean running;
    public Song song;

    private int index;
    private double tick; // Aka song position
    private long lastPlaybackTickAt = Util.TIMESTAMP_UNINITIALIZED;
    // The thread executing the tickPlayback method
    private Thread playbackThread = null;
    public long playbackLoopDelay = 5;
    // Just for external debugging purposes
    public float speed = 1.0f;
    public boolean didSongReachEnd = false;
    public boolean loopSong = false;
    private final RateLimiter rateLimiter = new RateLimiter();
    public final Tuner tuner = new Tuner();

    public SongPlayer() {
        Main.TICK_LISTENERS.add(this);
    }

    public synchronized void startPlaybackThread() {
        if(Main.config.disableAsyncPlayback) {
            playbackThread = null;
            return;
        }

        this.playbackThread = new Thread(() -> {
            Thread ownThread = this.playbackThread;
            while(ownThread == this.playbackThread) {
                try {
                    // Accuracy doesn't really matter at this precision imo
                    Thread.sleep(playbackLoopDelay);
                }catch (InterruptedException ignored) {}
                tickPlayback();
            }
        });
        this.playbackThread.start();
    }

    public synchronized void stopPlaybackThread() {
        this.playbackThread = null; // Should stop on its own then
    }

    public synchronized void start(Song song) {
        if (!Main.config.hideWarning && !warned) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.translatable("disc_jockey.warning").formatted(Formatting.BOLD, Formatting.RED));
            warned = true;
            return;
        }
        if (running) stop();
        tick = 0;
        index = 0;
        this.song = song;
        //Main.LOGGER.info("Song length: " + song.length + " and tempo " + song.tempo);
        if(this.playbackThread == null) startPlaybackThread();
        running = true;
        rateLimiter.reset();
        tuner.reset();
        didSongReachEnd = false;
    }

    public synchronized void stop() {
        stopPlaybackThread();
        running = false;
        index = 0;
        tick = 0;
        rateLimiter.reset();
        tuner.reset();
        didSongReachEnd = false; // Change after running stop() if actually ended cleanly
    }

    /**
     * Can be run from both a separate thread or on minecraft ticks. Decided by Main.config.disableAsyncPlayback
     */
    public synchronized void tickPlayback() {
        if (!running) {
            lastPlaybackTickAt = Util.TIMESTAMP_UNINITIALIZED;
            rateLimiter.reset();
            return;
        }
        long previousPlaybackTickAt = lastPlaybackTickAt;
        lastPlaybackTickAt = Util.now();
        rateLimiter.tick();

        if(!tuner.isTuned()) return;

        while (running) {
            MinecraftClient client = MinecraftClient.getInstance();
            GameMode gameMode = client.interactionManager == null ? null : client.interactionManager.getCurrentGameMode();
            // In the best case, gameMode would only be queried in sync Ticks, no here
            if (gameMode == null || !gameMode.isSurvivalLike()) {
                client.inGameHud.getChatHud().addMessage(Text.translatable(Main.MOD_ID+".player.invalid_game_mode", gameMode == null ? "unknown" : gameMode.getTranslatableName()).formatted(Formatting.RED));
                stop();
                return;
            }

            long note = song.notes[index];
            if ((short)note <= Math.round(tick)) {
                @Nullable BlockPos blockPos = tuner.getNoteBlocks().get(Note.INSTRUMENTS[(byte)(note >> Note.INSTRUMENT_SHIFT)]).get((byte)(note >> Note.NOTE_SHIFT));
                if(blockPos == null) {
                    // Instrument got likely mapped to "nothing". Skip it
                    index++;
                    continue;
                }
                if (!Util.canInteractWith(client.player, blockPos)) {
                    stop();
                    client.inGameHud.getChatHud().addMessage(Text.translatable(Main.MOD_ID+".player.too_far").formatted(Formatting.RED));
                    return;
                }
                Vec3d unit = Vec3d.ofCenter(blockPos, 0.5).subtract(client.player.getEyePos()).normalize();
                if(rateLimiter.canSendLookPacket()) {
                    client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(MathHelper.wrapDegrees((float) (MathHelper.atan2(unit.z, unit.x) * 57.2957763671875) - 90.0f), MathHelper.wrapDegrees((float) (-(MathHelper.atan2(unit.y, Math.sqrt(unit.x * unit.x + unit.z * unit.z)) * 57.2957763671875))), client.player.isOnGround(), client.player.horizontalCollision));                        rateLimiter.onLookPacketSent();
                    rateLimiter.onLookPacketSent();
                }
                if(rateLimiter.canSendAnyPacket()) {
                    // TODO: 5/30/2022 Check if the block needs tuning
                    client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP, 0));
                    rateLimiter.onPacketSent();
                }
                if(rateLimiter.canSendCosmeticPacket()) {
                    client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, Direction.UP, 0));
                    rateLimiter.onPacketSent();
                }
                if(rateLimiter.canSendSwingPacket()) {
                    client.executeSync(() -> client.player.swingHand(Hand.MAIN_HAND));
                    rateLimiter.onSwingPacketSent();
                }

                index++;
                if (index >= song.notes.length) {
                    stop();
                    didSongReachEnd = true;
                    if(loopSong) {
                        start(song);
                    }
                    break;
                }
            } else {
                break;
            }
        }

        if(running) { // Might not be running anymore (prevent small offset on song, even if that is not played anymore)
            long elapsedMs = previousPlaybackTickAt != -1L && lastPlaybackTickAt != -1L ? lastPlaybackTickAt - previousPlaybackTickAt : 16; // Assume 16ms if unknown
            tick += song.millisecondsToTicks(elapsedMs) * speed;
        }
    }

    @Override
    public void onStartTick(ClientWorld world) {
        MinecraftClient client = MinecraftClient.getInstance();
        if(world == null || client.world == null || client.player == null) return;
        if(song == null || !running) return;

        tuner.cleanup(); // Housekeeping

        // Select song
        if (!tuner.isSongSelected()) {
            if (!tuner.selectSong(client, song)) {
                if(!tuner.getMissingInstrumentBlocks().isEmpty()) {
                    ChatHud chatHud = MinecraftClient.getInstance().inGameHud.getChatHud();
                    chatHud.addMessage(Text.translatable(Main.MOD_ID + ".player.invalid_note_blocks").formatted(Formatting.RED));
                    tuner.getMissingInstrumentBlocks().forEach((block, integer) -> chatHud.addMessage(Text.literal(block.getName().getString() + " × " + integer).formatted(Formatting.RED)));
                    stop();
                    return;
                }else {
                    Main.LOGGER.error("Failed to select song to unknown / unexpected reason!");
                    client.inGameHud.getChatHud().addMessage(Text.translatable(Main.MOD_ID + ".selectsong_fail_unknown").formatted(Formatting.RED));
                    stop();
                    return;
                }
            }else {
                Main.LOGGER.info("Selected song: " + song.displayName + " (" + song.fileName + ")");
            }
        }

        // Tune
        if (!tuner.isTuned()) {
            Tuner.TuningFail tuningFail = tuner.tickTuning(client);
            if (tuningFail == Tuner.TuningFail.MovedTooFarAway) {
                stop();
                client.inGameHud.getChatHud().addMessage(Text.translatable(Main.MOD_ID + ".player.too_far").formatted(Formatting.RED));
                return;
            } else if(tuningFail != null) {
                stop();
                Main.LOGGER.error("Tuning song failed: " + tuningFail.name());
                client.inGameHud.getChatHud().addMessage(Text.translatable(Main.MOD_ID + ".player.tuning_fail_other", tuningFail.name()).formatted(Formatting.RED));
                return;
            }
        }

        if(tuner.isTuned() && (playbackThread == null || !playbackThread.isAlive()) && running && Main.config.disableAsyncPlayback) {
            // Sync playback (off by default). Replacement for playback thread
            try {
                tickPlayback();
            }catch (Exception ex) {
                Main.LOGGER.error("Failed to tick playback synchronously!", ex);
                stop();
            }
        }
    }

    public void setSongElapsedSeconds(double seconds) {
        tick = song.millisecondsToTicks((long) seconds * 1000);
        index = 0;
        for(int i = 0; i < song.notes.length; i++) {
            long note = song.notes[i];
            if((short) note >= Math.round(tick)) {
                index = i;
                //Main.LOGGER.info("Seconds: " + seconds + ", Tick: " + tick + ", Index: " + index);
                break;
            }
        }
    }

    public double getSongElapsedSeconds() {
        if(song == null) return 0;
        return song.ticksToMilliseconds(tick) / 1000;
    }
}
