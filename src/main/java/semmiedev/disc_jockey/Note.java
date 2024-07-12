package semmiedev.disc_jockey;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.NoteBlockInstrument;

import java.util.HashMap;

public record Note(NoteBlockInstrument instrument, byte note) {
    public static final HashMap<NoteBlockInstrument, Block> INSTRUMENT_BLOCKS = new HashMap<>();

    public static final byte LAYER_SHIFT = Short.SIZE;
    public static final byte INSTRUMENT_SHIFT = Short.SIZE * 2;
    public static final byte NOTE_SHIFT = Short.SIZE * 2 + Byte.SIZE;

    public static final NoteBlockInstrument[] INSTRUMENTS = new NoteBlockInstrument[]{
            NoteBlockInstrument.HARP,
            NoteBlockInstrument.BASS,
            NoteBlockInstrument.BASEDRUM,
            NoteBlockInstrument.SNARE,
            NoteBlockInstrument.HAT,
            NoteBlockInstrument.GUITAR,
            NoteBlockInstrument.FLUTE,
            NoteBlockInstrument.BELL,
            NoteBlockInstrument.CHIME,
            NoteBlockInstrument.XYLOPHONE,
            NoteBlockInstrument.IRON_XYLOPHONE,
            NoteBlockInstrument.COW_BELL,
            NoteBlockInstrument.DIDGERIDOO,
            NoteBlockInstrument.BIT,
            NoteBlockInstrument.BANJO,
            NoteBlockInstrument.PLING

    };

    static {
        INSTRUMENT_BLOCKS.put(NoteBlockInstrument.HARP, Blocks.AIR);
        INSTRUMENT_BLOCKS.put(NoteBlockInstrument.BASEDRUM, Blocks.STONE);
        INSTRUMENT_BLOCKS.put(NoteBlockInstrument.SNARE, Blocks.SAND);
        INSTRUMENT_BLOCKS.put(NoteBlockInstrument.HAT, Blocks.GLASS);
        INSTRUMENT_BLOCKS.put(NoteBlockInstrument.BASS, Blocks.OAK_PLANKS);
        INSTRUMENT_BLOCKS.put(NoteBlockInstrument.FLUTE, Blocks.CLAY);
        INSTRUMENT_BLOCKS.put(NoteBlockInstrument.BELL, Blocks.GOLD_BLOCK);
        INSTRUMENT_BLOCKS.put(NoteBlockInstrument.GUITAR, Blocks.WHITE_WOOL);
        INSTRUMENT_BLOCKS.put(NoteBlockInstrument.CHIME, Blocks.PACKED_ICE);
        INSTRUMENT_BLOCKS.put(NoteBlockInstrument.XYLOPHONE, Blocks.BONE_BLOCK);
        INSTRUMENT_BLOCKS.put(NoteBlockInstrument.IRON_XYLOPHONE, Blocks.IRON_BLOCK);
        INSTRUMENT_BLOCKS.put(NoteBlockInstrument.COW_BELL, Blocks.SOUL_SAND);
        INSTRUMENT_BLOCKS.put(NoteBlockInstrument.DIDGERIDOO, Blocks.PUMPKIN);
        INSTRUMENT_BLOCKS.put(NoteBlockInstrument.BIT, Blocks.EMERALD_BLOCK);
        INSTRUMENT_BLOCKS.put(NoteBlockInstrument.BANJO, Blocks.HAY_BLOCK);
        INSTRUMENT_BLOCKS.put(NoteBlockInstrument.PLING, Blocks.GLOWSTONE);
    }
}
