package semmiedev.disc_jockey.gui.screen;

import me.shedaniel.autoconfig.AutoConfigClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import semmiedev.disc_jockey.*;
import semmiedev.disc_jockey.gui.SongListWidget;
import semmiedev.disc_jockey.gui.SongTimeSliderWidget;
import semmiedev.disc_jockey.gui.hud.BlocksOverlay;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DiscJockeyScreen extends Screen {
    private static final MutableComponent
            SELECT_SONG = Component.translatable(Main.MOD_ID+".screen.select_song"),
            PLAY = Component.translatable(Main.MOD_ID+".screen.play"),
            PLAY_STOP = Component.translatable(Main.MOD_ID+".screen.play.stop"),
            PREVIEW = Component.translatable(Main.MOD_ID+".screen.preview"),
            PREVIEW_STOP = Component.translatable(Main.MOD_ID+".screen.preview.stop"),
            DROP_HINT = Component.translatable(Main.MOD_ID+".screen.drop_hint").withStyle(ChatFormatting.GRAY),
            SONGSTATE_PLAYING = Component.translatable(Main.MOD_ID+".screen.songstate.playing").withStyle((style) -> style.withItalic(true).withColor(0xDDDDDD)),
            SONGSTATE_PAUSED = Component.translatable(Main.MOD_ID+".screen.songstate.paused").withStyle((style) -> style.withItalic(true).withColor(0xDDDDDD)),
            SONGSTATE_FINISHED = Component.translatable(Main.MOD_ID+".screen.songstate.finished").withStyle((style) -> style.withItalic(true).withColor(0xDDDDDD)),
            SONGSTATE_STOPPED = Component.translatable(Main.MOD_ID+".screen.songstate.stopped").withStyle((style) -> style.withItalic(true).withColor(0xDDDDDD)),
            SONGSTATE_TUNING = Component.translatable(Main.MOD_ID+".screen.songstate.tuning").withStyle((style) -> style.withItalic(true).withColor(0xDDDDDD)),
            PLEASE_SELECT_SONG = Component.translatable(Main.MOD_ID+".screen.please_select_song").withStyle((style) -> style.withItalic(true)),
            CONFIG = Component.translatable(Main.MOD_ID+".screen.config")
    ;

    private StringWidget songTitle;
    private StringWidget songState;
    private CycleButton<Boolean> playPauseButton;
    private Button stopButton;
    private SongTimeSliderWidget timeBar;
    private Button configButton;

    private SongListWidget songListWidget;
    private Button playButton, previewButton;
    private boolean shouldFilter;
    private String query = "";

    public DiscJockeyScreen() {
        super(Main.NAME);
    }

    @Override
    protected void init() {
        shouldFilter = true;
        songListWidget = new SongListWidget(minecraft, width / 2 - 10, height - 64 - 32, 32, 20);
        songListWidget.setX(width / 2);
        addRenderableWidget(songListWidget);
        for (int i = 0; i < SongLoader.SONGS.size(); i++) {
            Song song = SongLoader.SONGS.get(i);
            song.entry.songListWidget = songListWidget;
            if (song.entry.selected) songListWidget.setSelected(song.entry);
        }

        playButton = Button.builder(PLAY, button -> {
            if (Main.SONG_PLAYER.running) {
                Main.SONG_PLAYER.stop();
            } else {
                SongListWidget.SongEntry entry = songListWidget.getSelected();
                if (entry != null) {
                    Main.SONG_PLAYER.start(entry.song);
                    //client.setScreen(null);
                }
            }
        }).bounds((width / 4 * 3) - 160, height - 61, 100, 20).build();
        addRenderableWidget(playButton);

        previewButton = Button.builder(PREVIEW, button -> {
            if (Main.PREVIEWER.running) {
                Main.PREVIEWER.stop();
            } else {
                SongListWidget.SongEntry entry = songListWidget.getSelected();
                if (entry != null) Main.PREVIEWER.start(entry.song);
            }
        }).bounds((width / 4 * 3) - 50, height - 61, 100, 20).build();
        addRenderableWidget(previewButton);

        addRenderableWidget(Button.builder(Component.translatable(Main.MOD_ID+".screen.blocks"), button -> {
            // TODO: 6/2/2022 Add an auto build mode
            if (BlocksOverlay.itemStacks == null) {
                SongListWidget.SongEntry entry = songListWidget.getSelected();
                if (entry != null) {
                    minecraft.gui.setScreen(null);

                    BlocksOverlay.itemStacks = new ItemStack[0];
                    BlocksOverlay.amounts = new int[0];
                    BlocksOverlay.amountOfNoteBlocks = entry.song.uniqueNotes.size();

                    for (Note note : entry.song.uniqueNotes) {
                        ItemStack itemStack = Note.INSTRUMENT_BLOCKS.get(note.instrument()).asItem().getDefaultInstance();
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
                minecraft.gui.setScreen(null);
            }
        }).bounds((width / 4 * 3) + 60, height - 61, 100, 20).build());

        EditBox searchBar = new EditBox(font, (width / 4 * 3) - 75, height - 31, 150, 20, Component.empty());
        searchBar.setHint(Component.translatable(Main.MOD_ID+".screen.search").withStyle((style) -> style.withItalic(true).withColor(0xDDDDDD)));
        searchBar.setResponder(query -> {
            query = query.toLowerCase().replaceAll("\\s", "");
            if (this.query.equals(query)) return;
            this.query = query;
            shouldFilter = true;
        });
        addRenderableWidget(searchBar);

        // TODO: 6/2/2022 Add a reload button

        // Player:
        songState = new StringWidget(10, 32, width / 2 - 20, 20, Component.empty(), getFont());
        addRenderableWidget(songState);
        songTitle = new StringWidget(10, 32 + 20, width / 2 - 20, 20, Component.empty(), getFont());
        //songTitle.alignLeft();
        addRenderableWidget(songTitle);
        timeBar = new SongTimeSliderWidget(10, 32 + 20 + 20, width / 2 - 20, 30);
        addRenderableWidget(timeBar);
        playPauseButton = CycleButton.<Boolean>builder((value) -> Component.literal(value ? "⏸" : "▶"), Main.SONG_PLAYER.running)
                .displayOnlyValue()
                .withValues(true, false)
                .create((width / 4) - 25, 32 + 20 + 20 + 30 + 5, 20, 20, Component.empty(), (button, value) -> {
            if (value && Main.SONG_PLAYER.song != null && Main.SONG_PLAYER.didSongReachEnd) {
                Main.SONG_PLAYER.start(Main.SONG_PLAYER.song); // Restart song
            } else {
                Main.SONG_PLAYER.running = value;
            }
        });
        addRenderableWidget(playPauseButton);
        stopButton = Button.builder(Component.literal("⏹"), button -> Main.SONG_PLAYER.stop())
                .pos((width / 4) + 5, 32 + 20 + 20 + 30 + 5)
                .size(20, 20)
                .build();
        addRenderableWidget(stopButton);

        // Config button in bottom left
        configButton = Button.builder(CONFIG, (button) ->
            minecraft.gui.setScreen(AutoConfigClient.<Config>getConfigScreen(Config.class, this).get())
        )
                .pos(10, height - 30)
                .size(100, 20)
                .build();
        addRenderableWidget(configButton);
    }

    private static Component getPlaybackStateText() {
        boolean running = Main.SONG_PLAYER.running;
        boolean tuned = Main.SONG_PLAYER.tuner.isTuned();
        boolean didSongReachEnd = Main.SONG_PLAYER.didSongReachEnd;

        if(!running) {
            if (didSongReachEnd) {
                return SONGSTATE_FINISHED;
            } else if (Main.SONG_PLAYER.getSongElapsedSeconds() == 0.0) {
                return SONGSTATE_STOPPED;
            } else {
                return SONGSTATE_PAUSED;
            }
        } else {
            if (!tuned) {
                return SONGSTATE_TUNING;
            } else {
                return SONGSTATE_PLAYING;
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractBackground(extractor, mouseX, mouseY, delta);
        extractor.blit(RenderPipelines.GUI_TEXTURED, Screen.MENU_BACKGROUND, 5, 32, width / 2, 32 + 20 + 20 + 30 + 5 + 20 + 5, this.width / 2 - 10, 20 + 20 + 30 + 5 + 20 + 5, 32, 32);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractRenderState(extractor, mouseX, mouseY, delta);

        extractor.centeredText(font, DROP_HINT, width / 2, 5, 0xFFFFFF);
        extractor.centeredText(font, SELECT_SONG, (width / 4 * 3), 20, 0xFFFFFF);
    }

    @Override
    public void tick() {
        songState.setMessage(getPlaybackStateText());
        timeBar.update();
        playPauseButton.setValue(Main.SONG_PLAYER.running);
        songTitle.setMessage(Main.SONG_PLAYER.song != null ? Component.literal(Main.SONG_PLAYER.song.displayName) : PLEASE_SELECT_SONG);

        previewButton.setMessage(Main.PREVIEWER.running ? PREVIEW_STOP : PREVIEW);
        playButton.setMessage(Main.SONG_PLAYER.running ? PLAY_STOP : PLAY);

        if (shouldFilter) {
            shouldFilter = false;
            songListWidget.setScrollAmount(0);
            java.util.List<SongListWidget.SongEntry> newEntries = new java.util.ArrayList<>();
            boolean empty = query.isEmpty();
            int favoriteIndex = 0;
            for (Song song : SongLoader.SONGS) {
                if (empty || song.searchableFileName.contains(query) || song.searchableName.contains(query)) {
                    song.entry.songListWidget = songListWidget;
                    if (song.entry.favorite) {
                        newEntries.add(favoriteIndex++, song.entry);
                    } else {
                        newEntries.add(song.entry);
                    }
                }
            }
            songListWidget.safeReplaceEntries(newEntries);
        }
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        String string = paths.stream().map(Path::getFileName).map(Path::toString).collect(Collectors.joining(", "));
        if (string.length() > 300) string = string.substring(0, 300)+"...";

        minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
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
            minecraft.gui.setScreen(this);
        }, Component.translatable(Main.MOD_ID+".screen.drop_confirm"), Component.literal(string)));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        new Thread(() -> Main.configHolder.save()).start();
    }
}
