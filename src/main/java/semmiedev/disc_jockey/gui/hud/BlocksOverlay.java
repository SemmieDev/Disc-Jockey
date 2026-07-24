package semmiedev.disc_jockey.gui.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import semmiedev.disc_jockey.Main;

public class BlocksOverlay {
    public static ItemStack[] itemStacks;
    public static int[] amounts;
    public static int amountOfNoteBlocks;

    private static final ItemStack NOTE_BLOCK = Blocks.NOTE_BLOCK.asItem().getDefaultInstance();

    public static void render(GuiGraphicsExtractor extractor, DeltaTracker tickCounter) {
        if (itemStacks == null) return;

        Minecraft client = Minecraft.getInstance();
        Font textRenderer = client.font;

        // Build display data: skip AIR items (HARP instrument)
        int visibleCount = 0;
        for (int i = 0; i < itemStacks.length; i++) {
            if (itemStacks[i].getItem() != Items.AIR) visibleCount++;
        }

        // Construct header text with translation label
        String headerLabel = Component.translatable(Main.MOD_ID + ".screen.blocks.title").getString();
        String headerText = headerLabel + "  × " + amountOfNoteBlocks;

        // Calculate the widest line for dynamic background width
        int maxLineWidth = textRenderer.width(headerText);
        String[] rowLabels = new String[itemStacks.length];
        ItemStack[] rowIcons = new ItemStack[itemStacks.length];
        int rowIndex = 0;
        for (int i = 0; i < itemStacks.length; i++) {
            ItemStack stack = itemStacks[i];
            if (stack.getItem() == Items.AIR) continue; // Skip AIR (HARP)

            String blockName = stack.getHoverName().getString();
            rowLabels[rowIndex] = blockName + "  × " + amounts[i];
            rowIcons[rowIndex] = stack;
            rowIndex++;

            int lineWidth = textRenderer.width(rowLabels[rowIndex - 1]);
            if (lineWidth > maxLineWidth) maxLineWidth = lineWidth;
        }

        // If no visible items after filtering AIR, just show the header
        int displayRows = Math.max(visibleCount, 0);

        // Box dimensions: 16px icon + 4px gap + maxTextWidth + 12px padding (6 each side)
        int boxLeft = 2;
        int boxTop = 2;
        int boxWidth = maxLineWidth + 32;
        int boxRight = boxLeft + boxWidth;
        int lineHeight = 20;
        int boxHeight = (1 + displayRows) * lineHeight + 7; // Header + rows
        int boxBottom = boxTop + boxHeight;

        // Draw background
        extractor.fill(boxLeft, boxTop, boxRight, boxBottom, ARGB.color(255, 22, 22, 27));
        extractor.fill(boxLeft + 2, boxTop + 2, boxRight - 2, boxBottom - 2, ARGB.color(255, 42, 42, 47));

        // Draw header: NOTE_BLOCK icon + label text + total count
        int textX = boxLeft + 24;
        extractor.item(NOTE_BLOCK, boxLeft + 4, 6);
        extractor.text(textRenderer, headerText, textX, 13, 0xFFFFFF, true);

        // Draw instrument rows: block icon + block name + count
        for (int i = 0; i < displayRows; i++) {
            int y = 6 + lineHeight * (i + 1);
            extractor.item(rowIcons[i], boxLeft + 4, y);
            extractor.text(textRenderer, rowLabels[i], textX, y + 7, 0xFFFFFF, true);
        }
    }
}
