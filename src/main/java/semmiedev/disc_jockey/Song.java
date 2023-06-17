package semmiedev.disc_jockey;

import semmiedev.disc_jockey.gui.SongListWidget;

import java.util.ArrayList;

public class Song {
    public final ArrayList<Note> uniqueNotes = new ArrayList<>();

    public long[] notes = new long[0];

    public short length, height, tempo, loopStartTick;
    public String fileName, name, author, originalAuthor, description, displayName;
    public byte autoSaving, autoSavingDuration, timeSignature, vanillaInstrumentCount, formatVersion, loop, maxLoopCount;
    public int minutesSpent, leftClicks, rightClicks, blocksAdded, blocksRemoved;
    public String importFileName;

    public SongListWidget.SongEntry entry;
    public String searchableFileName, searchableName;

    @Override
    public String toString() {
        return displayName;
    }

    public double millisecondsToTicks(long milliseconds) {
        // From NBS Format: The tempo of the song multiplied by 100 (for example, 1225 instead of 12.25). Measured in ticks per second.
        double songSpeed = (tempo / 100.0) / 20.0; // 20 Ticks per second (temp / 100 = 20) would be 1x speed
        double oneMsTo20TickFraction = 1.0 / 50.0;
        return milliseconds * oneMsTo20TickFraction * songSpeed;
    }

    public double ticksToMilliseconds(double ticks) {
        double songSpeed = (tempo / 100.0) / 20.0;
        double oneMsTo20TickFraction = 1.0 / 50.0;
        return ticks / oneMsTo20TickFraction / songSpeed;
    }

    public double getLengthInSeconds() {
        return ticksToMilliseconds(length) / 1000.0;
    }

}
