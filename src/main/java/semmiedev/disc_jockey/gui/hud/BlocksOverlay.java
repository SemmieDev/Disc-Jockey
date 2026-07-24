package semmiedev.disc_jockey.gui.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class BlocksOverlay {
    public static ItemStack[] itemStacks;
    public static int[] amounts;
    public static int amountOfNoteBlocks;

    private static final ItemStack NOTE_BLOCK = Blocks.NOTE_BLOCK.asItem().getDefaultInstance();

    public static void render(GuiGraphicsExtractor extractor, DeltaTracker tickCounter) {
        if (itemStacks != null) {
            extractor.fill(2, 2, 62, (itemStacks.length + 1) * 20 + 7, ARGB.color(255, 22, 22, 27));
            extractor.fill(4, 4, 60, (itemStacks.length + 1) * 20 + 5, ARGB.color(255, 42, 42, 47));

            Minecraft client = Minecraft.getInstance();
            Font textRenderer = client.font;

            //textRenderer.draw(matrices, " × "+amountOfNoteBlocks, 26, 13, 0xFFFFFF);
            extractor.text(textRenderer, " × " + amountOfNoteBlocks, 26, 13, 0xFFFFFF, true);
            //itemRenderer.renderInGui(matrices, NOTE_BLOCK, 6, 6);
            extractor.item(NOTE_BLOCK, 6, 6);

            for (int i = 0; i < itemStacks.length; i++) {
                //textRenderer.draw(matrices, " × "+amounts[i], 26, 13 + 20 * (i + 1), 0xFFFFFF);
                extractor.text(textRenderer, " × " + amounts[i], 26, 13 + 20 * (i + 1), 0xFFFFFF, true);
                //itemRenderer.renderInGui(matrices, itemStacks[i], 6, 6 + 20 * (i + 1));
                extractor.item(itemStacks[i], 6, 6 + 20 * (i + 1));
            }
        }
    }
}
