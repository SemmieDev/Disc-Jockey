package semmiedev.disc_jockey.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import semmiedev.disc_jockey.Main;
import semmiedev.disc_jockey.Song;

public class SongListWidget extends EntryListWidget<SongListWidget.SongEntry> {
    public SongListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
        super(client, width, height, top, bottom, itemHeight);
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        // Who cares
    }

    @Override
    public int getRowWidth() {
        return width - 40;
    }

    @Override
    protected int getScrollbarPositionX() {
        return width - 12;
    }

    @Override
    public void setSelected(@Nullable SongListWidget.SongEntry entry) {
        SongListWidget.SongEntry selectedEntry = getSelectedOrNull();
        if (selectedEntry != null) selectedEntry.selected = false;
        if (entry != null) entry.selected = true;
        super.setSelected(entry);
    }

    // TODO: 6/2/2022 Add a delete icon
    public static class SongEntry extends Entry<SongEntry> {
        private static final Identifier ICONS = new Identifier(Main.MOD_ID, "textures/gui/icons.png");

        public final int index;
        public final Song song;

        public boolean selected, favorite;
        public SongListWidget songListWidget;

        private final MinecraftClient client = MinecraftClient.getInstance();

        private int x, y, entryWidth, entryHeight;

        public SongEntry(Song song, int index) {
            this.song = song;
            this.index = index;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.x = x; this.y = y; this.entryWidth = entryWidth; this.entryHeight = entryHeight;

            if (selected) {
                //fill(matrices, x, y, x + entryWidth, y + entryHeight, 0xFFFFFF);
                context.fill(x, y, x + entryWidth, y + entryHeight, 0xFFFFFF);
                //fill(matrices, x + 1, y + 1, x + entryWidth - 1, y + entryHeight - 1, 0x000000);
                context.fill(x + 1, y + 1, x + entryWidth - 1, y + entryHeight - 1, 0x000000);
            }

            //drawCenteredTextWithShadow(matrices, client.textRenderer, song.displayName, x + entryWidth / 2, y + 5, selected ? 0xFFFFFF : 0x808080);
            context.drawCenteredTextWithShadow(client.textRenderer, song.displayName, x + entryWidth / 2, y + 5, selected ? 0xFFFFFF : 0x808080);

            //RenderSystem.setShaderTexture(0, ICONS);
            //drawTexture(matrices, x + 2, y + 2, (favorite ? 26 : 0) + (isOverFavoriteButton(mouseX, mouseY) ? 13 : 0), 0, 13, 12, 52, 12);
            context.drawTexture(ICONS, x + 2, y + 2, (favorite ? 26 : 0) + (isOverFavoriteButton(mouseX, mouseY) ? 13 : 0), 0, 13, 12, 52, 12);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isOverFavoriteButton(mouseX, mouseY)) {
                favorite = !favorite;
                if (favorite) {
                    Main.config.favorites.add(song.fileName);
                } else {
                    Main.config.favorites.remove(song.fileName);
                }
                return true;
            }
            songListWidget.setSelected(this);
            return true;
        }

        private boolean isOverFavoriteButton(double mouseX, double mouseY) {
            return mouseX > x + 2 && mouseX < x + 15 && mouseY > y + 2 && mouseY < y + 14;
        }
    }
}
