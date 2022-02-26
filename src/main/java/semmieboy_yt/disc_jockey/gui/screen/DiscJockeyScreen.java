package semmieboy_yt.disc_jockey.gui.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.TranslatableText;
import semmieboy_yt.disc_jockey.Main;
import semmieboy_yt.disc_jockey.Note;
import semmieboy_yt.disc_jockey.Song;
import semmieboy_yt.disc_jockey.SongLoader;
import semmieboy_yt.disc_jockey.gui.SongListWidget;
import semmieboy_yt.disc_jockey.gui.hud.BlocksOverlay;

import java.util.Arrays;

public class DiscJockeyScreen extends Screen {
    private static final TranslatableText
            SELECT_SONG = new TranslatableText(Main.MOD_ID+".screen.select_song"),
            PLAY = new TranslatableText(Main.MOD_ID+".screen.play"),
            PLAY_STOP = new TranslatableText(Main.MOD_ID+".screen.play.stop"),
            PREVIEW = new TranslatableText(Main.MOD_ID+".screen.preview"),
            PREVIEW_STOP = new TranslatableText(Main.MOD_ID+".screen.preview.stop")
    ;

    private SongListWidget songListWidget;
    private ButtonWidget playButton, previewButton;
    private boolean shouldFilter;
    private String query;

    public DiscJockeyScreen() {
        super(new TranslatableText(Main.MOD_ID+".screen.title"));
    }

    @Override
    protected void init() {
        songListWidget = new SongListWidget(client, width, height, 32, height - 64, 20);
        addDrawableChild(songListWidget);
        for (int i = 0; i < SongLoader.SONGS.size(); i++) {
            Song song = SongLoader.SONGS.get(i);
            songListWidget.children().add(song.entry);
            song.entry.songListWidget = songListWidget;
            if (song.entry.selected) songListWidget.setSelected(song.entry);
        }

        playButton = new ButtonWidget(width / 2 - 160, height - 61, 100, 20, PLAY, button -> {
            if (Main.SONG_PLAYER.running) {
                Main.SONG_PLAYER.stop();
            } else {
                SongListWidget.SongEntry entry = songListWidget.getSelectedOrNull();
                if (entry != null) {
                    Main.SONG_PLAYER.start(SongLoader.SONGS.get(entry.index));
                    client.setScreen(null);
                }
            }
        });
        addDrawableChild(playButton);

        previewButton = new ButtonWidget(width / 2 - 50, height - 61, 100, 20, PREVIEW, button -> {
            if (Main.PREVIEWER.running) {
                Main.PREVIEWER.stop();
            } else {
                SongListWidget.SongEntry entry = songListWidget.getSelectedOrNull();
                if (entry != null) Main.PREVIEWER.start(SongLoader.SONGS.get(entry.index));
            }
        });
        addDrawableChild(previewButton);

        addDrawableChild(new ButtonWidget(width / 2 + 60, height - 61, 100, 20, new TranslatableText(Main.MOD_ID+".screen.blocks"), button -> {
            SongListWidget.SongEntry entry = songListWidget.getSelectedOrNull();
            if (entry != null) {
                client.setScreen(null);
                Song song = SongLoader.SONGS.get(entry.index);

                BlocksOverlay.itemStacks = new ItemStack[0];
                BlocksOverlay.amounts = new int[0];
                BlocksOverlay.amountOfNoteBlocks = song.uniqueNotes.size();

                for (Note note : song.uniqueNotes) {
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
        }));

        TextFieldWidget searchBar = new TextFieldWidget(textRenderer, width / 2 - 75, height - 31, 150, 20, new TranslatableText(Main.MOD_ID+".screen.search"));
        searchBar.setChangedListener(query -> {
            this.query = query.toLowerCase().replaceAll("\\s", "");
            shouldFilter = true;
        });
        addDrawableChild(searchBar);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        drawCenteredText(matrices, textRenderer, SELECT_SONG, width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void tick() {
        previewButton.setMessage(Main.PREVIEWER.running ? PREVIEW_STOP : PREVIEW);
        playButton.setMessage(Main.SONG_PLAYER.running ? PLAY_STOP : PLAY);

        if (shouldFilter) {
            shouldFilter = false;
            songListWidget.setScrollAmount(0);
            songListWidget.children().clear();
            for (Song song : SongLoader.SONGS) if (song.searchableFileName.contains(query) || song.searchableName.contains(query)) songListWidget.children().add(song.entry);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
    }
}
