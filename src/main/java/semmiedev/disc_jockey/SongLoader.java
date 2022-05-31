package semmiedev.disc_jockey;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import semmiedev.disc_jockey.gui.SongListWidget;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

public class SongLoader {
    public static final ArrayList<Song> SONGS = new ArrayList<>();
    public static final ArrayList<String> SONG_SUGGESTIONS = new ArrayList<>();
    public static volatile boolean loadingSongs;

    public static void loadSongs() {
        if (loadingSongs) return;
        new Thread(() -> {
            loadingSongs = true;
            SONGS.clear();
            SONG_SUGGESTIONS.clear();
            SONG_SUGGESTIONS.add("Songs are loading, please wait");
            for (File file : Main.songsFolder.listFiles()) {
                if (file.isFile()) {
                    try {
                        BinaryReader reader = new BinaryReader(Files.newInputStream(file.toPath()));
                        Song song = new Song();

                        song.fileName = file.getName();

                        song.length = reader.readShort();

                        boolean newFormat = song.length == 0;
                        if (newFormat) {
                            song.formatVersion = reader.readByte();
                            song.vanillaInstrumentCount = reader.readByte();
                            song.length = reader.readShort();
                        }

                        song.height = reader.readShort();
                        song.name = reader.readString();
                        song.author = reader.readString();
                        song.originalAuthor = reader.readString();
                        song.description = reader.readString();
                        song.tempo = reader.readShort();
                        song.autoSaving = reader.readByte();
                        song.autoSavingDuration = reader.readByte();
                        song.timeSignature = reader.readByte();
                        song.minutesSpent = reader.readInt();
                        song.leftClicks = reader.readInt();
                        song.rightClicks = reader.readInt();
                        song.blocksAdded = reader.readInt();
                        song.blocksRemoved = reader.readInt();
                        song.importFileName = reader.readString();

                        if (newFormat) {
                            song.loop = reader.readByte();
                            song.maxLoopCount = reader.readByte();
                            song.loopStartTick = reader.readShort();
                        }

                        song.displayName = song.name.replaceAll("\\s", "").isEmpty() ? song.fileName : song.name+" ("+song.fileName+")";
                        song.entry = new SongListWidget.SongEntry(song.displayName, SONGS.size());
                        song.searchableFileName = song.fileName.toLowerCase().replaceAll("\\s", "");
                        song.searchableName = song.name.toLowerCase().replaceAll("\\s", "");

                        short tick = -1;
                        short jumps;
                        while ((jumps = reader.readShort()) != 0) {
                            tick += jumps;
                            short layer = -1;
                            while ((jumps = reader.readShort()) != 0) {
                                layer += jumps;

                                byte instrumentId = reader.readByte();
                                byte noteId = (byte)(reader.readByte() - 33);

                                if (newFormat) {
                                    // Data that is not needed as it only works with commands
                                    reader.readByte(); // Velocity
                                    reader.readByte(); // Panning
                                    reader.readShort(); // Pitch
                                }

                                if (noteId < 0) {
                                    noteId = 0;
                                } else if (noteId > 24) {
                                    noteId = 24;
                                }

                                Note note = new Note(Note.INSTRUMENTS[instrumentId], noteId);
                                if (!song.uniqueNotes.contains(note)) song.uniqueNotes.add(note);

                                song.notes = Arrays.copyOf(song.notes, song.notes.length + 1);
                                song.notes[song.notes.length - 1] = tick | layer << Note.LAYER_SHIFT | (long)instrumentId << Note.INSTRUMENT_SHIFT | (long)noteId << Note.NOTE_SHIFT;
                            }
                        }

                        SONGS.add(song);
                    } catch (Throwable exception) {
                        Main.LOGGER.error("Unable to read song "+file.getName(), exception);
                    }
                }
            }
            for (Song song : SONGS) SONG_SUGGESTIONS.add(song.displayName);
            SystemToast.add(MinecraftClient.getInstance().getToastManager(), SystemToast.Type.PACK_LOAD_FAILURE, Main.NAME, Text.translatable(Main.MOD_ID+".loading_done"));
            loadingSongs = false;
        }).start();
    }
}
