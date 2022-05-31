package semmiedev.disc_jockey;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.Instrument;

import java.util.HashMap;

public class Note {
    public static final HashMap<Instrument, Block> INSTRUMENT_BLOCKS = new HashMap<>();

    public static final byte LAYER_SHIFT = Short.SIZE;
    public static final byte INSTRUMENT_SHIFT = Short.SIZE * 2;
    public static final byte NOTE_SHIFT = Short.SIZE * 2 + Byte.SIZE;

    public static final Instrument[] INSTRUMENTS = new Instrument[] {
            Instrument.HARP,
            Instrument.BASS,
            Instrument.BASEDRUM,
            Instrument.SNARE,
            Instrument.HAT,
            Instrument.GUITAR,
            Instrument.FLUTE,
            Instrument.BELL,
            Instrument.CHIME,
            Instrument.XYLOPHONE,
            Instrument.IRON_XYLOPHONE,
            Instrument.COW_BELL,
            Instrument.DIDGERIDOO,
            Instrument.BIT,
            Instrument.BANJO,
            Instrument.PLING
    };

    static {
        INSTRUMENT_BLOCKS.put(Instrument.HARP, Blocks.AIR);
        INSTRUMENT_BLOCKS.put(Instrument.BASEDRUM, Blocks.STONE);
        INSTRUMENT_BLOCKS.put(Instrument.SNARE, Blocks.SAND);
        INSTRUMENT_BLOCKS.put(Instrument.HAT, Blocks.GLASS);
        INSTRUMENT_BLOCKS.put(Instrument.BASS, Blocks.OAK_PLANKS);
        INSTRUMENT_BLOCKS.put(Instrument.FLUTE, Blocks.CLAY);
        INSTRUMENT_BLOCKS.put(Instrument.BELL, Blocks.GOLD_BLOCK);
        INSTRUMENT_BLOCKS.put(Instrument.GUITAR, Blocks.WHITE_WOOL);
        INSTRUMENT_BLOCKS.put(Instrument.CHIME, Blocks.PACKED_ICE);
        INSTRUMENT_BLOCKS.put(Instrument.XYLOPHONE, Blocks.BONE_BLOCK);
        INSTRUMENT_BLOCKS.put(Instrument.IRON_XYLOPHONE, Blocks.IRON_BLOCK);
        INSTRUMENT_BLOCKS.put(Instrument.COW_BELL, Blocks.SOUL_SAND);
        INSTRUMENT_BLOCKS.put(Instrument.DIDGERIDOO, Blocks.PUMPKIN);
        INSTRUMENT_BLOCKS.put(Instrument.BIT, Blocks.EMERALD_BLOCK);
        INSTRUMENT_BLOCKS.put(Instrument.BANJO, Blocks.HAY_BLOCK);
        INSTRUMENT_BLOCKS.put(Instrument.PLING, Blocks.GLOWSTONE);
    }

    public final Instrument instrument;
    public final byte note;

    public Note(Instrument instrument, byte note) {
        this.instrument = instrument;
        this.note = note;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Note) {
            Note note = (Note)obj;
            return note.note == this.note && note.instrument == instrument;
        }
        return false;
    }
}
