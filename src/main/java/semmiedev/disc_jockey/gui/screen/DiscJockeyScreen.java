package semmiedev.disc_jockey.gui.screen;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
    private static final MutableText
            SELECT_SONG = Text.translatable(Main.MOD_ID+".screen.select_song"),
            PLAY = Text.translatable(Main.MOD_ID+".screen.play"),
            PLAY_STOP = Text.translatable(Main.MOD_ID+".screen.play.stop"),
            PREVIEW = Text.translatable(Main.MOD_ID+".screen.preview"),
            PREVIEW_STOP = Text.translatable(Main.MOD_ID+".screen.preview.stop"),
            DROP_HINT = Text.translatable(Main.MOD_ID+".screen.drop_hint").formatted(Formatting.GRAY),
            SONGSTATE_PLAYING = Text.translatable(Main.MOD_ID+".screen.songstate.playing").styled((style) -> style.withItalic(true).withColor(0xDDDDDD)),
            SONGSTATE_PAUSED = Text.translatable(Main.MOD_ID+".screen.songstate.paused").styled((style) -> style.withItalic(true).withColor(0xDDDDDD)),
            SONGSTATE_FINISHED = Text.translatable(Main.MOD_ID+".screen.songstate.finished").styled((style) -> style.withItalic(true).withColor(0xDDDDDD)),
            SONGSTATE_STOPPED = Text.translatable(Main.MOD_ID+".screen.songstate.stopped").styled((style) -> style.withItalic(true).withColor(0xDDDDDD)),
            SONGSTATE_TUNING = Text.translatable(Main.MOD_ID+".screen.songstate.tuning").styled((style) -> style.withItalic(true).withColor(0xDDDDDD)),
            PLEASE_SELECT_SONG = Text.translatable(Main.MOD_ID+".screen.please_select_song").styled((style) -> style.withItalic(true)),
            CONFIG = Text.translatable(Main.MOD_ID+".screen.config")
    ;

    private TextWidget songTitle;
    private TextWidget songState;
    private CyclingButtonWidget<Boolean> playPauseButton;
    private ButtonWidget stopButton;
    private SongTimeSliderWidget timeBar;
    private ButtonWidget configButton;

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
        songListWidget = new SongListWidget(client, width / 2 - 10, height - 64 - 32, 32, 20);
        songListWidget.setX(width / 2);
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
                    //client.setScreen(null);
                }
            }
        }).dimensions((width / 4 * 3) - 160, height - 61, 100, 20).build();
        addDrawableChild(playButton);

        previewButton = ButtonWidget.builder(PREVIEW, button -> {
            if (Main.PREVIEWER.running) {
                Main.PREVIEWER.stop();
            } else {
                SongListWidget.SongEntry entry = songListWidget.getSelectedOrNull();
                if (entry != null) Main.PREVIEWER.start(entry.song);
            }
        }).dimensions((width / 4 * 3) - 50, height - 61, 100, 20).build();
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
                        ItemStack itemStack = Note.INSTRUMENT_BLOCKS.get(note.instrument()).asItem().getDefaultStack();
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
        }).dimensions((width / 4 * 3) + 60, height - 61, 100, 20).build());

        TextFieldWidget searchBar = new TextFieldWidget(textRenderer, (width / 4 * 3) - 75, height - 31, 150, 20, Text.empty());
        searchBar.setPlaceholder(Text.translatable(Main.MOD_ID+".screen.search").styled((style) -> style.withItalic(true).withColor(0xDDDDDD)));
        searchBar.setChangedListener(query -> {
            query = query.toLowerCase().replaceAll("\\s", "");
            if (this.query.equals(query)) return;
            this.query = query;
            shouldFilter = true;
        });
        addDrawableChild(searchBar);

        // TODO: 6/2/2022 Add a reload button

        // Player:
        songState = new TextWidget(10, 32, width / 2 - 20, 20, Text.empty(), getTextRenderer());
        addDrawableChild(songState);
        songTitle = new TextWidget(10, 32 + 20, width / 2 - 20, 20, Text.empty(), getTextRenderer());
        //songTitle.alignLeft();
        addDrawableChild(songTitle);
        timeBar = new SongTimeSliderWidget(10, 32 + 20 + 20, width / 2 - 20, 30);
        addDrawableChild(timeBar);
        playPauseButton = CyclingButtonWidget.<Boolean>builder((value) -> Text.literal(value ? "⏸" : "▶"), Main.SONG_PLAYER.running)
                .omitKeyText()
                .values(true, false)
                .build((width / 4) - 25, 32 + 20 + 20 + 30 + 5, 20, 20, Text.empty(), (button, value) -> {
            if(value && Main.SONG_PLAYER.song != null && Main.SONG_PLAYER.didSongReachEnd) {
                Main.SONG_PLAYER.start(Main.SONG_PLAYER.song); // Restart song
            }else {
                Main.SONG_PLAYER.running = value;
            }
        });
        addDrawableChild(playPauseButton);
        stopButton = ButtonWidget.builder(Text.literal("⏹"), button -> Main.SONG_PLAYER.stop())
                .position((width / 4) + 5, 32 + 20 + 20 + 30 + 5)
                .size(20, 20)
                .build();
        addDrawableChild(stopButton);

        // Config button in bottom left
        configButton = ButtonWidget.builder(CONFIG, (button) -> client.setScreen(AutoConfig.getConfigScreen(Config.class, this).get()))
                .position(10, height - 30)
                .size(100, 20)
                .build();
        addDrawableChild(configButton);
    }

    private static Text getPlaybackStateText() {
        boolean running = Main.SONG_PLAYER.running;
        boolean tuned = Main.SONG_PLAYER.tuner.isTuned();
        boolean didSongReachEnd = Main.SONG_PLAYER.didSongReachEnd;

        if(!running) {
            if(didSongReachEnd)
                return SONGSTATE_FINISHED;
            else if(Main.SONG_PLAYER.getSongElapsedSeconds() == 0.0)
                return SONGSTATE_STOPPED;
            else
                return SONGSTATE_PAUSED;
        }else {
            if(!tuned)
                return SONGSTATE_TUNING;
            else
                return SONGSTATE_PLAYING;
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, EntryListWidget.INWORLD_MENU_LIST_BACKGROUND_TEXTURE, 5, 32, width / 2, 32 + 20 + 20 + 30 + 5 + 20 + 5, this.width / 2 - 10, 20 + 20 + 30 + 5 + 20 + 5, 32, 32);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, DROP_HINT, width / 2, 5, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, SELECT_SONG, (width / 4 * 3), 20, 0xFFFFFF);
    }

    @Override
    public void tick() {
        songState.setMessage(getPlaybackStateText());
        timeBar.update();
        playPauseButton.setValue(Main.SONG_PLAYER.running);
        songTitle.setMessage(Main.SONG_PLAYER.song != null ? Text.literal(Main.SONG_PLAYER.song.displayName) : PLEASE_SELECT_SONG);

        previewButton.setMessage(Main.PREVIEWER.running ? PREVIEW_STOP : PREVIEW);
        playButton.setMessage(Main.SONG_PLAYER.running ? PLAY_STOP : PLAY);

        if (shouldFilter) {
            shouldFilter = false;
            songListWidget.setScrollY(0);
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
    public void onFilesDropped(List<Path> paths) {
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
