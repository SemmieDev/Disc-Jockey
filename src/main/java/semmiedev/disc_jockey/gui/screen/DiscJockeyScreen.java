package semmiedev.disc_jockey.gui.screen;

import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import semmiedev.disc_jockey.Main;
import semmiedev.disc_jockey.Note;
import semmiedev.disc_jockey.Song;
import semmiedev.disc_jockey.SongLoader;
import semmiedev.disc_jockey.gui.SongListWidget;
import semmiedev.disc_jockey.gui.hud.BlocksOverlay;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DiscJockeyScreen extends Screen {
    private static final MutableText
            SELECT_SONG = Text.translatable(Main.MOD_ID+".screen.select_song"),
            PLAY = Text.translatable(Main.MOD_ID+".screen.play"),
            PLAY_STOP = Text.translatable(Main.MOD_ID+".screen.play.stop"),
            PREVIEW = Text.translatable(Main.MOD_ID+".screen.preview"),
            PREVIEW_STOP = Text.translatable(Main.MOD_ID+".screen.preview.stop"),
            DROP_HINT = Text.translatable(Main.MOD_ID+".screen.drop_hint").formatted(Formatting.GRAY)
    ;

    private SongListWidget songListWidget;
    private ButtonWidget playButton, previewButton;
    private boolean shouldFilter;
    private String query = "";

    public DiscJockeyScreen() {
        super(Main.NAME);
    }

    @Override
    protected void init() {
        shouldFilter = true;
        songListWidget = new SongListWidget(client, width, height, 32, height - 64, 20);
        addDrawableChild(songListWidget);
        for (int i = 0; i < SongLoader.SONGS.size(); i++) {
            Song song = SongLoader.SONGS.get(i);
            song.entry.songListWidget = songListWidget;
            if (song.entry.selected) songListWidget.setSelected(song.entry);
        }

        playButton = ButtonWidget.builder(PLAY, button -> {
            if (Main.SONG_PLAYER.running) {
                Main.SONG_PLAYER.stop();
            } else {
                SongListWidget.SongEntry entry = songListWidget.getSelectedOrNull();
                if (entry != null) {
                    Main.SONG_PLAYER.start(entry.song);
                    client.setScreen(null);
                }
            }
        }).dimensions(width / 2 - 160, height - 61, 100, 20).build();
        addDrawableChild(playButton);

        previewButton = ButtonWidget.builder(PREVIEW, button -> {
            if (Main.PREVIEWER.running) {
                Main.PREVIEWER.stop();
            } else {
                SongListWidget.SongEntry entry = songListWidget.getSelectedOrNull();
                if (entry != null) Main.PREVIEWER.start(entry.song);
            }
        }).dimensions(width / 2 - 50, height - 61, 100, 20).build();
        addDrawableChild(previewButton);

        addDrawableChild(ButtonWidget.builder(Text.translatable(Main.MOD_ID+".screen.blocks"), button -> {
            // TODO: 6/2/2022 Add an auto build mode
            if (BlocksOverlay.itemStacks == null) {
                SongListWidget.SongEntry entry = songListWidget.getSelectedOrNull();
                if (entry != null) {
                    client.setScreen(null);

                    BlocksOverlay.itemStacks = new ItemStack[0];
                    BlocksOverlay.amounts = new int[0];
                    BlocksOverlay.amountOfNoteBlocks = entry.song.uniqueNotes.size();

                    for (Note note : entry.song.uniqueNotes) {
                        ItemStack itemStack = Note.INSTRUMENT_BLOCKS.get(note.instrument).asItem().getDefaultStack();
                        int index = -1;

                        for (int i = 0; i < BlocksOverlay.itemStacks.length; i++) {
                            if (BlocksOverlay.itemStacks[i].getItem() == itemStack.getItem()) {
                                index = i;
                                break;
                            }
                        }

                        if (index == -1) {
                            BlocksOverlay.itemStacks = Arrays.copyOf(BlocksOverlay.itemStacks, BlocksOverlay.itemStacks.length + 1);
                            BlocksOverlay.amounts = Arrays.copyOf(BlocksOverlay.amounts, BlocksOverlay.amounts.length + 1);

                            BlocksOverlay.itemStacks[BlocksOverlay.itemStacks.length - 1] = itemStack;
                            BlocksOverlay.amounts[BlocksOverlay.amounts.length - 1] = 1;
                        } else {
                            BlocksOverlay.amounts[index] = BlocksOverlay.amounts[index] + 1;
                        }
                    }
                }
            } else {
                BlocksOverlay.itemStacks = null;
                client.setScreen(null);
            }
        }).dimensions(width / 2 + 60, height - 61, 100, 20).build());

        TextFieldWidget searchBar = new TextFieldWidget(textRenderer, width / 2 - 75, height - 31, 150, 20, Text.translatable(Main.MOD_ID+".screen.search"));
        searchBar.setChangedListener(query -> {
            query = query.toLowerCase().replaceAll("\\s", "");
            if (this.query.equals(query)) return;
            this.query = query;
            shouldFilter = true;
        });
        addDrawableChild(searchBar);

        // TODO: 6/2/2022 Add a reload button
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        drawCenteredTextWithShadow(matrices, textRenderer, DROP_HINT, width / 2, 5, 0xFFFFFF);
        drawCenteredTextWithShadow(matrices, textRenderer, SELECT_SONG, width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void tick() {
        previewButton.setMessage(Main.PREVIEWER.running ? PREVIEW_STOP : PREVIEW);
        playButton.setMessage(Main.SONG_PLAYER.running ? PLAY_STOP : PLAY);

        if (shouldFilter) {
            shouldFilter = false;
            songListWidget.setScrollAmount(0);
            songListWidget.children().clear();
            boolean empty = query.isEmpty();
            int favoriteIndex = 0;
            for (Song song : SongLoader.SONGS) {
                if (empty || song.searchableFileName.contains(query) || song.searchableName.contains(query)) {
                    if (song.entry.favorite) {
                        songListWidget.children().add(favoriteIndex++, song.entry);
                    } else {
                        songListWidget.children().add(song.entry);
                    }
                }
            }
        }
    }

    @Override
    public void filesDragged(List<Path> paths) {
        String string = paths.stream().map(Path::getFileName).map(Path::toString).collect(Collectors.joining(", "));
        if (string.length() > 300) string = string.substring(0, 300)+"...";

        client.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                paths.forEach(path -> {
                    try {
                        File file = path.toFile();

                        if (SongLoader.SONGS.stream().anyMatch(input -> input.fileName.equalsIgnoreCase(file.getName()))) return;

                        Song song = SongLoader.loadSong(file);
                        if (song != null) {
                            Files.copy(path, Main.songsFolder.toPath().resolve(file.getName()));
                            SongLoader.SONGS.add(song);
                        }
                    } catch (IOException exception) {
                        Main.LOGGER.warn("Failed to copy song file from {} to {}", path, Main.songsFolder.toPath(), exception);
                    }
                });

                SongLoader.sort();
            }
            client.setScreen(this);
        }, Text.translatable(Main.MOD_ID+".screen.drop_confirm"), Text.literal(string)));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        super.close();
        new Thread(() -> Main.configHolder.save()).start();
    }
}
