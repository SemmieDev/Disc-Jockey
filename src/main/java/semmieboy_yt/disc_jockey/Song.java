package semmieboy_yt.disc_jockey;

import semmieboy_yt.disc_jockey.gui.SongListWidget;

import java.util.ArrayList;

public class Song {
    public final ArrayList<Note> uniqueNotes = new ArrayList<>();

    public long[] notes = new long[0];

    public short length, height, tempo, loopStartTick;
    public String fileName, name, author, originalAuthor, description;
    public byte autoSaving, autoSavingDuration, timeSignature, vanillaInstrumentCount, formatVersion, loop, maxLoopCount;
    public int minutesSpent, leftClicks, rightClicks, blocksAdded, blocksRemoved;
    public String importFileName;

    public SongListWidget.SongEntry entry;
    public String searchableFileName, searchableName;
}
