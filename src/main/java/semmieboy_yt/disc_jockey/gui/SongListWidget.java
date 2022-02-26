package semmieboy_yt.disc_jockey.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;

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

    public static class SongEntry extends Entry<SongEntry> {
        public final int index;

        public boolean selected;
        public SongListWidget songListWidget;

        private final String name;
        private final MinecraftClient client = MinecraftClient.getInstance();

        public SongEntry(String name, int index) {
            this.name = name;
            this.index = index;
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (selected) {
                fill(matrices, x, y, x + entryWidth, y + entryHeight, 0xFFFFFF);
                fill(matrices, x + 1, y + 1, x + entryWidth - 1, y + entryHeight - 1, 0x000000);
            }
            drawCenteredText(matrices, client.textRenderer, name, x + entryWidth / 2, y + 5, selected ? 0xFFFFFF : 0x808080);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            // it always gets clicked on
            songListWidget.setSelected(this);
            return true;
        }
    }
}
