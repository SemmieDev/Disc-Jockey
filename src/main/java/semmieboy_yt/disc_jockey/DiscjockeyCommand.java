package semmieboy_yt.disc_jockey;

import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.TranslatableText;
import semmieboy_yt.disc_jockey.gui.screen.DiscJockeyScreen;

import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal;

public class DiscjockeyCommand {
    public static void register() {
        ClientCommandManager.DISPATCHER.register(
                literal("discjockey")
                        .executes(context -> {
                            FabricClientCommandSource source = context.getSource();
                            if (SongLoader.loadingSongs) {
                                source.sendError(new TranslatableText(Main.MOD_ID+".still_loading"));
                            } else {
                                MinecraftClient client = source.getClient();
                                client.send(() -> client.setScreen(new DiscJockeyScreen()));
                            }
                            return 1;
                        })
                        .then(literal("reload")
                                .executes(context -> {
                                    if (SongLoader.loadingSongs) {
                                        context.getSource().sendError(new TranslatableText(Main.MOD_ID+".still_loading"));
                                    } else {
                                        SongLoader.loadSongs();
                                    }
                                    return 1;
                                })
                        )
        );
    }
}
