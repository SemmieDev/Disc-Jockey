package semmieboy_yt.disc_jockey;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.Instrument;

import java.util.Map;

public record Note(Instrument instrument, byte note) {
    public static final Map<Instrument, Block> INSTRUMENT_BLOCKS = Map.ofEntries(
            Map.entry(Instrument.HARP, Blocks.AIR),
            Map.entry(Instrument.BASEDRUM, Blocks.STONE),
            Map.entry(Instrument.SNARE, Blocks.SAND),
            Map.entry(Instrument.HAT, Blocks.GLASS),
            Map.entry(Instrument.BASS, Blocks.OAK_PLANKS),
            Map.entry(Instrument.FLUTE, Blocks.CLAY),
            Map.entry(Instrument.BELL, Blocks.GOLD_BLOCK),
            Map.entry(Instrument.GUITAR, Blocks.WHITE_WOOL),
            Map.entry(Instrument.CHIME, Blocks.PACKED_ICE),
            Map.entry(Instrument.XYLOPHONE, Blocks.BONE_BLOCK),
            Map.entry(Instrument.IRON_XYLOPHONE, Blocks.IRON_BLOCK),
            Map.entry(Instrument.COW_BELL, Blocks.SOUL_SAND),
            Map.entry(Instrument.DIDGERIDOO, Blocks.PUMPKIN),
            Map.entry(Instrument.BIT, Blocks.EMERALD_BLOCK),
            Map.entry(Instrument.BANJO, Blocks.HAY_BLOCK),
            Map.entry(Instrument.PLING, Blocks.GLOWSTONE)
    );

    public static final byte LAYER_SHIFT = Short.SIZE;
    public static final byte INSTRUMENT_SHIFT = Short.SIZE * 2;
    public static final byte NOTE_SHIFT = Short.SIZE * 2 + Byte.SIZE;

    public static final Instrument[] INSTRUMENTS = Instrument.values();
}
