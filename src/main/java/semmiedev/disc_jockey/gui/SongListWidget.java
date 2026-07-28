package semmiedev.disc_jockey.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import semmiedev.disc_jockey.Main;
import semmiedev.disc_jockey.Song;
import semmiedev.disc_jockey.Util;
import semmiedev.disc_jockey.mixin.EntryListWidgetAccessor;

public class SongListWidget extends AbstractSelectionList<SongListWidget.SongEntry> {

    public SongListWidget(Minecraft client, int width, int height, int top, int itemHeight) {
        super(client, width, height, top, itemHeight);
    }

    public void safeClearEntries() {
        this.clearEntries();
    }

    public void safeReplaceEntries(java.util.Collection<SongListWidget.SongEntry> entries) {
        this.replaceEntries(entries);
    }

    @SuppressWarnings("unchecked")
    public java.util.List<SongListWidget.SongEntry> getModifiableChildren() {
        return (java.util.List<SongListWidget.SongEntry>) ((EntryListWidgetAccessor) this).getChildrenList();
    }

    public int getItemHeight() {
        return this.defaultEntryHeight;
    }


    @Override
    public int getRowWidth() {
        return width - 40;
    }

    @Override
    protected int scrollBarX() {
        return getX() + width - 12;
    }

    @Override
    public void setSelected(@Nullable SongListWidget.SongEntry entry) {
        SongListWidget.SongEntry selectedEntry = getSelected();
        if (selectedEntry != null) selectedEntry.selected = false;
        if (entry != null) entry.selected = true;
        super.setSelected(entry);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        // Who cares
    }

    public static class SongEntry extends Entry<SongEntry> {
        private static final Identifier ICONS = Identifier.fromNamespaceAndPath(Main.MOD_ID, "textures/gui/icons.png");

        public final int index;
        public final Song song;

        public boolean selected, favorite;
        public SongListWidget songListWidget;
        private long lastClickedAt = Util.TIMESTAMP_UNINITIALIZED;

        private final Minecraft client = Minecraft.getInstance();

        public SongEntry(Song song, int index) {
            this.song = song;
            this.index = index;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor extractor, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            int x = this.getX();
            int y = this.getY();
            int entryWidth = this.getWidth();
            int entryHeight = this.getHeight();

            if (selected) {
                extractor.fill(x, y, x + entryWidth, y + entryHeight, 0xFFFFFF);
                extractor.fill(x + 1, y + 1, x + entryWidth - 1, y + entryHeight - 1, 0x000000);
            }

            extractor.centeredText(client.font, song.displayName, x + entryWidth / 2, y + 5, selected ? 0xFFFFFFFF : 0xFF808080);

            extractor.blit(RenderPipelines.GUI_TEXTURED, ICONS, x + 2, y + 2, (favorite ? 26 : 0) + (isOverFavoriteButton(mouseX, mouseY) ? 13 : 0), 0, 13, 12, 52, 12);
        }

        public Component getNarrateText() {
            return Component.literal(song.displayName);
        }
        @Override
        public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
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

            if (songListWidget.getSelected() == this && lastClickedAt != -1L && Util.now() - lastClickedAt <= 350) {
                // Double click = start song
                Main.SONG_PLAYER.start(this.song);
            } else {
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