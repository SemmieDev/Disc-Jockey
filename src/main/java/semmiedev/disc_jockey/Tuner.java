package semmiedev.disc_jockey;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Tuner {

    private HashMap<NoteBlockInstrument, HashMap<Byte, BlockPos>> noteBlocks = null;
    private long tunedAfter = Util.TIMESTAMP_UNINITIALIZED;
    private final HashMap<BlockPos, Pair<Integer, Long>> notePredictions = new HashMap<>();
    private HashMap<Block, Integer> missingInstrumentBlocks = new HashMap<>();
    private long lastInteractAt = -1;
    private float availableInteracts = 8;
    private int tuneInitialUntunedBlocks = -1;
    private Song selectedSong = null;

    public @NotNull HashMap<NoteBlockInstrument, @Nullable NoteBlockInstrument> instrumentMap = new HashMap<>(); // Toy

    public boolean isTuned() {
        return isSongSelected() && tunedAfter != Util.TIMESTAMP_UNINITIALIZED && tunedAfter <= Util.now();
    }

    public void cleanup() {
        // Clear outdated note predictions
        ArrayList<BlockPos> outdatedPredictions = new ArrayList<>();
        for(Map.Entry<BlockPos, Pair<Integer, Long>> entry : notePredictions.entrySet()) {
            if(entry.getValue().getRight() < Util.now())
                outdatedPredictions.add(entry.getKey());
        }
        for(BlockPos outdatedPrediction : outdatedPredictions) notePredictions.remove(outdatedPrediction);
    }

    private HashMap<Byte, BlockPos> getNotes(NoteBlockInstrument instrument) {
        return noteBlocks.computeIfAbsent(instrument, k -> new HashMap<>());
    }

    public boolean isSongSelected() {
        return noteBlocks != null && missingInstrumentBlocks != null && selectedSong != null;
    }

    public void reset() {
        // Results of selectSong()
        selectedSong = null;
        noteBlocks = null;
        missingInstrumentBlocks = null;

        // State for tickTuning() / isTuned()
        resetTuned();
    }

    /**
     * Keep old song selection, but force full re-tuning (at least checks) next time
     */
    public void resetTuned() {
        noteBlocks = null;
        notePredictions.clear();
        tunedAfter = Util.TIMESTAMP_UNINITIALIZED;
        tuneInitialUntunedBlocks = -1;
        availableInteracts = 0;
    }

    public boolean selectSong(MinecraftClient client, Song song) {
        reset();

        final ClientPlayerEntity player = client.player;
        final ClientWorld world = client.world;
        if(player == null || world == null || song == null) return false;

        // Create list of available noteblock positions per used instrument
        HashMap<NoteBlockInstrument, ArrayList<BlockPos>> noteblocksForInstrument = new HashMap<>();
        for(NoteBlockInstrument instrument : NoteBlockInstrument.values())
            noteblocksForInstrument.put(instrument, new ArrayList<>());
        final Vec3d playerEyePos = player.getEyePos();

        final int maxOffset; // Rough estimates, of which blocks could be in reach
        if(Main.config.expectedServerVersion == Config.ExpectedServerVersion.v1_20_4_Or_Earlier) {
            maxOffset = 7;
        }else if(Main.config.expectedServerVersion == Config.ExpectedServerVersion.v1_20_5_Or_Later) {
            maxOffset = (int) Math.ceil(player.getBlockInteractionRange() + 1.0 + 1.0);
        }else if(Main.config.expectedServerVersion == Config.ExpectedServerVersion.All) {
            maxOffset = Math.min(7, (int) Math.ceil(player.getBlockInteractionRange() + 1.0 + 1.0));
        }else {
            throw new NotImplementedException("ExpectedServerVersion Value not implemented: " + Main.config.expectedServerVersion.name());
        }
        final ArrayList<Integer> orderedOffsets = new ArrayList<>();
        for(int offset = 0; offset <= maxOffset; offset++) {
            orderedOffsets.add(offset);
            if(offset != 0) orderedOffsets.add(offset * -1);
        }

        for(NoteBlockInstrument instrument : noteblocksForInstrument.keySet().toArray(new NoteBlockInstrument[0])) {
            for (int y : orderedOffsets) {
                for (int x : orderedOffsets) {
                    for (int z : orderedOffsets) {
                        Vec3d vec3d = playerEyePos.add(x, y, z);
                        BlockPos blockPos = new BlockPos(MathHelper.floor(vec3d.x), MathHelper.floor(vec3d.y), MathHelper.floor(vec3d.z));
                        if (!Util.canInteractWith(player, blockPos))
                            continue;
                        BlockState blockState = world.getBlockState(blockPos);
                        NoteBlockInstrument blockInstrument = getInstrument(client, blockPos, blockState);
                        if(blockInstrument == null) continue; // Not a noteblock or not playable due to obstruction

                        if (blockInstrument == instrument)
                            noteblocksForInstrument.get(instrument).add(blockPos);
                    }
                }
            }
        }

        // Remap instruments for funzies
        if(!instrumentMap.isEmpty()) {
            HashMap<NoteBlockInstrument, ArrayList<BlockPos>> newNoteblocksForInstrument = new HashMap<>();
            for(NoteBlockInstrument orig : noteblocksForInstrument.keySet()) {
                NoteBlockInstrument mappedInstrument = instrumentMap.getOrDefault(orig, orig);
                if(mappedInstrument == null) {
                    // Instrument got likely mapped to "nothing"
                    newNoteblocksForInstrument.put(orig, null);
                    continue;
                }

                newNoteblocksForInstrument.put(orig, noteblocksForInstrument.getOrDefault(instrumentMap.getOrDefault(orig, orig), new ArrayList<>()));
            }
            noteblocksForInstrument = newNoteblocksForInstrument;
        }

        noteBlocks = new HashMap<>();
        // Find fitting noteblocks with the least amount of adjustments required (to reduce tuning time)
        ArrayList<Note> capturedNotes = new ArrayList<>();
        for(Note note : song.uniqueNotes) {
            ArrayList<BlockPos> availableBlocks = noteblocksForInstrument.get(note.instrument());
            if(availableBlocks == null) {
                // Note was mapped to "nothing". Pretend it got captured, but just ignore it
                capturedNotes.add(note);
                getNotes(note.instrument()).put(note.note(), null);
                continue;
            }
            BlockPos bestBlockPos = null;
            int bestBlockTuningSteps = Integer.MAX_VALUE;
            for(BlockPos blockPos : availableBlocks) {
                int wantedNote = note.note();
                int currentNote = client.world.getBlockState(blockPos).get(Properties.NOTE);
                int tuningSteps = wantedNote >= currentNote ? wantedNote - currentNote : (25 - currentNote) + wantedNote;

                if(tuningSteps < bestBlockTuningSteps) {
                    bestBlockPos = blockPos;
                    bestBlockTuningSteps = tuningSteps;
                }
            }

            if(bestBlockPos != null) {
                capturedNotes.add(note);
                availableBlocks.remove(bestBlockPos);
                getNotes(note.instrument()).put(note.note(), bestBlockPos);
            } // else will be a missing note
        }

        ArrayList<Note> missingNotes = new ArrayList<>(song.uniqueNotes);
        missingNotes.removeAll(capturedNotes);

        missingInstrumentBlocks = new HashMap<>();
        // Fill in missingInstrumentBlocks
        for (Note note : missingNotes) {
            NoteBlockInstrument mappedInstrument = instrumentMap.getOrDefault(note.instrument(), note.instrument());
            if(mappedInstrument == null) continue; // Ignore if mapped to nothing
            Block block = Note.INSTRUMENT_BLOCKS.get(mappedInstrument);
            Integer got = missingInstrumentBlocks.get(block);
            if (got == null) got = 0;
            missingInstrumentBlocks.put(block, got + 1);
        }

        // Select song, if all noteblocks were found
        if(missingInstrumentBlocks.isEmpty()) {
            selectedSong = song;
            return true;
        }else {
            return false;
        }
    }

    private @Nullable NoteBlockInstrument getInstrument(MinecraftClient client, BlockPos pos, BlockState state) {
        if(!(state.getBlock() instanceof NoteBlock noteBlock)) return null; // Not a noteblock

        if(!Main.config.instrumentDetectionWorkaround) {
            NoteBlockInstrument instrument = state.get(Properties.INSTRUMENT);
            if(!instrument.isNotBaseBlock() /*Instrument block is below*/ && !client.world.isAir(pos.up())) return null; // Blocked off from playing
            return instrument;
        }

        // Workaround for instrument detection:

        // Pretty much NoteBlock.getStateWithInstrument, but ignoring blockstates and using default instead:
        NoteBlockInstrument aboveBlockInstrument = client.world.getBlockState(pos.up()).getBlock().getDefaultState().getInstrument();
        if (aboveBlockInstrument.isNotBaseBlock()) {
            return aboveBlockInstrument;
        } else {
            NoteBlockInstrument belowBlockInstrument = client.world.getBlockState(pos.down()).getBlock().getDefaultState().getInstrument();
            if(belowBlockInstrument.isNotBaseBlock()) return NoteBlockInstrument.HARP;
            if(!client.world.isAir(pos.up())) return null; // Noteblock can't be played
            return belowBlockInstrument;
        }
    }

    public enum TuningFail {
        /**
         * Player moved too far away from some selected note block
         */
        MovedTooFarAway,
        /**
         * Need to run selectSong() first!
         */
        NoSongSelected,
        /**
         * Player or World not existing due to player not being properly ingame
         */
        NotIngame,
        /**
         * Something - which shouldn't have happened - happened :|
         */
        Unexpected,
    }

    private int getOwnPing(MinecraftClient client) {
        int ping = 0;
        {
            PlayerListEntry playerListEntry;
            if (client.getNetworkHandler() != null && (playerListEntry = client.getNetworkHandler().getPlayerListEntry(client.player.getGameProfile().id())) != null)
                ping = playerListEntry.getLatency();
        }
        if(ping <= 0) {
            // Assume server did respond with a placeholder ping
            ping = 150; // Assume some bad-ish ping.
        }
        return ping;
    }

    public @Nullable TuningFail tickTuning(MinecraftClient client) {
        if(tunedAfter != Util.TIMESTAMP_UNINITIALIZED) return null; // Nothing to do. May need to wait a bit after tuning to be considered done (see isTuned()).

        if(client.player == null || client.world == null) return TuningFail.NotIngame;
        if(!isSongSelected()) return TuningFail.NoSongSelected;
        int ping = getOwnPing(client);

        // Update availableInteracts based on current tuningSpeed config
        switch(Main.config.tuningSpeed) {
            case Snail -> availableInteracts = Math.clamp(availableInteracts + 0.5f, 0f, 1f);
            case Safe -> availableInteracts = 1;
            case Spigot -> {
                if(lastInteractAt == Util.TIMESTAMP_UNINITIALIZED) {
                    availableInteracts = 9f;
                }else {
                    // Spigot (and Paper + forks) allow 9 interacts per 300 ms
                    availableInteracts += ((Util.now() - lastInteractAt) / (310.0f / 9.0f));
                    availableInteracts = Math.min(9f, Math.max(0f, availableInteracts));
                }
            }
            case Flash -> availableInteracts = Integer.MAX_VALUE;
        }

        if(lastInteractAt == Util.TIMESTAMP_UNINITIALIZED)
            lastInteractAt = Util.now();

        int fullyTunedBlocks = 0;
        HashMap<BlockPos, Integer> untunedNotes = new HashMap<>();
        for (Note note : selectedSong.uniqueNotes) {
            if(noteBlocks == null || noteBlocks.get(note.instrument()) == null)
                continue;
            BlockPos blockPos = noteBlocks.get(note.instrument()).get(note.note());
            if(blockPos == null) continue;
            BlockState blockState = client.world.getBlockState(blockPos);
            int assumedNote = notePredictions.containsKey(blockPos) ? notePredictions.get(blockPos).getLeft() : blockState.get(Properties.NOTE);

            if (blockState.contains(Properties.NOTE)) {
                if(assumedNote == note.note() && blockState.get(Properties.NOTE) == note.note())
                    fullyTunedBlocks++;
                if (assumedNote != note.note()) {
                    if (!Util.canInteractWith(client.player, blockPos))
                        return TuningFail.MovedTooFarAway;
                    untunedNotes.put(blockPos, blockState.get(Properties.NOTE));
                }
            } else {
                noteBlocks = null;
                break;
            }
        }

        if(tuneInitialUntunedBlocks == -1 || tuneInitialUntunedBlocks < untunedNotes.size())
            tuneInitialUntunedBlocks = untunedNotes.size();

        int existingUniqueNotesCount = 0;
        for(Note n : selectedSong.uniqueNotes) {
            if(noteBlocks.get(n.instrument()).get(n.note()) != null)
                existingUniqueNotesCount++;
        }

        if(untunedNotes.isEmpty() && fullyTunedBlocks == existingUniqueNotesCount) {
            // Wait roundrip + 100ms before considering tuned after changing notes (in case the server rejects an interact)
            if(lastInteractAt == Util.TIMESTAMP_UNINITIALIZED || Util.now() - lastInteractAt >= ping * 2L + 100) {
                // Tuning finished, delay a little bit, then officially done!
                tunedAfter = Util.now() + (long) Math.max(0, Main.config.delayPlaybackStartBySecs) * 1000;
                tuneInitialUntunedBlocks = -1;
            }
        }

        BlockPos lastBlockPos = null;
        int lastTunedNote = Integer.MIN_VALUE;
        //float roughTuneProgress = 1 - (untunedNotes.size() / Math.max(tuneInitialUntunedBlocks + 0f, 1f));
        while(availableInteracts >= 1f && !untunedNotes.isEmpty()) {
            BlockPos blockPos = null;
            int searches = 0;
            while(blockPos == null) {
                searches++;
                // Find higher note
                for (Map.Entry<BlockPos, Integer> entry : untunedNotes.entrySet()) {
                    if (entry.getValue() > lastTunedNote) {
                        blockPos = entry.getKey();
                        break;
                    }
                }
                // Find higher note or equal
                if (blockPos == null) {
                    for (Map.Entry<BlockPos, Integer> entry : untunedNotes.entrySet()) {
                        if (entry.getValue() >= lastTunedNote) {
                            blockPos = entry.getKey();
                            break;
                        }
                    }
                }
                // Not found. Reset last note
                if(blockPos == null)
                    lastTunedNote = Integer.MIN_VALUE;
                if(blockPos == null && searches > 1) {
                    // Something went wrong. Take any note (one should at least exist here)
                    blockPos = untunedNotes.keySet().toArray(new BlockPos[0])[0];
                    break;
                }
            }
            if(blockPos == null)
                return TuningFail.Unexpected; // Something went very, very wrong!

            lastTunedNote = untunedNotes.get(blockPos);
            untunedNotes.remove(blockPos);
            int assumedNote = notePredictions.containsKey(blockPos) ? notePredictions.get(blockPos).getLeft() : client.world.getBlockState(blockPos).get(Properties.NOTE);
            notePredictions.put(blockPos, new Pair<>((assumedNote + 1) % 25, Util.now() + ping * 2 + 100));
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(blockPos), Direction.UP, blockPos, false));
            lastInteractAt = Util.now();
            availableInteracts -= 1f;
            lastBlockPos = blockPos;
        }
        if(lastBlockPos != null) {
            // Turn head into spinning with time and lookup up further the further tuning is progressed
            //client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(((float) (System.currentTimeMillis() % 2000)) * (360f/2000f), (1 - roughTuneProgress) * 180 - 90, true));
            client.player.swingHand(Hand.MAIN_HAND);
        }

        return null;
    }

    public HashMap<Block, Integer> getMissingInstrumentBlocks() {
        return missingInstrumentBlocks;
    }

    public HashMap<NoteBlockInstrument, HashMap<Byte, BlockPos>> getNoteBlocks() {
        return noteBlocks;
    }
}
