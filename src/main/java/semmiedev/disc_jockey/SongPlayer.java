package semmiedev.disc_jockey;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.Instrument;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.HashMap;

public class SongPlayer implements ClientTickEvents.StartWorldTick {
    private static final Box BOX = new Box(0, 0, 0, 1, 1, 1);
    private static boolean warned;

    public boolean running;
    public Song song;

    private int index;
    private float tick;
    private HashMap<Instrument, HashMap<Byte, BlockPos>> noteBlocks = null;
    private boolean tuned;
    private int tuneDelay = 5;

    public void start(Song song) {
        if (!Main.config.hideWarning && !warned) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.translatable("disc_jockey.warning").formatted(Formatting.BOLD, Formatting.RED));
            warned = true;
            return;
        }
        if (running) stop();
        this.song = song;
        Main.TICK_LISTENERS.add(this);
        running = true;
    }

    public void stop() {
        MinecraftClient.getInstance().send(() -> Main.TICK_LISTENERS.remove(this));
        running = false;
        index = 0;
        tick = 0;
        noteBlocks = null;
        tuned = false;
    }

    // TODO: 6/2/2022 Play note blocks every song tick, instead of every tick. That way the song will sound better
    @Override
    public void onStartTick(ClientWorld world) {
        if (!running) return;
        if (noteBlocks == null) {
            noteBlocks = new HashMap<>();

            ClientPlayerEntity player = MinecraftClient.getInstance().player;

            ArrayList<Note> capturedNotes = new ArrayList<>();

            Vec3d playerPos = player.getEyePos();
            for (int x = -7; x <= 7; x++) {
                for (int y = -7; y <= 7; y++) {
                    for (int z = -7; z <= 7; z++) {
                        BlockPos blockPos = new BlockPos(playerPos.add(x, y, z));
                        if (intersect(playerPos, MinecraftClient.getInstance().interactionManager.getReachDistance(), BOX.offset(blockPos))) {
                            BlockState blockState = world.getBlockState(blockPos);
                            if (blockState.isOf(Blocks.NOTE_BLOCK) && world.isAir(blockPos.up())) {
                                for (Note note : song.uniqueNotes) {
                                    if (!capturedNotes.contains(note) && blockState.get(Properties.INSTRUMENT) == note.instrument) {
                                        getNotes(note.instrument).put(note.note, blockPos);
                                        capturedNotes.add(note);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ArrayList<Note> missingNotes = new ArrayList<>(song.uniqueNotes);
            missingNotes.removeAll(capturedNotes);
            if (!missingNotes.isEmpty()) {
                ChatHud chatHud = MinecraftClient.getInstance().inGameHud.getChatHud();
                chatHud.addMessage(Text.translatable(Main.MOD_ID+".player.invalid_note_blocks").formatted(Formatting.RED));

                HashMap<Block, Integer> missing = new HashMap<>();
                for (Note note : missingNotes) {
                    Block block = Note.INSTRUMENT_BLOCKS.get(note.instrument);
                    Integer got = missing.get(block);
                    if (got == null) got = 0;
                    missing.put(block, got + 1);
                }

                missing.forEach((block, integer) -> chatHud.addMessage(Text.literal(block.getName().getString()+" × "+integer).formatted(Formatting.RED)));
                stop();
            }
        } else if (!tuned) {
            if (tuneDelay > 0) {
                tuneDelay--;
                return;
            }
            tuned = true;
            MinecraftClient client = MinecraftClient.getInstance();
            int tuneAmount = 0;
            for (Note note : song.uniqueNotes) {
                BlockPos blockPos = noteBlocks.get(note.instrument).get(note.note);
                BlockState blockState = world.getBlockState(blockPos);

                if (blockState.contains(Properties.NOTE)) {
                    if (blockState.get(Properties.NOTE) != note.note) {
                        if (!intersect(client.player.getEyePos(), client.interactionManager.getReachDistance(), BOX.offset(blockPos))) {
                            stop();
                            client.inGameHud.getChatHud().addMessage(Text.translatable(Main.MOD_ID+".player.to_far").formatted(Formatting.RED));
                            return;
                        }
                        Vec3d unit = Vec3d.ofCenter(blockPos, 0.5).subtract(client.player.getEyePos()).normalize();
                        client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(MathHelper.wrapDegrees((float)(MathHelper.atan2(unit.z, unit.x) * 57.2957763671875) - 90.0f), MathHelper.wrapDegrees((float)(-(MathHelper.atan2(unit.y, Math.sqrt(unit.x * unit.x + unit.z * unit.z)) * 57.2957763671875))), true));
                        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(blockPos), Direction.UP, blockPos, false));
                        client.player.swingHand(Hand.MAIN_HAND);
                        tuned = false;
                        tuneDelay = 5;
                        if (++tuneAmount == 6) break;
                    }
                } else {
                    noteBlocks = null;
                    break;
                }
            }
        } else {
            while (running) {
                MinecraftClient client = MinecraftClient.getInstance();
                GameMode gameMode = client.interactionManager.getCurrentGameMode();
                if (!gameMode.isSurvivalLike()) {
                    client.inGameHud.getChatHud().addMessage(Text.translatable(Main.MOD_ID+".player.invalid_game_mode", gameMode.getTranslatableName()).formatted(Formatting.RED));
                    stop();
                    return;
                }

                long note = song.notes[index];
                if ((short)note == Math.round(tick)) {
                    BlockPos blockPos = noteBlocks.get(Note.INSTRUMENTS[(byte)(note >> Note.INSTRUMENT_SHIFT)]).get((byte)(note >> Note.NOTE_SHIFT));
                    if (!intersect(client.player.getEyePos(), client.interactionManager.getReachDistance(), BOX.offset(blockPos))) {
                        stop();
                        client.inGameHud.getChatHud().addMessage(Text.translatable(Main.MOD_ID+".player.to_far").formatted(Formatting.RED));
                        return;
                    }
                    Vec3d unit = Vec3d.ofCenter(blockPos, 0.5).subtract(client.player.getEyePos()).normalize();
                    client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(MathHelper.wrapDegrees((float)(MathHelper.atan2(unit.z, unit.x) * 57.2957763671875) - 90.0f), MathHelper.wrapDegrees((float)(-(MathHelper.atan2(unit.y, Math.sqrt(unit.x * unit.x + unit.z * unit.z)) * 57.2957763671875))), true));
                    // TODO: 5/30/2022 Check if the block needs tuning
                    client.interactionManager.attackBlock(blockPos, Direction.UP);
                    client.player.swingHand(Hand.MAIN_HAND);

                    index++;
                    if (index >= song.notes.length) {
                        stop();
                        break;
                    }
                } else {
                    break;
                }
            }

            tick += song.tempo / 100f / 20f;
        }
    }

    private boolean intersect(Vec3d pos, double radius, Box box) {
        double x = Math.max(box.minX, Math.min(pos.x, box.maxX));
        double y = Math.max(box.minY, Math.min(pos.y, box.maxY));
        double z = Math.max(box.minZ, Math.min(pos.z, box.maxZ));

        double distance = (x - pos.x) * (x - pos.x) + (y - pos.y) * (y - pos.y) + (z - pos.z) * (z - pos.z);

        return distance < radius * radius;
    }

    private HashMap<Byte, BlockPos> getNotes(Instrument instrument) {
        return noteBlocks.computeIfAbsent(instrument, k -> new HashMap<>());
    }
}
