package semmiedev.disc_jockey;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SongPlayer implements ClientTickEvents.StartLevelTick {
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
        if (Main.config.disableAsyncPlayback) {
            playbackThread = null;
            return;
        }

        this.playbackThread = new Thread(() -> {
            Thread ownThread = this.playbackThread;
            while (ownThread == this.playbackThread) {
                try {
                    // Accuracy doesn't really matter at this precision imo
                    Thread.sleep(playbackLoopDelay);
                } catch (InterruptedException ignored) {}
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
            Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.translatable("disc_jockey.warning").withStyle(ChatFormatting.BOLD, ChatFormatting.RED));
            warned = true;
            return;
        }
        if (running) stop();
        tick = 0;
        index = 0;
        this.song = song;
        //Main.LOGGER.info("Song length: " + song.length + " and tempo " + song.tempo);
        if (this.playbackThread == null) startPlaybackThread();
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
            Minecraft client = Minecraft.getInstance();
            GameType gameMode = client.gameMode == null ? null : client.gameMode.getPlayerMode();
            // In the best case, gameMode would only be queried in sync Ticks, no here
            if (gameMode == null || !gameMode.isSurvival()) {
                client.gui.hud.getChat().addClientSystemMessage(Component.translatable(Main.MOD_ID+".player.invalid_game_mode", gameMode == null ? "unknown" : gameMode.getLongDisplayName()).withStyle(ChatFormatting.RED));
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
                    client.gui.hud.getChat().addClientSystemMessage(Component.translatable(Main.MOD_ID+".player.too_far").withStyle(ChatFormatting.RED));
                    return;
                }
                Vec3 unit = Vec3.upFromBottomCenterOf(blockPos, 0.5).subtract(client.player.getEyePosition()).normalize();
                if (rateLimiter.canSendLookPacket()) {
                    client.getConnection().send(new ServerboundMovePlayerPacket.Rot(Mth.wrapDegrees((float) (Mth.atan2(unit.z, unit.x) * 57.2957763671875) - 90.0f), Mth.wrapDegrees((float) (-(Mth.atan2(unit.y, Math.sqrt(unit.x * unit.x + unit.z * unit.z)) * 57.2957763671875))), client.player.onGround(), client.player.horizontalCollision));                        rateLimiter.onLookPacketSent();
                    rateLimiter.onLookPacketSent();
                }
                if (rateLimiter.canSendAnyPacket()) {
                    // TODO: 5/30/2022 Check if the block needs tuning
                    client.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP, 0));
                    rateLimiter.onPacketSent();
                }
                if (rateLimiter.canSendCosmeticPacket()) {
                    client.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, blockPos, Direction.UP, 0));
                    rateLimiter.onPacketSent();
                }
                if (rateLimiter.canSendSwingPacket()) {
                    client.executeIfPossible(() -> client.player.swing(InteractionHand.MAIN_HAND));
                    rateLimiter.onSwingPacketSent();
                }

                index++;
                if (index >= song.notes.length) {
                    stop();
                    didSongReachEnd = true;
                    if (loopSong) {
                        start(song);
                    }
                    break;
                }
            } else {
                break;
            }
        }

        if (running) { // Might not be running anymore (prevent small offset on song, even if that is not played anymore)
            long elapsedMs = previousPlaybackTickAt != -1L && lastPlaybackTickAt != -1L ? lastPlaybackTickAt - previousPlaybackTickAt : 16; // Assume 16ms if unknown
            tick += song.millisecondsToTicks(elapsedMs) * speed;
        }
    }

    @Override
    public void onStartTick(ClientLevel world) {
        Minecraft client = Minecraft.getInstance();
        if (world == null || client.level == null || client.player == null) return;
        if (song == null || !running) return;

        tuner.cleanup(); // Housekeeping

        // Select song
        if (!tuner.isSongSelected()) {
            if (!tuner.selectSong(client, song)) {
                if (!tuner.getMissingInstrumentBlocks().isEmpty()) {
                    ChatComponent chatHud = Minecraft.getInstance().gui.hud.getChat();
                    chatHud.addClientSystemMessage(Component.translatable(Main.MOD_ID + ".player.invalid_note_blocks").withStyle(ChatFormatting.RED));
                    tuner.getMissingInstrumentBlocks().forEach((block, integer) -> chatHud.addClientSystemMessage(Component.literal(block.getName().getString() + " × " + integer).withStyle(ChatFormatting.RED)));
                    stop();
                    return;
                } else {
                    Main.LOGGER.error("Failed to select song to unknown / unexpected reason!");
                    client.gui.hud.getChat().addClientSystemMessage(Component.translatable(Main.MOD_ID + ".selectsong_fail_unknown").withStyle(ChatFormatting.RED));
                    stop();
                    return;
                }
            } else {
                Main.LOGGER.info("Selected song: " + song.displayName + " (" + song.fileName + ")");
            }
        }

        // Tune
        if (!tuner.isTuned()) {
            Tuner.TuningFail tuningFail = tuner.tickTuning(client);
            if (tuningFail == Tuner.TuningFail.MovedTooFarAway) {
                stop();
                client.gui.hud.getChat().addClientSystemMessage(Component.translatable(Main.MOD_ID + ".player.too_far").withStyle(ChatFormatting.RED));
                return;
            } else if (tuningFail != null) {
                stop();
                Main.LOGGER.error("Tuning song failed: " + tuningFail.name());
                client.gui.hud.getChat().addClientSystemMessage(Component.translatable(Main.MOD_ID + ".player.tuning_fail_other", tuningFail.name()).withStyle(ChatFormatting.RED));
                return;
            }
        }

        if (tuner.isTuned() && (playbackThread == null || !playbackThread.isAlive()) && running && Main.config.disableAsyncPlayback) {
            // Sync playback (off by default). Replacement for playback thread
            try {
                tickPlayback();
            } catch (Exception ex) {
                Main.LOGGER.error("Failed to tick playback synchronously!", ex);
                stop();
            }
        }
    }

    public void setSongElapsedSeconds(double seconds) {
        tick = song.millisecondsToTicks((long) seconds * 1000);
        index = 0;
        for (int i = 0; i < song.notes.length; i++) {
            long note = song.notes[i];
            if ((short) note >= Math.round(tick)) {
                index = i;
                //Main.LOGGER.info("Seconds: " + seconds + ", Tick: " + tick + ", Index: " + index);
                break;
            }
        }
    }

    public double getSongElapsedSeconds() {
        if (song == null) return 0;
        return song.ticksToMilliseconds(tick) / 1000;
    }
}
