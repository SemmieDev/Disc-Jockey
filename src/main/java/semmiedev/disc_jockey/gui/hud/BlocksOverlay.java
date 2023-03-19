package semmiedev.disc_jockey.gui.hud;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.ColorHelper;

public class BlocksOverlay {
    public static ItemStack[] itemStacks;
    public static int[] amounts;
    public static int amountOfNoteBlocks;

    private static final ItemStack NOTE_BLOCK = Blocks.NOTE_BLOCK.asItem().getDefaultStack();

    public static void render(MatrixStack matrices, float tickDelta) {
        if (itemStacks != null) {
            DrawableHelper.fill(matrices, 2, 2, 62, (itemStacks.length + 1) * 20 + 7, ColorHelper.Argb.getArgb(255, 22, 22, 27));
            DrawableHelper.fill(matrices, 4, 4, 60, (itemStacks.length + 1) * 20 + 5, ColorHelper.Argb.getArgb(255, 42, 42, 47));

            MinecraftClient client = MinecraftClient.getInstance();
            TextRenderer textRenderer = client.textRenderer;
            ItemRenderer itemRenderer = client.getItemRenderer();

            textRenderer.draw(matrices, " × "+amountOfNoteBlocks, 26, 13, 0xFFFFFF);
            itemRenderer.renderInGui(matrices, NOTE_BLOCK, 6, 6);

            for (int i = 0; i < itemStacks.length; i++) {
                textRenderer.draw(matrices, " × "+amounts[i], 26, 13 + 20 * (i + 1), 0xFFFFFF);
                itemRenderer.renderInGui(matrices, itemStacks[i], 6, 6 + 20 * (i + 1));
            }
        }
    }
}
