package semmiedev.disc_jockey.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import semmiedev.disc_jockey.Main;
import semmiedev.disc_jockey.Song;
import semmiedev.disc_jockey.Util;
import semmiedev.disc_jockey.mixin.EntryListWidgetAccessor;

public class SongListWidget extends EntryListWidget<SongListWidget.SongEntry> {

    public SongListWidget(MinecraftClient client, int width, int height, int top, int itemHeight) {
        super(client, width, height, top, itemHeight);
    }

    public void safeClearEntries() {
        this.clearEntries();
    }

    public void safeReplaceEntries(java.util.Collection<SongListWidget.SongEntry> entries) {
        this.replaceEntries(entries);
    }

    public java.util.List<SongListWidget.SongEntry> getModifiableChildren() {
        return (java.util.List<SongListWidget.SongEntry>) ((EntryListWidgetAccessor) this).getChildrenList();
    }

    public int getItemHeight() {
        return this.itemHeight;
    }


    @Override
    public int getRowWidth() {
        return width - 40;
    }

    @Override
    protected int getScrollbarX() {
        return getX() + width - 12;
    }

    @Override
    public void setSelected(@Nullable SongListWidget.SongEntry entry) {
        SongListWidget.SongEntry selectedEntry = getSelectedOrNull();
        if (selectedEntry != null) selectedEntry.selected = false;
        if (entry != null) entry.selected = true;
        super.setSelected(entry);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // Who cares
    }

    public static class SongEntry extends Entry<SongEntry> {
        private static final Identifier ICONS = Identifier.of(Main.MOD_ID, "textures/gui/icons.png");

        public final int index;
        public final Song song;

        public boolean selected, favorite;
        public SongListWidget songListWidget;
        private long lastClickedAt = Util.TIMESTAMP_UNINITIALIZED;

        private final MinecraftClient client = MinecraftClient.getInstance();

        public SongEntry(Song song, int index) {
            this.song = song;
            this.index = index;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            int x = this.getX();
            int y = this.getY();
            int entryWidth = this.getWidth();
            int entryHeight = this.getHeight();

            if (selected) {
                context.fill(x, y, x + entryWidth, y + entryHeight, 0xFFFFFF);
                context.fill(x + 1, y + 1, x + entryWidth - 1, y + entryHeight - 1, 0x000000);
            }

            context.drawCenteredTextWithShadow(client.textRenderer, song.displayName, x + entryWidth / 2, y + 5, selected ? 0xFFFFFFFF : 0xFF808080);

            context.drawTexture(RenderPipelines.GUI_TEXTURED, ICONS, x + 2, y + 2, (favorite ? 26 : 0) + (isOverFavoriteButton(mouseX, mouseY) ? 13 : 0), 0, 13, 12, 52, 12);
        }

        public Text getNarrateText() {
            return Text.literal(song.displayName);
        }
        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            double mouseX = click.x();
            double mouseY = click.y();
            int button = click.button();

            if (isOverFavoriteButton(mouseX, mouseY)) {
                favorite = !favorite;
                if (favorite) {
                    Main.config.favorites.add(song.fileName);
                } else {
                    Main.config.favorites.remove(song.fileName);
                }
                return true;
            }

            if(songListWidget.getSelectedOrNull() == this && lastClickedAt != -1L && Util.now() - lastClickedAt <= 350) {
                // Double click = start song
                Main.SONG_PLAYER.start(this.song);
            }else {
                songListWidget.setSelected(this);
                lastClickedAt = Util.now();
            }
            return true;
        }

        private boolean isOverFavoriteButton(double mouseX, double mouseY) {
            int x = this.getX();
            int y = this.getY();

            return mouseX > x + 2 && mouseX < x + 15 && mouseY > y + 2 && mouseY < y + 14;
        }
    }
}